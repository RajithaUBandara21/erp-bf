package ERP.erpbackend.identity;

import ERP.erpbackend.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A platform-wide account. Organization scope and Role assignment live on {@link Membership}, not here. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User extends AuditableEntity {

	@Column(nullable = false)
	private String email;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(name = "full_name", nullable = false)
	private String fullName;

	@Column(nullable = false)
	private boolean active = true;

	@Column(name = "platform_super_admin", nullable = false)
	private boolean platformSuperAdmin = false;

	@Column(name = "must_change_password", nullable = false)
	private boolean mustChangePassword = false;

}
