package ERP.erpbackend.identity;

import ERP.erpbackend.organization.OrganizationService;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

	private static final String INVALID_CREDENTIALS = "Invalid credentials";

	private final OrganizationService organizationService;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final RefreshTokenService refreshTokenService;

	public TokenResponse login(LoginRequest request) {
		UUID tenantId = organizationService.findTenantIdByCode(normalize(request.organizationCode()))
				.orElseThrow(AuthenticationService::invalidCredentials);

		User user = userRepository.findByTenantIdAndEmail(tenantId, request.email().toLowerCase(Locale.ROOT))
				.orElseThrow(AuthenticationService::invalidCredentials);

		if (!user.isActive() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw invalidCredentials();
		}

		return issueTokens(user);
	}

	public TokenResponse refresh(RefreshRequest request) {
		UUID userId = refreshTokenService.consume(request.refreshToken())
				.orElseThrow(AuthenticationService::invalidCredentials);

		User user = userRepository.findById(userId)
				.filter(User::isActive)
				.orElseThrow(AuthenticationService::invalidCredentials);

		return issueTokens(user);
	}

	public void logout(RefreshRequest request) {
		refreshTokenService.revoke(request.refreshToken());
	}

	private TokenResponse issueTokens(User user) {
		AuthenticatedUser authenticatedUser =
				new AuthenticatedUser(user.getId(), user.getTenantId(), user.getOrganizationId(), user.getEmail());
		String accessToken = jwtService.issueAccessToken(authenticatedUser);
		String refreshToken = refreshTokenService.issue(user.getId());
		return new TokenResponse(accessToken, refreshToken, jwtService.accessTokenTtlSeconds(),
				user.getId(), user.getTenantId(), user.getOrganizationId(), user.getEmail(), user.getFullName());
	}

	private static String normalize(String value) {
		return value.trim().toLowerCase(Locale.ROOT);
	}

	private static ResponseStatusException invalidCredentials() {
		return new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
	}

}
