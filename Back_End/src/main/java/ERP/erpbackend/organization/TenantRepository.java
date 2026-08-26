package ERP.erpbackend.organization;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {

	boolean existsByCode(String code);

	Optional<Tenant> findByCode(String code);

}
