package ERP.erpbackend.identity;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Constructed directly by {@link SecurityConfig} rather than exposed as a
 * {@code @Component} - a servlet {@code Filter} bean would otherwise also be
 * auto-registered by Spring Boot as a container filter, running the request
 * through it twice.
 */
class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtService jwtService;

	JwtAuthenticationFilter(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain) throws ServletException, IOException {
		String header = request.getHeader("Authorization");
		if (header != null && header.startsWith(BEARER_PREFIX)) {
			Optional<AuthenticatedUser> authenticatedUser =
					jwtService.parseAccessToken(header.substring(BEARER_PREFIX.length()));
			authenticatedUser.ifPresent(user -> SecurityContextHolder.getContext().setAuthentication(
					new UsernamePasswordAuthenticationToken(
							user, null, List.of(new SimpleGrantedAuthority("ROLE_USER")))));
		}
		filterChain.doFilter(request, response);
	}

}
