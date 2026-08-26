package ERP.erpbackend.identity;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ERP.erpbackend.common.JpaAuditingConfig;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(
		controllers = SessionController.class,
		excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class))
@Import(SecurityConfig.class)
@ImportAutoConfiguration({ServletWebSecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class SessionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SessionService sessionService;

	@MockitoBean
	private JwtService jwtService;

	private static final AuthenticatedUser PRINCIPAL = new AuthenticatedUser(
			UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "ada@acme.test", UUID.randomUUID());

	private static Authentication authenticatedPrincipal() {
		return new UsernamePasswordAuthenticationToken(
				PRINCIPAL, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
	}

	@Test
	void listReturnsSessionsForTheAuthenticatedCaller() throws Exception {
		SessionResponse session = new SessionResponse(
				UUID.randomUUID(), ClientType.WEB, Instant.now(), Instant.now(), true);
		when(sessionService.listSessions(PRINCIPAL)).thenReturn(List.of(session));

		mockMvc.perform(get("/api/auth/sessions").with(authentication(authenticatedPrincipal())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(session.id().toString()))
				.andExpect(jsonPath("$[0].clientType").value("WEB"))
				.andExpect(jsonPath("$[0].current").value(true));
	}

	@Test
	void listReturnsUnauthorizedWithNoAuthentication() throws Exception {
		mockMvc.perform(get("/api/auth/sessions"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void revokeReturnsNoContentOnSuccess() throws Exception {
		UUID sessionId = UUID.randomUUID();

		mockMvc.perform(delete("/api/auth/sessions/{id}", sessionId).with(authentication(authenticatedPrincipal())))
				.andExpect(status().isNoContent());

		verify(sessionService).revokeSession(PRINCIPAL, sessionId);
	}

	@Test
	void revokeReturnsNotFoundWhenNotOwnedByTheCaller() throws Exception {
		UUID sessionId = UUID.randomUUID();
		doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"))
				.when(sessionService).revokeSession(PRINCIPAL, sessionId);

		mockMvc.perform(delete("/api/auth/sessions/{id}", sessionId).with(authentication(authenticatedPrincipal())))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Session not found"));
	}

	@Test
	void revokeReturnsUnauthorizedWithNoAuthentication() throws Exception {
		mockMvc.perform(delete("/api/auth/sessions/{id}", UUID.randomUUID()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void revokeOthersReturnsNoContentOnSuccess() throws Exception {
		mockMvc.perform(post("/api/auth/sessions/revoke-others").with(authentication(authenticatedPrincipal())))
				.andExpect(status().isNoContent());

		verify(sessionService).revokeOtherSessions(PRINCIPAL);
	}

	@Test
	void revokeOthersReturnsUnauthorizedWithNoAuthentication() throws Exception {
		mockMvc.perform(post("/api/auth/sessions/revoke-others"))
				.andExpect(status().isUnauthorized());
	}

}
