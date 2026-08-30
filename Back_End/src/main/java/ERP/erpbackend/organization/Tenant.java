package ERP.erpbackend.organization;

import ERP.erpbackend.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tenants")
public class Tenant extends AuditableEntity {

	@Column(nullable = false)
	private String name;

	@Column(nullable = false, unique = true)
	private String code;

	@Column(nullable = false)
	private boolean active = true;

	@Column
	private String plan;

	@Column(name = "max_organizations", nullable = false)
	private int maxOrganizations = 1;

}
