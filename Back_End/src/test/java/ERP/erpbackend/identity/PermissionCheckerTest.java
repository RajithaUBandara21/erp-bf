package ERP.erpbackend.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class PermissionCheckerTest {

	private final EffectivePermissionResolver resolver = mock(EffectivePermissionResolver.class);
	private final PermissionChecker checker = new PermissionChecker(resolver);

	private static final AuthenticatedUser USER = new AuthenticatedUser(
			UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "ada@acme.test", UUID.randomUUID());

	@AfterEach
	void clearContext() {
		SecurityContextHolder.clearContext();
	}

	private void authenticate() {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(USER, null, List.of()));
	}

	@Test
	void returnsFalseWhenNoAuthenticationIsPresent() {
		assertThat(checker.has("role.view")).isFalse();
	}

	@Test
	void returnsTrueWhenTheResolvedSetContainsTheCode() {
		authenticate();
		when(resolver.resolve(USER.userId(), USER.tenantId())).thenReturn(Set.of("role.view", "role.edit"));

		assertThat(checker.has("role.view")).isTrue();
	}

	@Test
	void returnsFalseWhenTheResolvedSetLacksTheCode() {
		authenticate();
		when(resolver.resolve(USER.userId(), USER.tenantId())).thenReturn(Set.of("role.view"));

		assertThat(checker.has("role.delete")).isFalse();
	}

}
