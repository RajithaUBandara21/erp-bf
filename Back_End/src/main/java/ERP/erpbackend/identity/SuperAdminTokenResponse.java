package ERP.erpbackend.identity;

import java.util.UUID;

/**
 * A Super Admin's access token. Deliberately has no {@code refreshToken}, {@code tenantId}, or
 * {@code organizationId} - there is no refresh token and no Membership (see current-feature.md's
 * Key decisions).
 */
public record SuperAdminTokenResponse(
		String accessToken,
		long expiresIn,
		UUID userId,
		String email,
		String fullName,
		boolean mustChangePassword) {
}
