package ERP.erpbackend.identity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

		@NotBlank
		@Size(max = 255)
		String organizationName,

		@NotBlank
		@Size(max = 255)
		String fullName,

		@NotBlank
		@Email
		@Size(max = 255)
		String email,

		@NotBlank
		@MaxUtf8Bytes(72)
		@Pattern(
				regexp = "^(?=.*[0-9])(?=.*[A-Z]).{8,}$",
				message = "Password must be at least 8 characters, with one number and one uppercase letter"
		)
		String password

) {
}
