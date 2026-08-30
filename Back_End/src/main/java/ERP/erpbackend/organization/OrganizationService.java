package ERP.erpbackend.organization;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationService {

	TenantOrganization createTenantAndOrganization(String organizationName);

	/** Names for the given organization ids, keyed by id. A missing/deleted id is simply absent from the result. */
	Map<UUID, String> findNamesByIds(Collection<UUID> organizationIds);

	/** The owning tenant of an organization, or empty if no organization has that id. */
	Optional<UUID> findTenantId(UUID organizationId);

	/** Active organizations across the given tenants. Empty input yields an empty list without querying. */
	List<OrganizationSummary> findActiveByTenantIds(Collection<UUID> tenantIds);

	/** Names for the given tenant ids, keyed by id. A missing/deleted id is simply absent from the result. */
	Map<UUID, String> findTenantNamesByIds(Collection<UUID> tenantIds);

}
