package ERP.erpbackend.identity;

import ERP.erpbackend.audit.AuditEvent;
import ERP.erpbackend.audit.AuditService;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleService {

	private static final String ROLE_NOT_FOUND = "Role not found";

	private final RoleRepository roleRepository;
	private final RolePermissionRepository rolePermissionRepository;
	private final UserRoleRepository userRoleRepository;
	private final PermissionRepository permissionRepository;
	private final UserRepository userRepository;
	private final MembershipRepository membershipRepository;
	private final EffectivePermissionResolver effectivePermissionResolver;
	private final AuditService auditService;

	public List<RoleSummaryResponse> listRoles(AuthenticatedUser caller) {
		List<Role> roles = roleRepository.findByTenantId(caller.tenantId());
		List<UUID> roleIds = roles.stream().map(Role::getId).toList();
		if (roleIds.isEmpty()) {
			return List.of();
		}

		Map<UUID, Long> memberCounts = userRoleRepository.findByRoleIdIn(roleIds).stream()
				.collect(Collectors.groupingBy(UserRole::getRoleId, Collectors.counting()));
		Map<UUID, Long> permissionCounts = rolePermissionRepository.findByRoleIdIn(roleIds).stream()
				.collect(Collectors.groupingBy(RolePermission::getRoleId, Collectors.counting()));

		return roles.stream()
				.map(role -> new RoleSummaryResponse(role.getId(), role.getName(), role.getDescription(),
						role.isSystemManaged(),
						memberCounts.getOrDefault(role.getId(), 0L),
						permissionCounts.getOrDefault(role.getId(), 0L)))
				.toList();
	}

	public RoleDetailResponse getRole(AuthenticatedUser caller, UUID roleId) {
		Role role = requireRole(caller.tenantId(), roleId);

		List<UUID> permissionIds = rolePermissionRepository.findByRoleId(roleId).stream()
				.map(RolePermission::getPermissionId).toList();
		List<String> permissionCodes = permissionRepository.findAllById(permissionIds).stream()
				.map(Permission::getCode).sorted().toList();

		List<UUID> membershipIds = userRoleRepository.findByRoleId(roleId).stream()
				.map(UserRole::getMembershipId).toList();
		List<UUID> memberUserIds = membershipRepository.findAllById(membershipIds).stream()
				.map(Membership::getUserId).toList();
		List<RoleMemberResponse> members = userRepository.findAllById(memberUserIds).stream()
				.map(user -> new RoleMemberResponse(user.getId(), user.getFullName(), user.getEmail()))
				.sorted(Comparator.comparing(RoleMemberResponse::fullName, String.CASE_INSENSITIVE_ORDER))
				.toList();

		return new RoleDetailResponse(role.getId(), role.getName(), role.getDescription(), role.isSystemManaged(),
				members.size(), permissionCodes.size(), permissionCodes, members);
	}

	public List<PermissionResponse> listPermissions() {
		return permissionRepository.findAll().stream()
				.map(permission -> new PermissionResponse(
						permission.getCode(), permission.getResource(), permission.getAction()))
				.sorted(Comparator.comparing(PermissionResponse::code))
				.toList();
	}

	@Transactional
	public RoleDetailResponse createRole(AuthenticatedUser caller, CreateRoleRequest request) {
		UUID tenantId = caller.tenantId();
		String name = request.name().trim();
		assertNameFree(tenantId, name, null);
		List<Permission> permissions = resolveCodes(request.permissionCodes());
		assertCallerCanGrant(caller, codesOf(permissions));

		Role role = new Role();
		role.setTenantId(tenantId);
		role.setName(name);
		role.setDescription(trimToNull(request.description()));
		role.setSystemManaged(false);
		role = roleRepository.save(role);

		replaceGrants(role.getId(), permissions);
		auditService.log(auditEvent(caller, role.getId(), "role.created",
				null, new RoleSnapshot(name, role.getDescription(), codesOf(permissions))));
		return getRole(caller, role.getId());
	}

	@Transactional
	public RoleDetailResponse updateRole(AuthenticatedUser caller, UUID roleId, UpdateRoleRequest request) {
		Role role = requireRole(caller.tenantId(), roleId);
		assertNotSystemManaged(role);
		String name = request.name().trim();
		assertNameFree(caller.tenantId(), name, roleId);
		List<Permission> permissions = resolveCodes(request.permissionCodes());
		assertCallerCanGrant(caller, codesOf(permissions));
		RoleSnapshot before = new RoleSnapshot(role.getName(), role.getDescription(), grantedCodes(roleId));

		role.setName(name);
		role.setDescription(trimToNull(request.description()));
		roleRepository.save(role);

		replaceGrants(roleId, permissions);
		auditService.log(auditEvent(caller, roleId, "role.updated",
				before, new RoleSnapshot(name, role.getDescription(), codesOf(permissions))));
		return getRole(caller, roleId);
	}

	@Transactional
	public void assignMember(AuthenticatedUser caller, UUID roleId, UUID userId) {
		Role role = requireRole(caller.tenantId(), roleId);
		UUID membershipId = requireOrganizationMembership(caller, userId);

		if (isOwnerEquivalentRole(role) && !callerHoldsOwnerEquivalent(caller)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN,
					"Only an Owner or Tenant Admin can grant the " + role.getName() + " role.");
		}
		if (userRoleRepository.existsByMembershipIdAndRoleId(membershipId, roleId)) {
			return;
		}
		assertCallerCanGrant(caller, grantedCodes(roleId));

		UserRole assignment = new UserRole();
		assignment.setTenantId(caller.tenantId());
		assignment.setMembershipId(membershipId);
		assignment.setRoleId(roleId);
		userRoleRepository.save(assignment);

		auditService.log(auditEvent(caller, roleId, "role.member_assigned", null, Map.of("userId", userId)));
	}

	@Transactional
	public void unassignMember(AuthenticatedUser caller, UUID roleId, UUID userId) {
		Role role = requireRole(caller.tenantId(), roleId);
		UUID membershipId = activeOrganizationMembership(caller, userId)
				.map(Membership::getId).orElse(null);
		if (membershipId == null) {
			return;
		}
		UserRole assignment = userRoleRepository.findByMembershipIdAndRoleId(membershipId, roleId).orElse(null);
		if (assignment == null) {
			return;
		}
		if (isOwnerEquivalentRole(role)) {
			// Serialize concurrent last-holder removals so two can't both pass the count check.
			roleRepository.findByIdForUpdate(roleId);
			if (userRoleRepository.countByRoleId(roleId) <= 1) {
				throw new ResponseStatusException(HttpStatus.CONFLICT,
						"The last " + role.getName() + " cannot be removed. Assign the "
								+ role.getName() + " role to another user first.");
			}
		}
		userRoleRepository.delete(assignment);

		auditService.log(auditEvent(caller, roleId, "role.member_unassigned", Map.of("userId", userId), null));
	}

	@Transactional
	public void deleteRole(AuthenticatedUser caller, UUID roleId) {
		Role role = requireRole(caller.tenantId(), roleId);
		assertNotSystemManaged(role);
		if (!userRoleRepository.findByRoleId(roleId).isEmpty()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"This role still has members. Reassign them before deleting it.");
		}
		RoleSnapshot before = new RoleSnapshot(role.getName(), role.getDescription(), grantedCodes(roleId));
		rolePermissionRepository.deleteByRoleId(roleId);
		roleRepository.delete(role);

		auditService.log(auditEvent(caller, roleId, "role.deleted", before, null));
	}

	private Role requireRole(UUID tenantId, UUID roleId) {
		return roleRepository.findByIdAndTenantId(roleId, tenantId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ROLE_NOT_FOUND));
	}

	/**
	 * A role is tenant-scoped but attaches to a per-Organization Membership, and one user can hold
	 * several ACTIVE Memberships in the tenant (a Tenant Admin across sibling orgs). Resolve the
	 * target against the caller's currently selected Organization so the assignment is unambiguous.
	 */
	private UUID requireOrganizationMembership(AuthenticatedUser caller, UUID userId) {
		return activeOrganizationMembership(caller, userId)
				.map(Membership::getId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
	}

	private Optional<Membership> activeOrganizationMembership(AuthenticatedUser caller, UUID userId) {
		return membershipRepository.findByUserIdAndOrganizationId(userId, caller.organizationId())
				.filter(membership -> membership.getStatus() == MembershipStatus.ACTIVE);
	}

	/** Owner and Tenant Admin are both full-access system roles: granting or removing either is guarded. */
	private boolean isOwnerEquivalentRole(Role role) {
		return role.isSystemManaged()
				&& (SystemRole.OWNER.displayName().equals(role.getName())
						|| SystemRole.TENANT_ADMIN.displayName().equals(role.getName()));
	}

	private boolean callerHoldsOwnerEquivalent(AuthenticatedUser caller) {
		return callerHoldsSystemRole(caller, SystemRole.OWNER)
				|| callerHoldsSystemRole(caller, SystemRole.TENANT_ADMIN);
	}

	private boolean callerHoldsSystemRole(AuthenticatedUser caller, SystemRole systemRole) {
		return roleRepository.findByTenantIdAndName(caller.tenantId(), systemRole.displayName())
				.map(role -> userRoleRepository.existsByMembershipIdAndRoleId(caller.membershipId(), role.getId()))
				.orElse(false);
	}

	private void assertNotSystemManaged(Role role) {
		if (role.isSystemManaged()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "System roles cannot be modified or deleted.");
		}
	}

	private void assertNameFree(UUID tenantId, String name, UUID excludingRoleId) {
		roleRepository.findByTenantIdAndName(tenantId, name)
				.filter(existing -> !existing.getId().equals(excludingRoleId))
				.ifPresent(existing -> {
					throw new ResponseStatusException(HttpStatus.CONFLICT,
							"A role named '" + name + "' already exists.");
				});
	}

	private void assertCallerCanGrant(AuthenticatedUser caller, Collection<String> requestedCodes) {
		if (requestedCodes.isEmpty()) {
			return;
		}
		Set<String> held = effectivePermissionResolver.resolve(caller.membershipId());
		List<String> exceeding = requestedCodes.stream()
				.filter(code -> !held.contains(code)).distinct().sorted().toList();
		if (!exceeding.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN,
					"You cannot grant permissions you do not hold: " + String.join(", ", exceeding));
		}
	}

	private static List<String> codesOf(List<Permission> permissions) {
		return permissions.stream().map(Permission::getCode).toList();
	}

	private Set<String> grantedCodes(UUID roleId) {
		List<UUID> permissionIds = rolePermissionRepository.findByRoleId(roleId).stream()
				.map(RolePermission::getPermissionId).toList();
		return permissionRepository.findAllById(permissionIds).stream()
				.map(Permission::getCode).collect(Collectors.toSet());
	}

	private List<Permission> resolveCodes(List<String> codes) {
		List<String> wanted = codes.stream()
				.filter(Objects::nonNull).map(String::trim).filter(code -> !code.isEmpty())
				.distinct().toList();
		if (wanted.isEmpty()) {
			return List.of();
		}
		List<Permission> found = permissionRepository.findByCodeIn(wanted);
		if (found.size() != wanted.size()) {
			Set<String> knownCodes = found.stream().map(Permission::getCode).collect(Collectors.toSet());
			List<String> unknown = wanted.stream().filter(code -> !knownCodes.contains(code)).toList();
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Unknown permission codes: " + String.join(", ", unknown));
		}
		return found;
	}

	private void replaceGrants(UUID roleId, List<Permission> permissions) {
		rolePermissionRepository.deleteByRoleId(roleId);
		rolePermissionRepository.flush();
		List<RolePermission> grants = permissions.stream().map(permission -> {
			RolePermission grant = new RolePermission();
			grant.setRoleId(roleId);
			grant.setPermissionId(permission.getId());
			return grant;
		}).toList();
		rolePermissionRepository.saveAll(grants);
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	/** {@code AuditEvent} stays free of identity types (see coding-standards.md module
	 * boundaries), so the caller-to-actor mapping lives here instead of on the record. */
	private static AuditEvent auditEvent(AuthenticatedUser caller, UUID entityId, String action,
			Object beforeValue, Object afterValue) {
		return new AuditEvent(caller.tenantId(), caller.organizationId(), caller.userId(), "Role", entityId,
				action, beforeValue, afterValue);
	}

	private record RoleSnapshot(String name, String description, Collection<String> permissionCodes) {
	}

}
