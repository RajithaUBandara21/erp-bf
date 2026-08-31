package ERP.erpbackend.identity;

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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
		controllers = OrganizationInviteCodeController.class,
		excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class))
@Import({SecurityConfig.class, MethodSecurityConfig.class})
@ImportAutoConfiguration({ServletWebSecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class OrganizationInviteCodeControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OrganizationInviteCodeService organizationInviteCodeService;

	@MockitoBean(name = "perms")
	private PermissionChecker perms;

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
	void getReturnsUnauthorizedWithNoAuthentication() throws Exception {
		mockMvc.perform(get("/api/organizations/invite-code"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void getReturnsForbiddenWhenCallerLacksOrganizationEdit() throws Exception {
		mockMvc.perform(get("/api/organizations/invite-code").with(authentication(authenticatedPrincipal())))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").exists());
	}

	@Test
	void getReturnsTheCurrentInviteCodeWhenCallerHasOrganizationEdit() throws Exception {
		when(perms.has("organization.edit")).thenReturn(true);
		when(organizationInviteCodeService.read(PRINCIPAL)).thenReturn(new InviteCodeResponse("ABCDEFGHJK"));

		mockMvc.perform(get("/api/organizations/invite-code").with(authentication(authenticatedPrincipal())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.inviteCode").value("ABCDEFGHJK"));
	}

	@Test
	void rotateReturnsUnauthorizedWithNoAuthentication() throws Exception {
		mockMvc.perform(post("/api/organizations/invite-code/rotate"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void rotateReturnsForbiddenWhenCallerLacksOrganizationEdit() throws Exception {
		mockMvc.perform(post("/api/organizations/invite-code/rotate").with(authentication(authenticatedPrincipal())))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").exists());
	}

	@Test
	void rotateReturnsTheFreshInviteCodeWhenCallerHasOrganizationEdit() throws Exception {
		when(perms.has("organization.edit")).thenReturn(true);
		when(organizationInviteCodeService.rotate(PRINCIPAL)).thenReturn(new InviteCodeResponse("MNPQRSTVWX"));

		mockMvc.perform(post("/api/organizations/invite-code/rotate").with(authentication(authenticatedPrincipal())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.inviteCode").value("MNPQRSTVWX"));
	}
}
