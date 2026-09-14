package ERP.erpbackend.identity;

import java.util.UUID;

/**
 * The authenticated caller for one request. {@code tenantId}/{@code organizationId} are denormalized
 * from the currently selected {@link Membership} ({@code membershipId}), which is what actually carries
 * Organization scope and Role assignment from feature 5 on.
 *
 * <p>A Super Admin caller (see {@link #superAdmin}) has no Membership at all: {@code tenantId},
 * {@code organizationId}, {@code sessionId}, and {@code membershipId} are all {@code null} for it.
 */
public record AuthenticatedUser(UUID userId, UUID tenantId, UUID organizationId, String email, UUID sessionId,
		UUID membershipId, boolean platformSuperAdmin, boolean mustChangePassword) {

	/** Compatibility constructor for every existing Membership-scoped caller - never a Super Admin. */
	public AuthenticatedUser(UUID userId, UUID tenantId, UUID organizationId, String email, UUID sessionId,
			UUID membershipId) {
		this(userId, tenantId, organizationId, email, sessionId, membershipId, false, false);
	}

	/** A Super Admin principal: no tenant, organization, session, or Membership. */
	public static AuthenticatedUser superAdmin(UUID userId, String email, boolean mustChangePassword) {
		return new AuthenticatedUser(userId, null, null, email, null, null, true, mustChangePassword);
	}

}
