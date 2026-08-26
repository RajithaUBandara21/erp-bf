package ERP.erpbackend.identity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final RegistrationService registrationService;
	private final RegistrationRateLimiter registrationRateLimiter;

	@PostMapping("/register")
	public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request,
			HttpServletRequest servletRequest) {
		if (!registrationRateLimiter.allow(servletRequest.getRemoteAddr())) {
			throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
					"Too many registration attempts. Please try again later.");
		}
		RegisteredAccount account = registrationService.register(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(RegisterResponse.from(account));
	}

}
