package ledgerline.payment;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Remembers "user X already used key K for request hash H, and it produced transaction T".
 * The UNIQUE(user_id, idem_key) constraint is what makes duplicates impossible even under a race.
 */
@Entity
@Table(name = "idempotency_records",
        uniqueConstraints = @UniqueConstraint(name = "uq_idem_user_key", columnNames = {"user_id", "idem_key"}))
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "idem_key", nullable = false, length = 64)
    private String idemKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected IdempotencyRecord() {
    }

    public IdempotencyRecord(Long userId, String idemKey, String requestHash) {
        this.userId = userId;
        this.idemKey = idemKey;
        this.requestHash = requestHash;
    }

    public String getRequestHash() { return requestHash; }
    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }
}
