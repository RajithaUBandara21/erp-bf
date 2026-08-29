/** Mirrors the backend `LinkStatusResponse` one-for-one. */
export interface GoogleLinkStatus {
	linked: boolean;
	linkedEmail: string | null;
}

/** Mirrors the backend `AuthorizationUrlResponse` one-for-one. */
export interface AuthorizationUrlResponse {
	authorizationUrl: string;
}
