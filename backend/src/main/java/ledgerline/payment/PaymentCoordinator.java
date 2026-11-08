package ledgerline.payment;

import ledgerline.audit.AuditAction;
import ledgerline.audit.AuditOutcome;
import ledgerline.audit.AuditService;
import ledgerline.common.ApiException;
import ledgerline.common.Hashing;
import ledgerline.security.AuthenticatedUser;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Deliberately NOT transactional. It wraps PaymentService.run (which is) so that:
 * - a duplicate idempotency key (race between two identical requests) is turned into "return the first result",
 * - rejected/denied payments are written to the audit log AFTER the payment transaction rolled back.
 */
@Service
public class PaymentCoordinator {

    private static final int MAX_ATTEMPTS = 3;

    private final PaymentService paymentService;
    private final AuditService audit;

    public PaymentCoordinator(PaymentService paymentService, AuditService audit) {
        this.paymentService = paymentService;
        this.audit = audit;
    }

    public TransactionResult execute(PaymentCommand cmd, AuthenticatedUser actor, String key) {
        String hash = Hashing.sha256Hex(cmd.signature());
        try {
            for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
                Optional<TransactionResult> existing = paymentService.replay(actor, key, hash);
                if (existing.isPresent()) {
                    return existing.get();
                }
                try {
                    return paymentService.run(cmd, actor, key, hash);
                } catch (DataIntegrityViolationException duplicateKeyRace) {
                    // Another request with the same key won the race; loop and return its stored result.
                }
            }
            throw new ApiException(HttpStatus.CONFLICT, "REQUEST_IN_PROGRESS", "A request with this key is still being processed, retry shortly");
        } catch (ApiException e) {
            audit.recordIndependent(AuditAction.PAYMENT_REJECTED,
                    e.getStatus() == HttpStatus.NOT_FOUND ? AuditOutcome.DENIED : AuditOutcome.FAILED,
                    actor.id(), actor.username(), "ACCOUNT", cmd.ownedAccountNumber(),
                    e.getCode() + " (" + cmd.type() + " " + cmd.amount().toPlainString() + ")");
            throw e;
        }
    }
}
