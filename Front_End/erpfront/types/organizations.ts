// Mirrors the backend organization/identity module records one-for-one
// (ERP.erpbackend.organization: OrganizationDetail, OrganizationListView;
// ERP.erpbackend.identity: InviteCodeResponse, PendingJoinRequestResponse).
// Keep these in sync when the API changes.

/** One Organization under the caller's tenant - `OrganizationDetail`. */
export interface OrganizationDetail {
	id: string;
	name: string;
	code: string;
	active: boolean;
	createdAt: string;
}

/** `GET /api/organizations` response - the locked wire shape for this endpoint. */
export interface OrganizationListView {
	plan: string | null;
	maxOrganizations: number;
	organizations: OrganizationDetail[];
}

/** `GET /api/organizations/invite-code` / `POST /api/organizations/invite-code/rotate` - `InviteCodeResponse`. */
export interface InviteCode {
	inviteCode: string;
}

/** One verified-but-PENDING self-join request - `PendingJoinRequestResponse`. */
export interface PendingJoinRequest {
	membershipId: string;
	userId: string;
	fullName: string;
	email: string;
	requestedAt: string;
}
