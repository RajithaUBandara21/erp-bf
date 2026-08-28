package ERP.erpbackend.identity;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface RoleRepository extends JpaRepository<Role, UUID> {

	List<Role> findByTenantId(UUID tenantId);

	Optional<Role> findByIdAndTenantId(UUID id, UUID tenantId);

	Optional<Role> findByTenantIdAndName(UUID tenantId, String name);

	/** Pessimistic write lock on the role row, to serialize concurrent last-Owner removals. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT r FROM Role r WHERE r.id = :id")
	Optional<Role> findByIdForUpdate(UUID id);

}
