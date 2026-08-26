package ERP.erpbackend.identity;

import java.util.UUID;

public record RegisteredAccount(UUID userId, UUID tenantId, UUID organizationId, String email) {
}
