package ledgerline.payment;

import ledgerline.ledger.TransactionType;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** What the caller wants to do. fromAccount / toAccount are account numbers (null when not applicable). */
public record PaymentCommand(TransactionType type, String fromAccount, String toAccount, BigDecimal amount) {

    public PaymentCommand {
        amount = amount.setScale(2, RoundingMode.UNNECESSARY); // validation already guarantees max 2 decimals
    }

    public static PaymentCommand deposit(String account, BigDecimal amount) {
        return new PaymentCommand(TransactionType.DEPOSIT, null, account, amount);
    }

    public static PaymentCommand withdrawal(String account, BigDecimal amount) {
        return new PaymentCommand(TransactionType.WITHDRAWAL, account, null, amount);
    }

    public static PaymentCommand transfer(String from, String to, BigDecimal amount) {
        return new PaymentCommand(TransactionType.TRANSFER, from, to, amount);
    }

    /** The customer account the caller must own (the one that is credited by a deposit, or debited otherwise). */
    public String ownedAccountNumber() {
        return type == TransactionType.DEPOSIT ? toAccount : fromAccount;
    }

    /** Text that identifies the request; hashed and compared when an Idempotency-Key is reused. */
    public String signature() {
        return type + "|" + fromAccount + "|" + toAccount + "|" + amount.toPlainString();
    }
}
