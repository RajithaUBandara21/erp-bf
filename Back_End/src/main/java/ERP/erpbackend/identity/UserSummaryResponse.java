package ERP.erpbackend.identity;

import java.util.UUID;

/** One entry in the tenant user directory - {@code GET /api/users}. Minimal by design; a later Users module can widen it. */
public record UserSummaryResponse(UUID id, String fullName, String email) {
}
