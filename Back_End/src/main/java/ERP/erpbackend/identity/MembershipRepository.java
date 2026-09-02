package ERP.erpbackend.identity;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

	List<Membership> findByUserId(UUID userId);

	List<Membership> findByUserIdAndStatus(UUID userId, MembershipStatus status);

	Optional<Membership> findByUserIdAndOrganizationId(UUID userId, UUID organizationId);

	List<Membership> findByOrganizationIdAndStatus(UUID organizationId, MembershipStatus status);

	/** True when the user holds an ACTIVE Membership in the tenant that is assigned the named system role. */
	@Query("""
			SELECT COUNT(m) > 0 FROM Membership m, UserRole ur, Role r
			WHERE m.userId = :userId AND m.tenantId = :tenantId
			  AND m.status = ERP.erpbackend.identity.MembershipStatus.ACTIVE
			  AND ur.membershipId = m.id
			  AND r.id = ur.roleId AND r.systemManaged = TRUE AND r.name = :roleName
			""")
	boolean existsActiveMembershipAssignedSystemRole(@Param("userId") UUID userId,
			@Param("tenantId") UUID tenantId, @Param("roleName") String roleName);

	/** The tenants where the user holds an ACTIVE Membership assigned the named system role. */
	@Query("""
			SELECT DISTINCT m.tenantId FROM Membership m, UserRole ur, Role r
			WHERE m.userId = :userId
			  AND m.status = ERP.erpbackend.identity.MembershipStatus.ACTIVE
			  AND ur.membershipId = m.id
			  AND r.id = ur.roleId AND r.systemManaged = TRUE AND r.name = :roleName
			""")
	Set<UUID> findTenantIdsWithActiveMembershipAssignedSystemRole(@Param("userId") UUID userId,
			@Param("roleName") String roleName);

}
