package ERP.erpbackend.identity;

import ERP.erpbackend.audit.AuditEvent;
import ERP.erpbackend.audit.AuditService;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the platform's one Super Admin {@link User} from
 * {@code PLATFORM_SUPER_ADMIN_EMAIL}/{@code PLATFORM_SUPER_ADMIN_PASSWORD} on startup, when
 * neither is blank and no Super Admin exists yet. Never escalates or overwrites an existing
 * ordinary account that already holds the configured email - that would silently hand Super
 * Admin access to whoever controls that account.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlatformSuperAdminBootstrap implements ApplicationRunner {

	private final PlatformSuperAdminProperties properties;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuditService auditService;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (!properties.configured() || userRepository.existsByPlatformSuperAdminTrue()) {
			return;
		}

		String email = properties.getEmail().toLowerCase(Locale.ROOT);
		if (userRepository.findByEmail(email).isPresent()) {
			log.warn("PLATFORM_SUPER_ADMIN_EMAIL {} already belongs to an existing user; "
					+ "skipping Super Admin bootstrap", email);
			return;
		}

		User user = new User();
		user.setEmail(email);
		user.setPasswordHash(passwordEncoder.encode(properties.getPassword()));
		user.setFullName("Platform Super Admin");
		user.setPlatformSuperAdmin(true);
		user.setMustChangePassword(true);
		user = userRepository.save(user);

		auditService.log(new AuditEvent(null, null, user.getId(), "User", user.getId(),
				"user.platform_super_admin_bootstrapped", null, null));

		log.info("Bootstrapped the platform Super Admin account");
	}

}
