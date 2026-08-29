package ERP.erpbackend;

import ERP.erpbackend.common.JpaAuditingConfig;
import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * {@code @DataJpaTest}'s component scan doesn't pick up {@link JpaAuditingConfig}
 * on its own, so every repository test importing this class gets JPA auditing
 * (@CreatedDate/@LastModifiedDate) explicitly rather than silently missing it.
 */
@TestConfiguration(proxyBeanMethods = false)
@Import(JpaAuditingConfig.class)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer<?> postgresContainer() {
		return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));
	}

	@Bean
	@ServiceConnection(name = "redis")
	RedisContainer redisContainer() {
		return new RedisContainer(DockerImageName.parse("redis:7-alpine"));
	}

}
