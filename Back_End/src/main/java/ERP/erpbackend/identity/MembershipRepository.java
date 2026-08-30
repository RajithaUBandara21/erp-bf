package ERP.erpbackend.identity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

	List<Membership> findByUserId(UUID userId);

	Optional<Membership> findByUserIdAndOrganizationId(UUID userId, UUID organizationId);

}
