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
	private final PasswordEncoder passwordEncoder;
	private final SessionTokenIssuer sessionTokenIssuer;
	private final SystemRoleProvisioner systemRoleProvisioner;

	@Transactional
	public TokenResponse register(RegisterRequest request) {
		TenantOrganization tenantOrganization =
				organizationService.createTenantAndOrganization(request.organizationName());

		User user = new User();
		user.setTenantId(tenantOrganization.tenantId());
		user.setOrganizationId(tenantOrganization.organizationId());
		user.setEmail(request.email().toLowerCase(Locale.ROOT));
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setFullName(request.fullName());
		user = userRepository.save(user);

		systemRoleProvisioner.provisionForNewTenant(tenantOrganization.tenantId(), user.getId());

		Session session = sessionTokenIssuer.createSession(user, request.clientType());
		return sessionTokenIssuer.issueTokens(user, session);
	}

}
