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
	private final AuthenticationService authenticationService;
	private final LoginRateLimiter loginRateLimiter;

	@PostMapping("/register")
	public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest request,
			HttpServletRequest servletRequest) {
		if (!registrationRateLimiter.allow(servletRequest.getRemoteAddr())) {
			throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
					"Too many registration attempts. Please try again later.");
		}
		return ResponseEntity.status(HttpStatus.CREATED).body(registrationService.register(request));
	}

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
			HttpServletRequest servletRequest) {
		if (!loginRateLimiter.allow(servletRequest.getRemoteAddr())) {
			throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
					"Too many login attempts. Please try again later.");
		}
		return ResponseEntity.ok(authenticationService.login(request));
	}

	@PostMapping("/login/select")
	public ResponseEntity<TokenResponse> loginSelect(@Valid @RequestBody LoginSelectRequest request,
			HttpServletRequest servletRequest) {
		if (!loginRateLimiter.allow(servletRequest.getRemoteAddr())) {
			throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
					"Too many login attempts. Please try again later.");
		}
		return ResponseEntity.ok(authenticationService.selectOrganization(request));
	}

	@PostMapping("/refresh")
	public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
		return ResponseEntity.ok(authenticationService.refresh(request));
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
		authenticationService.logout(request);
		return ResponseEntity.noContent().build();
	}

}
