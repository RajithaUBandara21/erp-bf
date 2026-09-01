package ERP.erpbackend.organization;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

	List<Organization> findByTenantIdInAndActiveTrue(Collection<UUID> tenantIds);

	Optional<Organization> findByInviteCode(String inviteCode);

	List<Organization> findByTenantIdOrderByCreatedAtAsc(UUID tenantId);

	boolean existsByTenantIdAndCode(UUID tenantId, String code);

	boolean existsByInviteCode(String inviteCode);

	long countByTenantId(UUID tenantId);
}
