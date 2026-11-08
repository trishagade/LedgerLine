package ledgerline.payment;

import ledgerline.account.Account;
import ledgerline.account.AccountRepository;
import ledgerline.account.AccountService;
import ledgerline.audit.AuditAction;
import ledgerline.audit.AuditOutcome;
import ledgerline.audit.AuditService;
import ledgerline.common.ApiException;
import ledgerline.ledger.*;
import ledgerline.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Everything in run() happens in ONE database transaction:
 * idempotency record + balance changes + ledger entries + audit row. If anything fails, nothing is saved.
 */
@Service
public class PaymentService {

    private final AccountRepository accounts;
    private final LedgerTransactionRepository transactions;
    private final LedgerEntryRepository entries;
    private final IdempotencyRepository idempotency;
    private final AuditService audit;

    public PaymentService(AccountRepository accounts, LedgerTransactionRepository transactions,
                          LedgerEntryRepository entries, IdempotencyRepository idempotency, AuditService audit) {
        this.accounts = accounts;
        this.transactions = transactions;
        this.entries = entries;
        this.idempotency = idempotency;
        this.audit = audit;
    }

    @Transactional
    public TransactionResult run(PaymentCommand cmd, AuthenticatedUser actor, String key, String requestHash) {
        // 1. Claim the idempotency key FIRST. A concurrent request with the same key waits here,
        //    then fails on the unique constraint (the coordinator turns that into a replay).
        IdempotencyRecord idem = new IdempotencyRecord(actor.id(), key, requestHash);
        idempotency.saveAndFlush(idem);

        // 2. Authorisation (server-side): the caller must own the account being debited/credited-to.
        //    Only ids are read here, no entity, so we do not cache a stale balance before locking.
        if (accounts.findOwnedCustomerAccountId(cmd.ownedAccountNumber(), actor.id()).isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Account not found");
        }
        if (cmd.type() == TransactionType.TRANSFER) {
            if (cmd.fromAccount().equals(cmd.toAccount())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "SAME_ACCOUNT", "Cannot transfer to the same account");
            }
            if (accounts.findCustomerAccountId(cmd.toAccount()).isEmpty()) {
                throw new ApiException(HttpStatus.NOT_FOUND, "DESTINATION_NOT_FOUND", "Destination account not found");
            }
        }

        // 3. Decide which account is debited and which is credited. The bank's SYSTEM-CASH account is the other side
        //    of deposits and withdrawals, so every transaction has exactly one debit and one credit of equal amount.
        String debitNumber;
        String creditNumber;
        switch (cmd.type()) {
            case DEPOSIT -> {
                debitNumber = AccountService.SYSTEM_ACCOUNT_NUMBER;
                creditNumber = cmd.toAccount();
            }
            case WITHDRAWAL -> {
                debitNumber = cmd.fromAccount();
                creditNumber = AccountService.SYSTEM_ACCOUNT_NUMBER;
            }
            default -> {
                debitNumber = cmd.fromAccount();
                creditNumber = cmd.toAccount();
            }
        }

        // 4. Lock both rows (SELECT ... FOR UPDATE) in alphabetical order, so two opposite transfers cannot deadlock.
        Account debit = null;
        Account credit = null;
        for (String number : new TreeSet<>(List.of(debitNumber, creditNumber))) {
            Account locked = accounts.findByAccountNumberForUpdate(number)
                    .orElseThrow(() -> new IllegalStateException("Account vanished: " + number));
            if (number.equals(debitNumber)) debit = locked;
            if (number.equals(creditNumber)) credit = locked;
        }

        // 5. Business rule: customers cannot go below zero (checked AFTER locking, on the fresh balance).
        BigDecimal amount = cmd.amount();
        if (!debit.isSystem() && debit.getBalance().compareTo(amount) < 0) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INSUFFICIENT_FUNDS", "Insufficient funds");
        }

        // 6. Move the money and write the double-entry ledger lines.
        debit.setBalance(debit.getBalance().subtract(amount));
        credit.setBalance(credit.getBalance().add(amount));

        Instant now = Instant.now();
        LedgerTransaction tx = transactions.save(new LedgerTransaction(cmd.type(), amount, actor.id(), now));
        LedgerEntry debitEntry = new LedgerEntry(tx, debit, Direction.DEBIT, amount, debit.getBalance(), now);
        LedgerEntry creditEntry = new LedgerEntry(tx, credit, Direction.CREDIT, amount, credit.getBalance(), now);
        if (debitEntry.getAmount().compareTo(creditEntry.getAmount()) != 0) { // can never happen; guards future edits
            throw new IllegalStateException("Unbalanced ledger entries");
        }
        List<LedgerEntry> saved = entries.saveAll(List.of(debitEntry, creditEntry));

        idem.setTransactionId(tx.getId());
        audit.record(auditAction(cmd.type()), AuditOutcome.SUCCESS, actor.id(), actor.username(), "TRANSACTION",
                String.valueOf(tx.getId()), cmd.type() + " " + amount.toPlainString()
                        + (cmd.fromAccount() != null ? " from " + cmd.fromAccount() : "")
                        + (cmd.toAccount() != null ? " to " + cmd.toAccount() : ""));
        return TransactionResult.of(tx, saved, actor.id(), false);
    }

    /** Returns the stored result if this user already used this key. Throws if the key was used for a different request. */
    @Transactional(readOnly = true)
    public Optional<TransactionResult> replay(AuthenticatedUser actor, String key, String requestHash) {
        return idempotency.findByUserIdAndIdemKey(actor.id(), key)
                .filter(r -> r.getTransactionId() != null)
                .map(r -> {
                    if (!r.getRequestHash().equals(requestHash)) {
                        throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "IDEMPOTENCY_KEY_REUSED",
                                "This Idempotency-Key was already used for a different request");
                    }
                    LedgerTransaction tx = transactions.findById(r.getTransactionId()).orElseThrow();
                    List<LedgerEntry> lines = entries.findByTransactionIdOrderByIdAsc(tx.getId());
                    return TransactionResult.of(tx, lines, actor.id(), true);
                });
    }

    private static AuditAction auditAction(TransactionType type) {
        return switch (type) {
            case DEPOSIT -> AuditAction.DEPOSIT;
            case WITHDRAWAL -> AuditAction.WITHDRAWAL;
            case TRANSFER -> AuditAction.TRANSFER;
        };
    }
}
