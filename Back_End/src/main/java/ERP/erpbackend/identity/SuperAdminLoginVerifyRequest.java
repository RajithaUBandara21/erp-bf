package ERP.erpbackend.identity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SuperAdminLoginVerifyRequest(

		@NotBlank
		String challengeToken,

		@NotBlank
		@Pattern(regexp = "^[0-9]{6}$", message = "OTP must be 6 digits")
		String otp

) {
}
