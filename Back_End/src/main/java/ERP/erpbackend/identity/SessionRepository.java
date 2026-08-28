package ERP.erpbackend.identity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface SessionRepository extends JpaRepository<Session, UUID> {

	List<Session> findByTenantIdAndUserIdAndRevokedAtIsNullAndExpiresAtAfter(
			UUID tenantId, UUID userId, Instant now);

	Optional<Session> findByIdAndTenantIdAndUserId(UUID id, UUID tenantId, UUID userId);

	@Query("""
			SELECT s.id FROM Session s
			WHERE s.tenantId = :tenantId AND s.userId = :userId
			AND s.id <> :currentSessionId AND s.revokedAt IS NULL
			""")
	List<UUID> findActiveIdsExceptCurrent(UUID tenantId, UUID userId, UUID currentSessionId);

	@Modifying
	@Query("""
			UPDATE Session s SET s.revokedAt = :now
			WHERE s.tenantId = :tenantId AND s.userId = :userId
			AND s.id <> :currentSessionId AND s.revokedAt IS NULL
			""")
	void revokeAllExceptCurrent(UUID tenantId, UUID userId, UUID currentSessionId, Instant now);

}
