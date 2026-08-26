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
