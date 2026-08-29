package ERP.erpbackend.audit;

import ERP.erpbackend.identity.AuthenticatedUser;
import ERP.erpbackend.identity.UserDirectoryService;
import ERP.erpbackend.identity.UserSummaryResponse;
import ERP.erpbackend.organization.OrganizationService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/** Public entry point for reading audit logs back - never query {@link AuditLogRepository} directly from outside this package. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditLogQueryService {

	private final AuditLogRepository auditLogRepository;
	private final UserDirectoryService userDirectoryService;
	private final OrganizationService organizationService;
	private final ObjectMapper objectMapper;

	public AuditLogPageResponse search(AuthenticatedUser caller, AuditLogFilter filter, Pageable pageable) {
		Page<AuditLog> page = auditLogRepository.search(caller.tenantId(), filter.entityType(), filter.action(),
				filter.actorId(), filter.from(), filter.to(), pageable);

		Set<UUID> actorIds = page.getContent().stream().map(AuditLog::getUserId).filter(Objects::nonNull)
				.collect(Collectors.toSet());
		Set<UUID> organizationIds = page.getContent().stream().map(AuditLog::getOrganizationId)
				.filter(Objects::nonNull).collect(Collectors.toSet());

		Map<UUID, UserSummaryResponse> actors = userDirectoryService.findSummariesByIds(actorIds);
		Map<UUID, String> organizationNames = organizationService.findNamesByIds(organizationIds);

		List<AuditLogResponse> content = page.getContent().stream()
				.map(log -> toResponse(log, actors, organizationNames)).toList();
		return new AuditLogPageResponse(content, page.getNumber(), page.getSize(), page.getTotalElements(),
				page.getTotalPages());
	}

	private AuditLogResponse toResponse(AuditLog log, Map<UUID, UserSummaryResponse> actors,
			Map<UUID, String> organizationNames) {
		UserSummaryResponse actor = log.getUserId() == null ? null : actors.get(log.getUserId());
		String organizationName = log.getOrganizationId() == null ? null
				: organizationNames.get(log.getOrganizationId());

		return new AuditLogResponse(
				log.getId(),
				log.getCreatedAt(),
				log.getUserId(),
				actor == null ? null : actor.fullName(),
				actor == null ? null : actor.email(),
				log.getEntityType(),
				log.getEntityId(),
				log.getAction(),
				log.getOrganizationId(),
				organizationName,
				fromJson(log.getBeforeValue()),
				fromJson(log.getAfterValue()));
	}

	private Map<String, Object> fromJson(String json) {
		return json == null ? null : objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
		});
	}

}
