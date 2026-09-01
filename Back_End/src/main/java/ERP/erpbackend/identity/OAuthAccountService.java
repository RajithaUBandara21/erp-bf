package ERP.erpbackend.identity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class OAuthAccountService {

	private static final String SCOPE = "openid email profile";

	private final GoogleOAuthProperties googleOAuthProperties;
	private final OAuthStateService oAuthStateService;
	private final OAuthLoginExchangeService oAuthLoginExchangeService;
	private final UserRepository userRepository;
	private final MembershipRepository membershipRepository;
	private final OAuthAccountRepository oAuthAccountRepository;
	private final SessionTokenIssuer sessionTokenIssuer;
	private final LoginSelectionService loginSelectionService;

	public AuthorizationUrlResponse buildAuthorizationUrl(UUID linkedUserId) {
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

	public OAuthLinkResult link(UUID userId, GoogleIdentity identity) {
		Optional<User> user = userRepository.findById(userId).filter(User::isActive);
		if (user.isEmpty()) {
			return OAuthLinkResult.failure(null);
		}

		Optional<Membership> membership = membershipRepository.findByUserId(userId).stream().findFirst();
		if (membership.isEmpty()) {
			return OAuthLinkResult.failure(null);
		}
		UUID tenantId = membership.get().getTenantId();

		Optional<OAuthAccount> linkedToAnotherUser = oAuthAccountRepository
				.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, identity.providerUserId())
				.filter(account -> !account.getUserId().equals(userId));
		if (linkedToAnotherUser.isPresent()) {
			return OAuthLinkResult.failure("already-linked");
		}

		OAuthAccount account = oAuthAccountRepository
				.findByTenantIdAndUserIdAndProvider(tenantId, userId, OAuthProvider.GOOGLE)
				.orElseGet(OAuthAccount::new);
		account.setTenantId(tenantId);
		account.setUserId(userId);
		account.setProvider(OAuthProvider.GOOGLE);
		account.setProviderUserId(identity.providerUserId());
		account.setProviderEmail(identity.email());
		oAuthAccountRepository.save(account);

		return OAuthLinkResult.success();
	}

	public OAuthLoginResult login(GoogleIdentity identity) {
		Optional<OAuthAccount> account = oAuthAccountRepository
				.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, identity.providerUserId());
		if (account.isEmpty()) {
			return OAuthLoginResult.failure("not-linked");
		}

		Optional<User> user = userRepository.findById(account.get().getUserId()).filter(User::isActive);
		if (user.isEmpty()) {
			return OAuthLoginResult.failure(null);
		}

		// Same 0 / 1 / many handling as AuthenticationService.login: a PENDING-only account cannot sign
		// in (it was never approved for any Organization), a single ACTIVE Membership goes straight in,
		// and several route through the Organization Selector rather than a non-deterministic pick.
		List<Membership> memberships = membershipRepository.findByUserIdAndStatus(
				user.get().getId(), MembershipStatus.ACTIVE);
		if (memberships.isEmpty()) {
			return OAuthLoginResult.failure(null);
		}

		LoginResponse loginResponse = memberships.size() == 1
				? LoginResponse.authenticated(issueForMembership(user.get(), memberships.getFirst()))
				: loginSelectionService.beginSelection(user.get().getId(), memberships);

		return OAuthLoginResult.success(oAuthLoginExchangeService.issue(loginResponse));
	}

	private TokenResponse issueForMembership(User user, Membership membership) {
		Session session = sessionTokenIssuer.createSession(user, membership, ClientType.WEB);
		return sessionTokenIssuer.issueTokens(user, membership, session);
	}

	public LinkStatusResponse status(AuthenticatedUser caller) {
		return oAuthAccountRepository
				.findByTenantIdAndUserIdAndProvider(caller.tenantId(), caller.userId(), OAuthProvider.GOOGLE)
				.map(account -> new LinkStatusResponse(true, account.getProviderEmail()))
				.orElseGet(() -> new LinkStatusResponse(false, null));
	}

	public void unlink(AuthenticatedUser caller) {
		oAuthAccountRepository.deleteByTenantIdAndUserIdAndProvider(
				caller.tenantId(), caller.userId(), OAuthProvider.GOOGLE);
	}

}
