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

/** Links a {@link User} to an external identity provider account (Google today) for sign-in and account linking. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "oauth_accounts")
public class OAuthAccount extends AuditableEntity {

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private OAuthProvider provider;

	@Column(name = "provider_user_id", nullable = false)
	private String providerUserId;

	// Informational display data only - never used for authorization (see current-feature.md).
	@Column(name = "provider_email")
	private String providerEmail;

}
