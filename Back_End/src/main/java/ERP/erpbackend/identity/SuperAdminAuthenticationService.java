package ERP.erpbackend.identity;

import ERP.erpbackend.audit.AuditEvent;
import ERP.erpbackend.audit.AuditService;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SuperAdminAuthenticationService {

	private static final String INVALID_CREDENTIALS = "Invalid credentials";
	private static final String INVALID_OR_EXPIRED_CODE = "Invalid or expired code";

	// A valid BCrypt hash of a throwaway value, matching AuthenticationService.login's technique:
	// compared against on every miss so a wrong password, an inactive account, and a correct
	// password on a non-Super-Admin account all take the same code path and response time.
	private static final String DUMMY_PASSWORD_HASH =
			"$2a$10$rj1PzUggzShtDluMqkrXZ.JRijLCAjBsdaPs5nbO6M66EzAE2gyS2";

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final SuperAdminLoginChallengeService challengeService;
	private final SuperAdminOtpService otpService;
	private final SuperAdminOtpMailer otpMailer;
	private final JwtService jwtService;
	private final AuditService auditService;

	public SuperAdminLoginChallengeResponse login(SuperAdminLoginRequest request) {
		Optional<User> user = userRepository.findByEmail(request.email().toLowerCase(Locale.ROOT));

		boolean passwordMatches = passwordEncoder.matches(
				request.password(), user.map(User::getPasswordHash).orElse(DUMMY_PASSWORD_HASH));

		if (user.isEmpty() || !user.get().isPlatformSuperAdmin() || !user.get().isActive() || !passwordMatches) {
			throw invalidCredentials();
		}

		String otp = otpService.issue(user.get().getId());
		otpMailer.send(user.get().getEmail(), otp);
		return new SuperAdminLoginChallengeResponse(challengeService.issue(user.get().getId()));
	}

	public SuperAdminTokenResponse verify(SuperAdminLoginVerifyRequest request) {
		// Peeked, not consumed: a wrong OTP below must not burn the challenge token, mirroring
		// SuperAdminOtpService's own "a wrong guess doesn't burn the real code" guarantee.
		UUID userId = challengeService.resolve(request.challengeToken())
				.orElseThrow(SuperAdminAuthenticationService::invalidOtp);

		// Re-check Super Admin status/active now: the user could have been deactivated between the
		// login and verify calls.
		User user = userRepository.findById(userId)
				.filter(User::isPlatformSuperAdmin)
				.filter(User::isActive)
				.orElseThrow(SuperAdminAuthenticationService::invalidOtp);

		if (!otpService.consume(userId, request.otp())) {
			throw invalidOtp();
		}
		challengeService.invalidate(request.challengeToken());

		AuthenticatedUser authenticatedUser = AuthenticatedUser.superAdmin(
				user.getId(), user.getEmail(), user.isMustChangePassword());
		String accessToken = jwtService.issueAccessToken(authenticatedUser);

		auditService.log(new AuditEvent(null, null, user.getId(), "User", user.getId(),
				"auth.super_admin_login", null, null));

		return new SuperAdminTokenResponse(accessToken, jwtService.superAdminAccessTokenTtlSeconds(),
				user.getId(), user.getEmail(), user.getFullName(), user.isMustChangePassword());
	}

	@Transactional
	public SuperAdminTokenResponse changePassword(AuthenticatedUser caller, SuperAdminChangePasswordRequest request) {
		User user = userRepository.findById(caller.userId())
				.filter(User::isPlatformSuperAdmin)
				.orElseThrow(SuperAdminAuthenticationService::invalidCredentials);

		if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
			throw invalidCredentials();
		}

		user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
		user.setMustChangePassword(false);
		userRepository.save(user);

		auditService.log(new AuditEvent(null, null, user.getId(), "User", user.getId(),
				"auth.super_admin_password_changed", null, null));

		// Re-issue immediately so the caller isn't forced to fully re-authenticate (email + password
		// + a fresh OTP) right after a change the server itself demanded.
		AuthenticatedUser authenticatedUser = AuthenticatedUser.superAdmin(user.getId(), user.getEmail(), false);
		String accessToken = jwtService.issueAccessToken(authenticatedUser);

		return new SuperAdminTokenResponse(accessToken, jwtService.superAdminAccessTokenTtlSeconds(),
				user.getId(), user.getEmail(), user.getFullName(), false);
	}

	private static ResponseStatusException invalidCredentials() {
		return new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
	}

	private static ResponseStatusException invalidOtp() {
		return new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_OR_EXPIRED_CODE);
	}

}
