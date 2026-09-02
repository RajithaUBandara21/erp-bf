package ERP.erpbackend.identity;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
		controllers = MembershipApprovalController.class,
		excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class))
@Import({SecurityConfig.class, MethodSecurityConfig.class})
@ImportAutoConfiguration({ServletWebSecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class MembershipApprovalControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private MembershipApprovalService membershipApprovalService;

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
	void listReturnsUnauthorizedWithNoAuthentication() throws Exception {
		mockMvc.perform(get("/api/organizations/join-requests"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void listReturnsForbiddenWhenCallerLacksUserApprove() throws Exception {
		mockMvc.perform(get("/api/organizations/join-requests").with(authentication(authenticatedPrincipal())))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").exists());
	}

	@Test
	void listReturnsThePendingQueueWhenCallerHasUserApprove() throws Exception {
		UUID membershipId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		when(perms.has("user.approve")).thenReturn(true);
		when(membershipApprovalService.listPending(PRINCIPAL)).thenReturn(List.of(
				new PendingJoinRequestResponse(membershipId, userId, "Joe Joiner", "joe@acme.test",
						Instant.parse("2026-08-01T10:15:00Z"))));

		mockMvc.perform(get("/api/organizations/join-requests").with(authentication(authenticatedPrincipal())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].membershipId").value(membershipId.toString()))
				.andExpect(jsonPath("$[0].userId").value(userId.toString()))
				.andExpect(jsonPath("$[0].fullName").value("Joe Joiner"))
				.andExpect(jsonPath("$[0].email").value("joe@acme.test"))
				.andExpect(jsonPath("$[0].requestedAt").value("2026-08-01T10:15:00Z"));
	}

	@Test
	void listReturnsAnEmptyArrayWhenTheQueueIsEmpty() throws Exception {
		when(perms.has("user.approve")).thenReturn(true);
		when(membershipApprovalService.listPending(PRINCIPAL)).thenReturn(List.of());

		mockMvc.perform(get("/api/organizations/join-requests").with(authentication(authenticatedPrincipal())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void approveReturnsUnauthorizedWithNoAuthentication() throws Exception {
		mockMvc.perform(post("/api/organizations/join-requests/{id}/approve", UUID.randomUUID())
						.contentType("application/json")
						.content("{ \"roleId\": \"" + UUID.randomUUID() + "\" }"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void approveReturnsForbiddenWhenCallerLacksUserApprove() throws Exception {
		mockMvc.perform(post("/api/organizations/join-requests/{id}/approve", UUID.randomUUID())
						.with(authentication(authenticatedPrincipal()))
						.contentType("application/json")
						.content("{ \"roleId\": \"" + UUID.randomUUID() + "\" }"))
				.andExpect(status().isForbidden());
	}

	@Test
	void approveReturns400WhenRoleIdMissing() throws Exception {
		when(perms.has("user.approve")).thenReturn(true);

		mockMvc.perform(post("/api/organizations/join-requests/{id}/approve", UUID.randomUUID())
						.with(authentication(authenticatedPrincipal()))
						.contentType("application/json").content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.roleId").exists());
	}

	@Test
	void approveReturns204OnSuccess() throws Exception {
		UUID membershipId = UUID.randomUUID();
		UUID roleId = UUID.randomUUID();
		when(perms.has("user.approve")).thenReturn(true);

		mockMvc.perform(post("/api/organizations/join-requests/{id}/approve", membershipId)
						.with(authentication(authenticatedPrincipal()))
						.contentType("application/json")
						.content("{ \"roleId\": \"" + roleId + "\" }"))
				.andExpect(status().isNoContent());

		verify(membershipApprovalService).approve(PRINCIPAL, membershipId, roleId);
	}

	@Test
	void rejectReturnsUnauthorizedWithNoAuthentication() throws Exception {
		mockMvc.perform(post("/api/organizations/join-requests/{id}/reject", UUID.randomUUID()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void rejectReturnsForbiddenWhenCallerLacksUserApprove() throws Exception {
		mockMvc.perform(post("/api/organizations/join-requests/{id}/reject", UUID.randomUUID())
						.with(authentication(authenticatedPrincipal())))
				.andExpect(status().isForbidden());
	}

	@Test
	void rejectReturns204OnSuccess() throws Exception {
		UUID membershipId = UUID.randomUUID();
		when(perms.has("user.approve")).thenReturn(true);

		mockMvc.perform(post("/api/organizations/join-requests/{id}/reject", membershipId)
						.with(authentication(authenticatedPrincipal())))
				.andExpect(status().isNoContent());

		verify(membershipApprovalService).reject(PRINCIPAL, membershipId);
	}
}
