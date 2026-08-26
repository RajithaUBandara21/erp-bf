package ERP.erpbackend.identity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegisterRequest(

		@NotBlank
		String organizationName,

		@NotBlank
		String fullName,

		@NotBlank
		@Email
		String email,

		@NotBlank
		@Pattern(
				regexp = "^(?=.*[0-9])(?=.*[A-Z]).{8,}$",
				message = "Password must be at least 8 characters, with one number and one uppercase letter"
		)
		String password

) {
}
