package ERP.erpbackend.identity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SuperAdminChangePasswordRequest(

		@NotBlank
		@MaxUtf8Bytes(72)
		String currentPassword,

		@NotBlank
		@MaxUtf8Bytes(72)
		@Pattern(
				regexp = "^(?=.*[0-9])(?=.*[A-Z]).{8,}$",
				message = "Password must be at least 8 characters, with one number and one uppercase letter"
		)
		String newPassword

) {
}
