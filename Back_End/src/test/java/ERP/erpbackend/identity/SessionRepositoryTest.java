package ERP.erpbackend.identity;

import static org.assertj.core.api.Assertions.assertThat;

import ERP.erpbackend.TestcontainersConfiguration;
import ERP.erpbackend.organization.Organization;
import ERP.erpbackend.organization.OrganizationRepository;
import ERP.erpbackend.organization.Tenant;
import ERP.erpbackend.organization.TenantRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = Replace.NONE)
class SessionRepositoryTest {

	@Autowired
	private TenantRepository tenantRepository;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private SessionRepository sessionRepository;

	private Tenant newTenant(String code) {
		Tenant tenant = new Tenant();
		tenant.setName(code);
		tenant.setCode(code);
		return tenantRepository.saveAndFlush(tenant);
	}

	private Organization newOrganization(Tenant tenant, String code) {
		Organization organization = new Organization();
		organization.setTenantId(tenant.getId());
		organization.setName(code);
		organization.setCode(code);
		return organizationRepository.saveAndFlush(organization);
	}

	private User newUser(Tenant tenant, Organization organization, String email) {
		User user = new User();
		user.setTenantId(tenant.getId());
		user.setOrganizationId(organization.getId());
		user.setEmail(email);
		user.setPasswordHash("hashed-password");
		user.setFullName("Session Owner");
		return userRepository.saveAndFlush(user);
	}

	@Test
	void savesAndFindsSessionWithAllFieldsAndAuditTimestampsIntact() {
		Tenant tenant = newTenant("TEN-SES-1");
		Organization organization = newOrganization(tenant, "ORG-SES-1");
		User user = newUser(tenant, organization, "owner@acme.test");

		Instant lastUsedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
		Instant expiresAt = lastUsedAt.plusSeconds(3600);

		Session session = new Session();
		session.setTenantId(tenant.getId());
		session.setUserId(user.getId());
		session.setClientType(ClientType.WEB);
		session.setLastUsedAt(lastUsedAt);
		session.setExpiresAt(expiresAt);

		Session saved = sessionRepository.saveAndFlush(session);

		Session found = sessionRepository.findById(saved.getId()).orElseThrow();
		assertThat(found.getTenantId()).isEqualTo(tenant.getId());
		assertThat(found.getUserId()).isEqualTo(user.getId());
		assertThat(found.getClientType()).isEqualTo(ClientType.WEB);
		assertThat(found.getLastUsedAt()).isEqualTo(lastUsedAt);
		assertThat(found.getExpiresAt()).isEqualTo(expiresAt);
		assertThat(found.getRevokedAt()).isNull();
		assertThat(found.getCreatedAt()).isNotNull();
		assertThat(found.getUpdatedAt()).isNotNull();
	}

	@Test
	void savesAndFindsRevokedSession() {
		Tenant tenant = newTenant("TEN-SES-2");
		Organization organization = newOrganization(tenant, "ORG-SES-2");
		User user = newUser(tenant, organization, "revoked@acme.test");

		Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);

		Session session = new Session();
		session.setTenantId(tenant.getId());
		session.setUserId(user.getId());
		session.setClientType(ClientType.MOBILE);
		session.setLastUsedAt(now);
		session.setExpiresAt(now.plusSeconds(3600));
		session.setRevokedAt(now);

		Session saved = sessionRepository.saveAndFlush(session);

		Session found = sessionRepository.findById(saved.getId()).orElseThrow();
		assertThat(found.getClientType()).isEqualTo(ClientType.MOBILE);
		assertThat(found.getRevokedAt()).isEqualTo(now);
	}

}
