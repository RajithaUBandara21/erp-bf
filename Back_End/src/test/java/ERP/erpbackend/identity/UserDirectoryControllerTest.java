package ERP.erpbackend.identity;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
		controllers = UserDirectoryController.class,
		excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class))
@Import({SecurityConfig.class, MethodSecurityConfig.class})
@ImportAutoConfiguration({ServletWebSecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class UserDirectoryControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserDirectoryService userDirectoryService;

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
	void listReturnsOrganizationUsersWhenCallerHasUserView() throws Exception {
		UUID adaId = UUID.randomUUID();
		when(perms.has("user.view")).thenReturn(true);
		when(userDirectoryService.listActiveOrganizationMembers(PRINCIPAL.organizationId())).thenReturn(List.of(
				new UserSummaryResponse(adaId, "Ada Lovelace", "ada@acme.test"),
				new UserSummaryResponse(UUID.randomUUID(), "Grace Hopper", "grace@acme.test")));

		mockMvc.perform(get("/api/users").with(authentication(authenticatedPrincipal())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].id").value(adaId.toString()))
				.andExpect(jsonPath("$[0].fullName").value("Ada Lovelace"))
				.andExpect(jsonPath("$[0].email").value("ada@acme.test"));
	}

	@Test
	void listReturnsForbiddenWhenCallerLacksUserView() throws Exception {
		mockMvc.perform(get("/api/users").with(authentication(authenticatedPrincipal())))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").exists());
	}

	@Test
	void listReturnsUnauthorizedWithNoAuthentication() throws Exception {
		mockMvc.perform(get("/api/users"))
				.andExpect(status().isUnauthorized());
	}

}
