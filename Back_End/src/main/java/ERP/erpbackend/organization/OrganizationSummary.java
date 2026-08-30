package ERP.erpbackend.organization;

import java.util.UUID;

/** A lightweight view of one Organization: its id, owning tenant, and name. */
public record OrganizationSummary(UUID id, UUID tenantId, String name) {
}
