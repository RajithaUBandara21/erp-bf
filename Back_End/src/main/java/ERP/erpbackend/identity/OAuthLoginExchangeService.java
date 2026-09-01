package ERP.erpbackend.identity;

import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * Single-use, Redis-backed handoff of a freshly resolved {@link LoginResponse} from the callback
 * redirect to the frontend's same-origin exchange call, so a token never travels in a URL query
 * string. The response is either {@code AUTHENTICATED} (Google sign-in resolved to one Organization)
 * or {@code SELECT_ORGANIZATION} (several - the frontend then runs the same selector as password
 * login). Mirrors {@link OAuthStateService}, with a shorter TTL since the frontend consumes it
 * immediately on page load.
 */
@Service
@RequiredArgsConstructor
public class OAuthLoginExchangeService {

	private static final String KEY_PREFIX = "oauth:google:exchange:";
	private static final int CODE_BYTES = 32;
	private static final Duration TTL = Duration.ofSeconds(60);

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	public String issue(LoginResponse loginResponse) {
		String code = SecureRandomToken.generate(CODE_BYTES);
		redisTemplate.opsForValue().set(keyFor(code), objectMapper.writeValueAsString(loginResponse), TTL);
		return code;
	}

	/** Deletes the key on a hit, so a code is valid for exactly one use. */
	public Optional<LoginResponse> consume(String code) {
		String json = redisTemplate.opsForValue().getAndDelete(keyFor(code));
		return Optional.ofNullable(json).map(value -> objectMapper.readValue(value, LoginResponse.class));
	}

	private static String keyFor(String code) {
		return KEY_PREFIX + SecureRandomToken.sha256Hex(code);
	}

}
