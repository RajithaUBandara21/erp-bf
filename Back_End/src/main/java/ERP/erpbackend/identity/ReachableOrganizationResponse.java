package ERP.erpbackend.identity;

import java.util.UUID;

/**
 * One Organization the authenticated caller can switch into. {@code viaTenantAdmin} means the caller
 * holds no Membership here yet: switching provisions an Owner Membership via the 5b.2 primitive.
 */
public record ReachableOrganizationResponse(
		UUID organizationId,
		String organizationName,
		UUID tenantId,
		String tenantName,
		boolean current,
		boolean viaTenantAdmin) {
}
