package ERP.erpbackend.identity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, UUID> {

	Optional<OAuthAccount> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);

	Optional<OAuthAccount> findByUserIdAndProvider(UUID userId, OAuthProvider provider);

	// A derived delete query loads then removes each match, unlike SimpleJpaRepository's other CRUD
	// methods - it needs its own transaction boundary rather than relying on one from the caller.
	@Transactional
	void deleteByUserIdAndProvider(UUID userId, OAuthProvider provider);

}
