package ERP.erpbackend.identity;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {

	List<Permission> findByCodeIn(Collection<String> codes);

	/** The distinct permission codes a user holds across every role assigned to them within one tenant. */
	@Query("""
			SELECT DISTINCT p.code FROM Permission p, RolePermission rp, UserRole ur
			WHERE rp.permissionId = p.id AND ur.roleId = rp.roleId
			  AND ur.userId = :userId AND ur.tenantId = :tenantId
			""")
	List<String> findEffectiveCodes(@Param("userId") UUID userId, @Param("tenantId") UUID tenantId);

}
