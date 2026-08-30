package ERP.erpbackend.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import ERP.erpbackend.common.GlobalExceptionHandler;
import ERP.erpbackend.common.JpaAuditingConfig;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(
		controllers = AuthController.class,
		excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class))
@Import(SecurityConfig.class)
@ImportAutoConfiguration({ServletWebSecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private RegistrationService registrationService;

	@MockitoBean
	private RegistrationRateLimiter registrationRateLimiter;

	@MockitoBean
	private AuthenticationService authenticationService;

	@MockitoBean
	private LoginRateLimiter loginRateLimiter;

	@MockitoBean
	private JwtService jwtService;

	@MockitoBean
	private RevokedSessionRegistry revokedSessionRegistry;

	private final Logger exceptionHandlerLogger =
			(Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
	private final ListAppender<ILoggingEvent> logEvents = new ListAppender<>();

	@BeforeEach
	void allowByDefault() {
		when(registrationRateLimiter.allow(anyString())).thenReturn(true);
		when(loginRateLimiter.allow(anyString())).thenReturn(true);
		logEvents.start();
		exceptionHandlerLogger.addAppender(logEvents);
	}

	@AfterEach
	void detachAppender() {
		exceptionHandlerLogger.detachAppender(logEvents);
	}

	@Test
	void rejectsMalformedJsonBodyWithBadRequestAndNoErrorLog() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ not valid json "))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").exists())
				.andExpect(jsonPath("$.errors").exists());

		verify(authenticationService, never()).login(any(LoginRequest.class));
		assertThat(logEvents.list).noneMatch(event -> event.getLevel() == Level.ERROR);
	}

	@Test
	void registersAccountAndReturns201() throws Exception {
		TokenResponse account = new TokenResponse("access-token", "refresh-token", 900L, 2592000L,
				UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "ada@acme.test", "Ada Owner");
		when(registrationService.register(any(RegisterRequest.class))).thenReturn(account);

		String requestBody = """
				{
				  "organizationName": "Acme Corp",
				  "fullName": "Ada Owner",
				  "email": "ada@acme.test",
				  "password": "Sunrise8",
				  "clientType": "WEB"
				}
				""";

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.accessToken").value("access-token"))
				.andExpect(jsonPath("$.refreshToken").value("refresh-token"))
				.andExpect(jsonPath("$.expiresIn").value(900))
				.andExpect(jsonPath("$.refreshExpiresIn").value(2592000))
				.andExpect(jsonPath("$.userId").value(account.userId().toString()))
				.andExpect(jsonPath("$.tenantId").value(account.tenantId().toString()))
				.andExpect(jsonPath("$.organizationId").value(account.organizationId().toString()))
				.andExpect(jsonPath("$.email").value("ada@acme.test"));
	}

	@Test
	void rejectsInvalidRequestWithValidationDetails() throws Exception {
		String requestBody = """
				{
				  "organizationName": "Acme Corp",
				  "fullName": "Ada Owner",
				  "email": "",
				  "password": "weak",
				  "clientType": "WEB"
				}
				""";

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.email").exists())
				.andExpect(jsonPath("$.errors.password").exists());
	}

	@Test
	void rejectsMissingClientTypeWithValidationDetails() throws Exception {
		String requestBody = """
				{
				  "organizationName": "Acme Corp",
				  "fullName": "Ada Owner",
				  "email": "ada@acme.test",
				  "password": "Sunrise8"
				}
				""";

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.clientType").exists());
	}

	@Test
	void registersAccountWithPasswordAtExactUtf8ByteLimit() throws Exception {
		String passwordAtLimit = "A1" + "a".repeat(70);
		TokenResponse account = new TokenResponse("access-token", "refresh-token", 900L, 2592000L,
				UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "ada@acme.test", "Ada Owner");
		when(registrationService.register(any(RegisterRequest.class))).thenReturn(account);

		String requestBody = """
				{
				  "organizationName": "Acme Corp",
				  "fullName": "Ada Owner",
				  "email": "ada@acme.test",
				  "password": "%s",
				  "clientType": "WEB"
				}
				""".formatted(passwordAtLimit);

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isCreated());
	}

	@Test
	void rejectsOversizedPasswordWithValidationDetails() throws Exception {
		String oversizedPassword = "A1" + "a".repeat(72);
		String requestBody = """
				{
				  "organizationName": "Acme Corp",
				  "fullName": "Ada Owner",
				  "email": "ada@acme.test",
				  "password": "%s",
				  "clientType": "WEB"
				}
				""".formatted(oversizedPassword);

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.password").exists());
	}

	@Test
	void rejectsPasswordOverUtf8ByteLimitEvenWithinCharacterLimit() throws Exception {
		String nonAsciiPassword = "A1" + "é".repeat(70);
		String requestBody = """
				{
				  "organizationName": "Acme Corp",
				  "fullName": "Ada Owner",
				  "email": "ada@acme.test",
				  "password": "%s",
				  "clientType": "WEB"
				}
				""".formatted(nonAsciiPassword);

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.password").exists());
	}

	@Test
	void rejectsOversizedOrganizationNameWithValidationDetails() throws Exception {
		String oversizedName = "a".repeat(256);
		String requestBody = """
				{
				  "organizationName": "%s",
				  "fullName": "Ada Owner",
				  "email": "ada@acme.test",
				  "password": "Sunrise8",
				  "clientType": "WEB"
				}
				""".formatted(oversizedName);

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.organizationName").exists());
	}

	@Test
	void rejectsOversizedEmailWithValidationDetails() throws Exception {
		String oversizedEmail = "a".repeat(250) + "@acme.test";
		String requestBody = """
				{
				  "organizationName": "Acme Corp",
				  "fullName": "Ada Owner",
				  "email": "%s",
				  "password": "Sunrise8",
				  "clientType": "WEB"
				}
				""".formatted(oversizedEmail);

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.email").exists());
	}

	@Test
	void returnsConflictWhenRegistrationRacesAnotherRequestForTheSameCode() throws Exception {
		when(registrationService.register(any(RegisterRequest.class)))
				.thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

		String requestBody = """
				{
				  "organizationName": "Acme Corp",
				  "fullName": "Ada Owner",
				  "email": "ada@acme.test",
				  "password": "Sunrise8",
				  "clientType": "WEB"
				}
				""";

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").exists());
	}

	@Test
	void returnsTooManyRequestsWhenRateLimitExceeded() throws Exception {
		when(registrationRateLimiter.allow(anyString())).thenReturn(false);

		String requestBody = """
				{
				  "organizationName": "Acme Corp",
				  "fullName": "Ada Owner",
				  "email": "ada@acme.test",
				  "password": "Sunrise8",
				  "clientType": "WEB"
				}
				""";

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.message").exists());

		verify(registrationService, never()).register(any(RegisterRequest.class));
	}

	@Test
	void loginReturnsAuthenticatedOutcomeWithTheSessionTokens() throws Exception {
		TokenResponse tokenResponse = new TokenResponse(
				"access-token", "refresh-token", 900L, 2592000L,
				UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "ada@acme.test", "Ada Owner");
		when(authenticationService.login(any(LoginRequest.class)))
				.thenReturn(LoginResponse.authenticated(tokenResponse));

		String requestBody = """
				{
				  "email": "ada@acme.test",
				  "password": "Sunrise8",
				  "clientType": "WEB"
				}
				""";

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.outcome").value("AUTHENTICATED"))
				.andExpect(jsonPath("$.selectionToken").doesNotExist())
				.andExpect(jsonPath("$.session.accessToken").value("access-token"))
				.andExpect(jsonPath("$.session.refreshToken").value("refresh-token"))
				.andExpect(jsonPath("$.session.expiresIn").value(900))
				.andExpect(jsonPath("$.session.userId").value(tokenResponse.userId().toString()))
				.andExpect(jsonPath("$.session.email").value("ada@acme.test"));
	}

	@Test
	void loginReturnsSelectOrganizationOutcomeWithTokenAndOptions() throws Exception {
		UUID membershipId = UUID.randomUUID();
		UUID organizationId = UUID.randomUUID();
		when(authenticationService.login(any(LoginRequest.class))).thenReturn(LoginResponse.selectOrganization(
				"selection-token",
				List.of(new MembershipOption(membershipId, organizationId, "Head Office"))));

		String requestBody = """
				{
				  "email": "ada@acme.test",
				  "password": "Sunrise8",
				  "clientType": "WEB"
				}
				""";

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.outcome").value("SELECT_ORGANIZATION"))
				.andExpect(jsonPath("$.session").doesNotExist())
				.andExpect(jsonPath("$.selectionToken").value("selection-token"))
				.andExpect(jsonPath("$.organizations[0].membershipId").value(membershipId.toString()))
				.andExpect(jsonPath("$.organizations[0].organizationName").value("Head Office"));
	}

	@Test
	void loginReturnsUnauthorizedOnBadCredentials() throws Exception {
		when(authenticationService.login(any(LoginRequest.class)))
				.thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

		String requestBody = """
				{
				  "email": "ada@acme.test",
				  "password": "WrongPass9",
				  "clientType": "WEB"
				}
				""";

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Invalid credentials"));
	}

	@Test
	void loginRejectsAMissingEmailWithValidationDetails() throws Exception {
		String requestBody = """
				{
				  "password": "Sunrise8",
				  "clientType": "WEB"
				}
				""";

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.email").exists());

		verify(authenticationService, never()).login(any(LoginRequest.class));
	}

	@Test
	void loginReturnsTooManyRequestsWhenRateLimitExceeded() throws Exception {
		when(loginRateLimiter.allow(anyString())).thenReturn(false);

		String requestBody = """
				{
				  "email": "ada@acme.test",
				  "password": "Sunrise8",
				  "clientType": "WEB"
				}
				""";

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.message").exists());

		verify(authenticationService, never()).login(any(LoginRequest.class));
	}

	@Test
	void loginSelectReturnsTheSessionTokensOnSuccess() throws Exception {
		TokenResponse tokenResponse = new TokenResponse(
				"access-token", "refresh-token", 900L, 2592000L,
				UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "ada@acme.test", "Ada Owner");
		when(authenticationService.selectOrganization(any(LoginSelectRequest.class))).thenReturn(tokenResponse);

		String requestBody = """
				{
				  "selectionToken": "a-selection-token",
				  "membershipId": "%s",
				  "clientType": "WEB"
				}
				""".formatted(UUID.randomUUID());

		mockMvc.perform(post("/api/auth/login/select")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value("access-token"))
				.andExpect(jsonPath("$.refreshToken").value("refresh-token"));
	}

	@Test
	void loginSelectRejectsAMissingMembershipIdWithValidationDetails() throws Exception {
		String requestBody = """
				{
				  "selectionToken": "a-selection-token",
				  "clientType": "WEB"
				}
				""";

		mockMvc.perform(post("/api/auth/login/select")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.membershipId").exists());

		verify(authenticationService, never()).selectOrganization(any(LoginSelectRequest.class));
	}

	@Test
	void loginSelectReturnsTooManyRequestsWhenRateLimitExceeded() throws Exception {
		when(loginRateLimiter.allow(anyString())).thenReturn(false);

		String requestBody = """
				{
				  "selectionToken": "a-selection-token",
				  "membershipId": "%s",
				  "clientType": "WEB"
				}
				""".formatted(UUID.randomUUID());

		mockMvc.perform(post("/api/auth/login/select")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isTooManyRequests());

		verify(authenticationService, never()).selectOrganization(any(LoginSelectRequest.class));
	}

	@Test
	void refreshReturnsTokenShapeOnSuccess() throws Exception {
		TokenResponse tokenResponse = new TokenResponse(
				"new-access-token", "new-refresh-token", 900L, 2592000L,
				UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "ada@acme.test", "Ada Owner");
		when(authenticationService.refresh(any(RefreshRequest.class))).thenReturn(tokenResponse);

		String requestBody = """
				{
				  "refreshToken": "some-refresh-token"
				}
				""";

		mockMvc.perform(post("/api/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value("new-access-token"))
				.andExpect(jsonPath("$.refreshToken").value("new-refresh-token"))
				.andExpect(jsonPath("$.expiresIn").value(900));
	}

	@Test
	void refreshReturnsUnauthorizedOnInvalidToken() throws Exception {
		when(authenticationService.refresh(any(RefreshRequest.class)))
				.thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

		String requestBody = """
				{
				  "refreshToken": "bad-token"
				}
				""";

		mockMvc.perform(post("/api/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Invalid credentials"));
	}

	@Test
	void logoutReturnsNoContent() throws Exception {
		String requestBody = """
				{
				  "refreshToken": "some-refresh-token"
				}
				""";

		mockMvc.perform(post("/api/auth/logout")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isNoContent());

		verify(authenticationService).logout(any(RefreshRequest.class));
	}

}
