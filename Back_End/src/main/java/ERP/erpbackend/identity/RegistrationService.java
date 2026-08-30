package ERP.erpbackend.identity;

import ERP.erpbackend.organization.OrganizationService;
import ERP.erpbackend.organization.TenantOrganization;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegistrationService {

	private final OrganizationService organizationService;
	private final UserRepository userRepository;
	private final MembershipRepository membershipRepository;
	private final PasswordEncoder passwordEncoder;
	private final SessionTokenIssuer sessionTokenIssuer;
	private final SystemRoleProvisioner systemRoleProvisioner;

	@Transactional
	public TokenResponse register(RegisterRequest request) {
		TenantOrganization tenantOrganization =
				organizationService.createTenantAndOrganization(request.organizationName());

		User user = new User();
		user.setEmail(request.email().toLowerCase(Locale.ROOT));
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setFullName(request.fullName());
		user = userRepository.save(user);

		Membership membership = new Membership();
		membership.setUserId(user.getId());
		membership.setTenantId(tenantOrganization.tenantId());
		membership.setOrganizationId(tenantOrganization.organizationId());
		membership.setStatus(MembershipStatus.ACTIVE);
		membership = membershipRepository.save(membership);

		systemRoleProvisioner.provisionForNewTenant(tenantOrganization.tenantId(), membership.getId());

		Session session = sessionTokenIssuer.createSession(user, membership, request.clientType());
		return sessionTokenIssuer.issueTokens(user, membership, session);
	}

}
