package ledgerline.account;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ledgerline.ledger.LedgerEntry;
import ledgerline.ledger.LedgerEntryRepository;
import ledgerline.security.AuthenticatedUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    private final LedgerEntryRepository ledgerEntries;

    public AccountController(AccountService accountService, LedgerEntryRepository ledgerEntries) {
        this.accountService = accountService;
        this.ledgerEntries = ledgerEntries;
    }

    /** Both fields are optional: a missing account number is generated, a missing name defaults to your username. */
    public record CreateAccountRequest(
            @Pattern(regexp = "^[A-Z0-9-]{6,20}$", message = "6-20 characters: capital letters, digits, dash") String accountNumber,
            @Size(max = 100) String ownerName
    ) {}

    public record AccountResponse(String accountNumber, String ownerName, BigDecimal balance, Long ownerUserId, boolean system) {
        public static AccountResponse of(Account a) {
            return new AccountResponse(a.getAccountNumber(), a.getOwnerName(), a.getBalance(),
                    a.getOwner() == null ? null : a.getOwner().getId(), a.isSystem());
        }
    }

    public record LedgerLineView(Long transactionId, String type, String direction, BigDecimal amount,
                                 BigDecimal balanceAfter, String createdAt) {
        static LedgerLineView of(LedgerEntry e) {
            return new LedgerLineView(e.getTransaction().getId(), e.getTransaction().getType().name(),
                    e.getDirection().name(), e.getAmount(), e.getBalanceAfter(), e.getCreatedAt().toString());
        }
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@AuthenticationPrincipal AuthenticatedUser user,
                                                         @Valid @RequestBody CreateAccountRequest request) {
        Account created = accountService.createAccount(user, request.accountNumber(), request.ownerName());
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse.of(created));
    }

    @GetMapping
    public List<AccountResponse> myAccounts(@AuthenticationPrincipal AuthenticatedUser user) {
        return accountService.getAccountsOf(user).stream().map(AccountResponse::of).toList();
    }

    @GetMapping("/{accountNumber}")
    public AccountResponse getAccount(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String accountNumber) {
        return AccountResponse.of(accountService.getReadableAccount(user, accountNumber));
    }

    @GetMapping("/{accountNumber}/ledger")
    public List<LedgerLineView> ledger(@AuthenticationPrincipal AuthenticatedUser user,
                                       @PathVariable String accountNumber,
                                       @RequestParam(defaultValue = "50") int limit) {
        Account account = accountService.getReadableAccount(user, accountNumber);
        int size = Math.max(1, Math.min(limit, 100));
        return ledgerEntries.findByAccountIdOrderByIdDesc(account.getId(), PageRequest.of(0, size))
                .stream().map(LedgerLineView::of).toList();
    }
}
