package ledgerline.ledger;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

import java.util.List;

/** Add and read only: ledger rows are never changed or deleted. */
public interface LedgerEntryRepository extends Repository<LedgerEntry, Long> {

    List<LedgerEntry> saveAll(Iterable<LedgerEntry> entries);

    List<LedgerEntry> findByTransactionIdOrderByIdAsc(Long transactionId);

    List<LedgerEntry> findByAccountIdOrderByIdDesc(Long accountId, Pageable pageable);
}
