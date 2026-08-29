package ERP.erpbackend.identity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/** Single-use, Redis-backed state for the OAuth handshake - mirrors {@link RefreshTokenService}'s issue/consume pattern. */
@Service
@RequiredArgsConstructor
public class OAuthStateService {

	private static final String KEY_PREFIX = "oauth:google:state:";
	private static final int STATE_BYTES = 32;
	private static final Duration TTL = Duration.ofMinutes(10);
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final StringRedisTemplate redisTemplate;

	/** {@code linkedUserId} is {@code null} for a login flow, or the caller's id for a link flow. */
	public String issue(UUID linkedUserId) {
		String state = generateState();
		String value = linkedUserId == null ? "" : linkedUserId.toString();
		redisTemplate.opsForValue().set(keyFor(state), value, TTL);
		return state;
	}

	/** Deletes the key on a hit, so a state is valid for exactly one use. Empty string means a login flow. */
	public Optional<String> consume(String state) {
		return Optional.ofNullable(redisTemplate.opsForValue().getAndDelete(keyFor(state)));
	}

	private static String generateState() {
		byte[] bytes = new byte[STATE_BYTES];
		SECURE_RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private static String keyFor(String state) {
		return KEY_PREFIX + sha256Hex(state);
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
