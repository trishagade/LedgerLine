package ledgerline.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

/**
 * Deliberately NOT a JpaRepository: it only offers "add" and "read".
 * There is no delete or update method for audit logs anywhere in the code.
 */
public interface AuditLogRepository extends Repository<AuditLog, Long> {

    AuditLog save(AuditLog log);

    Page<AuditLog> findAllBy(Pageable pageable);
}
