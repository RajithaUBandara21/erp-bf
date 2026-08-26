package ERP.erpbackend.identity;

import java.util.UUID;

public record RegisterResponse(UUID userId, UUID tenantId, UUID organizationId, String email) {

	public static RegisterResponse from(RegisteredAccount account) {
		return new RegisterResponse(account.userId(), account.tenantId(), account.organizationId(), account.email());
	}

}
