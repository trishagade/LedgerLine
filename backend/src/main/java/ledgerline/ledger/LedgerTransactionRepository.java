package ledgerline.ledger;

import org.springframework.data.repository.Repository;

import java.util.Optional;

/** Add and read only: ledger rows are never changed or deleted. */
public interface LedgerTransactionRepository extends Repository<LedgerTransaction, Long> {
    LedgerTransaction save(LedgerTransaction transaction);

    Optional<LedgerTransaction> findById(Long id);
}
