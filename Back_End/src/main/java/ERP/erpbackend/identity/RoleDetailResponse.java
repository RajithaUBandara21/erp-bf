package ERP.erpbackend.identity;

import java.util.List;
import java.util.UUID;

public record RoleDetailResponse(UUID id, String name, String description, boolean systemManaged,
		long memberCount, long permissionCount, List<String> permissionCodes, List<RoleMemberResponse> members) {
}
