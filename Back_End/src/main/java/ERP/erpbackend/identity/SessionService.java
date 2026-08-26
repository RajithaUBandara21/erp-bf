package ERP.erpbackend.identity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SessionService {

	private static final String SESSION_NOT_FOUND = "Session not found";

	private final SessionRepository sessionRepository;

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
	}

	@Transactional
	public void revokeOtherSessions(AuthenticatedUser authenticatedUser) {
		sessionRepository.revokeAllExceptCurrent(
				authenticatedUser.tenantId(), authenticatedUser.userId(), authenticatedUser.sessionId(), Instant.now());
	}

	private static SessionResponse toResponse(Session session, UUID currentSessionId) {
		return new SessionResponse(session.getId(), session.getClientType(), session.getCreatedAt(),
				session.getLastUsedAt(), session.getId().equals(currentSessionId));
	}

}
