package ERP.erpbackend.identity;

import java.util.UUID;

public record RoleMemberResponse(UUID userId, String fullName, String email) {
}
