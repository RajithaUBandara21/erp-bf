package ERP.erpbackend.organization;

import java.util.Locale;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {

	private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
	private static final Pattern EDGE_HYPHENS = Pattern.compile("^-+|-+$");

	private final TenantRepository tenantRepository;
	private final OrganizationRepository organizationRepository;

	@Override
	public TenantOrganization createTenantAndOrganization(String organizationName) {
		String code = uniqueCodeFor(organizationName);

		Tenant tenant = new Tenant();
		tenant.setName(organizationName);
		tenant.setCode(code);
		tenant = tenantRepository.save(tenant);

		Organization organization = new Organization();
		organization.setTenantId(tenant.getId());
		organization.setName(organizationName);
		organization.setCode(code);
		organization = organizationRepository.save(organization);

		return new TenantOrganization(tenant.getId(), organization.getId());
	}

	private String uniqueCodeFor(String organizationName) {
		String base = slugify(organizationName);
		String candidate = base;
		int suffix = 2;
		while (tenantRepository.existsByCode(candidate)) {
			candidate = base + "-" + suffix;
			suffix++;
		}
		return candidate;
	}

	private static String slugify(String value) {
		String slug = NON_ALPHANUMERIC.matcher(value.toLowerCase(Locale.ROOT)).replaceAll("-");
		slug = EDGE_HYPHENS.matcher(slug).replaceAll("");
		return slug.isEmpty() ? "org" : slug;
	}

}
