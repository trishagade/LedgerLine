package ledgerline.audit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    /**
     * Joins the caller's transaction: for a successful payment the audit row and the money movement
     * are committed together or not at all.
     */
    @Transactional
    public void record(AuditAction action, AuditOutcome outcome, Long actorId, String actorName,
                       String targetType, String targetId, String details) {
        repository.save(build(action, outcome, actorId, actorName, targetType, targetId, details));
    }

    /**
     * Always commits in its own transaction, so denials/failures are kept even though
     * the business transaction was rolled back. Call it only when no other DB transaction is open.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordIndependent(AuditAction action, AuditOutcome outcome, Long actorId, String actorName,
                                  String targetType, String targetId, String details) {
        repository.save(build(action, outcome, actorId, actorName, targetType, targetId, details));
    }

    private AuditLog build(AuditAction action, AuditOutcome outcome, Long actorId, String actorName,
                           String targetType, String targetId, String details) {
        return new AuditLog(action, outcome, actorId, cut(actorName, 60), cut(targetType, 40),
                cut(targetId, 80), cut(details, 500), currentIp());
    }

    private static String cut(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String currentIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return cut(attrs.getRequest().getRemoteAddr(), 64);
        }
        return null;
    }
}
