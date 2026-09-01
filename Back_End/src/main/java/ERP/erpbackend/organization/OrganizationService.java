package ERP.erpbackend.organization;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationService {

	TenantOrganization createTenantAndOrganization(String organizationName);

	/**
	 * Create a new Organization under an existing tenant with a per-tenant-unique {@code code}, active.
	 *
	 * @throws org.springframework.web.server.ResponseStatusException 409 if the tenant is already at or
	 *     over its {@code maxOrganizations} limit
	 * @throws IllegalStateException if no tenant has that id
	 */
	OrganizationDetail createOrganization(UUID tenantId, String name);

	/** Names for the given organization ids, keyed by id. A missing/deleted id is simply absent from the result. */
	Map<UUID, String> findNamesByIds(Collection<UUID> organizationIds);

	/** The owning tenant of an organization, or empty if no organization has that id. */
	Optional<UUID> findTenantId(UUID organizationId);

	/** Active organizations across the given tenants. Empty input yields an empty list without querying. */
	List<OrganizationSummary> findActiveByTenantIds(Collection<UUID> tenantIds);

	/**
	 * Every organization under one tenant (active and inactive), oldest first, plus that tenant's plan
	 * and organization limit.
	 *
	 * @throws IllegalStateException if no tenant has that id
	 */
	OrganizationListView findAllByTenantId(UUID tenantId);

	/** Names for the given tenant ids, keyed by id. A missing/deleted id is simply absent from the result. */
	Map<UUID, String> findTenantNamesByIds(Collection<UUID> tenantIds);

	/**
	 * The Organization's current employee-self-join invite code.
	 *
	 * @throws IllegalStateException if no organization has that id
	 */
	String findInviteCode(UUID organizationId);

	/**
	 * Resolve a normalized invite code to its Organization, but only while that Organization is active.
	 * Empty for an unknown code or an inactive Organization - the caller cannot tell the two apart.
	 */
	Optional<OrganizationInviteTarget> findByInviteCode(String inviteCode);

	/**
	 * Replace the Organization's invite code with a fresh unique one and return it. Any code shared
	 * before the call stops working immediately.
	 *
	 * @throws IllegalStateException if no organization has that id
	 */
	String rotateInviteCode(UUID organizationId);

}
