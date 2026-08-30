package ERP.erpbackend.organization;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {

	private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
	private static final Pattern EDGE_HYPHENS = Pattern.compile("^-+|-+$");

	/** Highest numeric suffix tried before falling back to a random suffix. */
	private static final int MAX_NUMERIC_SUFFIX = 6;

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

	@Override
	public Map<UUID, String> findNamesByIds(Collection<UUID> organizationIds) {
		if (organizationIds.isEmpty()) {
			return Map.of();
		}
		return organizationRepository.findAllById(organizationIds).stream()
				.collect(Collectors.toMap(Organization::getId, Organization::getName));
	}

	@Override
	public Optional<UUID> findTenantId(UUID organizationId) {
		return organizationRepository.findById(organizationId).map(Organization::getTenantId);
	}

	@Override
	public List<OrganizationSummary> findActiveByTenantIds(Collection<UUID> tenantIds) {
		if (tenantIds.isEmpty()) {
			return List.of();
		}
		return organizationRepository.findByTenantIdInAndActiveTrue(tenantIds).stream()
				.map(organization -> new OrganizationSummary(
						organization.getId(), organization.getTenantId(), organization.getName()))
				.toList();
	}

	@Override
	public Map<UUID, String> findTenantNamesByIds(Collection<UUID> tenantIds) {
		if (tenantIds.isEmpty()) {
			return Map.of();
		}
		return tenantRepository.findAllById(tenantIds).stream()
				.collect(Collectors.toMap(Tenant::getId, Tenant::getName));
	}

	private String uniqueCodeFor(String organizationName) {
		String base = slugify(organizationName);
		String candidate = base;
		for (int suffix = 2; suffix <= MAX_NUMERIC_SUFFIX; suffix++) {
			if (!tenantRepository.existsByCode(candidate)) {
				return candidate;
			}
			candidate = base + "-" + suffix;
		}
		if (!tenantRepository.existsByCode(candidate)) {
			return candidate;
		}
		// Every numeric suffix up to MAX_NUMERIC_SUFFIX collided (or another
		// request is racing this one) - fall back to a random suffix rather
		// than probing forever. A leftover collision here still surfaces as a
		// 409 via GlobalExceptionHandler, never a raw 500.
		return base + "-" + UUID.randomUUID().toString().substring(0, 8);
	}

	private static String slugify(String value) {
		String slug = NON_ALPHANUMERIC.matcher(value.toLowerCase(Locale.ROOT)).replaceAll("-");
		slug = EDGE_HYPHENS.matcher(slug).replaceAll("");
		return slug.isEmpty() ? "org" : slug;
	}

}
