package ERP.erpbackend.identity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByTenantIdAndEmail(UUID tenantId, String email);

	Optional<User> findByIdAndTenantId(UUID id, UUID tenantId);

	List<User> findByTenantIdOrderByFullNameAsc(UUID tenantId);

}
