import { NextResponse, type NextRequest } from "next/server";
import type { TokenResponse } from "@/types/auth";
import {
	ACCESS_TOKEN_COOKIE,
	REFRESH_TOKEN_COOKIE,
	REMEMBER_TOKEN_COOKIE,
	accessCookieOptions,
	refreshCookieOptions,
} from "@/lib/auth-cookies";
import { fetchWithTimeout, API_BASE_URL } from "@/lib/http";

export const config = { matcher: "/settings/:path*" };

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
		return redirectToSignIn(request);
	}

	// Mirror the new access token onto the request so this same render sees it, then persist the
	// cookies on the response for the browser. The remember-flag cookie carries the original login's
	// "remember me" choice: present means keep the refreshed refresh-token cookie persistent, absent
	// means keep it session-scoped so a deliberate "don't remember me" is not silently upgraded.
	const remember = request.cookies.has(REMEMBER_TOKEN_COOKIE);
	request.cookies.set(ACCESS_TOKEN_COOKIE, tokens.accessToken);
	const response = NextResponse.next({ request });
	response.cookies.set(ACCESS_TOKEN_COOKIE, tokens.accessToken, accessCookieOptions(tokens.expiresIn));
	response.cookies.set(REFRESH_TOKEN_COOKIE, tokens.refreshToken, refreshCookieOptions(tokens.refreshExpiresIn, remember));
	if (remember) {
		response.cookies.set(REMEMBER_TOKEN_COOKIE, "1", refreshCookieOptions(tokens.refreshExpiresIn, true));
	}
	return response;
}

// Deliberately does not clear the auth cookies. Refresh tokens are single-use, so two overlapping
// /settings/* requests after the access token expires both spend the same cookie - one wins, one 401s
// here. Deleting cookies on that loss would race the winner's fresh Set-Cookie and force a real
// re-login (F-04). A genuinely dead token just fails the next refresh too and the user signs in then.
function redirectToSignIn(request: NextRequest): NextResponse {
	return NextResponse.redirect(new URL("/sign-in", request.url));
}

async function refreshTokens(refreshToken: string): Promise<TokenResponse | null> {
	try {
		const res = await fetchWithTimeout(`${API_BASE_URL}/api/auth/refresh`, {
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
