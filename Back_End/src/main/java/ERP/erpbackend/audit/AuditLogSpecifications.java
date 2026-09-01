package ERP.erpbackend.audit;

import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/**
 * Builds the {@link AuditLog} filter predicate dynamically from only the
 * criteria actually supplied. Kept as plain Criteria predicates (no `? IS
 * NULL OR ...` JPQL) so an unset filter never reaches the generated SQL at
 * all - a static query with that pattern hits a Postgres parameter-type
 * inference error on the `createdAt` bounds.
 */
final class AuditLogSpecifications {

	private AuditLogSpecifications() {
	}

	static Specification<AuditLog> matching(UUID tenantId, UUID organizationId, String entityType, String action,
			UUID userId, Instant from, Instant to) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(cb.equal(root.get("tenantId"), tenantId));
			if (organizationId != null) {
				predicates.add(cb.equal(root.get("organizationId"), organizationId));
			}
			if (entityType != null) {
				predicates.add(cb.equal(root.get("entityType"), entityType));
			}
			if (action != null) {
				predicates.add(cb.equal(root.get("action"), action));
			}
			if (userId != null) {
				predicates.add(cb.equal(root.get("userId"), userId));
			}
			if (from != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
			}
			if (to != null) {
				predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
			}
			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}

}
