package ERP.erpbackend.identity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * Single-use, Redis-backed handoff of a freshly issued {@link TokenResponse} from the callback
 * redirect to the frontend's same-origin exchange call, so the token never travels in a URL query
 * string. Mirrors {@link OAuthStateService}, with a shorter TTL since the frontend consumes it
 * immediately on page load.
 */
@Service
@RequiredArgsConstructor
public class OAuthLoginExchangeService {

	private static final String KEY_PREFIX = "oauth:google:exchange:";
	private static final int CODE_BYTES = 32;
	private static final Duration TTL = Duration.ofSeconds(60);
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	public String issue(TokenResponse tokenResponse) {
		String code = generateCode();
		redisTemplate.opsForValue().set(keyFor(code), objectMapper.writeValueAsString(tokenResponse), TTL);
		return code;
	}

	/** Deletes the key on a hit, so a code is valid for exactly one use. */
	public Optional<TokenResponse> consume(String code) {
		String json = redisTemplate.opsForValue().getAndDelete(keyFor(code));
		return Optional.ofNullable(json).map(value -> objectMapper.readValue(value, TokenResponse.class));
	}

	private static String generateCode() {
		byte[] bytes = new byte[CODE_BYTES];
		SECURE_RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private static String keyFor(String code) {
		return KEY_PREFIX + sha256Hex(code);
	}

	private static String sha256Hex(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 algorithm not available", ex);
		}
	}

}
