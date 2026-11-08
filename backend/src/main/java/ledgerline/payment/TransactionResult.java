package ledgerline.payment;

import ledgerline.ledger.LedgerEntry;
import ledgerline.ledger.LedgerTransaction;

import java.math.BigDecimal;
import java.util.List;

/** API response for a money movement. */
public record TransactionResult(Long transactionId, String type, BigDecimal amount, String createdAt,
                                boolean replayed, List<EntryView> entries) {

    public record EntryView(String accountNumber, String direction, BigDecimal amount, BigDecimal balanceAfter) {
    }

    /**
     * balanceAfter is shown only for accounts the caller owns, so a transfer never reveals
     * the other person's (or the bank's) balance.
     */
    public static TransactionResult of(LedgerTransaction tx, List<LedgerEntry> entries, Long actorId, boolean replayed) {
        List<EntryView> views = entries.stream()
                .map(e -> new EntryView(
                        e.getAccount().getAccountNumber(),
                        e.getDirection().name(),
                        e.getAmount(),
                        e.getAccount().isOwnedBy(actorId) ? e.getBalanceAfter() : null))
                .toList();
        return new TransactionResult(tx.getId(), tx.getType().name(), tx.getAmount(),
                tx.getCreatedAt().toString(), replayed, views);
    }

    public TransactionResult asReplay() {
        return new TransactionResult(transactionId, type, amount, createdAt, true, entries);
    }
}
