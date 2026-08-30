package ERP.erpbackend.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ERP.erpbackend.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = Replace.NONE)
class UserRepositoryTest {

	@Autowired
	private UserRepository userRepository;

	private User newUser(String email, String fullName) {
		User user = new User();
		user.setEmail(email);
		user.setPasswordHash("hashed-password");
		user.setFullName(fullName);
		return user;
	}

	@Test
	void savesAndFindsUserByEmailWithAuditFields() {
		User saved = userRepository.saveAndFlush(newUser("owner@acme.test", "Ada Owner"));

		User found = userRepository.findByEmail("owner@acme.test").orElseThrow();
		assertThat(found.getId()).isEqualTo(saved.getId());
		assertThat(found.getPasswordHash()).isEqualTo("hashed-password");
		assertThat(found.getFullName()).isEqualTo("Ada Owner");
		assertThat(found.isActive()).isTrue();
		assertThat(found.getCreatedAt()).isNotNull();
		assertThat(found.getUpdatedAt()).isNotNull();
	}

	@Test
	void rejectsDuplicateEmailPlatformWide() {
		userRepository.saveAndFlush(newUser("dup@acme.test", "First User"));

		assertThrows(DataIntegrityViolationException.class,
				() -> userRepository.saveAndFlush(newUser("dup@acme.test", "Second User")));
	}

}
