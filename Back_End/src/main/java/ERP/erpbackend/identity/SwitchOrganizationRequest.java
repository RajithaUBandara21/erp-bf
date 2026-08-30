package ERP.erpbackend.identity;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Body of {@code POST /api/auth/switch-organization}: the Organization to re-point the caller's Session at. */
public record SwitchOrganizationRequest(

		@NotNull
		UUID organizationId

) {
}
