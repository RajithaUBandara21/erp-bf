package ERP.erpbackend.organization;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {

	boolean existsByCode(String code);

	/** Pessimistic write lock on the tenant row, to serialize concurrent organization-limit checks. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT t FROM Tenant t WHERE t.id = :id")
	Optional<Tenant> findByIdForUpdate(UUID id);

}
