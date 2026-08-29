package ERP.erpbackend.audit;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {

	default Page<AuditLog> search(UUID tenantId, String entityType, String action, UUID userId, Instant from,
			Instant to, Pageable pageable) {
		return findAll(AuditLogSpecifications.matching(tenantId, entityType, action, userId, from, to), pageable);
	}

}
