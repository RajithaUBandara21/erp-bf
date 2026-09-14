package ERP.erpbackend.identity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth/super-admin")
@RequiredArgsConstructor
public class SuperAdminAuthController {

	private final SuperAdminAuthenticationService superAdminAuthenticationService;
	private final SuperAdminLoginRateLimiter superAdminLoginRateLimiter;
	private final SuperAdminOtpVerifyRateLimiter superAdminOtpVerifyRateLimiter;

	@PostMapping("/login")
	public ResponseEntity<SuperAdminLoginChallengeResponse> login(
			@Valid @RequestBody SuperAdminLoginRequest request, HttpServletRequest servletRequest) {
		if (!superAdminLoginRateLimiter.allow(servletRequest.getRemoteAddr())) {
			throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
					"Too many login attempts. Please try again later.");
		}
		return ResponseEntity.ok(superAdminAuthenticationService.login(request));
	}

	@PostMapping("/login/verify")
	public ResponseEntity<SuperAdminTokenResponse> verify(
			@Valid @RequestBody SuperAdminLoginVerifyRequest request, HttpServletRequest servletRequest) {
		if (!superAdminOtpVerifyRateLimiter.allow(servletRequest.getRemoteAddr())) {
			throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
					"Too many attempts. Please try again later.");
		}
		return ResponseEntity.ok(superAdminAuthenticationService.verify(request));
	}

	@PostMapping("/change-password")
	@PreAuthorize("@perms.isSuperAdminPrincipal()")
	public ResponseEntity<SuperAdminTokenResponse> changePassword(
			@AuthenticationPrincipal AuthenticatedUser caller,
			@Valid @RequestBody SuperAdminChangePasswordRequest request) {
		return ResponseEntity.ok(superAdminAuthenticationService.changePassword(caller, request));
	}

}
