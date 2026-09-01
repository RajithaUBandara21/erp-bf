package ERP.erpbackend.identity;

import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * Single-use, Redis-backed token bridging {@code POST /api/auth/join} and
 * {@code POST /api/auth/verify-email}. Carries the {@link JoinIntent} as JSON under a 24h TTL;
 * {@code getAndDelete} on consume makes the token valid for exactly one use. Mirrors
 * {@link LoginSelectionService} / {@link OAuthLoginExchangeService}.
 */
@Service
@RequiredArgsConstructor
public class EmailVerificationTokenService {

	private static final String KEY_PREFIX = "selfjoin:verify:";
	private static final int TOKEN_BYTES = 32;
	private static final Duration TTL = Duration.ofHours(24);

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	public String issue(JoinIntent intent) {
		String token = SecureRandomToken.generate(TOKEN_BYTES);
		redisTemplate.opsForValue().set(keyFor(token), objectMapper.writeValueAsString(intent), TTL);
		return token;
	}

	/** Deletes the key on a hit, so a token is valid for exactly one use. */
	public Optional<JoinIntent> consume(String token) {
		String json = redisTemplate.opsForValue().getAndDelete(keyFor(token));
		return Optional.ofNullable(json).map(value -> objectMapper.readValue(value, JoinIntent.class));
	}

	private static String keyFor(String token) {
		return KEY_PREFIX + SecureRandomToken.sha256Hex(token);
	}

}
