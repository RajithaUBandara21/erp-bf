package ERP.erpbackend.identity;

import java.util.UUID;

public record TokenResponse(
		String accessToken,
		String refreshToken,
		long expiresIn,
		long refreshExpiresIn,
		UUID userId,
		UUID tenantId,
		UUID organizationId,
		String email,
		String fullName) {
}
