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

/**
 * Unauthenticated employee self-join. Kept separate from {@link AuthController} so that file's
 * credential flow stays untouched; both endpoints are permit-all in {@link SecurityConfig} and behind
 * {@link SelfJoinRateLimiter}. Request/response mapping plus the rate-limit guard only - all logic is
 * in {@link SelfJoinService}.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class SelfJoinController {

	private final SelfJoinService selfJoinService;
	private final SelfJoinRateLimiter selfJoinRateLimiter;

	@PostMapping("/join")
	public ResponseEntity<SelfJoinResponse> join(@Valid @RequestBody JoinRequest request,
			HttpServletRequest servletRequest) {
		if (!selfJoinRateLimiter.allow(servletRequest.getRemoteAddr())) {
			throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
					"Too many requests. Please try again later.");
		}
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(selfJoinService.requestJoin(request));
	}

	@PostMapping("/verify-email")
	public ResponseEntity<VerifyEmailResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request,
			HttpServletRequest servletRequest) {
		if (!selfJoinRateLimiter.allow(servletRequest.getRemoteAddr())) {
			throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
					"Too many requests. Please try again later.");
		}
		return ResponseEntity.ok(selfJoinService.verifyEmail(request.token()));
	}

}
