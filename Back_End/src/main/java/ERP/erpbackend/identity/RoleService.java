package ERP.erpbackend.identity;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
	private final EffectivePermissionResolver effectivePermissionResolver;

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

		List<UUID> memberIds = userRoleRepository.findByRoleId(roleId).stream()
				.map(UserRole::getUserId).toList();
		List<RoleMemberResponse> members = userRepository.findAllById(memberIds).stream()
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

		role.setName(name);
		role.setDescription(trimToNull(request.description()));
		roleRepository.save(role);

		replaceGrants(roleId, permissions);
		return getRole(caller, roleId);
	}

	@Transactional
	public void assignMember(AuthenticatedUser caller, UUID roleId, UUID userId) {
		Role role = requireRole(caller.tenantId(), roleId);
		requireTenantUser(caller.tenantId(), userId);

		if (isOwnerRole(role) && !callerHoldsOwner(caller)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only an Owner can grant the Owner role.");
		}
		if (userRoleRepository.existsByUserIdAndRoleId(userId, roleId)) {
			return;
		}
		assertCallerCanGrant(caller, grantedCodes(roleId));

		UserRole assignment = new UserRole();
		assignment.setTenantId(caller.tenantId());
		assignment.setUserId(userId);
		assignment.setRoleId(roleId);
		userRoleRepository.save(assignment);
	}

	@Transactional
	public void unassignMember(AuthenticatedUser caller, UUID roleId, UUID userId) {
		Role role = requireRole(caller.tenantId(), roleId);
		UserRole assignment = userRoleRepository.findByUserIdAndRoleId(userId, roleId).orElse(null);
		if (assignment == null) {
			return;
		}
		if (isOwnerRole(role)) {
			// Serialize concurrent last-Owner removals so two can't both pass the count check.
			roleRepository.findByIdForUpdate(roleId);
			if (userRoleRepository.countByRoleId(roleId) <= 1) {
				throw new ResponseStatusException(HttpStatus.CONFLICT,
						"The last Owner cannot be removed. Assign the Owner role to another user first.");
			}
		}
		userRoleRepository.delete(assignment);
	}

	@Transactional
	public void deleteRole(AuthenticatedUser caller, UUID roleId) {
		Role role = requireRole(caller.tenantId(), roleId);
		assertNotSystemManaged(role);
		if (!userRoleRepository.findByRoleId(roleId).isEmpty()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"This role still has members. Reassign them before deleting it.");
		}
		rolePermissionRepository.deleteByRoleId(roleId);
		roleRepository.delete(role);
	}

	private Role requireRole(UUID tenantId, UUID roleId) {
		return roleRepository.findByIdAndTenantId(roleId, tenantId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ROLE_NOT_FOUND));
	}

	private void requireTenantUser(UUID tenantId, UUID userId) {
		userRepository.findByIdAndTenantId(userId, tenantId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
	}

	private boolean isOwnerRole(Role role) {
		return role.isSystemManaged() && SystemRole.OWNER.displayName().equals(role.getName());
	}

	private boolean callerHoldsOwner(AuthenticatedUser caller) {
		return roleRepository.findByTenantIdAndName(caller.tenantId(), SystemRole.OWNER.displayName())
				.map(owner -> userRoleRepository.existsByUserIdAndRoleId(caller.userId(), owner.getId()))
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
		Set<String> held = effectivePermissionResolver.resolve(caller.userId(), caller.tenantId());
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

}
