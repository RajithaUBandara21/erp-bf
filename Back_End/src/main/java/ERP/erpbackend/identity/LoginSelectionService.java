package ERP.erpbackend.identity;

import ERP.erpbackend.organization.OrganizationService;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Single-use, Redis-backed token bridging the two halves of a multi-Organization login. Carries only
 * the {@code userId}; the ACTIVE Membership set is re-resolved when the token is consumed. Mirrors
 * {@link RefreshTokenService} / {@link OAuthStateService}.
 */
@Service
@RequiredArgsConstructor
public class LoginSelectionService {

	private static final String KEY_PREFIX = "login:select:";
	private static final int TOKEN_BYTES = 32;
	private static final Duration TTL = Duration.ofMinutes(5);

	private final StringRedisTemplate redisTemplate;
	private final OrganizationService organizationService;

	/**
	 * Start a two-step login: issue a selection token for {@code userId} and pair it with the
	 * Organizations they can choose between. Shared by password login and Google sign-in so both
	 * present the same Organization Selector.
	 */
	public LoginResponse beginSelection(UUID userId, List<Membership> memberships) {
		return LoginResponse.selectOrganization(issue(userId), toOptions(memberships));
	}

	private List<MembershipOption> toOptions(List<Membership> memberships) {
		Map<UUID, String> names = organizationService.findNamesByIds(
				memberships.stream().map(Membership::getOrganizationId).toList());
		return memberships.stream()
				.map(membership -> new MembershipOption(membership.getId(), membership.getOrganizationId(),
						names.getOrDefault(membership.getOrganizationId(), "")))
				.sorted(Comparator.comparing(MembershipOption::organizationName, String.CASE_INSENSITIVE_ORDER))
				.toList();
	}

	public String issue(UUID userId) {
		String token = SecureRandomToken.generate(TOKEN_BYTES);
		redisTemplate.opsForValue().set(keyFor(token), userId.toString(), TTL);
		return token;
	}

	/** Deletes the key on a hit, so a token is valid for exactly one use. */
	public Optional<UUID> consume(String token) {
		String userId = redisTemplate.opsForValue().getAndDelete(keyFor(token));
		return Optional.ofNullable(userId).map(UUID::fromString);
	}

	private static String keyFor(String token) {
		return KEY_PREFIX + SecureRandomToken.sha256Hex(token);
	}

}
