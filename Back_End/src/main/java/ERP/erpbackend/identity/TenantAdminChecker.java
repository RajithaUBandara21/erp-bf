package ERP.erpbackend.identity;

import java.util.UUID;

/** Cross-module check for whether a user is a Tenant Admin of a given tenant, for modules outside {@code identity}. */
public interface TenantAdminChecker {

	/** True when the user holds an ACTIVE Membership in the tenant assigned that tenant's system Tenant Admin role. */
	boolean isTenantAdmin(UUID userId, UUID tenantId);

}
