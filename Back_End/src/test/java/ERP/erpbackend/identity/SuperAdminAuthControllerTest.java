package ERP.erpbackend.identity;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Only the {@code @PreAuthorize} gate on {@code change-password} - {@link SuperAdminAuthenticationServiceTest}
 * covers the business logic directly. Mirrors {@code AuditLogControllerTest}'s slice-test shape.
 */
@WebMvcTest(
		controllers = SuperAdminAuthController.class,
		excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class))
@Import({SecurityConfig.class, MethodSecurityConfig.class})
@ImportAutoConfiguration({ServletWebSecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class SuperAdminAuthControllerTest {

	private static final String REQUEST_BODY = "{\"currentPassword\":\"Sunrise8\",\"newPassword\":\"Newpass9\"}";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SuperAdminAuthenticationService superAdminAuthenticationService;

	@MockitoBean
	private SuperAdminLoginRateLimiter superAdminLoginRateLimiter;

	@MockitoBean
	private SuperAdminOtpVerifyRateLimiter superAdminOtpVerifyRateLimiter;

	@MockitoBean(name = "perms")
	private PermissionChecker perms;

	@MockitoBean
	private JwtService jwtService;

	@MockitoBean
	private RevokedSessionRegistry revokedSessionRegistry;

	private static Authentication authenticatedAs(AuthenticatedUser principal) {
		return new UsernamePasswordAuthenticationToken(
				principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
	}

	@Test
	void changePasswordReachesTheServiceWhenCallerIsSuperAdminPrincipal() throws Exception {
		AuthenticatedUser superAdmin = AuthenticatedUser.superAdmin(UUID.randomUUID(), "root@platform.test", true);
		when(perms.isSuperAdminPrincipal()).thenReturn(true);
		when(superAdminAuthenticationService.changePassword(eq(superAdmin), any(SuperAdminChangePasswordRequest.class)))
				.thenReturn(new SuperAdminTokenResponse("token", 1800L, superAdmin.userId(), superAdmin.email(),
						"Platform Super Admin", false));

		mockMvc.perform(post("/api/auth/super-admin/change-password")
						.with(authentication(authenticatedAs(superAdmin)))
						.contentType(MediaType.APPLICATION_JSON)
						.content(REQUEST_BODY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.mustChangePassword").value(false));
	}

	@Test
	void changePasswordIsDeniedForAnOrdinaryMembershipScopedCaller() throws Exception {
		AuthenticatedUser ordinaryCaller = new AuthenticatedUser(
				UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "ada@acme.test", UUID.randomUUID(),
				UUID.randomUUID());
		when(perms.isSuperAdminPrincipal()).thenReturn(false);

		mockMvc.perform(post("/api/auth/super-admin/change-password")
						.with(authentication(authenticatedAs(ordinaryCaller)))
						.contentType(MediaType.APPLICATION_JSON)
						.content(REQUEST_BODY))
				.andExpect(status().isForbidden());
	}

}
