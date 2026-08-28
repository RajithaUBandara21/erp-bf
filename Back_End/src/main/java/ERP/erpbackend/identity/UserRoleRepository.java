package ERP.erpbackend.identity;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

	List<UserRole> findByUserId(UUID userId);

	boolean existsByUserIdAndRoleId(UUID userId, UUID roleId);

}
