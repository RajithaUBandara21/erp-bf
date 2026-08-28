package ERP.erpbackend.identity;

import ERP.erpbackend.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Maps one {@link Role} to one {@link Permission} it grants. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "role_permissions")
public class RolePermission extends AuditableEntity {

	@Column(name = "role_id", nullable = false)
	private UUID roleId;

	@Column(name = "permission_id", nullable = false)
	private UUID permissionId;

}
