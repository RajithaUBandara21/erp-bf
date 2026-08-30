package ERP.erpbackend.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ERP.erpbackend.TestcontainersConfiguration;
import ERP.erpbackend.organization.Organization;
import ERP.erpbackend.organization.OrganizationRepository;
import ERP.erpbackend.organization.Tenant;
import ERP.erpbackend.organization.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = Replace.NONE)
class MembershipRepositoryTest {

	@Autowired
	private TenantRepository tenantRepository;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private MembershipRepository membershipRepository;

	private Tenant tenant(String code) {
		Tenant tenant = new Tenant();
		tenant.setName(code);
		tenant.setCode(code);
		return tenantRepository.saveAndFlush(tenant);
	}

	private Organization organization(Tenant tenant, String code) {
		Organization organization = new Organization();
		organization.setTenantId(tenant.getId());
		organization.setName(code);
		organization.setCode(code);
		return organizationRepository.saveAndFlush(organization);
	}

	private User user(String email) {
		User user = new User();
		user.setEmail(email);
		user.setPasswordHash("hashed-password");
		user.setFullName("Membership Holder");
		return userRepository.saveAndFlush(user);
	}

	private Membership membership(Tenant tenant, Organization organization, User user) {
		Membership membership = new Membership();
		membership.setUserId(user.getId());
		membership.setTenantId(tenant.getId());
		membership.setOrganizationId(organization.getId());
		membership.setStatus(MembershipStatus.ACTIVE);
		return membership;
	}

	@Test
	void savesAndFindsMembershipWithAuditFields() {
		Tenant tenant = tenant("TEN-MEM-1");
		Organization organization = organization(tenant, "ORG-MEM-1");
		User user = user("holder@acme.test");

		Membership saved = membershipRepository.saveAndFlush(membership(tenant, organization, user));

		Membership found = membershipRepository.findById(saved.getId()).orElseThrow();
		assertThat(found.getUserId()).isEqualTo(user.getId());
		assertThat(found.getTenantId()).isEqualTo(tenant.getId());
		assertThat(found.getOrganizationId()).isEqualTo(organization.getId());
		assertThat(found.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
		assertThat(found.getLocationType()).isNull();
		assertThat(found.getLocationId()).isNull();
		assertThat(found.getCreatedAt()).isNotNull();
		assertThat(found.getUpdatedAt()).isNotNull();
	}

	@Test
	void rejectsDuplicateMembershipForSameUserAndOrganization() {
		Tenant tenant = tenant("TEN-MEM-2");
		Organization organization = organization(tenant, "ORG-MEM-2");
		User user = user("dupe@acme.test");
		membershipRepository.saveAndFlush(membership(tenant, organization, user));

		Membership duplicate = membership(tenant, organization, user);

		assertThrows(DataIntegrityViolationException.class,
				() -> membershipRepository.saveAndFlush(duplicate));
	}

	@Test
	void findsActiveMembershipByUserAndTenant() {
		Tenant tenant = tenant("TEN-MEM-4");
		Organization organization = organization(tenant, "ORG-MEM-4");
		User user = user("active-lookup@acme.test");
		Membership saved = membershipRepository.saveAndFlush(membership(tenant, organization, user));

		assertThat(membershipRepository.findByUserIdAndTenantIdAndStatus(
				user.getId(), tenant.getId(), MembershipStatus.ACTIVE))
				.map(Membership::getId)
				.contains(saved.getId());
		assertThat(membershipRepository.findByUserIdAndTenantIdAndStatus(
				user.getId(), tenant.getId(), MembershipStatus.PENDING))
				.isEmpty();
	}

	@Test
	void allowsSameUserToHoldMembershipsInDifferentOrganizations() {
		Tenant tenant = tenant("TEN-MEM-3");
		Organization organizationA = organization(tenant, "ORG-MEM-3A");
		Organization organizationB = organization(tenant, "ORG-MEM-3B");
		User user = user("multi-org@acme.test");

		membershipRepository.saveAndFlush(membership(tenant, organizationA, user));
		Membership second = membershipRepository.saveAndFlush(membership(tenant, organizationB, user));

		assertThat(second.getId()).isNotNull();
		assertThat(membershipRepository.findByUserId(user.getId())).hasSize(2);
	}

}
