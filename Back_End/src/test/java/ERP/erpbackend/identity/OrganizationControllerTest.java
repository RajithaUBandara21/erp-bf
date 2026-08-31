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
import ERP.erpbackend.organization.OrganizationDetail;
import ERP.erpbackend.organization.OrganizationListView;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(
		controllers = OrganizationController.class,
		excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class))
@Import(SecurityConfig.class)
@ImportAutoConfiguration({ServletWebSecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class OrganizationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OrganizationProvisioningService organizationProvisioningService;

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
	void listReturnsUnauthorizedWithNoAuthentication() throws Exception {
		mockMvc.perform(get("/api/organizations"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void listReturnsTheTenantOrganizationsWithPlanAndLimitForTheAuthenticatedCaller() throws Exception {
		OrganizationDetail head = new OrganizationDetail(
				UUID.randomUUID(), "Head Office", "acme-corp", true, Instant.parse("2026-01-01T00:00:00Z"));
		when(organizationProvisioningService.list(PRINCIPAL))
				.thenReturn(new OrganizationListView("Pro E-commerce", 5, List.of(head)));

		mockMvc.perform(get("/api/organizations").with(authentication(authenticatedPrincipal())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.plan").value("Pro E-commerce"))
				.andExpect(jsonPath("$.maxOrganizations").value(5))
				.andExpect(jsonPath("$.organizations[0].id").value(head.id().toString()))
				.andExpect(jsonPath("$.organizations[0].name").value("Head Office"))
				.andExpect(jsonPath("$.organizations[0].code").value("acme-corp"))
				.andExpect(jsonPath("$.organizations[0].active").value(true));
	}

	@Test
	void createReturnsUnauthorizedWithNoAuthentication() throws Exception {
		mockMvc.perform(post("/api/organizations")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"New Org\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void createRejectsABlankNameWithValidationDetails() throws Exception {
		mockMvc.perform(post("/api/organizations")
						.with(authentication(authenticatedPrincipal()))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"   \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.name").exists());
	}

	@Test
	void createReturnsTheCreatedOrganizationWith201() throws Exception {
		OrganizationDetail created = new OrganizationDetail(
				UUID.randomUUID(), "New Org", "new-org", true, Instant.parse("2026-08-30T00:00:00Z"));
		when(organizationProvisioningService.create(PRINCIPAL, "New Org")).thenReturn(created);

		mockMvc.perform(post("/api/organizations")
						.with(authentication(authenticatedPrincipal()))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"New Org\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(created.id().toString()))
				.andExpect(jsonPath("$.code").value("new-org"))
				.andExpect(jsonPath("$.active").value(true));
	}

	@Test
	void createMapsAServiceConflictThrough() throws Exception {
		when(organizationProvisioningService.create(eq(PRINCIPAL), any(String.class)))
				.thenThrow(new ResponseStatusException(HttpStatus.CONFLICT,
						"Organization limit reached for this plan. Ask your administrator to raise the limit."));

		mockMvc.perform(post("/api/organizations")
						.with(authentication(authenticatedPrincipal()))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Over Limit\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message")
						.value("Organization limit reached for this plan. Ask your administrator to raise the limit."));
	}
}
