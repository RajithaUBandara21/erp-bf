package ERP.erpbackend.identity;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class UserDirectoryServiceImpl implements UserDirectoryService {

	private final UserRepository userRepository;

	@Override
	public Map<UUID, UserSummaryResponse> findSummariesByIds(Collection<UUID> userIds) {
		if (userIds.isEmpty()) {
			return Map.of();
		}
		return userRepository.findAllById(userIds).stream()
				.collect(Collectors.toMap(User::getId,
						user -> new UserSummaryResponse(user.getId(), user.getFullName(), user.getEmail())));
	}

}
