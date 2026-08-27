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
