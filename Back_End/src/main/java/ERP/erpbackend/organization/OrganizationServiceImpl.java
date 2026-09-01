package ERP.erpbackend.organization;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {

	private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
	private static final Pattern EDGE_HYPHENS = Pattern.compile("^-+|-+$");

	/** Highest numeric suffix tried before falling back to a random suffix. */
	private static final int MAX_NUMERIC_SUFFIX = 6;

	/** Invite-code redraws before giving up: ~50 bits of entropy each, so a real collision needs the DB UNIQUE backstop. */
	private static final int MAX_INVITE_CODE_ATTEMPTS = 5;

	private final TenantRepository tenantRepository;
	private final OrganizationRepository organizationRepository;

	@Override
	public TenantOrganization createTenantAndOrganization(String organizationName) {
		String code = uniqueCode(organizationName, tenantRepository::existsByCode);

		Tenant tenant = new Tenant();
		tenant.setName(organizationName);
		tenant.setCode(code);
		tenant = tenantRepository.save(tenant);

		Organization organization = new Organization();
		organization.setTenantId(tenant.getId());
		organization.setName(organizationName);
		organization.setCode(code);
		organization.setInviteCode(uniqueInviteCode());
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
	public OrganizationListView findAllByTenantId(UUID tenantId) {
		Tenant tenant = tenantRepository.findById(tenantId)
				.orElseThrow(() -> new IllegalStateException("No tenant with id " + tenantId));
		List<OrganizationDetail> organizations = organizationRepository.findByTenantIdOrderByCreatedAtAsc(tenantId).stream()
				.map(OrganizationServiceImpl::toDetail)
				.toList();
		return new OrganizationListView(tenant.getPlan(), tenant.getMaxOrganizations(), organizations);
	}

	@Override
	@Transactional
	public OrganizationDetail createOrganization(UUID tenantId, String name) {
		Tenant tenant = tenantRepository.findByIdForUpdate(tenantId)
				.orElseThrow(() -> new IllegalStateException("No tenant with id " + tenantId));
		if (organizationRepository.countByTenantId(tenantId) >= tenant.getMaxOrganizations()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Organization limit reached for this plan. Ask your administrator to raise the limit.");
		}
		String code = uniqueCode(name, candidate -> organizationRepository.existsByTenantIdAndCode(tenantId, candidate));

		Organization organization = new Organization();
		organization.setTenantId(tenantId);
		organization.setName(name);
		organization.setCode(code);
		organization.setInviteCode(uniqueInviteCode());
		return toDetail(organizationRepository.save(organization));
	}

	@Override
	public String findInviteCode(UUID organizationId) {
		return organizationRepository.findById(organizationId)
				.map(Organization::getInviteCode)
				.orElseThrow(() -> new IllegalStateException("No organization with id " + organizationId));
	}

	@Override
	public Optional<OrganizationInviteTarget> findByInviteCode(String inviteCode) {
		return organizationRepository.findByInviteCode(inviteCode)
				.filter(Organization::isActive)
				.map(organization -> new OrganizationInviteTarget(
						organization.getId(), organization.getTenantId(), organization.getName()));
	}

	@Override
	@Transactional
	public String rotateInviteCode(UUID organizationId) {
		Organization organization = organizationRepository.findById(organizationId)
				.orElseThrow(() -> new IllegalStateException("No organization with id " + organizationId));
		organization.setInviteCode(uniqueInviteCode());
		return organizationRepository.save(organization).getInviteCode();
	}

	@Override
	public Map<UUID, String> findTenantNamesByIds(Collection<UUID> tenantIds) {
		if (tenantIds.isEmpty()) {
			return Map.of();
		}
		return tenantRepository.findAllById(tenantIds).stream()
				.collect(Collectors.toMap(Tenant::getId, Tenant::getName));
	}

	/**
	 * A slug of {@code name} that {@code taken} reports free: the bare slug, then {@code -2}..{@code -6},
	 * then a random suffix. A residual collision still surfaces as a 409 via {@code GlobalExceptionHandler},
	 * never a raw 500.
	 */
	private static String uniqueCode(String name, Predicate<String> taken) {
		String base = slugify(name);
		String candidate = base;
		for (int suffix = 2; suffix <= MAX_NUMERIC_SUFFIX; suffix++) {
			if (!taken.test(candidate)) {
				return candidate;
			}
			candidate = base + "-" + suffix;
		}
		return taken.test(candidate) ? base + "-" + UUID.randomUUID().toString().substring(0, 8) : candidate;
	}

	/**
	 * A fresh invite code that no organization holds yet. {@link Organization#newInviteCode()} owns the
	 * format; the retry only removes the (negligible) chance of a random clash before the DB UNIQUE
	 * constraint would. After {@link #MAX_INVITE_CODE_ATTEMPTS} redraws a residual collision surfaces as
	 * a 409 via {@code GlobalExceptionHandler}.
	 */
	private String uniqueInviteCode() {
		String candidate = Organization.newInviteCode();
		for (int attempt = 0; attempt < MAX_INVITE_CODE_ATTEMPTS && organizationRepository.existsByInviteCode(candidate);
				attempt++) {
			candidate = Organization.newInviteCode();
		}
		return candidate;
	}

	private static OrganizationDetail toDetail(Organization organization) {
		return new OrganizationDetail(organization.getId(), organization.getName(), organization.getCode(),
				organization.isActive(), organization.getCreatedAt());
	}

	private static String slugify(String value) {
		String slug = NON_ALPHANUMERIC.matcher(value.toLowerCase(Locale.ROOT)).replaceAll("-");
		slug = EDGE_HYPHENS.matcher(slug).replaceAll("");
		return slug.isEmpty() ? "org" : slug;
	}

}
