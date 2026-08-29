package ERP.erpbackend.identity;

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

	private final StringRedisTemplate redisTemplate;
	private final JwtProperties jwtProperties;

	public String issue(UUID sessionId) {
		String token = SecureRandomToken.generate(TOKEN_BYTES);
		redisTemplate.opsForValue().set(keyFor(token), sessionId.toString(), jwtProperties.refreshTokenTtl());
		return token;
	}

	/** Deletes the key on a hit, so a token is valid for exactly one use. */
	public Optional<UUID> consume(String token) {
		String sessionId = redisTemplate.opsForValue().getAndDelete(keyFor(token));
		return Optional.ofNullable(sessionId).map(UUID::fromString);
	}

	private static String keyFor(String token) {
		return KEY_PREFIX + SecureRandomToken.sha256Hex(token);
	}

}
