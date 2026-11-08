package ledgerline.audit;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.time.Instant;

/**
 * One row = one security-relevant event. Append-only:
 * - @Immutable tells Hibernate to never UPDATE this entity,
 * - there are no setters,
 * - AuditLogRepository only exposes save and read methods (no delete / update).
 * db/append_only_triggers.sql adds a database-level guarantee on PostgreSQL.
 */
@Entity
@Immutable
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "actor_user_id", updatable = false)
    private Long actorUserId;

    @Column(name = "actor_username", updatable = false, length = 60)
    private String actorUsername;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, updatable = false, length = 40)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, updatable = false, length = 20)
    private AuditOutcome outcome;

    @Column(name = "target_type", updatable = false, length = 40)
    private String targetType;

    @Column(name = "target_id", updatable = false, length = 80)
    private String targetId;

    @Column(name = "details", updatable = false, length = 500)
    private String details;

    @Column(name = "ip_address", updatable = false, length = 64)
    private String ipAddress;

    protected AuditLog() {
    }

    public AuditLog(AuditAction action, AuditOutcome outcome, Long actorUserId, String actorUsername,
                    String targetType, String targetId, String details, String ipAddress) {
        this.createdAt = Instant.now();
        this.action = action;
        this.outcome = outcome;
        this.actorUserId = actorUserId;
        this.actorUsername = actorUsername;
        this.targetType = targetType;
        this.targetId = targetId;
        this.details = details;
        this.ipAddress = ipAddress;
    }

    public Long getId() { return id; }
    public Instant getCreatedAt() { return createdAt; }
    public Long getActorUserId() { return actorUserId; }
    public String getActorUsername() { return actorUsername; }
    public AuditAction getAction() { return action; }
    public AuditOutcome getOutcome() { return outcome; }
    public String getTargetType() { return targetType; }
    public String getTargetId() { return targetId; }
    public String getDetails() { return details; }
    public String getIpAddress() { return ipAddress; }
}
