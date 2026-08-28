package ERP.erpbackend.identity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateRoleRequest(

		@NotBlank
		@Size(max = 255)
		String name,

		@Size(max = 255)
		String description,

		@NotNull
		List<@NotBlank String> permissionCodes

) {
}
