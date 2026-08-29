package ERP.erpbackend.audit;

import java.time.Instant;
import java.util.UUID;

/** Optional audit log search criteria - every field may be {@code null} to mean "don't filter on this". */
public record AuditLogFilter(String entityType, String action, UUID actorId, Instant from, Instant to) {
}
