package ERP.erpbackend.identity;

import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Fails startup outside local dev when the JWT signing secret is missing, still the committed dev
 * default, or too short to be safe (F-10). The dev default stays usable only under the {@code dev}
 * or {@code test} profile, so local runs and the test suite need no {@code JWT_SECRET}.
 */
@Component
@RequiredArgsConstructor
class JwtSecretGuard implements InitializingBean {

	static final String DEV_DEFAULT_SECRET =
			"dev-only-insecure-jwt-signing-secret-change-me-in-production-0123456789";
	private static final int MIN_SECRET_BYTES = 32;

	private final JwtProperties jwtProperties;
	private final Environment environment;

	@Override
	public void afterPropertiesSet() {
		if (runsUnderLocalProfile()) {
			return;
		}

		String secret = jwtProperties.secret();
		if (secret == null || secret.isBlank()) {
			throw new IllegalStateException(
					"app.jwt.secret is not set - provide a real JWT_SECRET outside the dev/test profile");
		}
		if (secret.equals(DEV_DEFAULT_SECRET)) {
			throw new IllegalStateException(
					"app.jwt.secret is still the committed dev default - set a real JWT_SECRET outside the dev/test profile");
		}
		if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
			throw new IllegalStateException("app.jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes");
		}
	}

	private boolean runsUnderLocalProfile() {
		for (String profile : environment.getActiveProfiles()) {
			if (profile.equals("dev") || profile.equals("test")) {
				return true;
			}
		}
		return false;
	}
}
