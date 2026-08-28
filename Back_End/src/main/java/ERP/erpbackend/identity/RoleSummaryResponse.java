package ERP.erpbackend.identity;

import java.util.UUID;

public record RoleSummaryResponse(UUID id, String name, String description, boolean systemManaged,
		long memberCount, long permissionCount) {
}
