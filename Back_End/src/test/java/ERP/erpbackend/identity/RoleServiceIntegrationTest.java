package ERP.erpbackend.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ERP.erpbackend.TestcontainersConfiguration;
import ERP.erpbackend.audit.AuditLog;
import ERP.erpbackend.audit.AuditLogRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RoleServiceIntegrationTest {

	private static final String PASSWORD = "Sunrise8";

	@Autowired
	private RoleService roleService;

	@Autowired
	private RegistrationService registrationService;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private RolePermissionRepository rolePermissionRepository;

	@Autowired
	private UserRoleRepository userRoleRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AuditLogRepository auditLogRepository;

	private List<AuditLog> auditLogsFor(UUID entityId, String action) {
		return auditLogRepository.findAll().stream()
				.filter(entry -> entityId.equals(entry.getEntityId()) && action.equals(entry.getAction()))
				.toList();
	}

	private AuthenticatedUser registerOwner(String email) {
		TokenResponse response = registrationService.register(
				new RegisterRequest("Acme " + email, "Ada Owner", email, PASSWORD, ClientType.WEB));
		return jwtService.parseAccessToken(response.accessToken()).orElseThrow();
	}

	private User createTenantUser(AuthenticatedUser owner, String email) {
		User user = new User();
		user.setTenantId(owner.tenantId());
		user.setOrganizationId(owner.organizationId());
		user.setEmail(email);
		user.setPasswordHash("hashed-password");
		user.setFullName(email);
		return userRepository.save(user);
	}

	private static AuthenticatedUser asCaller(User user) {
		return new AuthenticatedUser(user.getId(), user.getTenantId(), user.getOrganizationId(),
				user.getEmail(), UUID.randomUUID());
	}

	private AuthenticatedUser administrator(AuthenticatedUser owner, String email) {
		User user = createTenantUser(owner, email);
		roleService.assignMember(owner, roleIdNamed(owner, "Administrator"), user.getId());
		return asCaller(user);
	}

	private UUID roleIdNamed(AuthenticatedUser caller, String name) {
		return roleService.listRoles(caller).stream()
				.filter(role -> role.name().equals(name)).findFirst().orElseThrow().id();
	}

	private UUID ownerRoleId(AuthenticatedUser caller) {
		return roleIdNamed(caller, "Owner");
	}

	private void assertStatus(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, HttpStatus status) {
		assertThatThrownBy(call).isInstanceOfSatisfying(ResponseStatusException.class,
				ex -> assertThat(ex.getStatusCode()).isEqualTo(status));
	}

	private void assertConflict(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
		assertStatus(call, HttpStatus.CONFLICT);
	}

	@Test
	void listRolesReturnsOnlyTheCallersTenantRolesWithCounts() {
		AuthenticatedUser callerA = registerOwner("role-list-a@acme.test");
		registerOwner("role-list-b@acme.test");

		var roles = roleService.listRoles(callerA);

		assertThat(roles).extracting(RoleSummaryResponse::name)
				.containsExactlyInAnyOrder("Owner", "Administrator", "Viewer");
		assertThat(roles).allMatch(RoleSummaryResponse::systemManaged);

		RoleSummaryResponse owner = roles.stream()
				.filter(role -> role.name().equals("Owner")).findFirst().orElseThrow();
		assertThat(owner.permissionCount()).isEqualTo(95);
		assertThat(owner.memberCount()).isEqualTo(1);

		RoleSummaryResponse viewer = roles.stream()
				.filter(role -> role.name().equals("Viewer")).findFirst().orElseThrow();
		assertThat(viewer.permissionCount()).isEqualTo(19);
		assertThat(viewer.memberCount()).isZero();
	}

	@Test
	void getRoleForAnotherTenantsRoleIsNotFound() {
		AuthenticatedUser callerA = registerOwner("role-detail-a@acme.test");
		AuthenticatedUser callerB = registerOwner("role-detail-b@acme.test");

		UUID tenantBOwnerRoleId = roleService.listRoles(callerB).stream()
				.filter(role -> role.name().equals("Owner")).findFirst().orElseThrow().id();

		assertThatThrownBy(() -> roleService.getRole(callerA, tenantBOwnerRoleId))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("Role not found");
	}

	@Test
	void getRoleReturnsSortedPermissionCodesAndMembers() {
		AuthenticatedUser caller = registerOwner("role-detail-codes@acme.test");

		RoleDetailResponse detail = roleService.getRole(caller, ownerRoleId(caller));

		assertThat(detail.permissionCodes()).hasSize(95).isSorted();
		assertThat(detail.members()).extracting(RoleMemberResponse::email)
				.containsExactly("role-detail-codes@acme.test");
	}

	@Test
	void createUpdateDeleteRoundTripKeepsGrantsConsistent() {
		AuthenticatedUser caller = registerOwner("role-crud@acme.test");

		RoleDetailResponse created = roleService.createRole(caller,
				new CreateRoleRequest("Cashier", "POS staff", List.of("pos.view", "sales.view")));
		assertThat(created.systemManaged()).isFalse();
		assertThat(created.permissionCodes()).containsExactly("pos.view", "sales.view");

		AuditLog createdLog = auditLogsFor(created.id(), "role.created").stream().findFirst().orElseThrow();
		assertThat(createdLog.getEntityType()).isEqualTo("Role");
		assertThat(createdLog.getBeforeValue()).isNull();
		assertThat(createdLog.getAfterValue()).contains("Cashier").contains("pos.view");

		RoleDetailResponse updated = roleService.updateRole(caller, created.id(),
				new UpdateRoleRequest("Cashier Lead", null, List.of("pos.view", "sales.view", "product.view")));
		assertThat(updated.name()).isEqualTo("Cashier Lead");
		assertThat(updated.permissionCodes()).containsExactly("pos.view", "product.view", "sales.view");
		assertThat(rolePermissionRepository.findByRoleId(created.id())).hasSize(3);

		AuditLog updatedLog = auditLogsFor(created.id(), "role.updated").stream().findFirst().orElseThrow();
		assertThat(updatedLog.getBeforeValue()).contains("Cashier").doesNotContain("Cashier Lead");
		assertThat(updatedLog.getAfterValue()).contains("Cashier Lead").contains("product.view");

		roleService.deleteRole(caller, created.id());
		assertThat(roleService.listRoles(caller)).extracting(RoleSummaryResponse::name)
				.doesNotContain("Cashier Lead");
		assertThat(rolePermissionRepository.findByRoleId(created.id())).isEmpty();

		AuditLog deletedLog = auditLogsFor(created.id(), "role.deleted").stream().findFirst().orElseThrow();
		assertThat(deletedLog.getBeforeValue()).contains("Cashier Lead").contains("product.view");
		assertThat(deletedLog.getAfterValue()).isNull();
	}

	@Test
	void renamingACustomRoleToAnotherRolesNameIsConflict() {
		AuthenticatedUser caller = registerOwner("role-update-dup@acme.test");
		roleService.createRole(caller, new CreateRoleRequest("Cashier", null, List.of()));
		RoleDetailResponse manager = roleService.createRole(caller,
				new CreateRoleRequest("Manager", null, List.of()));

		assertConflict(() -> roleService.updateRole(caller, manager.id(),
				new UpdateRoleRequest("Cashier", null, List.of())));
	}

	@Test
	void renamingARoleToItsOwnNameSucceeds() {
		AuthenticatedUser caller = registerOwner("role-update-noop@acme.test");
		RoleDetailResponse cashier = roleService.createRole(caller,
				new CreateRoleRequest("Cashier", null, List.of("pos.view")));

		RoleDetailResponse updated = roleService.updateRole(caller, cashier.id(),
				new UpdateRoleRequest("Cashier", "Now with sales", List.of("pos.view", "sales.view")));

		assertThat(updated.name()).isEqualTo("Cashier");
		assertThat(updated.description()).isEqualTo("Now with sales");
		assertThat(updated.permissionCodes()).containsExactly("pos.view", "sales.view");
	}

	@Test
	void unknownPermissionCodeOnUpdateIsBadRequest() {
		AuthenticatedUser caller = registerOwner("role-update-badcode@acme.test");
		RoleDetailResponse cashier = roleService.createRole(caller,
				new CreateRoleRequest("Cashier", null, List.of("pos.view")));

		assertThatThrownBy(() -> roleService.updateRole(caller, cashier.id(),
				new UpdateRoleRequest("Cashier", null, List.of("pos.view", "bogus.code"))))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST))
				.hasMessageContaining("bogus.code");
	}

	@Test
	void editingASystemRoleIsConflict() {
		AuthenticatedUser caller = registerOwner("role-sys-edit@acme.test");
		UUID ownerRoleId = ownerRoleId(caller);

		assertConflict(() -> roleService.updateRole(caller, ownerRoleId,
				new UpdateRoleRequest("Owner", null, List.of())));
	}

	@Test
	void deletingASystemRoleIsConflict() {
		AuthenticatedUser caller = registerOwner("role-sys-delete@acme.test");
		UUID ownerRoleId = ownerRoleId(caller);

		assertConflict(() -> roleService.deleteRole(caller, ownerRoleId));
	}

	@Test
	void duplicateRoleNameIsConflict() {
		AuthenticatedUser caller = registerOwner("role-dup@acme.test");
		roleService.createRole(caller, new CreateRoleRequest("Cashier", null, List.of()));

		assertConflict(() -> roleService.createRole(caller, new CreateRoleRequest("Cashier", null, List.of())));
	}

	@Test
	void unknownPermissionCodeIsBadRequest() {
		AuthenticatedUser caller = registerOwner("role-badcode@acme.test");

		assertThatThrownBy(() -> roleService.createRole(caller,
				new CreateRoleRequest("Cashier", null, List.of("pos.view", "bogus.code"))))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST))
				.hasMessageContaining("bogus.code");
	}

	@Test
	void deletingARoleThatStillHasMembersIsConflict() {
		AuthenticatedUser caller = registerOwner("role-has-members@acme.test");
		RoleDetailResponse role = roleService.createRole(caller,
				new CreateRoleRequest("Cashier", null, List.of("pos.view")));

		UserRole assignment = new UserRole();
		assignment.setTenantId(caller.tenantId());
		assignment.setUserId(caller.userId());
		assignment.setRoleId(role.id());
		userRoleRepository.save(assignment);

		assertConflict(() -> roleService.deleteRole(caller, role.id()));
	}

	@Test
	void assignThenUnassignMemberRoundTrip() {
		AuthenticatedUser owner = registerOwner("member-roundtrip@acme.test");
		User teammate = createTenantUser(owner, "teammate@member-roundtrip.test");
		UUID viewerRoleId = roleIdNamed(owner, "Viewer");

		roleService.assignMember(owner, viewerRoleId, teammate.getId());
		assertThat(roleService.getRole(owner, viewerRoleId).members())
				.extracting(RoleMemberResponse::userId).containsExactly(teammate.getId());
		AuditLog assignedLog = auditLogsFor(viewerRoleId, "role.member_assigned").stream().findFirst().orElseThrow();
		assertThat(assignedLog.getAfterValue()).contains(teammate.getId().toString());

		roleService.unassignMember(owner, viewerRoleId, teammate.getId());
		assertThat(roleService.getRole(owner, viewerRoleId).members()).isEmpty();
		AuditLog unassignedLog = auditLogsFor(viewerRoleId, "role.member_unassigned").stream()
				.findFirst().orElseThrow();
		assertThat(unassignedLog.getBeforeValue()).contains(teammate.getId().toString());
	}

	@Test
	void unassigningANonMemberProducesNoAuditRow() {
		AuthenticatedUser owner = registerOwner("member-unassign-noop@acme.test");
		User teammate = createTenantUser(owner, "teammate@member-unassign-noop.test");
		UUID viewerRoleId = roleIdNamed(owner, "Viewer");

		roleService.unassignMember(owner, viewerRoleId, teammate.getId());

		assertThat(auditLogsFor(viewerRoleId, "role.member_unassigned")).isEmpty();
	}

	@Test
	void duplicateAssignIsIdempotent() {
		AuthenticatedUser owner = registerOwner("member-idempotent@acme.test");
		User teammate = createTenantUser(owner, "teammate@member-idempotent.test");
		UUID viewerRoleId = roleIdNamed(owner, "Viewer");

		roleService.assignMember(owner, viewerRoleId, teammate.getId());
		roleService.assignMember(owner, viewerRoleId, teammate.getId());

		assertThat(userRoleRepository.findByRoleId(viewerRoleId)).hasSize(1);
		assertThat(auditLogsFor(viewerRoleId, "role.member_assigned")).hasSize(1);
	}

	@Test
	void assigningAUserFromAnotherTenantIsNotFound() {
		AuthenticatedUser ownerA = registerOwner("member-tenant-a@acme.test");
		AuthenticatedUser ownerB = registerOwner("member-tenant-b@acme.test");
		UUID viewerRoleA = roleIdNamed(ownerA, "Viewer");

		assertStatus(() -> roleService.assignMember(ownerA, viewerRoleA, ownerB.userId()), HttpStatus.NOT_FOUND);
	}

	@Test
	void assigningToAnotherTenantsRoleIsNotFound() {
		AuthenticatedUser ownerA = registerOwner("member-role-a@acme.test");
		AuthenticatedUser ownerB = registerOwner("member-role-b@acme.test");
		User teammate = createTenantUser(ownerA, "teammate@member-role-a.test");
		UUID viewerRoleB = roleIdNamed(ownerB, "Viewer");

		assertStatus(() -> roleService.assignMember(ownerA, viewerRoleB, teammate.getId()), HttpStatus.NOT_FOUND);
	}

	@Test
	void nonOwnerCannotGrantTheOwnerRole() {
		AuthenticatedUser owner = registerOwner("owner-grant@acme.test");
		User nonOwner = createTenantUser(owner, "nonowner@owner-grant.test");
		User target = createTenantUser(owner, "target@owner-grant.test");
		UUID ownerRoleId = ownerRoleId(owner);

		assertStatus(() -> roleService.assignMember(asCaller(nonOwner), ownerRoleId, target.getId()),
				HttpStatus.FORBIDDEN);
	}

	@Test
	void theLastOwnerCannotBeUnassignedButANonLastOwnerCan() {
		AuthenticatedUser owner = registerOwner("last-owner@acme.test");
		UUID ownerRoleId = ownerRoleId(owner);

		assertConflict(() -> roleService.unassignMember(owner, ownerRoleId, owner.userId()));

		User secondOwner = createTenantUser(owner, "second@last-owner.test");
		roleService.assignMember(owner, ownerRoleId, secondOwner.getId());

		roleService.unassignMember(owner, ownerRoleId, secondOwner.getId());
		assertThat(userRoleRepository.countByRoleId(ownerRoleId)).isEqualTo(1);
	}

	@Test
	void administratorCannotCreateARoleGrantingBillingCodesItLacks() {
		AuthenticatedUser owner = registerOwner("grant-billing-create@acme.test");
		AuthenticatedUser admin = administrator(owner, "admin@grant-billing-create.test");

		assertStatus(() -> roleService.createRole(admin,
				new CreateRoleRequest("Billing Manager", null, List.of("billing.view", "billing.edit"))),
				HttpStatus.FORBIDDEN);
	}

	@Test
	void administratorCannotEscalateAnExistingRoleWithBillingCodesOnUpdate() {
		AuthenticatedUser owner = registerOwner("grant-billing-update@acme.test");
		AuthenticatedUser admin = administrator(owner, "admin@grant-billing-update.test");
		RoleDetailResponse role = roleService.createRole(admin,
				new CreateRoleRequest("Sales Lead", null, List.of("sales.view")));

		assertStatus(() -> roleService.updateRole(admin, role.id(),
				new UpdateRoleRequest("Sales Lead", null, List.of("sales.view", "billing.view"))),
				HttpStatus.FORBIDDEN);
	}

	@Test
	void ownerCanCreateARoleGrantingBillingCodes() {
		AuthenticatedUser owner = registerOwner("grant-billing-owner@acme.test");

		RoleDetailResponse role = roleService.createRole(owner,
				new CreateRoleRequest("Billing Manager", null, List.of("billing.view", "billing.edit")));

		assertThat(role.permissionCodes()).containsExactly("billing.edit", "billing.view");
	}

	@Test
	void assigningAMemberToARoleThatExceedsTheCallersPermissionsIsForbidden() {
		AuthenticatedUser owner = registerOwner("grant-assign-exceed@acme.test");
		AuthenticatedUser admin = administrator(owner, "admin@grant-assign-exceed.test");
		User target = createTenantUser(owner, "target@grant-assign-exceed.test");
		RoleDetailResponse billingRole = roleService.createRole(owner,
				new CreateRoleRequest("Billing Manager", null, List.of("billing.view")));

		assertStatus(() -> roleService.assignMember(admin, billingRole.id(), target.getId()),
				HttpStatus.FORBIDDEN);
	}

	@Test
	void reAssigningAnExistingMemberIsIdempotentEvenWhenTheCallerLacksTheRolesCodes() {
		AuthenticatedUser owner = registerOwner("grant-reassign-idempotent@acme.test");
		AuthenticatedUser admin = administrator(owner, "admin@grant-reassign-idempotent.test");
		User target = createTenantUser(owner, "target@grant-reassign-idempotent.test");
		RoleDetailResponse billingRole = roleService.createRole(owner,
				new CreateRoleRequest("Billing Manager", null, List.of("billing.view")));
		roleService.assignMember(owner, billingRole.id(), target.getId());

		roleService.assignMember(admin, billingRole.id(), target.getId());

		assertThat(userRoleRepository.findByRoleId(billingRole.id())).hasSize(1);
	}

	@Test
	void assigningAMemberToARoleWithinTheCallersPermissionsSucceeds() {
		AuthenticatedUser owner = registerOwner("grant-assign-ok@acme.test");
		AuthenticatedUser admin = administrator(owner, "admin@grant-assign-ok.test");
		User target = createTenantUser(owner, "target@grant-assign-ok.test");
		RoleDetailResponse salesRole = roleService.createRole(admin,
				new CreateRoleRequest("Sales Rep", null, List.of("sales.view", "sales.create")));

		roleService.assignMember(admin, salesRole.id(), target.getId());

		assertThat(userRoleRepository.existsByUserIdAndRoleId(target.getId(), salesRole.id())).isTrue();
	}

}
