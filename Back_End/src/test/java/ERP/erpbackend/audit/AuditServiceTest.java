package ERP.erpbackend.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

class AuditServiceTest {

	private final AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
	private final AuditService auditService = new AuditService(auditLogRepository, new JsonMapper());

	@Test
	void logsAnEventWithBeforeAndAfterValuesSerializedToJson() {
		UUID tenantId = UUID.randomUUID();
		UUID organizationId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID entityId = UUID.randomUUID();
		AuditEvent event = new AuditEvent(tenantId, organizationId, userId, "Role", entityId, "role.updated",
				Map.of("name", "Cashier"), Map.of("name", "Cashier Lead"));
		when(auditLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		auditService.log(event);

		ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
		verify(auditLogRepository).save(captor.capture());
		AuditLog saved = captor.getValue();
		assertThat(saved.getTenantId()).isEqualTo(tenantId);
		assertThat(saved.getOrganizationId()).isEqualTo(organizationId);
		assertThat(saved.getUserId()).isEqualTo(userId);
		assertThat(saved.getEntityType()).isEqualTo("Role");
		assertThat(saved.getEntityId()).isEqualTo(entityId);
		assertThat(saved.getAction()).isEqualTo("role.updated");
		assertThat(saved.getBeforeValue()).isEqualTo("{\"name\":\"Cashier\"}");
		assertThat(saved.getAfterValue()).isEqualTo("{\"name\":\"Cashier Lead\"}");
	}

	@Test
	void logsAnEventWithoutBeforeOrAfterValuesAsNullSnapshots() {
		AuditEvent event = new AuditEvent(UUID.randomUUID(), null, UUID.randomUUID(), "Session",
				UUID.randomUUID(), "auth.login", null, null);
		when(auditLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		auditService.log(event);

		ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
		verify(auditLogRepository).save(captor.capture());
		AuditLog saved = captor.getValue();
		assertThat(saved.getOrganizationId()).isNull();
		assertThat(saved.getBeforeValue()).isNull();
		assertThat(saved.getAfterValue()).isNull();
	}

	@Test
	void propagatesSerializationFailuresInsteadOfSwallowingThem() {
		AuditEvent event = new AuditEvent(UUID.randomUUID(), null, null, "Role", null, "role.updated",
				null, new Unserializable());

		assertThatThrownBy(() -> auditService.log(event)).isInstanceOf(RuntimeException.class);
		verify(auditLogRepository, never()).save(any());
	}

	/** A getter that blows up mid-serialization, so Jackson fails after starting the write. */
	static class Unserializable {
		public String getValue() {
			throw new IllegalStateException("cannot serialize this");
		}
	}

}
