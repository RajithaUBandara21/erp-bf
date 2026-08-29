package ERP.erpbackend.audit;

import ERP.erpbackend.identity.AuthenticatedUser;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuditLogController {

	private static final int DEFAULT_PAGE_SIZE = 20;
	private static final int MAX_PAGE_SIZE = 100;

	private final AuditLogQueryService auditLogQueryService;

	@GetMapping("/audit-logs")
	@PreAuthorize("@perms.has('audit.view')")
	public ResponseEntity<AuditLogPageResponse> search(@AuthenticationPrincipal AuthenticatedUser caller,
			@RequestParam(required = false) String entityType,
			@RequestParam(required = false) String action,
			@RequestParam(required = false) UUID actorId,
			@RequestParam(required = false) Instant from,
			@RequestParam(required = false) Instant to,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(required = false) Integer size) {
		AuditLogFilter filter = new AuditLogFilter(entityType, action, actorId, from, to);
		Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size), Sort.by(Sort.Direction.DESC, "createdAt"));
		return ResponseEntity.ok(auditLogQueryService.search(caller, filter, pageable));
	}

	private static int clampSize(Integer requested) {
		int size = requested == null ? DEFAULT_PAGE_SIZE : requested;
		return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
	}

}
