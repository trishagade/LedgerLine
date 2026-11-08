package ledgerline.ledger;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.Instant;

/** One business event (deposit / withdrawal / transfer). Its entries always balance. Never updated. */
@Entity
@Immutable
@Table(name = "ledger_transactions")
public class LedgerTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tx_type", nullable = false, updatable = false, length = 20)
    private TransactionType type;

    @Column(name = "amount", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "initiated_by", nullable = false, updatable = false)
    private Long initiatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LedgerTransaction() {
    }

    public LedgerTransaction(TransactionType type, BigDecimal amount, Long initiatedBy, Instant createdAt) {
        this.type = type;
        this.amount = amount;
        this.initiatedBy = initiatedBy;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public TransactionType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public Long getInitiatedBy() { return initiatedBy; }
    public Instant getCreatedAt() { return createdAt; }
}
