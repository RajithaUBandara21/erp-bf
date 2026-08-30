package ERP.erpbackend.identity;

import ERP.erpbackend.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Links a {@link User} to one {@link ERP.erpbackend.organization.Organization}, carrying Role
 * scope. {@code tenantId} is denormalized so tenant-scoped queries need no join through
 * {@code organizations}. {@code locationId} has no DB-level foreign key: {@code locationType}
 * picks which of Branch/Store/Warehouse it refers to, since those are separate tables with no
 * shared supertype.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "memberships")
public class Membership extends AuditableEntity {

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "tenant_id", nullable = false)
	private UUID tenantId;

	@Column(name = "organization_id", nullable = false)
	private UUID organizationId;

	@Enumerated(EnumType.STRING)
	@Column(name = "location_type")
	private LocationType locationType;

	@Column(name = "location_id")
	private UUID locationId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private MembershipStatus status;

}
