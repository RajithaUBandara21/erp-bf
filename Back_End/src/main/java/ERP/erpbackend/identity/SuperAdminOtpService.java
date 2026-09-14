package ERP.erpbackend.identity;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Single-use, Redis-backed one-time code for the Super Admin login's mandatory OTP step.
 * Keyed by user id rather than a hash of the code, unlike {@link OAuthStateService} /
 * {@link RefreshTokenService}: the user id isn't secret, the code is, so there's nothing to
 * hash in the key.
 */
@Service
@RequiredArgsConstructor
public class SuperAdminOtpService {

	private static final String KEY_PREFIX = "superadmin:otp:";
	private static final Duration TTL = Duration.ofMinutes(5);
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final StringRedisTemplate redisTemplate;

	public String issue(UUID userId) {
		String code = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
		redisTemplate.opsForValue().set(keyFor(userId), code, TTL);
		return code;
	}

	/** Deletes the key only on an exact match, so a wrong guess never burns a still-valid code. */
	public boolean consume(UUID userId, String code) {
		String key = keyFor(userId);
		String stored = redisTemplate.opsForValue().get(key);
		if (stored == null || !stored.equals(code)) {
			return false;
		}
		redisTemplate.delete(key);
		return true;
	}

	private static String keyFor(UUID userId) {
		return KEY_PREFIX + userId;
	}

}
