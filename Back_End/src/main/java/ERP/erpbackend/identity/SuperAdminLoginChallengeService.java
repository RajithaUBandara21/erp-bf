package ERP.erpbackend.identity;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Single-use, Redis-backed token bridging the two halves of Super Admin login. Unlike
 * {@link LoginSelectionService}'s {@code consume()} (delete-on-read), {@link #resolve} only
 * peeks: the OTP check that follows is a second secret in the same flow, and a wrong OTP guess
 * must not burn this token too - mirroring {@link SuperAdminOtpService}'s own "a wrong guess
 * doesn't burn the real code" guarantee from feature 6a. {@link #invalidate} explicitly deletes
 * the token once the OTP also checks out, so a captured challenge token can't be replayed to
 * mint a second access token after a successful login.
 */
@Service
@RequiredArgsConstructor
public class SuperAdminLoginChallengeService {

	private static final String KEY_PREFIX = "superadmin:login:";
	private static final int TOKEN_BYTES = 32;
	private static final Duration TTL = Duration.ofMinutes(5);

	private final StringRedisTemplate redisTemplate;

	public String issue(UUID userId) {
		String token = SecureRandomToken.generate(TOKEN_BYTES);
		redisTemplate.opsForValue().set(keyFor(token), userId.toString(), TTL);
		return token;
	}

	/** Reads the pending userId without invalidating the token. */
	public Optional<UUID> resolve(String token) {
		String userId = redisTemplate.opsForValue().get(keyFor(token));
		return Optional.ofNullable(userId).map(UUID::fromString);
	}

	/** Call once the paired OTP has also been verified, so the token can't be reused. */
	public void invalidate(String token) {
		redisTemplate.delete(keyFor(token));
	}

	private static String keyFor(String token) {
		return KEY_PREFIX + SecureRandomToken.sha256Hex(token);
	}

}
