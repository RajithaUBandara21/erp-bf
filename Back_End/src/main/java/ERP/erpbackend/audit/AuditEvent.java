package ERP.erpbackend.audit;

import java.util.UUID;

/**
 * One audit event to record. {@code beforeValue}/{@code afterValue} are plain objects
 * ({@link AuditService} serializes them to JSON) - pass {@code null} when a snapshot isn't
 * meaningful for the action (e.g. a login has nothing to diff).
 */
public record AuditEvent(
		UUID tenantId,
		UUID organizationId,
		UUID userId,
		String entityType,
		UUID entityId,
		String action,
		Object beforeValue,
		Object afterValue) {
}
