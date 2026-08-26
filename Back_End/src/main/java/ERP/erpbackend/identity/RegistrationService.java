package ERP.erpbackend.identity;

import ERP.erpbackend.organization.OrganizationService;
import ERP.erpbackend.organization.TenantOrganization;
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

	@Transactional
	public RegisteredAccount register(RegisterRequest request) {
		TenantOrganization tenantOrganization =
				organizationService.createTenantAndOrganization(request.organizationName());

		User user = new User();
		user.setTenantId(tenantOrganization.tenantId());
		user.setOrganizationId(tenantOrganization.organizationId());
		user.setEmail(request.email());
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setFullName(request.fullName());
		user = userRepository.save(user);

		return new RegisteredAccount(user.getId(), tenantOrganization.tenantId(),
				tenantOrganization.organizationId(), user.getEmail());
	}

}
