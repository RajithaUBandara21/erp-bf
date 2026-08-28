package ERP.erpbackend.identity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, UUID> {

	List<Role> findByTenantId(UUID tenantId);

	Optional<Role> findByTenantIdAndName(UUID tenantId, String name);

	boolean existsByTenantIdAndName(UUID tenantId, String name);

}
