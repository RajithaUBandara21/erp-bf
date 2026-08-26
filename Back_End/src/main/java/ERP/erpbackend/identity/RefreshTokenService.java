package ERP.erpbackend.identity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

	private static final String KEY_PREFIX = "refresh:";
	private static final int TOKEN_BYTES = 32;
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final StringRedisTemplate redisTemplate;
	private final JwtProperties jwtProperties;

	public String issue(UUID userId) {
		String token = generateToken();
		redisTemplate.opsForValue().set(keyFor(token), userId.toString(), jwtProperties.refreshTokenTtl());
		return token;
	}

	/** Deletes the key on a hit, so a token is valid for exactly one use. */
	public Optional<UUID> consume(String token) {
		String userId = redisTemplate.opsForValue().getAndDelete(keyFor(token));
		return Optional.ofNullable(userId).map(UUID::fromString);
	}

	private static String generateToken() {
		byte[] bytes = new byte[TOKEN_BYTES];
		SECURE_RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private static String keyFor(String token) {
		return KEY_PREFIX + sha256Hex(token);
	}

	private static String sha256Hex(String token) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 algorithm not available", ex);
		}
	}

}
