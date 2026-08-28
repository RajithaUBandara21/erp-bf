package ERP.erpbackend.identity;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates the built-in {@link SystemRole}s for a newly created tenant and assigns its first user the Owner role. */
@Service
@RequiredArgsConstructor
public class SystemRoleProvisioner {

	private final PermissionRepository permissionRepository;
	private final RoleRepository roleRepository;
	private final RolePermissionRepository rolePermissionRepository;
	private final UserRoleRepository userRoleRepository;

	@Transactional
	public void provisionForNewTenant(UUID tenantId, UUID ownerUserId) {
		List<Permission> catalog = permissionRepository.findAll();

		for (SystemRole systemRole : SystemRole.values()) {
			Role role = new Role();
			role.setTenantId(tenantId);
			role.setName(systemRole.displayName());
			role.setDescription(systemRole.description());
			role.setSystemManaged(true);
			role = roleRepository.save(role);

			UUID roleId = role.getId();
			List<RolePermission> grants = systemRole.permissionsFrom(catalog).stream()
					.map(permission -> grant(roleId, permission.getId()))
					.toList();
			rolePermissionRepository.saveAll(grants);

			if (systemRole == SystemRole.OWNER) {
				userRoleRepository.save(assignment(tenantId, ownerUserId, roleId));
			}
		}
	}

	private RolePermission grant(UUID roleId, UUID permissionId) {
		RolePermission grant = new RolePermission();
		grant.setRoleId(roleId);
		grant.setPermissionId(permissionId);
		return grant;
	}

	private UserRole assignment(UUID tenantId, UUID userId, UUID roleId) {
		UserRole assignment = new UserRole();
		assignment.setTenantId(tenantId);
		assignment.setUserId(userId);
		assignment.setRoleId(roleId);
		return assignment;
	}

}
