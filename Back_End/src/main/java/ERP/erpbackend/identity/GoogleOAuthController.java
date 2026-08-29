package ERP.erpbackend.identity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/auth/oauth/google")
@RequiredArgsConstructor
public class GoogleOAuthController {

	private final OAuthStateService oAuthStateService;
	private final GoogleTokenExchangeClient googleTokenExchangeClient;
	private final OAuthLoginExchangeService oAuthLoginExchangeService;
	private final OAuthAccountService oAuthAccountService;
	private final GoogleOAuthRateLimiter googleOAuthRateLimiter;

	@Value("${FRONTEND_BASE_URL:http://localhost:3000}")
	private String frontendBaseUrl;

	@PostMapping("/login-url")
	public ResponseEntity<AuthorizationUrlResponse> loginUrl(HttpServletRequest servletRequest) {
		if (!googleOAuthRateLimiter.allow(servletRequest.getRemoteAddr())) {
			throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
					"Too many requests. Please try again later.");
		}
		return ResponseEntity.ok(oAuthAccountService.buildAuthorizationUrl(null));
	}

	@PostMapping("/link-url")
	public ResponseEntity<AuthorizationUrlResponse> linkUrl(@AuthenticationPrincipal AuthenticatedUser caller) {
		return ResponseEntity.ok(oAuthAccountService.buildAuthorizationUrl(caller.userId()));
	}

	@GetMapping("/callback")
	public ResponseEntity<Void> callback(
			@RequestParam(required = false) String code,
			@RequestParam(required = false) String state,
			@RequestParam(required = false) String error) {

		if (error != null || code == null || code.isBlank() || state == null || state.isBlank()) {
			return redirectTo(signInError(null));
		}

		Optional<String> linkedUserId = oAuthStateService.consume(state);
		if (linkedUserId.isEmpty()) {
			return redirectTo(signInError(null));
		}

		GoogleIdentity identity = googleTokenExchangeClient.exchange(code);
		if (!identity.emailVerified()) {
			return redirectTo(signInError("email-unverified"));
		}

		return linkedUserId.get().isBlank()
				? handleLogin(identity)
				: handleLink(UUID.fromString(linkedUserId.get()), identity);
	}

	@PostMapping("/exchange")
	public ResponseEntity<TokenResponse> exchange(@Valid @RequestBody ExchangeCodeRequest request,
			HttpServletRequest servletRequest) {
		if (!googleOAuthRateLimiter.allow(servletRequest.getRemoteAddr())) {
			throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
					"Too many requests. Please try again later.");
		}
		return oAuthLoginExchangeService.consume(request.code())
				.map(ResponseEntity::ok)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired code"));
	}

	@GetMapping
	public ResponseEntity<LinkStatusResponse> status(@AuthenticationPrincipal AuthenticatedUser caller) {
		return ResponseEntity.ok(oAuthAccountService.status(caller));
	}

	@DeleteMapping
	public ResponseEntity<Void> unlink(@AuthenticationPrincipal AuthenticatedUser caller) {
		oAuthAccountService.unlink(caller);
		return ResponseEntity.noContent().build();
	}

	private ResponseEntity<Void> handleLink(UUID userId, GoogleIdentity identity) {
		OAuthLinkResult result = oAuthAccountService.link(userId, identity);
		return result.linked() ? redirectTo(settingsLinked()) : redirectTo(signInError(result.errorReason()));
	}

	private ResponseEntity<Void> handleLogin(GoogleIdentity identity) {
		OAuthLoginResult result = oAuthAccountService.login(identity);
		return result.succeeded()
				? redirectTo(signInCode(result.exchangeCode()))
				: redirectTo(signInError(result.errorReason()));
	}

	private static ResponseEntity<Void> redirectTo(URI uri) {
		return ResponseEntity.status(HttpStatus.FOUND).location(uri).build();
	}

	private URI signInError(String reason) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(frontendBaseUrl + "/sign-in")
				.queryParam("oauth", "error");
		if (reason != null) {
			builder.queryParam("reason", reason);
		}
		return builder.build().toUri();
	}

	private URI signInCode(String exchangeCode) {
		return UriComponentsBuilder.fromUriString(frontendBaseUrl + "/sign-in")
				.queryParam("oauth", "code")
				.queryParam("code", exchangeCode)
				.build().toUri();
	}

	private URI settingsLinked() {
		return UriComponentsBuilder.fromUriString(frontendBaseUrl + "/settings")
				.queryParam("oauth", "linked")
				.build().toUri();
	}

}
