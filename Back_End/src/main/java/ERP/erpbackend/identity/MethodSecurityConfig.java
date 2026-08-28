package ERP.erpbackend.identity;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/** Enables {@code @PreAuthorize} so endpoints can gate on {@code @perms.has('<code>')}. */
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {
}
