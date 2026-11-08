package ledgerline.account;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByOwnerIdOrderByIdAsc(Long ownerId);

    /**
     * SELECT ... FOR UPDATE. Any other transaction touching this row waits until we commit,
     * which is what stops two payments from spending the same money.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.accountNumber = :number")
    Optional<Account> findByAccountNumberForUpdate(@Param("number") String number);

    /** Returns only an id (no entity is loaded, so no stale copy ends up in the persistence context). */
    @Query("select a.id from Account a where a.accountNumber = :number and a.owner.id = :ownerId "
            + "and (a.systemAccount is null or a.systemAccount = false)")
    Optional<Long> findOwnedCustomerAccountId(@Param("number") String number, @Param("ownerId") Long ownerId);

    @Query("select a.id from Account a where a.accountNumber = :number "
            + "and (a.systemAccount is null or a.systemAccount = false)")
    Optional<Long> findCustomerAccountId(@Param("number") String number);
}
