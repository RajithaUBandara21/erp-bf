package ERP.erpbackend.identity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/** Shared generation/hashing for the single-use, Redis-backed tokens issued across the identity module. */
final class SecureRandomToken {

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private SecureRandomToken() {
	}

	static String generate(int bytes) {
		byte[] buffer = new byte[bytes];
		SECURE_RANDOM.nextBytes(buffer);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
	}

	static String sha256Hex(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 algorithm not available", ex);
		}
	}

}
