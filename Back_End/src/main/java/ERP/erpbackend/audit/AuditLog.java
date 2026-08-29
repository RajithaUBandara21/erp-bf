package ERP.erpbackend.audit;

import ERP.erpbackend.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Append-only audit trail row. Nothing updates a row after insert; {@code
 * updatedAt} is inherited for consistency with every other entity but is
 * never meaningfully different from {@code createdAt} here.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "audit_logs")
public class AuditLog extends AuditableEntity {

	@Column(name = "tenant_id", nullable = false)
	private UUID tenantId;

	@Column(name = "organization_id")
	private UUID organizationId;

	@Column(name = "user_id")
	private UUID userId;

	@Column(name = "entity_type", nullable = false)
	private String entityType;

	@Column(name = "entity_id")
	private UUID entityId;

	@Column(nullable = false)
	private String action;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "before_value", columnDefinition = "jsonb")
	private String beforeValue;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "after_value", columnDefinition = "jsonb")
	private String afterValue;

}
