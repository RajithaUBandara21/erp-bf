package ERP.erpbackend.identity;

import java.util.UUID;

public record AuthenticatedUser(UUID userId, UUID tenantId, UUID organizationId, String email) {
}
