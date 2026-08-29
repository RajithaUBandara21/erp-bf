package ERP.erpbackend.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * Public entry point every module calls to record an audit trail row - never write
 * {@link AuditLog} via {@link AuditLogRepository} directly from outside this package.
 *
 * <p>A serialization or persistence failure here propagates rather than being swallowed:
 * a caller inside a {@code @Transactional} method gets that failure rolled back together
 * with the change it was meant to record, instead of silently losing the audit trail.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

	private final AuditLogRepository auditLogRepository;
	private final ObjectMapper objectMapper;

	public void log(AuditEvent event) {
		AuditLog entry = new AuditLog();
		entry.setTenantId(event.tenantId());
		entry.setOrganizationId(event.organizationId());
		entry.setUserId(event.userId());
		entry.setEntityType(event.entityType());
		entry.setEntityId(event.entityId());
		entry.setAction(event.action());
		entry.setBeforeValue(toJson(event.beforeValue()));
		entry.setAfterValue(toJson(event.afterValue()));
		auditLogRepository.save(entry);
	}

	private String toJson(Object value) {
		return value == null ? null : objectMapper.writeValueAsString(value);
	}

}
