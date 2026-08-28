// Mirrors the backend identity module's role/permission DTOs one-for-one
// (ERP.erpbackend.identity: RoleSummaryResponse, RoleDetailResponse, RoleMemberResponse,
// PermissionResponse, MePermissionsResponse). Keep these in sync when the API changes.

export type PermissionAction = "VIEW" | "CREATE" | "EDIT" | "DELETE" | "APPROVE";

/** One row of the role list - `RoleSummaryResponse`. */
export interface RoleSummary {
	id: string;
	name: string;
	description: string | null;
	systemManaged: boolean;
	memberCount: number;
	permissionCount: number;
}

/** A user assigned to a role - `RoleMemberResponse`. */
export interface RoleMember {
	userId: string;
	fullName: string;
	email: string;
}

/** Full role record - `RoleDetailResponse`. */
export interface RoleDetail {
	id: string;
	name: string;
	description: string | null;
	systemManaged: boolean;
	memberCount: number;
	permissionCount: number;
	permissionCodes: string[];
	members: RoleMember[];
}

/** One grantable capability in the catalog - `PermissionResponse`. `code` is `"<resource>.<action lowercased>"`. */
export interface PermissionCatalogEntry {
	code: string;
	resource: string;
	action: PermissionAction;
}

/** The caller's own effective permission codes - `MePermissionsResponse`. */
export interface MePermissions {
	permissions: string[];
}

/** One entry in the tenant user directory - `GET /api/users` / `UserSummaryResponse`. Backs the member picker. */
export interface UserSummary {
	id: string;
	fullName: string;
	email: string;
}

/** Body for `POST /api/roles` and `PUT /api/roles/{id}`. */
export interface RoleWriteRequest {
	name: string;
	description: string | null;
	permissionCodes: string[];
}
