package ERP.erpbackend.organization;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationService {

	TenantOrganization createTenantAndOrganization(String organizationName);

	Optional<UUID> findTenantIdByCode(String code);

	/** Names for the given organization ids, keyed by id. A missing/deleted id is simply absent from the result. */
	Map<UUID, String> findNamesByIds(Collection<UUID> organizationIds);

}
