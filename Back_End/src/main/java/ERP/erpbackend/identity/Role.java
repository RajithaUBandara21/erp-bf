package ERP.erpbackend.identity;

import ERP.erpbackend.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A tenant-owned bundle of permissions assignable to users. {@code systemManaged} roles are provisioned per tenant and not user-editable. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "roles")
public class Role extends AuditableEntity {

	@Column(name = "tenant_id", nullable = false)
	private UUID tenantId;

	@Column(nullable = false)
	private String name;

	@Column
	private String description;

	@Column(name = "system_managed", nullable = false)
	private boolean systemManaged;

}
