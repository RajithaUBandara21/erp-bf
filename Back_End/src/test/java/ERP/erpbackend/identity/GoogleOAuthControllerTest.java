package ERP.erpbackend.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ERP.erpbackend.TestcontainersConfiguration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * A full {@code @SpringBootTest} slice (real Postgres/Redis via {@link TestcontainersConfiguration}),
 * not a {@code @WebMvcTest}, because the round-trip flows need real {@link User}/{@link OAuthAccount}
 * rows and a real session/token issuance path. Only {@link GoogleTokenExchangeClient} is mocked - it's
 * the one dependency that would otherwise call Google's network.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class GoogleOAuthControllerTest {

	private static final String PASSWORD = "Sunrise8";
	private static final String FRONTEND_BASE_URL = "https://app.test";

	@DynamicPropertySource
	static void googleOAuthProperties(DynamicPropertyRegistry registry) {
		registry.add("GOOGLE_CLIENT_ID", () -> "test-client-id");
		registry.add("GOOGLE_CLIENT_SECRET", () -> "test-client-secret");
		registry.add("FRONTEND_BASE_URL", () -> FRONTEND_BASE_URL);
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RegistrationService registrationService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private OAuthAccountRepository oAuthAccountRepository;

	@Autowired
	private OAuthStateService oAuthStateService;

	@MockitoBean
	private GoogleTokenExchangeClient googleTokenExchangeClient;

	@MockitoBean
	private GoogleOAuthRateLimiter googleOAuthRateLimiter;

	@BeforeEach
	void allowByDefault() {
		when(googleOAuthRateLimiter.allow(anyString())).thenReturn(true);
	}

	private User registerUser(String email) {
		TokenResponse account = registrationService.register(
				new RegisterRequest("Acme " + email, "Owner", email, PASSWORD, ClientType.WEB));
		return userRepository.findById(account.userId()).orElseThrow();
	}

	private static Authentication authenticationFor(User user) {
		AuthenticatedUser principal = new AuthenticatedUser(
				user.getId(), user.getTenantId(), user.getOrganizationId(), user.getEmail(), UUID.randomUUID());
		return new UsernamePasswordAuthenticationToken(
				principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
	}

	private static String queryParam(String location, String name) {
		return UriComponentsBuilder.fromUriString(location).build().getQueryParams().getFirst(name);
	}

	private void linkUser(User user, String providerUserId, String providerEmail) {
		OAuthAccount account = new OAuthAccount();
		account.setTenantId(user.getTenantId());
		account.setUserId(user.getId());
		account.setProvider(OAuthProvider.GOOGLE);
		account.setProviderUserId(providerUserId);
		account.setProviderEmail(providerEmail);
		oAuthAccountRepository.save(account);
	}

	@Test
	void loginUrlReturnsAnAuthorizationUrlWithClientIdAndScope() throws Exception {
		mockMvc.perform(post("/api/auth/oauth/google/login-url"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.authorizationUrl", containsString("client_id=test-client-id")))
				.andExpect(jsonPath("$.authorizationUrl", containsString("scope=openid")));
	}

	@Test
	void loginUrlReturnsTooManyRequestsWhenRateLimitExceeded() throws Exception {
		when(googleOAuthRateLimiter.allow(anyString())).thenReturn(false);

		mockMvc.perform(post("/api/auth/oauth/google/login-url"))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.message").exists());
	}

	@Test
	void linkUrlReturnsUnauthorizedWithNoAuthentication() throws Exception {
		mockMvc.perform(post("/api/auth/oauth/google/link-url"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void linkUrlReturnsAnAuthorizationUrlForTheAuthenticatedCaller() throws Exception {
		User user = registerUser("link-url@acme.test");

		mockMvc.perform(post("/api/auth/oauth/google/link-url").with(authentication(authenticationFor(user))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.authorizationUrl", containsString("client_id=test-client-id")));
	}

	@Test
	void googleErrorParamRedirectsToErrorWithoutCallingExchange() throws Exception {
		mockMvc.perform(get("/api/auth/oauth/google/callback").param("error", "access_denied"))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", FRONTEND_BASE_URL + "/sign-in?oauth=error"));

		verify(googleTokenExchangeClient, never()).exchange(any());
	}

	@Test
	void unknownOrExpiredStateRedirectsToErrorWithoutCallingExchange() throws Exception {
		mockMvc.perform(get("/api/auth/oauth/google/callback")
						.param("code", "auth-code")
						.param("state", "never-issued"))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", FRONTEND_BASE_URL + "/sign-in?oauth=error"));

		verify(googleTokenExchangeClient, never()).exchange(any());
	}

	@Test
	void unverifiedEmailRedirectsToErrorWithoutCreatingASession() throws Exception {
		String state = oAuthStateService.issue(null);
		when(googleTokenExchangeClient.exchange("auth-code"))
				.thenReturn(new GoogleIdentity("google-sub-unverified", "person@gmail.test", false));

		mockMvc.perform(get("/api/auth/oauth/google/callback")
						.param("code", "auth-code")
						.param("state", state))
				.andExpect(status().isFound())
				.andExpect(header().string("Location",
						FRONTEND_BASE_URL + "/sign-in?oauth=error&reason=email-unverified"));
	}

	@Test
	void loginWithAnUnlinkedIdentityRedirectsToNotLinked() throws Exception {
		String state = oAuthStateService.issue(null);
		when(googleTokenExchangeClient.exchange("auth-code"))
				.thenReturn(new GoogleIdentity("unlinked-sub", "nobody@gmail.test", true));

		mockMvc.perform(get("/api/auth/oauth/google/callback")
						.param("code", "auth-code")
						.param("state", state))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", FRONTEND_BASE_URL + "/sign-in?oauth=error&reason=not-linked"));
	}

	@Test
	void fullLinkThenLoginRoundTripIssuesAWorkingTokenResponse() throws Exception {
		User user = registerUser("round-trip@acme.test");
		GoogleIdentity identity = new GoogleIdentity("round-trip-sub", user.getEmail(), true);
		when(googleTokenExchangeClient.exchange("link-code")).thenReturn(identity);
		when(googleTokenExchangeClient.exchange("login-code")).thenReturn(identity);

		String linkState = oAuthStateService.issue(user.getId());
		mockMvc.perform(get("/api/auth/oauth/google/callback").param("code", "link-code").param("state", linkState))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", FRONTEND_BASE_URL + "/settings?oauth=linked"));

		OAuthAccount account = oAuthAccountRepository
				.findByTenantIdAndUserIdAndProvider(user.getTenantId(), user.getId(), OAuthProvider.GOOGLE)
				.orElseThrow();
		assertThat(account.getProviderUserId()).isEqualTo("round-trip-sub");

		String loginState = oAuthStateService.issue(null);
		MvcResult loginResult = mockMvc.perform(get("/api/auth/oauth/google/callback")
						.param("code", "login-code")
						.param("state", loginState))
				.andExpect(status().isFound())
				.andReturn();

		String location = loginResult.getResponse().getHeader("Location");
		assertThat(location).startsWith(FRONTEND_BASE_URL + "/sign-in?oauth=code&code=");
		String exchangeCode = queryParam(location, "code");

		mockMvc.perform(post("/api/auth/oauth/google/exchange")
						.contentType("application/json")
						.content("{ \"code\": \"" + exchangeCode + "\" }"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId").value(user.getId().toString()))
				.andExpect(jsonPath("$.tenantId").value(user.getTenantId().toString()));

		mockMvc.perform(post("/api/auth/oauth/google/exchange")
						.contentType("application/json")
						.content("{ \"code\": \"" + exchangeCode + "\" }"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void reLinkingAnIdentityAlreadyLinkedToADifferentUserRedirectsToAlreadyLinked() throws Exception {
		User userA = registerUser("owner-a@acme.test");
		User userB = registerUser("owner-b@acme.test");
		GoogleIdentity identity = new GoogleIdentity("shared-sub", "shared@gmail.test", true);
		when(googleTokenExchangeClient.exchange("link-a")).thenReturn(identity);
		when(googleTokenExchangeClient.exchange("link-b")).thenReturn(identity);

		String stateA = oAuthStateService.issue(userA.getId());
		mockMvc.perform(get("/api/auth/oauth/google/callback").param("code", "link-a").param("state", stateA))
				.andExpect(header().string("Location", FRONTEND_BASE_URL + "/settings?oauth=linked"));

		String stateB = oAuthStateService.issue(userB.getId());
		mockMvc.perform(get("/api/auth/oauth/google/callback").param("code", "link-b").param("state", stateB))
				.andExpect(status().isFound())
				.andExpect(header().string("Location",
						FRONTEND_BASE_URL + "/sign-in?oauth=error&reason=already-linked"));

		OAuthAccount account = oAuthAccountRepository
				.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, "shared-sub")
				.orElseThrow();
		assertThat(account.getUserId()).isEqualTo(userA.getId());
	}

	@Test
	void loginWithAnInactiveLinkedUserRedirectsToAGenericError() throws Exception {
		User user = registerUser("inactive-login@acme.test");
		GoogleIdentity identity = new GoogleIdentity("inactive-sub", user.getEmail(), true);
		when(googleTokenExchangeClient.exchange("link-code")).thenReturn(identity);
		when(googleTokenExchangeClient.exchange("login-code")).thenReturn(identity);

		String linkState = oAuthStateService.issue(user.getId());
		mockMvc.perform(get("/api/auth/oauth/google/callback").param("code", "link-code").param("state", linkState))
				.andExpect(header().string("Location", FRONTEND_BASE_URL + "/settings?oauth=linked"));

		user.setActive(false);
		userRepository.save(user);

		String loginState = oAuthStateService.issue(null);
		mockMvc.perform(get("/api/auth/oauth/google/callback").param("code", "login-code").param("state", loginState))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", FRONTEND_BASE_URL + "/sign-in?oauth=error"));
	}

	@Test
	void exchangeReturnsUnauthorizedForAnUnknownCode() throws Exception {
		mockMvc.perform(post("/api/auth/oauth/google/exchange")
						.contentType("application/json")
						.content("{ \"code\": \"does-not-exist\" }"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void exchangeReturnsTooManyRequestsWhenRateLimitExceeded() throws Exception {
		when(googleOAuthRateLimiter.allow(anyString())).thenReturn(false);

		mockMvc.perform(post("/api/auth/oauth/google/exchange")
						.contentType("application/json")
						.content("{ \"code\": \"does-not-exist\" }"))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.message").exists());
	}

	@Test
	void statusReturnsUnauthorizedWithNoAuthentication() throws Exception {
		mockMvc.perform(get("/api/auth/oauth/google"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void unlinkReturnsUnauthorizedWithNoAuthentication() throws Exception {
		mockMvc.perform(delete("/api/auth/oauth/google"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void statusReturnsNotLinkedWhenNoAccountIsLinked() throws Exception {
		User user = registerUser("status-unlinked@acme.test");

		mockMvc.perform(get("/api/auth/oauth/google").with(authentication(authenticationFor(user))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.linked").value(false))
				.andExpect(jsonPath("$.linkedEmail").doesNotExist());
	}

	@Test
	void statusReturnsLinkedWithTheStoredEmailAfterLinking() throws Exception {
		User user = registerUser("status-linked@acme.test");
		linkUser(user, "status-sub", "google-account@gmail.test");

		mockMvc.perform(get("/api/auth/oauth/google").with(authentication(authenticationFor(user))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.linked").value(true))
				.andExpect(jsonPath("$.linkedEmail").value("google-account@gmail.test"));
	}

	@Test
	void unlinkThenStatusShowsNotLinked() throws Exception {
		User user = registerUser("unlink@acme.test");
		linkUser(user, "unlink-sub", "unlink@gmail.test");

		mockMvc.perform(delete("/api/auth/oauth/google").with(authentication(authenticationFor(user))))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/auth/oauth/google").with(authentication(authenticationFor(user))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.linked").value(false));
	}

	@Test
	void unlinkWhenNothingIsLinkedIsANoOp() throws Exception {
		User user = registerUser("unlink-noop@acme.test");

		mockMvc.perform(delete("/api/auth/oauth/google").with(authentication(authenticationFor(user))))
				.andExpect(status().isNoContent());
	}

}
