package ERP.erpbackend.organization;

import java.util.UUID;

/** An active Organization resolved from its employee self-join invite code - the organization module's published contract for the self-join flow. */
public record OrganizationInviteTarget(UUID organizationId, UUID tenantId, String organizationName) {
}
