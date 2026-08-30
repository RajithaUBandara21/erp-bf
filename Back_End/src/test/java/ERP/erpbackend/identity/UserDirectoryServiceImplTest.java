package ERP.erpbackend.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserDirectoryServiceImplTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private MembershipRepository membershipRepository;

	@InjectMocks
	private UserDirectoryServiceImpl userDirectoryService;

	private User userWith(UUID id, String fullName, String email) {
		User user = new User();
		user.setFullName(fullName);
		user.setEmail(email);
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}

	private Membership membershipFor(UUID userId) {
		Membership membership = new Membership();
		membership.setUserId(userId);
		membership.setStatus(MembershipStatus.ACTIVE);
		return membership;
	}

	@Test
	void returnsSummariesKeyedByIdForExistingUsers() {
		UUID id1 = UUID.randomUUID();
		UUID id2 = UUID.randomUUID();
		User user1 = userWith(id1, "R. Haritha", "r.haritha@acme.test");
		User user2 = userWith(id2, "A. Perera", "a.perera@acme.test");
		when(userRepository.findAllById(Set.of(id1, id2))).thenReturn(List.of(user1, user2));

		Map<UUID, UserSummaryResponse> summaries = userDirectoryService.findSummariesByIds(Set.of(id1, id2));

		assertThat(summaries).hasSize(2);
		assertThat(summaries.get(id1)).isEqualTo(new UserSummaryResponse(id1, "R. Haritha", "r.haritha@acme.test"));
		assertThat(summaries.get(id2)).isEqualTo(new UserSummaryResponse(id2, "A. Perera", "a.perera@acme.test"));
	}

	@Test
	void omitsIdsThatDoNotResolveToAUser() {
		UUID existingId = UUID.randomUUID();
		UUID deletedId = UUID.randomUUID();
		User user = userWith(existingId, "R. Haritha", "r.haritha@acme.test");
		when(userRepository.findAllById(Set.of(existingId, deletedId))).thenReturn(List.of(user));

		Map<UUID, UserSummaryResponse> summaries = userDirectoryService
				.findSummariesByIds(Set.of(existingId, deletedId));

		assertThat(summaries).containsOnlyKeys(existingId);
	}

	@Test
	void emptyInputShortCircuitsWithoutQuerying() {
		Map<UUID, UserSummaryResponse> summaries = userDirectoryService.findSummariesByIds(Set.of());

		assertThat(summaries).isEmpty();
		verifyNoInteractions(userRepository);
	}

	@Test
	void listActiveTenantMembersReturnsMembersOrderedByFullName() {
		UUID tenantId = UUID.randomUUID();
		UUID graceId = UUID.randomUUID();
		UUID adaId = UUID.randomUUID();
		when(membershipRepository.findByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE))
				.thenReturn(List.of(membershipFor(graceId), membershipFor(adaId)));
		when(userRepository.findAllById(List.of(graceId, adaId))).thenReturn(List.of(
				userWith(graceId, "Grace Hopper", "grace@acme.test"),
				userWith(adaId, "Ada Lovelace", "ada@acme.test")));

		List<UserSummaryResponse> members = userDirectoryService.listActiveTenantMembers(tenantId);

		assertThat(members).extracting(UserSummaryResponse::fullName)
				.containsExactly("Ada Lovelace", "Grace Hopper");
	}

	@Test
	void listActiveTenantMembersShortCircuitsWhenTenantHasNoMembers() {
		UUID tenantId = UUID.randomUUID();
		when(membershipRepository.findByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE)).thenReturn(List.of());

		assertThat(userDirectoryService.listActiveTenantMembers(tenantId)).isEmpty();
		verifyNoInteractions(userRepository);
	}

}
