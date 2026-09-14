package ERP.erpbackend.identity;

import static org.assertj.core.api.Assertions.assertThat;

import ERP.erpbackend.TestcontainersConfiguration;
import ERP.erpbackend.audit.AuditLog;
import ERP.erpbackend.audit.AuditLogRepository;
import ERP.erpbackend.audit.AuditService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Ordered: {@link PlatformSuperAdminBootstrap} enforces a platform-wide singleton (at most
 * one Super Admin ever), so later cases build on the row the earlier ones create or
 * withhold, the same way a real app's successive restarts would.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PlatformSuperAdminBootstrapTest {

	private static final String SUPER_ADMIN_EMAIL = "root@platform.test";
	private static final String SUPER_ADMIN_PASSWORD = "s3cret-pass";

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private AuditService auditService;

	@Autowired
	private AuditLogRepository auditLogRepository;

	private PlatformSuperAdminBootstrap bootstrapWith(String email, String password) {
		return new PlatformSuperAdminBootstrap(new PlatformSuperAdminProperties(email, password),
				userRepository, passwordEncoder, auditService);
	}

	private List<AuditLog> bootstrapAuditRows(UUID userId) {
		return auditLogRepository.findAll().stream()
				.filter(log -> "user.platform_super_admin_bootstrapped".equals(log.getAction())
						&& userId.equals(log.getUserId()))
				.toList();
	}

	@Test
	@Order(1)
	void doesNotCreateARowWhenTheEnvVarsAreBlank() {
		bootstrapWith("", "").run(new DefaultApplicationArguments());

		assertThat(userRepository.existsByPlatformSuperAdminTrue()).isFalse();
	}

	@Test
	@Order(2)
	void leavesAnExistingOrdinaryAccountUnmodifiedWhenItsEmailIsConfigured() {
		User existing = new User();
		existing.setEmail("collide@acme.test");
		existing.setPasswordHash("hashed-password");
		existing.setFullName("Ada Owner");
		existing = userRepository.save(existing);

		bootstrapWith("collide@acme.test", SUPER_ADMIN_PASSWORD).run(new DefaultApplicationArguments());

		User reloaded = userRepository.findByEmail("collide@acme.test").orElseThrow();
		assertThat(reloaded.getId()).isEqualTo(existing.getId());
		assertThat(reloaded.isPlatformSuperAdmin()).isFalse();
		assertThat(reloaded.getPasswordHash()).isEqualTo("hashed-password");
		assertThat(userRepository.existsByPlatformSuperAdminTrue()).isFalse();
	}

	@Test
	@Order(3)
	void createsAndAuditsTheSuperAdminWhenConfiguredAndNoneExistsYet() {
		bootstrapWith(SUPER_ADMIN_EMAIL, SUPER_ADMIN_PASSWORD).run(new DefaultApplicationArguments());

		User created = userRepository.findByEmail(SUPER_ADMIN_EMAIL).orElseThrow();
		assertThat(created.isPlatformSuperAdmin()).isTrue();
		assertThat(created.isMustChangePassword()).isTrue();
		assertThat(created.getFullName()).isEqualTo("Platform Super Admin");
		assertThat(passwordEncoder.matches(SUPER_ADMIN_PASSWORD, created.getPasswordHash())).isTrue();
		assertThat(bootstrapAuditRows(created.getId())).hasSize(1);
	}

	@Test
	@Order(4)
	void runningAgainAfterOneExistsLeavesExactlyOneSuperAdminRow() {
		bootstrapWith("someone-else@platform.test", "another-pass").run(new DefaultApplicationArguments());

		assertThat(userRepository.findByEmail("someone-else@platform.test")).isEmpty();
		assertThat(userRepository.findAll().stream().filter(User::isPlatformSuperAdmin).count()).isEqualTo(1);
	}

}
