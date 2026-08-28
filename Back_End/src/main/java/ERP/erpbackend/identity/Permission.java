package ERP.erpbackend.identity;

import ERP.erpbackend.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A grantable capability, identified by the canonical {@code resource.action} code. Global, not tenant-owned. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "permissions")
public class Permission extends AuditableEntity {

	@Column(nullable = false, unique = true)
	private String code;

	@Column(nullable = false)
	private String resource;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PermissionAction action;

}
