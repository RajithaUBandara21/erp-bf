package ERP.erpbackend.identity;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

/**
 * Calls Google's real token and userinfo endpoints. Kept behind {@link GoogleTokenExchangeClient} so
 * controller tests mock it instead of reaching the network. The authorization request/response pair
 * below is a synthetic replay of the handshake already completed by the browser redirect - only the
 * code and redirect URI need to match what Google issued the code for.
 */
@Component
@RequiredArgsConstructor
public class GoogleTokenExchangeClientImpl implements GoogleTokenExchangeClient {

	private final GoogleOAuthProperties googleOAuthProperties;

	private final OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> tokenResponseClient =
			new RestClientAuthorizationCodeTokenResponseClient();

	private final OAuth2UserService<OAuth2UserRequest, OAuth2User> userService = new DefaultOAuth2UserService();

	@Override
	public GoogleIdentity exchange(String code) {
		ClientRegistration registration = googleOAuthProperties.toClientRegistration();

		OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
				.authorizationUri(registration.getProviderDetails().getAuthorizationUri())
				.clientId(registration.getClientId())
				.redirectUri(registration.getRedirectUri())
				.scopes(registration.getScopes())
				.state("replay")
				.build();
		OAuth2AuthorizationResponse authorizationResponse = OAuth2AuthorizationResponse.success(code)
				.redirectUri(registration.getRedirectUri())
				.state("replay")
				.build();
		OAuth2AuthorizationCodeGrantRequest grantRequest = new OAuth2AuthorizationCodeGrantRequest(
				registration, new OAuth2AuthorizationExchange(authorizationRequest, authorizationResponse));

		OAuth2AccessTokenResponse tokenResponse = tokenResponseClient.getTokenResponse(grantRequest);
		OAuth2User oAuth2User = userService.loadUser(new OAuth2UserRequest(registration, tokenResponse.getAccessToken()));

		String providerUserId = oAuth2User.getName();
		String email = oAuth2User.getAttribute("email");
		Boolean emailVerified = oAuth2User.getAttribute("email_verified");
		return new GoogleIdentity(providerUserId, email, Boolean.TRUE.equals(emailVerified));
	}

}
