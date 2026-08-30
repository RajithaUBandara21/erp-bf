package ERP.erpbackend.identity;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(
		controllers = RoleController.class,
		excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class))
@Import({SecurityConfig.class, MethodSecurityConfig.class})
@ImportAutoConfiguration({ServletWebSecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class RoleControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private RoleService roleService;

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
	void listReturnsRolesWhenCallerHasRoleView() throws Exception {
		when(perms.has("role.view")).thenReturn(true);
		when(roleService.listRoles(PRINCIPAL)).thenReturn(List.of(
				new RoleSummaryResponse(UUID.randomUUID(), "Owner", "Full access", true, 1, 95)));

		mockMvc.perform(get("/api/roles").with(authentication(authenticatedPrincipal())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Owner"))
				.andExpect(jsonPath("$[0].systemManaged").value(true))
				.andExpect(jsonPath("$[0].memberCount").value(1))
				.andExpect(jsonPath("$[0].permissionCount").value(95));
	}

	@Test
	void listReturnsForbiddenWhenCallerLacksRoleView() throws Exception {
		mockMvc.perform(get("/api/roles").with(authentication(authenticatedPrincipal())))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").exists());
	}

	@Test
	void listReturnsUnauthorizedWithNoAuthentication() throws Exception {
		mockMvc.perform(get("/api/roles"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void getReturnsRoleDetailWhenCallerHasRoleView() throws Exception {
		UUID roleId = UUID.randomUUID();
		when(perms.has("role.view")).thenReturn(true);
		when(roleService.getRole(PRINCIPAL, roleId)).thenReturn(new RoleDetailResponse(
				roleId, "Cashier", "POS only", false, 2, 3,
				List.of("pos.view", "sales.view", "product.view"), List.of()));

		mockMvc.perform(get("/api/roles/{id}", roleId).with(authentication(authenticatedPrincipal())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Cashier"))
				.andExpect(jsonPath("$.permissionCodes.length()").value(3));
	}

	@Test
	void getReturnsNotFoundWhenRoleIsNotInCallersTenant() throws Exception {
		UUID roleId = UUID.randomUUID();
		when(perms.has("role.view")).thenReturn(true);
		when(roleService.getRole(PRINCIPAL, roleId))
				.thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));

		mockMvc.perform(get("/api/roles/{id}", roleId).with(authentication(authenticatedPrincipal())))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Role not found"));
	}

	@Test
	void permissionsReturnsCatalogWhenCallerHasRoleView() throws Exception {
		when(perms.has("role.view")).thenReturn(true);
		when(roleService.listPermissions()).thenReturn(List.of(
				new PermissionResponse("product.view", "product", PermissionAction.VIEW)));

		mockMvc.perform(get("/api/permissions").with(authentication(authenticatedPrincipal())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].code").value("product.view"))
				.andExpect(jsonPath("$[0].action").value("VIEW"));
	}

	@Test
	void permissionsReturnsForbiddenWhenCallerLacksRoleView() throws Exception {
		mockMvc.perform(get("/api/permissions").with(authentication(authenticatedPrincipal())))
				.andExpect(status().isForbidden());
	}

	@Test
	void createReturns201WithRoleDetailWhenCallerHasRoleCreate() throws Exception {
		UUID roleId = UUID.randomUUID();
		when(perms.has("role.create")).thenReturn(true);
		when(roleService.createRole(eq(PRINCIPAL), any(CreateRoleRequest.class))).thenReturn(new RoleDetailResponse(
				roleId, "Cashier", "POS only", false, 0, 1, List.of("pos.view"), List.of()));

		mockMvc.perform(post("/api/roles").with(authentication(authenticatedPrincipal()))
						.contentType("application/json")
						.content("""
								{ "name": "Cashier", "description": "POS only", "permissionCodes": ["pos.view"] }
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(roleId.toString()))
				.andExpect(jsonPath("$.systemManaged").value(false));
	}

	@Test
	void createReturns400WhenNameIsBlank() throws Exception {
		when(perms.has("role.create")).thenReturn(true);

		mockMvc.perform(post("/api/roles").with(authentication(authenticatedPrincipal()))
						.contentType("application/json")
						.content("""
								{ "name": "  ", "permissionCodes": [] }
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.name").exists());
	}

	@Test
	void createReturns403WhenCallerLacksRoleCreate() throws Exception {
		mockMvc.perform(post("/api/roles").with(authentication(authenticatedPrincipal()))
						.contentType("application/json")
						.content("""
								{ "name": "Cashier", "permissionCodes": [] }
								"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void updateReturns200WhenCallerHasRoleEdit() throws Exception {
		UUID roleId = UUID.randomUUID();
		when(perms.has("role.edit")).thenReturn(true);
		when(roleService.updateRole(eq(PRINCIPAL), eq(roleId), any(UpdateRoleRequest.class)))
				.thenReturn(new RoleDetailResponse(roleId, "Cashier+", null, false, 0, 0, List.of(), List.of()));

		mockMvc.perform(put("/api/roles/{id}", roleId).with(authentication(authenticatedPrincipal()))
						.contentType("application/json")
						.content("""
								{ "name": "Cashier+", "permissionCodes": [] }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Cashier+"));
	}

	@Test
	void updateReturns409WhenRoleIsSystemManaged() throws Exception {
		UUID roleId = UUID.randomUUID();
		when(perms.has("role.edit")).thenReturn(true);
		when(roleService.updateRole(eq(PRINCIPAL), eq(roleId), any(UpdateRoleRequest.class)))
				.thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "System roles cannot be modified or deleted."));

		mockMvc.perform(put("/api/roles/{id}", roleId).with(authentication(authenticatedPrincipal()))
						.contentType("application/json")
						.content("""
								{ "name": "Owner", "permissionCodes": [] }
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("System roles cannot be modified or deleted."));
	}

	@Test
	void deleteReturns204WhenCallerHasRoleDelete() throws Exception {
		UUID roleId = UUID.randomUUID();
		when(perms.has("role.delete")).thenReturn(true);

		mockMvc.perform(delete("/api/roles/{id}", roleId).with(authentication(authenticatedPrincipal())))
				.andExpect(status().isNoContent());

		verify(roleService).deleteRole(PRINCIPAL, roleId);
	}

	@Test
	void deleteReturns409WhenRoleStillHasMembers() throws Exception {
		UUID roleId = UUID.randomUUID();
		when(perms.has("role.delete")).thenReturn(true);
		doThrow(new ResponseStatusException(HttpStatus.CONFLICT,
				"This role still has members. Reassign them before deleting it."))
				.when(roleService).deleteRole(PRINCIPAL, roleId);

		mockMvc.perform(delete("/api/roles/{id}", roleId).with(authentication(authenticatedPrincipal())))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").exists());
	}

	@Test
	void deleteReturns403WhenCallerLacksRoleDelete() throws Exception {
		mockMvc.perform(delete("/api/roles/{id}", UUID.randomUUID()).with(authentication(authenticatedPrincipal())))
				.andExpect(status().isForbidden());
	}

	@Test
	void assignMemberReturns204WhenCallerHasRoleEdit() throws Exception {
		UUID roleId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		when(perms.has("role.edit")).thenReturn(true);

		mockMvc.perform(post("/api/roles/{id}/members", roleId).with(authentication(authenticatedPrincipal()))
						.contentType("application/json")
						.content("{ \"userId\": \"" + userId + "\" }"))
				.andExpect(status().isNoContent());

		verify(roleService).assignMember(PRINCIPAL, roleId, userId);
	}

	@Test
	void assignMemberReturns400WhenUserIdMissing() throws Exception {
		when(perms.has("role.edit")).thenReturn(true);

		mockMvc.perform(post("/api/roles/{id}/members", UUID.randomUUID())
						.with(authentication(authenticatedPrincipal()))
						.contentType("application/json").content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.userId").exists());
	}

	@Test
	void assignMemberReturns403WhenCallerLacksRoleEdit() throws Exception {
		mockMvc.perform(post("/api/roles/{id}/members", UUID.randomUUID())
						.with(authentication(authenticatedPrincipal()))
						.contentType("application/json")
						.content("{ \"userId\": \"" + UUID.randomUUID() + "\" }"))
				.andExpect(status().isForbidden());
	}

	@Test
	void assignMemberReturns403WhenGrantingOwnerWithoutOwner() throws Exception {
		UUID roleId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		when(perms.has("role.edit")).thenReturn(true);
		doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only an Owner can grant the Owner role."))
				.when(roleService).assignMember(PRINCIPAL, roleId, userId);

		mockMvc.perform(post("/api/roles/{id}/members", roleId).with(authentication(authenticatedPrincipal()))
						.contentType("application/json")
						.content("{ \"userId\": \"" + userId + "\" }"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Only an Owner can grant the Owner role."));
	}

	@Test
	void unassignMemberReturns204WhenCallerHasRoleEdit() throws Exception {
		UUID roleId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		when(perms.has("role.edit")).thenReturn(true);

		mockMvc.perform(delete("/api/roles/{id}/members/{userId}", roleId, userId)
						.with(authentication(authenticatedPrincipal())))
				.andExpect(status().isNoContent());

		verify(roleService).unassignMember(PRINCIPAL, roleId, userId);
	}

	@Test
	void unassignMemberReturns409ForLastOwner() throws Exception {
		UUID roleId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		when(perms.has("role.edit")).thenReturn(true);
		doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "The last Owner cannot be removed."
				+ " Assign the Owner role to another user first."))
				.when(roleService).unassignMember(PRINCIPAL, roleId, userId);

		mockMvc.perform(delete("/api/roles/{id}/members/{userId}", roleId, userId)
						.with(authentication(authenticatedPrincipal())))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").exists());
	}

}
