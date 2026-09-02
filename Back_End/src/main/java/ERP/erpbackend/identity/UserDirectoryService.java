package ERP.erpbackend.identity;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Read-only cross-module lookup of user display info, for modules that don't own the identity tables. */
public interface UserDirectoryService {

	/** Summaries for the given user ids, keyed by id. A missing/deleted id is simply absent from the result. */
	Map<UUID, UserSummaryResponse> findSummariesByIds(Collection<UUID> userIds);

	/** Active members of one organization, ordered by full name - backs {@code GET /api/users}. */
	List<UserSummaryResponse> listActiveOrganizationMembers(UUID organizationId);

}
