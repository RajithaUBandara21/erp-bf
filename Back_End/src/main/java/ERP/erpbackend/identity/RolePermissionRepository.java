package ERP.erpbackend.identity;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RolePermissionRepository extends JpaRepository<RolePermission, UUID> {

	List<RolePermission> findByRoleId(UUID roleId);

	List<RolePermission> findByRoleIdIn(Collection<UUID> roleIds);

	@Modifying
	@Query("DELETE FROM RolePermission rp WHERE rp.roleId = :roleId")
	void deleteByRoleId(UUID roleId);

}
