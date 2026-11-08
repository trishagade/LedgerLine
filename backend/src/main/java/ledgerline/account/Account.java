package ledgerline.account;

import jakarta.persistence.*;
import ledgerline.user.User;

import java.math.BigDecimal;

/**
 * Original fields (id, accountNumber, ownerName, balance) are kept.
 * Added: owner (who may use this account) and systemAccount (the bank's own counter-account).
 * Balance convention: CREDIT increases, DEBIT decreases. All balances across the system add up to zero.
 */
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String accountNumber;

    @Column(nullable = false)
    private String ownerName;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    /** Nullable on purpose: accounts created before authentication existed have no owner (admin-only). */
    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(name = "system_account")
    private Boolean systemAccount = Boolean.FALSE;

    public Account() {
    }

    public Long getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public boolean isSystem() {
        return Boolean.TRUE.equals(systemAccount);
    }

    public void setSystemAccount(boolean systemAccount) {
        this.systemAccount = systemAccount;
    }

    public boolean isOwnedBy(Long userId) {
        return owner != null && owner.getId().equals(userId);
    }
}
