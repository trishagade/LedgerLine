package ledgerline.payment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ledgerline.common.ApiException;
import ledgerline.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.regex.Pattern;

@RestController
public class PaymentController {

    private static final Pattern KEY_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{8,64}$");

    public record MoneyRequest(
            @NotNull @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
            @DecimalMax(value = "1000000.00", message = "Amount must not exceed 1,000,000.00")
            @Digits(integer = 7, fraction = 2, message = "Amount may have at most 2 decimal places") BigDecimal amount) {
    }

    public record TransferRequest(
            @NotBlank @Size(max = 40) String fromAccountNumber,
            @NotBlank @Size(max = 40) String toAccountNumber,
            @NotNull @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
            @DecimalMax(value = "1000000.00", message = "Amount must not exceed 1,000,000.00")
            @Digits(integer = 7, fraction = 2, message = "Amount may have at most 2 decimal places") BigDecimal amount) {
    }

    private final PaymentCoordinator coordinator;

    public PaymentController(PaymentCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    @PostMapping("/api/accounts/{accountNumber}/deposit")
    public ResponseEntity<TransactionResult> deposit(@AuthenticationPrincipal AuthenticatedUser user,
                                                     @PathVariable String accountNumber,
                                                     @RequestHeader("Idempotency-Key") String key,
                                                     @Valid @RequestBody MoneyRequest body) {
        return respond(coordinator.execute(PaymentCommand.deposit(accountNumber, body.amount()), user, checkKey(key)));
    }

    @PostMapping("/api/accounts/{accountNumber}/withdraw")
    public ResponseEntity<TransactionResult> withdraw(@AuthenticationPrincipal AuthenticatedUser user,
                                                      @PathVariable String accountNumber,
                                                      @RequestHeader("Idempotency-Key") String key,
                                                      @Valid @RequestBody MoneyRequest body) {
        return respond(coordinator.execute(PaymentCommand.withdrawal(accountNumber, body.amount()), user, checkKey(key)));
    }

    @PostMapping("/api/transfers")
    public ResponseEntity<TransactionResult> transfer(@AuthenticationPrincipal AuthenticatedUser user,
                                                      @RequestHeader("Idempotency-Key") String key,
                                                      @Valid @RequestBody TransferRequest body) {
        PaymentCommand cmd = PaymentCommand.transfer(body.fromAccountNumber(), body.toAccountNumber(), body.amount());
        return respond(coordinator.execute(cmd, user, checkKey(key)));
    }

    private static String checkKey(String key) {
        if (key == null || !KEY_PATTERN.matcher(key).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_IDEMPOTENCY_KEY",
                    "Idempotency-Key must be 8-64 characters: letters, digits, dash, underscore");
        }
        return key;
    }

    private static ResponseEntity<TransactionResult> respond(TransactionResult result) {
        return ResponseEntity.status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
                .header("Idempotent-Replay", String.valueOf(result.replayed()))
                .body(result);
    }
}
