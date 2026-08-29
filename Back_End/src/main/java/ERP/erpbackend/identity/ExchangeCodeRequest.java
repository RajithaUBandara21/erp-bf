package ERP.erpbackend.identity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExchangeCodeRequest(

		@NotBlank
		@Size(max = 512)
		String code

) {
}
