package ERP.erpbackend.identity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Second half of a multi-Organization login: the token from {@code /api/auth/login} plus the chosen Membership. */
public record LoginSelectRequest(

		@NotBlank
		@Size(max = 255)
		String selectionToken,

		@NotNull
		UUID membershipId,

		@NotNull
		ClientType clientType

) {
}
