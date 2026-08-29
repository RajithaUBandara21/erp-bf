package ERP.erpbackend.audit;

import java.util.List;

/** A page envelope with just the fields the viewer UI needs - not Spring Data's {@code Page}, whose JSON shape isn't a stable contract. */
public record AuditLogPageResponse(
		List<AuditLogResponse> content, int page, int size, long totalElements, int totalPages) {
}
