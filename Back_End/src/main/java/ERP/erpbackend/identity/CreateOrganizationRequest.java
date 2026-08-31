package ERP.erpbackend.identity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of {@code POST /api/organizations}: the name for a new Organization under the caller's Tenant. */
public record CreateOrganizationRequest(

		@NotBlank
		@Size(max = 255)
		String name

) {
}
