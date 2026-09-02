export type ClientType = "WEB" | "MOBILE" | "DESKTOP" | "TABLET";

export interface TokenResponse {
	accessToken: string;
	refreshToken: string;
	expiresIn: number;
	refreshExpiresIn: number;
	userId: string;
	tenantId: string;
	organizationId: string;
	email: string;
	fullName: string;
}

/** One Organization offered during the two-step login (`POST /api/auth/login` -> `SELECT_ORGANIZATION`). */
export interface MembershipOption {
	membershipId: string;
	organizationId: string;
	organizationName: string;
}

/**
 * Result of `POST /api/auth/login`. `AUTHENTICATED` carries the issued `session`; `SELECT_ORGANIZATION`
 * carries a single-use `selectionToken` and the Organizations to choose between, with no session yet.
 */
export type LoginResponse =
	| { outcome: "AUTHENTICATED"; session: TokenResponse; selectionToken: null; organizations: null }
	| { outcome: "SELECT_ORGANIZATION"; session: null; selectionToken: string; organizations: MembershipOption[] };

/** `200` body of `POST /api/auth/verify-email` - a human-readable status message plus the Organization name. */
export interface VerifyEmailResponse {
	message: string;
	organizationName: string;
}

export interface ErrorResponse {
	message: string;
	errors: Record<string, string>;
}

export type ActionResult<T> =
	| { success: true; data: T }
	| { success: false; error: string; fieldErrors?: Record<string, string> };

/**
 * `ActionResult` plus the `unauthorized` case an authed request can hit; callers must branch on it,
 * not fold it into `error`. The error case also carries the HTTP `status` (when there was a response)
 * so callers can special-case things like a 404.
 */
export type AuthedResult<T> =
	| { success: true; data: T }
	| { success: false; error: string; fieldErrors?: Record<string, string>; status?: number }
	| { success: false; unauthorized: true };
