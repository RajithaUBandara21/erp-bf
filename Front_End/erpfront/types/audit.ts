// Mirrors the backend audit module's DTOs one-for-one (ERP.erpbackend.audit:
// AuditLogResponse, AuditLogPageResponse). Keep these in sync when the API changes.

/** One audit log row - `AuditLogResponse`. `actorName`/`actorEmail`/`organizationName` are `null`
 * when the referenced user or organization no longer exists; `beforeValue`/`afterValue` are `null`
 * when the action didn't record that side (e.g. a create has no `beforeValue`). */
export interface AuditLogEntry {
	id: string;
	createdAt: string;
	actorId: string | null;
	actorName: string | null;
	actorEmail: string | null;
	entityType: string;
	entityId: string | null;
	action: string;
	organizationId: string | null;
	organizationName: string | null;
	beforeValue: Record<string, unknown> | null;
	afterValue: Record<string, unknown> | null;
}

/** A page envelope for the viewer UI - `AuditLogPageResponse`, not Spring Data's `Page`. */
export interface AuditLogPage {
	content: AuditLogEntry[];
	page: number;
	size: number;
	totalElements: number;
	totalPages: number;
}

/** Search criteria for `GET /api/audit-logs` - every field optional, mirroring `AuditLogFilter`. */
export interface AuditLogQueryParams {
	entityType?: string;
	action?: string;
	actorId?: string;
	from?: string;
	to?: string;
	page?: number;
	size?: number;
}
