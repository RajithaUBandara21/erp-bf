package ERP.erpbackend.identity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

	List<UserRole> findByUserId(UUID userId);

	List<UserRole> findByRoleId(UUID roleId);

	List<UserRole> findByRoleIdIn(Collection<UUID> roleIds);

	Optional<UserRole> findByUserIdAndRoleId(UUID userId, UUID roleId);

	boolean existsByUserIdAndRoleId(UUID userId, UUID roleId);

	long countByRoleId(UUID roleId);

}
