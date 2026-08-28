package ERP.erpbackend.identity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

	private static final String SESSION_NOT_FOUND = "Session not found";

	private final SessionRepository sessionRepository;
	private final RevokedSessionRegistry revokedSessionRegistry;

	public List<SessionResponse> listSessions(AuthenticatedUser authenticatedUser) {
		return sessionRepository
				.findByTenantIdAndUserIdAndRevokedAtIsNullAndExpiresAtAfter(
						authenticatedUser.tenantId(), authenticatedUser.userId(), Instant.now())
				.stream()
				.map(session -> toResponse(session, authenticatedUser.sessionId()))
				.toList();
	}

	public void revokeSession(AuthenticatedUser authenticatedUser, UUID sessionId) {
		Session session = sessionRepository
				.findByIdAndTenantIdAndUserId(sessionId, authenticatedUser.tenantId(), authenticatedUser.userId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, SESSION_NOT_FOUND));
		session.setRevokedAt(Instant.now());
		sessionRepository.save(session);
		revokedSessionRegistry.revoke(session.getId());
	}

	@Transactional
	public void revokeOtherSessions(AuthenticatedUser authenticatedUser) {
		List<UUID> revokedIds = sessionRepository.findActiveIdsExceptCurrent(
				authenticatedUser.tenantId(), authenticatedUser.userId(), authenticatedUser.sessionId());
		sessionRepository.revokeAllExceptCurrent(
				authenticatedUser.tenantId(), authenticatedUser.userId(), authenticatedUser.sessionId(), Instant.now());

		// The Redis fast-path writes are best-effort enforcement (Postgres is the record). Run them after
		// the DB commits so no blocking Redis round-trip sits inside the transaction boundary (F-18); if
		// the transaction rolls back they are skipped, and a Redis outage never fails the revoke.
		markRevokedInRegistryAfterCommit(revokedIds);
	}

	private void markRevokedInRegistryAfterCommit(List<UUID> revokedIds) {
		if (revokedIds.isEmpty() || !TransactionSynchronizationManager.isSynchronizationActive()) {
			revokedIds.forEach(this::safeRegistryRevoke);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				revokedIds.forEach(SessionService.this::safeRegistryRevoke);
			}
		});
	}

	private void safeRegistryRevoke(UUID sessionId) {
		try {
			revokedSessionRegistry.revoke(sessionId);
		} catch (DataAccessException ex) {
			log.warn("Could not record revoked session {} in Redis; access token blocked only after its TTL",
					sessionId, ex);
		}
	}

	private static SessionResponse toResponse(Session session, UUID currentSessionId) {
		return new SessionResponse(session.getId(), session.getClientType(), session.getCreatedAt(),
				session.getLastUsedAt(), session.getId().equals(currentSessionId));
	}

}
