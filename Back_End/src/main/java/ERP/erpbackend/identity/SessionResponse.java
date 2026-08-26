package ERP.erpbackend.identity;

import java.time.Instant;
import java.util.UUID;

public record SessionResponse(UUID id, ClientType clientType, Instant createdAt, Instant lastUsedAt, boolean current) {
}
