package ERP.erpbackend.identity;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ERP.erpbackend.common.JpaAuditingConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(
		controllers = SelfJoinController.class,
		excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class))
@Import(SecurityConfig.class)
@ImportAutoConfiguration({ServletWebSecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class SelfJoinControllerTest {

	private static final String ACCEPTED_MESSAGE =
			"If those details are valid, check your email for a link to finish your request.";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SelfJoinService selfJoinService;

	@MockitoBean
	private SelfJoinRateLimiter selfJoinRateLimiter;

	@MockitoBean
	private JwtService jwtService;

	@MockitoBean
	private RevokedSessionRegistry revokedSessionRegistry;

	@BeforeEach
	void allowByDefault() {
		when(selfJoinRateLimiter.allow(anyString())).thenReturn(true);
	}

	private static String joinBody(String password, String inviteCode) {
		return """
				{
				  "email": "newbie@acme.test",
				  "password": "%s",
				  "fullName": "New Bie",
				  "inviteCode": "%s"
				}
				""".formatted(password, inviteCode);
	}

	@Test
	void returns202WithTheFixedBodyOnSuccess() throws Exception {
		when(selfJoinService.requestJoin(any(JoinRequest.class)))
				.thenReturn(new SelfJoinResponse(ACCEPTED_MESSAGE));

		mockMvc.perform(post("/api/auth/join")
						.contentType(MediaType.APPLICATION_JSON)
						.content(joinBody("Sunrise8", "ABCDEFGHJK")))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.message").value(ACCEPTED_MESSAGE));
	}

	@Test
	void returns404WhenTheInviteCodeIsNotValid() throws Exception {
		when(selfJoinService.requestJoin(any(JoinRequest.class)))
				.thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "That invite code is not valid."));

		mockMvc.perform(post("/api/auth/join")
						.contentType(MediaType.APPLICATION_JSON)
						.content(joinBody("Sunrise8", "BADCODE999")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("That invite code is not valid."));
	}

	@Test
	void rejectsAWeakPasswordWithValidationDetails() throws Exception {
		mockMvc.perform(post("/api/auth/join")
						.contentType(MediaType.APPLICATION_JSON)
						.content(joinBody("weak", "ABCDEFGHJK")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.password").exists());

		verify(selfJoinService, never()).requestJoin(any());
	}

	@Test
	void rejectsAMissingInviteCodeWithValidationDetails() throws Exception {
		String body = """
				{
				  "email": "newbie@acme.test",
				  "password": "Sunrise8",
				  "fullName": "New Bie"
				}
				""";

		mockMvc.perform(post("/api/auth/join")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.inviteCode").exists());

		verify(selfJoinService, never()).requestJoin(any());
	}

	@Test
	void returns429WhenRateLimitExceeded() throws Exception {
		when(selfJoinRateLimiter.allow(anyString())).thenReturn(false);

		mockMvc.perform(post("/api/auth/join")
						.contentType(MediaType.APPLICATION_JSON)
						.content(joinBody("Sunrise8", "ABCDEFGHJK")))
				.andExpect(status().isTooManyRequests());

		verify(selfJoinService, never()).requestJoin(any());
	}

	@Test
	void verifyEmailReturns200WithMessageAndOrganizationName() throws Exception {
		when(selfJoinService.verifyEmail("a-token")).thenReturn(new VerifyEmailResponse(
				"Email verified. Your request to join Head Office is now awaiting approval.", "Head Office"));

		mockMvc.perform(post("/api/auth/verify-email")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"token\":\"a-token\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message")
						.value("Email verified. Your request to join Head Office is now awaiting approval."))
				.andExpect(jsonPath("$.organizationName").value("Head Office"));
	}

	@Test
	void verifyEmailRejectsABlankTokenWithValidationDetails() throws Exception {
		mockMvc.perform(post("/api/auth/verify-email")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"token\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.token").exists());

		verify(selfJoinService, never()).verifyEmail(anyString());
	}

	@Test
	void verifyEmailReturns429WhenRateLimitExceeded() throws Exception {
		when(selfJoinRateLimiter.allow(anyString())).thenReturn(false);

		mockMvc.perform(post("/api/auth/verify-email")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"token\":\"a-token\"}"))
				.andExpect(status().isTooManyRequests());

		verify(selfJoinService, never()).verifyEmail(anyString());
	}

}
