package ERP.erpbackend.organization;

import ERP.erpbackend.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.security.SecureRandom;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "organizations")
public class Organization extends AuditableEntity {

	/** Crockford base32 minus the glyphs that misread by hand (I, L, O, U). */
	private static final String INVITE_CODE_ALPHABET = "ABCDEFGHJKMNPQRSTVWXYZ0123456789";
	private static final int INVITE_CODE_LENGTH = 10;
	private static final SecureRandom RANDOM = new SecureRandom();

	@Column(name = "tenant_id", nullable = false)
	private UUID tenantId;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String code;

	@Column(nullable = false)
	private boolean active = true;

	/** Rotatable shared secret for employee self-join. Never null once persisted (see {@link #assignInviteCodeIfMissing}). */
	@Column(name = "invite_code", nullable = false)
	private String inviteCode;

	/** A random self-join code. Guaranteeing it is unique against existing rows is the caller's job (the DB UNIQUE constraint is the backstop). */
	public static String newInviteCode() {
		StringBuilder code = new StringBuilder(INVITE_CODE_LENGTH);
		for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
			code.append(INVITE_CODE_ALPHABET.charAt(RANDOM.nextInt(INVITE_CODE_ALPHABET.length())));
		}
		return code.toString();
	}

	@PrePersist
	void assignInviteCodeIfMissing() {
		if (inviteCode == null) {
			inviteCode = newInviteCode();
		}
	}

}
