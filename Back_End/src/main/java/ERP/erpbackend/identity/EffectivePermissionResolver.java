package ERP.erpbackend.identity;

import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves a Membership's effective permission-code set from the roles assigned to it. Read per
 * request rather than baked into the access token, so a role change takes effect immediately.
 */
@Service
@RequiredArgsConstructor
public class EffectivePermissionResolver {

	private final PermissionRepository permissionRepository;

	@Transactional(readOnly = true)
	public Set<String> resolve(UUID membershipId) {
		return Set.copyOf(permissionRepository.findEffectiveCodes(membershipId));
	}

}
