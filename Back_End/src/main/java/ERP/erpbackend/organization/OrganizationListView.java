package ERP.erpbackend.organization;

import java.util.List;

/**
 * Every Organization under one Tenant plus the Tenant's plan and organization limit, so a caller
 * can render "N of {@code maxOrganizations}" and gate the create control. Distinct from
 * {@link TenantOrganization}, which is a single tenant id + organization id pair.
 */
public record OrganizationListView(String plan, int maxOrganizations, List<OrganizationDetail> organizations) {
}
