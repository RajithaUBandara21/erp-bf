package ERP.erpbackend.identity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of {@code POST /api/auth/verify-email} - the raw single-use token from the verification link. */
public record VerifyEmailRequest(

		@NotBlank
		@Size(max = 255)
		String token

) {
}
