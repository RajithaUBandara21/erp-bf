package ERP.erpbackend.audit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * One audit log row for the viewer UI. {@code actorName}/{@code actorEmail}/{@code organizationName}
 * are resolved from the identity/organization modules and may be {@code null} if the referenced user
 * or organization no longer exists.
 */
public record AuditLogResponse(
		UUID id,
		Instant createdAt,
		UUID actorId,
		String actorName,
		String actorEmail,
		String entityType,
		UUID entityId,
		String action,
		UUID organizationId,
		String organizationName,
		Map<String, Object> beforeValue,
		Map<String, Object> afterValue) {
}
