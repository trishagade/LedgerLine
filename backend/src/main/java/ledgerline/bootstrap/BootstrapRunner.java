package ledgerline.bootstrap;

import ledgerline.account.Account;
import ledgerline.account.AccountRepository;
import ledgerline.account.AccountService;
import ledgerline.user.Role;
import ledgerline.user.User;
import ledgerline.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Runs once at startup:
 * 1. makes sure the bank's SYSTEM-CASH counter-account exists (the other side of every deposit/withdrawal),
 * 2. creates the first admin ONLY if LEDGERLINE_ADMIN_USERNAME and LEDGERLINE_ADMIN_PASSWORD are provided.
 */
@Component
public class BootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapRunner.class);

    private final AccountRepository accounts;
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String adminUsername;
    private final String adminPassword;

    public BootstrapRunner(AccountRepository accounts, UserRepository users, PasswordEncoder encoder,
                           @Value("${ledgerline.admin.username:}") String adminUsername,
                           @Value("${ledgerline.admin.password:}") String adminPassword) {
        this.accounts = accounts;
        this.users = users;
        this.encoder = encoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureSystemAccount();
        ensureAdmin();
    }

    private void ensureSystemAccount() {
        if (accounts.findByAccountNumber(AccountService.SYSTEM_ACCOUNT_NUMBER).isPresent()) return;
        Account system = new Account();
        system.setAccountNumber(AccountService.SYSTEM_ACCOUNT_NUMBER);
        system.setOwnerName("LedgerLine system");
        system.setBalance(BigDecimal.ZERO);
        system.setSystemAccount(true);
        try {
            accounts.save(system);
        } catch (DataIntegrityViolationException e) {
            log.info("System account was created by another instance");
        }
    }

    private void ensureAdmin() {
        if (adminUsername.isBlank() || adminPassword.isBlank()) {
            log.info("No admin configured (set LEDGERLINE_ADMIN_USERNAME and LEDGERLINE_ADMIN_PASSWORD to create one)");
            return;
        }
        if (adminPassword.length() < 10) {
            throw new IllegalStateException("LEDGERLINE_ADMIN_PASSWORD must be at least 10 characters");
        }
        String name = adminUsername.trim().toLowerCase(Locale.ROOT);
        if (users.findByUsername(name).isEmpty()) {
            users.save(new User(name, encoder.encode(adminPassword), Role.ADMIN));
            log.info("Admin user '{}' created", name);
        }
    }
}
