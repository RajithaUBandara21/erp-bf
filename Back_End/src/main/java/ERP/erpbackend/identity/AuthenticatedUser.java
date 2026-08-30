package ERP.erpbackend.identity;

import java.util.UUID;

/**
 * The authenticated caller for one request. {@code tenantId}/{@code organizationId} are denormalized
 * from the currently selected {@link Membership} ({@code membershipId}), which is what actually carries
 * Organization scope and Role assignment from feature 5 on.
 */
public record AuthenticatedUser(UUID userId, UUID tenantId, UUID organizationId, String email, UUID sessionId,
		UUID membershipId) {
}
