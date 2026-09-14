package ERP.erpbackend.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import ERP.erpbackend.TestcontainersConfiguration;
import ERP.erpbackend.audit.AuditLog;
import ERP.erpbackend.audit.AuditLogRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SuperAdminAuthenticationServiceTest {

	private static final String PASSWORD = "Sunrise8";

	@Autowired
	private SuperAdminAuthenticationService superAdminAuthenticationService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private SuperAdminLoginChallengeService challengeService;

	@Autowired
	private AuditLogRepository auditLogRepository;

	@MockitoSpyBean
	private SuperAdminOtpMailer superAdminOtpMailer;

	private User createUser(String email, boolean platformSuperAdmin, boolean active) {
		return createUser(email, platformSuperAdmin, active, false);
	}

	private User createUser(String email, boolean platformSuperAdmin, boolean active, boolean mustChangePassword) {
		User user = new User();
		user.setEmail(email);
		user.setPasswordHash(passwordEncoder.encode(PASSWORD));
		user.setFullName("Test User");
		user.setPlatformSuperAdmin(platformSuperAdmin);
		user.setActive(active);
		user.setMustChangePassword(mustChangePassword);
		return userRepository.save(user);
	}

	@AfterEach
	void clearContext() {
		SecurityContextHolder.clearContext();
	}

	private static void authenticateAs(AuthenticatedUser principal) {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
	}

	private record Challenge(String token, String otp) {
	}

	/** Logs in and captures the real OTP the mailer was sent, so verify() tests can drive the full flow. */
	private Challenge loginAndCaptureOtp(User superAdmin) {
		SuperAdminLoginChallengeResponse response = superAdminAuthenticationService.login(
				new SuperAdminLoginRequest(superAdmin.getEmail(), PASSWORD));
		ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
		verify(superAdminOtpMailer).send(eq(superAdmin.getEmail()), otpCaptor.capture());
		return new Challenge(response.challengeToken(), otpCaptor.getValue());
	}

	@Test
	void correctSuperAdminCredentialsIssueAChallengeTokenAndEmailAnOtp() {
		User superAdmin = createUser("root-" + UUID.randomUUID() + "@platform.test", true, true);

		SuperAdminLoginChallengeResponse response = superAdminAuthenticationService.login(
				new SuperAdminLoginRequest(superAdmin.getEmail(), PASSWORD));

		assertThat(response.challengeToken()).isNotBlank();
		assertThat(challengeService.resolve(response.challengeToken())).contains(superAdmin.getId());
		verify(superAdminOtpMailer).send(eq(superAdmin.getEmail()), matches("\\d{6}"));
	}

	@Test
	void wrongPasswordIsRejectedWithNoOtpIssued() {
		User superAdmin = createUser("root-" + UUID.randomUUID() + "@platform.test", true, true);

		assertThatExceptionOfType(ResponseStatusException.class)
				.isThrownBy(() -> superAdminAuthenticationService.login(
						new SuperAdminLoginRequest(superAdmin.getEmail(), "wrong-password")))
				.satisfies(ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
		verifyNoInteractions(superAdminOtpMailer);
	}

	@Test
	void inactiveSuperAdminIsRejectedWithNoOtpIssued() {
		User superAdmin = createUser("root-" + UUID.randomUUID() + "@platform.test", true, false);

		assertThatExceptionOfType(ResponseStatusException.class)
				.isThrownBy(() -> superAdminAuthenticationService.login(
						new SuperAdminLoginRequest(superAdmin.getEmail(), PASSWORD)))
				.satisfies(ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
		verifyNoInteractions(superAdminOtpMailer);
	}

	@Test
	void correctCredentialedNonSuperAdminUserIsRejectedWithTheIdenticalGeneric401() {
		User ordinaryUser = createUser("ordinary-" + UUID.randomUUID() + "@acme.test", false, true);

		assertThatExceptionOfType(ResponseStatusException.class)
				.isThrownBy(() -> superAdminAuthenticationService.login(
						new SuperAdminLoginRequest(ordinaryUser.getEmail(), PASSWORD)))
				.satisfies(ex -> {
					assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
					assertThat(ex.getReason()).isEqualTo("Invalid credentials");
				});
		verifyNoInteractions(superAdminOtpMailer);
	}

	@Test
	void unknownEmailIsRejectedWithTheIdenticalGeneric401() {
		assertThatExceptionOfType(ResponseStatusException.class)
				.isThrownBy(() -> superAdminAuthenticationService.login(
						new SuperAdminLoginRequest("nobody-" + UUID.randomUUID() + "@platform.test", PASSWORD)))
				.satisfies(ex -> {
					assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
					assertThat(ex.getReason()).isEqualTo("Invalid credentials");
				});
		verifyNoInteractions(superAdminOtpMailer);
	}

	@Test
	void correctChallengeAndOtpIssueATokenReflectingMustChangePasswordAndWriteAnAuditRow() {
		User superAdmin = createUser("root-" + UUID.randomUUID() + "@platform.test", true, true, true);
		Challenge challenge = loginAndCaptureOtp(superAdmin);

		SuperAdminTokenResponse response = superAdminAuthenticationService.verify(
				new SuperAdminLoginVerifyRequest(challenge.token(), challenge.otp()));

		assertThat(response.accessToken()).isNotBlank();
		assertThat(response.userId()).isEqualTo(superAdmin.getId());
		assertThat(response.email()).isEqualTo(superAdmin.getEmail());
		assertThat(response.fullName()).isEqualTo(superAdmin.getFullName());
		assertThat(response.mustChangePassword()).isTrue();

		AuditLog auditLog = auditLogRepository.findAll().stream()
				.filter(entry -> superAdmin.getId().equals(entry.getUserId()))
				.filter(entry -> "auth.super_admin_login".equals(entry.getAction()))
				.findFirst().orElseThrow();
		assertThat(auditLog.getTenantId()).isNull();
		assertThat(auditLog.getOrganizationId()).isNull();
	}

	@Test
	void aWrongOtpIsRejectedButASubsequentCorrectAttemptOnTheSameChallengeStillSucceeds() {
		User superAdmin = createUser("root-" + UUID.randomUUID() + "@platform.test", true, true);
		Challenge challenge = loginAndCaptureOtp(superAdmin);

		assertThatExceptionOfType(ResponseStatusException.class)
				.isThrownBy(() -> superAdminAuthenticationService.verify(
						new SuperAdminLoginVerifyRequest(challenge.token(), "000000")))
				.satisfies(ex -> {
					assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
					assertThat(ex.getReason()).isEqualTo("Invalid or expired code");
				});

		SuperAdminTokenResponse response = superAdminAuthenticationService.verify(
				new SuperAdminLoginVerifyRequest(challenge.token(), challenge.otp()));

		assertThat(response.accessToken()).isNotBlank();
	}

	@Test
	void anUnknownChallengeTokenIsRejectedWithTheIdenticalGeneric401() {
		assertThatExceptionOfType(ResponseStatusException.class)
				.isThrownBy(() -> superAdminAuthenticationService.verify(
						new SuperAdminLoginVerifyRequest("unknown-token", "123456")))
				.satisfies(ex -> {
					assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
					assertThat(ex.getReason()).isEqualTo("Invalid or expired code");
				});
	}

	@Test
	void correctCurrentPasswordClearsMustChangePasswordAndReturnsAFreshTokenAndAuditRow() {
		User superAdmin = createUser("root-" + UUID.randomUUID() + "@platform.test", true, true, true);
		authenticateAs(AuthenticatedUser.superAdmin(superAdmin.getId(), superAdmin.getEmail(), true));

		SuperAdminTokenResponse response = superAdminAuthenticationService.changePassword(
				AuthenticatedUser.superAdmin(superAdmin.getId(), superAdmin.getEmail(), true),
				new SuperAdminChangePasswordRequest(PASSWORD, "Newpass9"));

		assertThat(response.accessToken()).isNotBlank();
		assertThat(response.mustChangePassword()).isFalse();

		User reloaded = userRepository.findById(superAdmin.getId()).orElseThrow();
		assertThat(reloaded.isMustChangePassword()).isFalse();
		assertThat(passwordEncoder.matches("Newpass9", reloaded.getPasswordHash())).isTrue();

		AuditLog auditLog = auditLogRepository.findAll().stream()
				.filter(entry -> superAdmin.getId().equals(entry.getUserId()))
				.filter(entry -> "auth.super_admin_password_changed".equals(entry.getAction()))
				.findFirst().orElseThrow();
		assertThat(auditLog.getTenantId()).isNull();
	}

	@Test
	void wrongCurrentPasswordChangesNothingAndReturns401() {
		User superAdmin = createUser("root-" + UUID.randomUUID() + "@platform.test", true, true, true);
		authenticateAs(AuthenticatedUser.superAdmin(superAdmin.getId(), superAdmin.getEmail(), true));

		assertThatExceptionOfType(ResponseStatusException.class)
				.isThrownBy(() -> superAdminAuthenticationService.changePassword(
						AuthenticatedUser.superAdmin(superAdmin.getId(), superAdmin.getEmail(), true),
						new SuperAdminChangePasswordRequest("wrong-password", "Newpass9")))
				.satisfies(ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));

		User reloaded = userRepository.findById(superAdmin.getId()).orElseThrow();
		assertThat(reloaded.isMustChangePassword()).isTrue();
		assertThat(passwordEncoder.matches(PASSWORD, reloaded.getPasswordHash())).isTrue();
	}

	// The @PreAuthorize gate lives on SuperAdminAuthController, not this service (matching
	// AuditLogController's precedent) - see SuperAdminAuthControllerTest for the denial case.

}
