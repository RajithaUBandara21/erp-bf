package ERP.erpbackend.identity;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Read-only cross-module lookup of user display info, for modules that don't own the identity tables. */
public interface UserDirectoryService {

	/** Summaries for the given user ids, keyed by id. A missing/deleted id is simply absent from the result. */
	Map<UUID, UserSummaryResponse> findSummariesByIds(Collection<UUID> userIds);

}
