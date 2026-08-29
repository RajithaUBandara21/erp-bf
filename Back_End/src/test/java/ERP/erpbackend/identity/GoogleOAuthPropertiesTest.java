package ERP.erpbackend.identity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GoogleOAuthPropertiesTest {

	private static final String REDIRECT_URI = "http://localhost:8080/api/auth/oauth/google/callback";

	@Test
	void notConfiguredWithBlankClientSecret() {
		GoogleOAuthProperties properties = new GoogleOAuthProperties("client-id", "", REDIRECT_URI);

		assertThat(properties.configured()).isFalse();
	}

	@Test
	void notConfiguredWithBlankClientId() {
		GoogleOAuthProperties properties = new GoogleOAuthProperties("", "client-secret", REDIRECT_URI);

		assertThat(properties.configured()).isFalse();
	}

	@Test
	void configuredWhenBothClientIdAndClientSecretAreSet() {
		GoogleOAuthProperties properties = new GoogleOAuthProperties("client-id", "client-secret", REDIRECT_URI);

		assertThat(properties.configured()).isTrue();
	}

}
