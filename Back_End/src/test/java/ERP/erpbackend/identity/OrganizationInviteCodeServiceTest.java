package ERP.erpbackend.identity;

import static org.assertj.core.api.Assertions.assertThat;

import ERP.erpbackend.TestcontainersConfiguration;
import ERP.erpbackend.audit.AuditLog;
import ERP.erpbackend.audit.AuditLogRepository;
import ERP.erpbackend.organization.OrganizationRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class OrganizationInviteCodeServiceTest {

	private static final String PASSWORD = "Sunrise8";
	private static final String INVITE_CODE = "[ABCDEFGHJKMNPQRSTVWXYZ0-9]{10}";

	@Autowired
	private OrganizationInviteCodeService organizationInviteCodeService;

	@Autowired
	private RegistrationService registrationService;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private AuditLogRepository auditLogRepository;

	private record Fixture(UUID tenantId, UUID userId, UUID organizationId) {
	}

	private Fixture register(String email) {
		TokenResponse account = registrationService.register(
				new RegisterRequest("Acme " + email, "Ada Owner", email, PASSWORD, ClientType.WEB));
		return new Fixture(account.tenantId(), account.userId(), account.organizationId());
	}

	private AuthenticatedUser caller(Fixture fixture) {
		return new AuthenticatedUser(fixture.userId(), fixture.tenantId(), fixture.organizationId(),
				"caller@acme.test", UUID.randomUUID(), UUID.randomUUID());
	}

	private String storedCode(UUID organizationId) {
		return organizationRepository.findById(organizationId).orElseThrow().getInviteCode();
	}

	@Test
	void readReturnsTheOrganizationsCurrentInviteCode() {
		Fixture fixture = register("ic-read@acme.test");

		assertThat(organizationInviteCodeService.read(caller(fixture)).inviteCode())
				.isEqualTo(storedCode(fixture.organizationId()));
	}

	@Test
	void rotateReplacesTheCodePersistsItAndWritesExactlyOneAuditRow() {
		Fixture fixture = register("ic-rotate@acme.test");
		String before = storedCode(fixture.organizationId());

		String rotated = organizationInviteCodeService.rotate(caller(fixture)).inviteCode();

		assertThat(rotated).matches(INVITE_CODE).isNotEqualTo(before);
		assertThat(storedCode(fixture.organizationId())).isEqualTo(rotated);

		List<AuditLog> rows = auditLogRepository.findAll().stream()
				.filter(log -> "organization.invite_code_rotated".equals(log.getAction())
						&& fixture.organizationId().equals(log.getEntityId()))
				.toList();
		assertThat(rows).singleElement().satisfies(log -> {
			assertThat(log.getEntityType()).isEqualTo("Organization");
			assertThat(log.getTenantId()).isEqualTo(fixture.tenantId());
			assertThat(log.getOrganizationId()).isEqualTo(fixture.organizationId());
			assertThat(log.getUserId()).isEqualTo(fixture.userId());
			assertThat(log.getBeforeValue()).isNull();
			assertThat(log.getAfterValue()).isNull();
		});
	}
}
