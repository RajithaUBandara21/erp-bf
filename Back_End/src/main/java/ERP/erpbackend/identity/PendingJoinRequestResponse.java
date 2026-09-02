package ERP.erpbackend.identity;

import java.time.Instant;
import java.util.UUID;

/**
 * One verified-but-PENDING self-join request in the Org Admin's approval queue. {@code requestedAt}
 * is the Membership's {@code createdAt} - the moment the PENDING row was written at email verification.
 */
public record PendingJoinRequestResponse(
		UUID membershipId, UUID userId, String fullName, String email, Instant requestedAt) {
}
