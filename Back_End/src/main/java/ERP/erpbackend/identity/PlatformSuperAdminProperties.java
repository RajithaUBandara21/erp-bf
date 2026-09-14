package ERP.erpbackend.identity;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Startup-bootstrap config for the platform Super Admin, read from plain env vars.
 * {@link #configured()} gates {@link PlatformSuperAdminBootstrap} so the app boots cleanly
 * with no Super Admin configured, mirroring {@link GoogleOAuthProperties}.
 */
@Getter
@Component
public class PlatformSuperAdminProperties {

	private final String email;
	private final String password;

	public PlatformSuperAdminProperties(
			@Value("${PLATFORM_SUPER_ADMIN_EMAIL:}") String email,
			@Value("${PLATFORM_SUPER_ADMIN_PASSWORD:}") String password) {
		this.email = email;
		this.password = password;
	}

	public boolean configured() {
		return !email.isBlank() && !password.isBlank();
	}

}
