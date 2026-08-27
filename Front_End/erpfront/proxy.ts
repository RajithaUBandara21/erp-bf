import { NextResponse, type NextRequest } from "next/server";
import type { TokenResponse } from "@/types/auth";
import {
	ACCESS_TOKEN_COOKIE,
	REFRESH_TOKEN_COOKIE,
	accessCookieOptions,
	refreshCookieOptions,
} from "@/lib/auth-cookies";

export const config = { matcher: "/settings/:path*" };

// Proxy runs on the Node.js runtime, so this env var is available (unlike the old Edge middleware).
const API_BASE_URL = process.env.INTERNAL_API_BASE_URL ?? "http://localhost:8080";

/**
 * Guards `/settings/*`: a live access token passes straight through; an expired one is silently
 * rotated from the refresh token; anything else redirects to `/sign-in`. Not a complete auth
 * check on its own - the settings layout and every Server Action re-check independently.
 */
export async function proxy(request: NextRequest): Promise<NextResponse> {
	if (request.cookies.has(ACCESS_TOKEN_COOKIE)) {
		return NextResponse.next();
	}

	const refreshToken = request.cookies.get(REFRESH_TOKEN_COOKIE)?.value;
	if (!refreshToken) {
		return redirectToSignIn(request);
	}

	const tokens = await refreshTokens(refreshToken);
	if (!tokens) {
		// The refresh token is gone or rejected - clear it so /sign-in isn't stuck thinking there's a session.
		return redirectToSignIn(request);
	}

	// Mirror the new access token onto the request so this same render sees it, then persist both
	// cookies on the response for the browser. The proxy has no "remember me" signal, so the
	// refreshed refresh-token cookie is always persistent.
	request.cookies.set(ACCESS_TOKEN_COOKIE, tokens.accessToken);
	const response = NextResponse.next({ request });
	response.cookies.set(ACCESS_TOKEN_COOKIE, tokens.accessToken, accessCookieOptions(tokens.expiresIn));
	response.cookies.set(REFRESH_TOKEN_COOKIE, tokens.refreshToken, refreshCookieOptions(tokens.refreshExpiresIn));
	return response;
}

function redirectToSignIn(request: NextRequest): NextResponse {
	const response = NextResponse.redirect(new URL("/sign-in", request.url));
	response.cookies.delete(ACCESS_TOKEN_COOKIE);
	response.cookies.delete(REFRESH_TOKEN_COOKIE);
	return response;
}

async function refreshTokens(refreshToken: string): Promise<TokenResponse | null> {
	try {
		const res = await fetch(`${API_BASE_URL}/api/auth/refresh`, {
			method: "POST",
			headers: { "Content-Type": "application/json" },
			body: JSON.stringify({ refreshToken }),
			cache: "no-store",
		});
		return res.ok ? ((await res.json()) as TokenResponse) : null;
	} catch {
		return null;
	}
}
