package ERP.erpbackend.identity;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.stereotype.Component;

/**
 * Google OAuth2 client config, read from plain env vars instead of Spring Boot's
 * {@code spring.security.oauth2.client.*} properties - those would try to build a
 * {@link ClientRegistration} at startup and fail with no Google credentials set (see
 * current-feature.md Notes for the AI). {@link #configured()} gates every endpoint that needs a
 * real client registration, so the app boots cleanly either way.
 */
@Getter
@Component
public class GoogleOAuthProperties {

	// CommonOAuth2Provider.GOOGLE was dropped in this Spring Security version, so Google's OIDC
	// endpoints are hardcoded here instead - these are the same values it used to supply.
	private static final String AUTHORIZATION_URI = "https://accounts.google.com/o/oauth2/v2/auth";
	private static final String TOKEN_URI = "https://www.googleapis.com/oauth2/v4/token";
	private static final String USER_INFO_URI = "https://www.googleapis.com/oauth2/v3/userinfo";
	private static final String JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";
	private static final String ISSUER_URI = "https://accounts.google.com";

	private final String clientId;
	private final String clientSecret;
	private final String redirectUri;

	public GoogleOAuthProperties(
			@Value("${GOOGLE_CLIENT_ID:}") String clientId,
			@Value("${GOOGLE_CLIENT_SECRET:}") String clientSecret,
			@Value("${GOOGLE_OAUTH_REDIRECT_URI:http://localhost:8080/api/auth/oauth/google/callback}") String redirectUri) {
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.redirectUri = redirectUri;
	}

	public boolean configured() {
		return !clientId.isBlank() && !clientSecret.isBlank();
	}

	/** Only call once {@link #configured()} is true. */
	public ClientRegistration toClientRegistration() {
		return ClientRegistration.withRegistrationId("google")
				.clientId(clientId)
				.clientSecret(clientSecret)
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.redirectUri(redirectUri)
				.scope("openid", "profile", "email")
				.authorizationUri(AUTHORIZATION_URI)
				.tokenUri(TOKEN_URI)
				.userInfoUri(USER_INFO_URI)
				.userNameAttributeName("sub")
				.jwkSetUri(JWK_SET_URI)
				.issuerUri(ISSUER_URI)
				.clientName("Google")
				.build();
	}

}
