// Mirrors the backend organization module's records one-for-one
// (ERP.erpbackend.organization: OrganizationDetail, OrganizationListView).
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
