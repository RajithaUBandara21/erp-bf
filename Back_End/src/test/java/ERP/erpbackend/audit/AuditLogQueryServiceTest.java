package ERP.erpbackend.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ERP.erpbackend.identity.AuthenticatedUser;
import ERP.erpbackend.identity.UserDirectoryService;
import ERP.erpbackend.identity.UserSummaryResponse;
import ERP.erpbackend.organization.OrganizationService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

class AuditLogQueryServiceTest {

	private final AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
	private final UserDirectoryService userDirectoryService = mock(UserDirectoryService.class);
	private final OrganizationService organizationService = mock(OrganizationService.class);
	private final AuditLogQueryService auditLogQueryService = new AuditLogQueryService(
			auditLogRepository, userDirectoryService, organizationService, new JsonMapper());

	private static final AuthenticatedUser CALLER = new AuthenticatedUser(
			UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "ada@acme.test", UUID.randomUUID());

	private AuditLog auditLogWith(UUID id, UUID userId, UUID organizationId, String beforeValue,
			String afterValue) {
		AuditLog log = new AuditLog();
		log.setTenantId(CALLER.tenantId());
		log.setUserId(userId);
		log.setOrganizationId(organizationId);
		log.setEntityType("Role");
		log.setEntityId(UUID.randomUUID());
		log.setAction("role.updated");
		log.setBeforeValue(beforeValue);
		log.setAfterValue(afterValue);
		ReflectionTestUtils.setField(log, "id", id);
		ReflectionTestUtils.setField(log, "createdAt", Instant.parse("2026-08-28T10:00:00Z"));
		return log;
	}

	@Test
	void enrichesResultsWithResolvedActorAndOrganizationNames() {
		UUID entryId = UUID.randomUUID();
		UUID actorId = UUID.randomUUID();
		UUID organizationId = UUID.randomUUID();
		AuditLog log = auditLogWith(entryId, actorId, organizationId, "{\"name\":\"Cashier\"}",
				"{\"name\":\"Cashier Lead\"}");
		Pageable pageable = PageRequest.of(0, 20);
		when(auditLogRepository.search(eq(CALLER.tenantId()), any(), any(), any(), any(), any(), eq(pageable)))
				.thenReturn(new PageImpl<>(List.of(log), pageable, 1));
		when(userDirectoryService.findSummariesByIds(Set.of(actorId)))
				.thenReturn(Map.of(actorId, new UserSummaryResponse(actorId, "R. Haritha", "r.haritha@acme.test")));
		when(organizationService.findNamesByIds(Set.of(organizationId)))
				.thenReturn(Map.of(organizationId, "Head Office"));

		AuditLogPageResponse result = auditLogQueryService.search(CALLER,
				new AuditLogFilter(null, null, null, null, null), pageable);

		assertThat(result.content()).hasSize(1);
		AuditLogResponse response = result.content().get(0);
		assertThat(response.id()).isEqualTo(entryId);
		assertThat(response.actorName()).isEqualTo("R. Haritha");
		assertThat(response.actorEmail()).isEqualTo("r.haritha@acme.test");
		assertThat(response.organizationName()).isEqualTo("Head Office");
		assertThat(response.beforeValue()).isEqualTo(Map.of("name", "Cashier"));
		assertThat(response.afterValue()).isEqualTo(Map.of("name", "Cashier Lead"));
		assertThat(result.totalElements()).isEqualTo(1);
	}

	@Test
	void leavesActorAndOrganizationNamesNullWhenTheyNoLongerResolve() {
		UUID entryId = UUID.randomUUID();
		UUID deletedActorId = UUID.randomUUID();
		UUID deletedOrgId = UUID.randomUUID();
		AuditLog log = auditLogWith(entryId, deletedActorId, deletedOrgId, null, null);
		Pageable pageable = PageRequest.of(0, 20);
		when(auditLogRepository.search(eq(CALLER.tenantId()), any(), any(), any(), any(), any(), eq(pageable)))
				.thenReturn(new PageImpl<>(List.of(log), pageable, 1));
		when(userDirectoryService.findSummariesByIds(Set.of(deletedActorId))).thenReturn(Map.of());
		when(organizationService.findNamesByIds(Set.of(deletedOrgId))).thenReturn(Map.of());

		AuditLogPageResponse result = auditLogQueryService.search(CALLER,
				new AuditLogFilter(null, null, null, null, null), pageable);

		AuditLogResponse response = result.content().get(0);
		assertThat(response.actorName()).isNull();
		assertThat(response.actorEmail()).isNull();
		assertThat(response.organizationName()).isNull();
		assertThat(response.beforeValue()).isNull();
		assertThat(response.afterValue()).isNull();
	}

	@Test
	void skipsLookupsWhenNoRowsHaveAnActorOrOrganization() {
		UUID entryId = UUID.randomUUID();
		AuditLog log = auditLogWith(entryId, null, null, null, null);
		Pageable pageable = PageRequest.of(0, 20);
		when(auditLogRepository.search(eq(CALLER.tenantId()), any(), any(), any(), any(), any(), eq(pageable)))
				.thenReturn(new PageImpl<>(List.of(log), pageable, 1));
		when(userDirectoryService.findSummariesByIds(Set.of())).thenReturn(Map.of());
		when(organizationService.findNamesByIds(Set.of())).thenReturn(Map.of());

		AuditLogPageResponse result = auditLogQueryService.search(CALLER,
				new AuditLogFilter(null, null, null, null, null), pageable);

		assertThat(result.content().get(0).actorName()).isNull();
		assertThat(result.content().get(0).organizationName()).isNull();
	}

}
