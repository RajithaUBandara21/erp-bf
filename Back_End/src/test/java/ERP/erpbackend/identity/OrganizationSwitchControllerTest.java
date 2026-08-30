package ERP.erpbackend.identity;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ERP.erpbackend.common.JpaAuditingConfig;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(
		controllers = OrganizationSwitchController.class,
		excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class))
@Import(SecurityConfig.class)
@ImportAutoConfiguration({ServletWebSecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class OrganizationSwitchControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OrganizationSwitchService organizationSwitchService;

	@MockitoBean
	private JwtService jwtService;

	@MockitoBean
	private RevokedSessionRegistry revokedSessionRegistry;

	private static final AuthenticatedUser PRINCIPAL = new AuthenticatedUser(
			UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "ada@acme.test", UUID.randomUUID(),
			UUID.randomUUID());

	private static Authentication authenticatedPrincipal() {
		return new UsernamePasswordAuthenticationToken(
				PRINCIPAL, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
	}

	@Test
	void membershipsReturnsReachableOrganizationsForTheAuthenticatedCaller() throws Exception {
		ReachableOrganizationResponse row = new ReachableOrganizationResponse(
				UUID.randomUUID(), "Head Office", UUID.randomUUID(), "Acme Corp", true, false);
		when(organizationSwitchService.listReachable(PRINCIPAL)).thenReturn(List.of(row));

		mockMvc.perform(get("/api/auth/memberships").with(authentication(authenticatedPrincipal())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].organizationId").value(row.organizationId().toString()))
				.andExpect(jsonPath("$[0].organizationName").value("Head Office"))
				.andExpect(jsonPath("$[0].tenantName").value("Acme Corp"))
				.andExpect(jsonPath("$[0].current").value(true))
				.andExpect(jsonPath("$[0].viaTenantAdmin").value(false));
	}

	@Test
	void membershipsReturnsUnauthorizedWithNoAuthentication() throws Exception {
		mockMvc.perform(get("/api/auth/memberships"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void switchOrganizationReturnsUnauthorizedWithNoAuthentication() throws Exception {
		mockMvc.perform(post("/api/auth/switch-organization")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"organizationId\":\"" + UUID.randomUUID() + "\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void switchOrganizationRejectsAMissingOrganizationIdWithValidationDetails() throws Exception {
		mockMvc.perform(post("/api/auth/switch-organization")
						.with(authentication(authenticatedPrincipal()))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.organizationId").exists());
	}

	@Test
	void switchOrganizationReturnsTheReissuedTokenResponseOnSuccess() throws Exception {
		UUID targetOrgId = UUID.randomUUID();
		TokenResponse tokens = new TokenResponse("new-access-token", "new-refresh-token", 900L, 2592000L,
				PRINCIPAL.userId(), UUID.randomUUID(), targetOrgId, "ada@acme.test", "Ada Owner");
		when(organizationSwitchService.switchOrganization(PRINCIPAL, targetOrgId)).thenReturn(tokens);

		mockMvc.perform(post("/api/auth/switch-organization")
						.with(authentication(authenticatedPrincipal()))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"organizationId\":\"" + targetOrgId + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value("new-access-token"))
				.andExpect(jsonPath("$.refreshToken").value("new-refresh-token"))
				.andExpect(jsonPath("$.organizationId").value(targetOrgId.toString()));
	}

	@Test
	void switchOrganizationMapsAServiceResponseStatusExceptionThrough() throws Exception {
		when(organizationSwitchService.switchOrganization(eq(PRINCIPAL), any(UUID.class)))
				.thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a Tenant Admin of this organization's tenant."));

		mockMvc.perform(post("/api/auth/switch-organization")
						.with(authentication(authenticatedPrincipal()))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"organizationId\":\"" + UUID.randomUUID() + "\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Not a Tenant Admin of this organization's tenant."));
	}
}
