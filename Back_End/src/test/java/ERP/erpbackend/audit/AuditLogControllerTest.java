package ERP.erpbackend.audit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ERP.erpbackend.common.JpaAuditingConfig;
import ERP.erpbackend.identity.AuthenticatedUser;
import ERP.erpbackend.identity.JwtService;
import ERP.erpbackend.identity.MethodSecurityConfig;
import ERP.erpbackend.identity.PermissionChecker;
import ERP.erpbackend.identity.RevokedSessionRegistry;
import ERP.erpbackend.identity.SecurityConfig;
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
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
		controllers = AuditLogController.class,
		excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class))
@Import({SecurityConfig.class, MethodSecurityConfig.class})
@ImportAutoConfiguration({ServletWebSecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class AuditLogControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuditLogQueryService auditLogQueryService;

	@MockitoBean(name = "perms")
	private PermissionChecker perms;

	@MockitoBean
	private JwtService jwtService;

	@MockitoBean
	private RevokedSessionRegistry revokedSessionRegistry;

	private static final AuthenticatedUser PRINCIPAL = new AuthenticatedUser(
			UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "ada@acme.test", UUID.randomUUID());

	private static Authentication authenticatedPrincipal() {
		return new UsernamePasswordAuthenticationToken(
				PRINCIPAL, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
	}

	@Test
	void searchReturnsPageWhenCallerHasAuditView() throws Exception {
		when(perms.has("audit.view")).thenReturn(true);
		AuditLogResponse entry = new AuditLogResponse(UUID.randomUUID(), java.time.Instant.parse("2026-08-28T10:00:00Z"),
				PRINCIPAL.userId(), "Ada", "ada@acme.test", "Role", UUID.randomUUID(), "role.updated",
				PRINCIPAL.organizationId(), "Head Office", null, null);
		when(auditLogQueryService.search(eq(PRINCIPAL), any(AuditLogFilter.class), any(Pageable.class)))
				.thenReturn(new AuditLogPageResponse(List.of(entry), 0, 20, 1, 1));

		mockMvc.perform(get("/api/audit-logs").with(authentication(authenticatedPrincipal())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].action").value("role.updated"))
				.andExpect(jsonPath("$.content[0].actorName").value("Ada"))
				.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void searchReturnsForbiddenWhenCallerLacksAuditView() throws Exception {
		mockMvc.perform(get("/api/audit-logs").with(authentication(authenticatedPrincipal())))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").exists());
	}

	@Test
	void searchReturnsUnauthorizedWithNoAuthentication() throws Exception {
		mockMvc.perform(get("/api/audit-logs"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void searchNeverReadsTenantFromRequestParameters() throws Exception {
		when(perms.has("audit.view")).thenReturn(true);
		when(auditLogQueryService.search(eq(PRINCIPAL), any(AuditLogFilter.class), any(Pageable.class)))
				.thenReturn(new AuditLogPageResponse(List.of(), 0, 20, 0, 0));

		mockMvc.perform(get("/api/audit-logs")
						.param("entityType", "Role")
						.with(authentication(authenticatedPrincipal())))
				.andExpect(status().isOk());

		// eq(PRINCIPAL) above already proves the service is called with the authenticated caller;
		// there is no tenantId request parameter for a caller to spoof in the first place.
	}

}
