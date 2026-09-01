package ERP.erpbackend.audit;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {

	/**
	 * @param organizationId when non-null, restricts results to that Organization; pass null only for a
	 *     caller entitled to the whole Tenant's trail (a Tenant Admin today, {@code PLATFORM_SUPER_ADMIN} later)
	 */
	default Page<AuditLog> search(UUID tenantId, UUID organizationId, AuditLogFilter filter, Pageable pageable) {
		return findAll(AuditLogSpecifications.matching(tenantId, organizationId, filter.entityType(),
				filter.action(), filter.actorId(), filter.from(), filter.to()), pageable);
	}

}
