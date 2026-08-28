import { cookies } from "next/headers";
import type { TokenResponse } from "@/types/auth";

export const ACCESS_TOKEN_COOKIE = "accessToken";
export const REFRESH_TOKEN_COOKIE = "refreshToken";
// Non-sensitive flag: present iff the login opted into "remember me". The proxy reads it to decide
// whether a refreshed `refreshToken` cookie stays persistent or session-scoped, since the browser
// never tells the server how the original cookie was scoped.
export const REMEMBER_TOKEN_COOKIE = "rememberMe";

// httpOnly + lax + path=/ , shared so setAuthCookies and proxy.ts write these cookies identically.
const SHARED_COOKIE_OPTIONS = {
	httpOnly: true,
	sameSite: "lax",
	path: "/",
} as const;

function secureOutsideDev(): boolean {
	return process.env.NODE_ENV !== "development";
}

/** Options for the short-lived access-token cookie. Reused by `setAuthCookies` and the proxy's refresh path. */
export function accessCookieOptions(expiresIn: number) {
	return { ...SHARED_COOKIE_OPTIONS, secure: secureOutsideDev(), maxAge: expiresIn };
}

/**
 * Options for the refresh-token cookie. `remember === false` omits `maxAge` so the cookie dies with
 * the browser session; `true` persists it for the refresh-token lifetime. The argument is required -
 * an ambiguous `undefined` is what let the proxy silently upgrade a session cookie to persistent.
 */
export function refreshCookieOptions(refreshExpiresIn: number, remember: boolean) {
	return {
		...SHARED_COOKIE_OPTIONS,
		secure: secureOutsideDev(),
		...(remember ? { maxAge: refreshExpiresIn } : {}),
	};
}

/** The only code that should read/write these cookies - later features (refresh, logout, session list) extend this file, not touch cookies directly. */
export async function setAuthCookies(tokens: TokenResponse, remember: boolean): Promise<void> {
	const cookieStore = await cookies();
	cookieStore.set(ACCESS_TOKEN_COOKIE, tokens.accessToken, accessCookieOptions(tokens.expiresIn));
	cookieStore.set(REFRESH_TOKEN_COOKIE, tokens.refreshToken, refreshCookieOptions(tokens.refreshExpiresIn, remember));
	if (remember) {
		cookieStore.set(REMEMBER_TOKEN_COOKIE, "1", refreshCookieOptions(tokens.refreshExpiresIn, true));
	} else {
		cookieStore.delete(REMEMBER_TOKEN_COOKIE);
	}
}

export async function clearAuthCookies(): Promise<void> {
	const cookieStore = await cookies();
	cookieStore.delete(ACCESS_TOKEN_COOKIE);
	cookieStore.delete(REFRESH_TOKEN_COOKIE);
	cookieStore.delete(REMEMBER_TOKEN_COOKIE);
}

/** A live access token is present. Use this where only a real, usable session should count (e.g. the sign-in page). */
export async function hasAccessToken(): Promise<boolean> {
	return (await cookies()).has(ACCESS_TOKEN_COOKIE);
}

export async function getRefreshToken(): Promise<string | undefined> {
	return (await cookies()).get(REFRESH_TOKEN_COOKIE)?.value;
}

/**
 * Any auth cookie is present - a lone refresh cookie counts, since the proxy can trade it for a
 * fresh access token. Use this to guard `/settings/*` so an in-flight refresh isn't bounced to sign-in.
 */
export async function hasSession(): Promise<boolean> {
	const cookieStore = await cookies();
	return cookieStore.has(ACCESS_TOKEN_COOKIE) || cookieStore.has(REFRESH_TOKEN_COOKIE);
}
