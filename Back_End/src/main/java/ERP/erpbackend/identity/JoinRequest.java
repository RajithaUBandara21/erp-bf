package ERP.erpbackend.identity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Body of {@code POST /api/auth/join}. {@code inviteCode} is normalized server-side before use. */
public record JoinRequest(

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
		String password,

		@NotBlank
		@Size(max = 255)
		String fullName,

		@NotBlank
		@Size(max = 64)
		String inviteCode

) {
}
