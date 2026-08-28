package ERP.erpbackend.identity;

import ERP.erpbackend.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Assigns one {@link Role} to one {@link User}. {@code tenantId} is denormalized so tenant-scoped queries need no join through {@code roles}. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_roles")
public class UserRole extends AuditableEntity {

	@Column(name = "tenant_id", nullable = false)
	private UUID tenantId;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "role_id", nullable = false)
	private UUID roleId;

}
