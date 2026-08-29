package ERP.erpbackend.identity;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
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

	private static final String SCOPE = "openid email profile";

	private final GoogleOAuthProperties googleOAuthProperties;
	private final OAuthStateService oAuthStateService;
	private final GoogleTokenExchangeClient googleTokenExchangeClient;
	private final OAuthLoginExchangeService oAuthLoginExchangeService;
	private final UserRepository userRepository;
	private final OAuthAccountRepository oAuthAccountRepository;
	private final SessionTokenIssuer sessionTokenIssuer;

	@Value("${FRONTEND_BASE_URL:http://localhost:3000}")
	private String frontendBaseUrl;

	@PostMapping("/login-url")
	public ResponseEntity<AuthorizationUrlResponse> loginUrl() {
		return ResponseEntity.ok(buildAuthorizationUrl(null));
	}

	@PostMapping("/link-url")
	public ResponseEntity<AuthorizationUrlResponse> linkUrl(@AuthenticationPrincipal AuthenticatedUser caller) {
		return ResponseEntity.ok(buildAuthorizationUrl(caller.userId()));
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
	public ResponseEntity<TokenResponse> exchange(@Valid @RequestBody ExchangeCodeRequest request) {
		return oAuthLoginExchangeService.consume(request.code())
				.map(ResponseEntity::ok)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired code"));
	}

	@GetMapping
	public ResponseEntity<LinkStatusResponse> status(@AuthenticationPrincipal AuthenticatedUser caller) {
		return oAuthAccountRepository
				.findByTenantIdAndUserIdAndProvider(caller.tenantId(), caller.userId(), OAuthProvider.GOOGLE)
				.map(account -> ResponseEntity.ok(new LinkStatusResponse(true, account.getProviderEmail())))
				.orElseGet(() -> ResponseEntity.ok(new LinkStatusResponse(false, null)));
	}

	@DeleteMapping
	public ResponseEntity<Void> unlink(@AuthenticationPrincipal AuthenticatedUser caller) {
		oAuthAccountRepository.deleteByTenantIdAndUserIdAndProvider(
				caller.tenantId(), caller.userId(), OAuthProvider.GOOGLE);
		return ResponseEntity.noContent().build();
	}

	private ResponseEntity<Void> handleLink(UUID userId, GoogleIdentity identity) {
		Optional<User> user = userRepository.findById(userId).filter(User::isActive);
		if (user.isEmpty()) {
			return redirectTo(signInError(null));
		}

		Optional<OAuthAccount> linkedToAnotherUser = oAuthAccountRepository
				.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, identity.providerUserId())
				.filter(account -> !account.getUserId().equals(userId));
		if (linkedToAnotherUser.isPresent()) {
			return redirectTo(signInError("already-linked"));
		}

		OAuthAccount account = oAuthAccountRepository
				.findByTenantIdAndUserIdAndProvider(user.get().getTenantId(), userId, OAuthProvider.GOOGLE)
				.orElseGet(OAuthAccount::new);
		account.setTenantId(user.get().getTenantId());
		account.setUserId(userId);
		account.setProvider(OAuthProvider.GOOGLE);
		account.setProviderUserId(identity.providerUserId());
		account.setProviderEmail(identity.email());
		oAuthAccountRepository.save(account);

		return redirectTo(settingsLinked());
	}

	private ResponseEntity<Void> handleLogin(GoogleIdentity identity) {
		Optional<OAuthAccount> account = oAuthAccountRepository
				.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, identity.providerUserId());
		if (account.isEmpty()) {
			return redirectTo(signInError("not-linked"));
		}

		Optional<User> user = userRepository.findById(account.get().getUserId()).filter(User::isActive);
		if (user.isEmpty()) {
			return redirectTo(signInError(null));
		}

		Session session = sessionTokenIssuer.createSession(user.get(), ClientType.WEB);
		TokenResponse tokenResponse = sessionTokenIssuer.issueTokens(user.get(), session);
		String exchangeCode = oAuthLoginExchangeService.issue(tokenResponse);

		return redirectTo(signInCode(exchangeCode));
	}

	private AuthorizationUrlResponse buildAuthorizationUrl(UUID linkedUserId) {
		if (!googleOAuthProperties.configured()) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Google sign-in is not configured");
		}

		ClientRegistration registration = googleOAuthProperties.toClientRegistration();
		String state = oAuthStateService.issue(linkedUserId);

		String authorizationUrl = UriComponentsBuilder
				.fromUriString(registration.getProviderDetails().getAuthorizationUri())
				.queryParam("client_id", registration.getClientId())
				.queryParam("redirect_uri", registration.getRedirectUri())
				.queryParam("response_type", "code")
				.queryParam("scope", SCOPE)
				.queryParam("state", state)
				.build()
				.toUriString();

		return new AuthorizationUrlResponse(authorizationUrl);
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
