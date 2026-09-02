package ERP.erpbackend.identity;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class UserDirectoryServiceImpl implements UserDirectoryService {

	private final UserRepository userRepository;
	private final MembershipRepository membershipRepository;

	@Override
	public Map<UUID, UserSummaryResponse> findSummariesByIds(Collection<UUID> userIds) {
		if (userIds.isEmpty()) {
			return Map.of();
		}
		return userRepository.findAllById(userIds).stream()
				.collect(Collectors.toMap(User::getId,
						user -> new UserSummaryResponse(user.getId(), user.getFullName(), user.getEmail())));
	}

	@Override
	public List<UserSummaryResponse> listActiveOrganizationMembers(UUID organizationId) {
		List<UUID> userIds = membershipRepository.findByOrganizationIdAndStatus(organizationId, MembershipStatus.ACTIVE)
				.stream().map(Membership::getUserId).toList();
		if (userIds.isEmpty()) {
			return List.of();
		}
		return userRepository.findAllById(userIds).stream()
				.map(user -> new UserSummaryResponse(user.getId(), user.getFullName(), user.getEmail()))
				.sorted(Comparator.comparing(UserSummaryResponse::fullName, String.CASE_INSENSITIVE_ORDER))
				.toList();
	}

}
