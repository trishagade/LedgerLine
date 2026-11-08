package ledgerline.account;

import ledgerline.audit.AuditAction;
import ledgerline.audit.AuditOutcome;
import ledgerline.audit.AuditService;
import ledgerline.common.ApiException;
import ledgerline.security.AuthenticatedUser;
import ledgerline.user.User;
import ledgerline.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;

@Service
public class AccountService {

    public static final String SYSTEM_ACCOUNT_NUMBER = "SYSTEM-CASH";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AuditService audit;

    public AccountService(AccountRepository accountRepository, UserRepository userRepository, AuditService audit) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.audit = audit;
    }

    /** Creates an account owned by the logged-in user. If no number is given, a random one is generated. */
    @Transactional
    public Account createAccount(AuthenticatedUser actor, String accountNumber, String ownerName) {
        User owner = userRepository.findById(actor.id())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Authentication required"));

        String number = (accountNumber == null || accountNumber.isBlank()) ? generateNumber() : accountNumber.trim();
        if (number.startsWith("SYSTEM")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "RESERVED_NUMBER", "Account numbers starting with SYSTEM are reserved");
        }
        if (accountRepository.findByAccountNumber(number).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "ACCOUNT_NUMBER_EXISTS", "Account number already exists");
        }

        Account account = new Account();
        account.setAccountNumber(number);
        account.setOwnerName((ownerName == null || ownerName.isBlank()) ? actor.username() : ownerName.trim());
        account.setBalance(BigDecimal.ZERO);
        account.setOwner(owner);
        Account saved = accountRepository.save(account);

        audit.record(AuditAction.ACCOUNT_CREATED, AuditOutcome.SUCCESS, actor.id(), actor.username(),
                "ACCOUNT", number, null);
        return saved;
    }

    /**
     * Server-side ownership check. Customers only see their own accounts; admins can read any.
     * Someone else's account gives "not found" (not "forbidden") so account numbers cannot be probed.
     * Not @Transactional on purpose: the denial audit row uses its own transaction.
     */
    public Account getReadableAccount(AuthenticatedUser actor, String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber).orElse(null);
        boolean allowed = account != null
                && (actor.isAdmin() || (!account.isSystem() && account.isOwnedBy(actor.id())));
        if (!allowed) {
            audit.recordIndependent(AuditAction.ACCESS_DENIED, AuditOutcome.DENIED, actor.id(), actor.username(),
                    "ACCOUNT", accountNumber, "Read of missing or foreign account");
            throw new ApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Account not found");
        }
        return account;
    }

    public List<Account> getAccountsOf(AuthenticatedUser actor) {
        return accountRepository.findByOwnerIdOrderByIdAsc(actor.id());
    }

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    private String generateNumber() {
        for (int i = 0; i < 10; i++) {
            StringBuilder sb = new StringBuilder("LL");
            for (int d = 0; d < 10; d++) sb.append(RANDOM.nextInt(10));
            if (accountRepository.findByAccountNumber(sb.toString()).isEmpty()) return sb.toString();
        }
        throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "NUMBER_GENERATION_FAILED", "Please try again");
    }
}
