package ERP.erpbackend.organization;

import java.util.UUID;

public record TenantOrganization(UUID tenantId, UUID organizationId) {
}
