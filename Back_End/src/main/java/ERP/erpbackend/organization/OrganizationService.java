package ERP.erpbackend.organization;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationService {

	TenantOrganization createTenantAndOrganization(String organizationName);

	Optional<UUID> findTenantIdByCode(String code);

}
