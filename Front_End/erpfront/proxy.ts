import { NextResponse, type NextRequest } from "next/server";
import createMiddleware from "next-intl/middleware";
import { hasLocale } from "next-intl";
import { routing } from "@/i18n/routing";
import type { TokenResponse } from "@/types/auth";
import {
	ACCESS_TOKEN_COOKIE,
	REFRESH_TOKEN_COOKIE,
	REMEMBER_TOKEN_COOKIE,
	accessCookieOptions,
	refreshCookieOptions,
} from "@/lib/auth-cookies";
import { fetchWithTimeout, API_BASE_URL } from "@/lib/http";

// Runs on every route except Next's static/image internals and the favicon - locale detection
// (below) needs to see sign-in/sign-up too, unlike the old literal exclusion, so the auth guard
// carves those back out itself once the locale prefix is resolved.
export const config = {
	matcher: ["/((?!_next/static|_next/image|favicon.ico).*)"],
};

const handleI18nRouting = createMiddleware(routing);
const PUBLIC_PATHS = ["/sign-in", "/sign-up"];

/**
 * Merges next-intl's locale routing with the auth guard - only one proxy function is allowed per
 * project. Locale detection runs first: a bare or mismatched path (e.g. `/sign-in`) is redirected
 * to its locale-prefixed form (e.g. `/en/sign-in`) and the auth guard is skipped for that redirect,
 * since the browser re-requests the prefixed URL and hits this function again. Once a request
 * already carries a valid locale prefix, the auth guard reads the pathname with that prefix
 * stripped so its sign-in/sign-up allowlist keeps matching unprefixed, like before i18n routing.
 */
export async function proxy(request: NextRequest): Promise<NextResponse> {
	const intlResponse = handleI18nRouting(request);
	if (intlResponse.headers.has("location")) {
		return intlResponse;
	}

	const { locale, pathname } = splitLocale(request.nextUrl.pathname);
	if (PUBLIC_PATHS.some((path) => pathname === path || pathname.startsWith(`${path}/`))) {
		return intlResponse;
	}

	if (request.cookies.has(ACCESS_TOKEN_COOKIE)) {
		return intlResponse;
	}

	const refreshToken = request.cookies.get(REFRESH_TOKEN_COOKIE)?.value;
	if (!refreshToken) {
		return redirectToSignIn(request, locale);
	}

	const tokens = await refreshTokens(refreshToken);
	if (!tokens) {
		return redirectToSignIn(request, locale);
	}

	// Mirror the new access token onto the request so this same render sees it, then persist the
	// cookies on the response for the browser. The remember-flag cookie carries the original login's
	// "remember me" choice: present means keep the refreshed refresh-token cookie persistent, absent
	// means keep it session-scoped so a deliberate "don't remember me" is not silently upgraded.
	const remember = request.cookies.has(REMEMBER_TOKEN_COOKIE);
	request.cookies.set(ACCESS_TOKEN_COOKIE, tokens.accessToken);
	// next-intl's own NextResponse.next({request}) call above never touched this request - it built
	// intlResponse from its own copy of the headers and encoded them as x-middleware-override-headers/
	// x-middleware-request-* response headers (Next's mechanism for forwarding request header changes
	// out of middleware). Replaying that encoding onto our own request here carries x-next-intl-locale
	// forward (F-02); without it, this branch's render falls back to the default locale on every
	// access-token refresh.
	const response = NextResponse.next({ request: { headers: mergeIntlRequestHeaders(request, intlResponse) } });
	// Carry over whatever next-intl's own routing response set (e.g. the NEXT_LOCALE cookie) onto
	// the response we're building for the token refresh, instead of the two clobbering each other.
	for (const cookie of intlResponse.cookies.getAll()) {
		response.cookies.set(cookie);
	}
	response.cookies.set(ACCESS_TOKEN_COOKIE, tokens.accessToken, accessCookieOptions(tokens.expiresIn));
	response.cookies.set(REFRESH_TOKEN_COOKIE, tokens.refreshToken, refreshCookieOptions(tokens.refreshExpiresIn, remember));
	if (remember) {
		response.cookies.set(REMEMBER_TOKEN_COOKIE, "1", refreshCookieOptions(tokens.refreshExpiresIn, true));
	}
	return response;
}

// Decodes the request-header overrides next-intl's middleware encoded onto its response (Next's
// x-middleware-override-headers/x-middleware-request-* convention for forwarding header changes out
// of middleware) and replays them onto `request`'s own headers, so a response built from `request`
// still carries locale detection's result (e.g. x-next-intl-locale) instead of losing it.
function mergeIntlRequestHeaders(request: NextRequest, intlResponse: NextResponse): Headers {
	const headers = new Headers(request.headers);
	const overridden = intlResponse.headers.get("x-middleware-override-headers");
	if (!overridden) {
		return headers;
	}
	for (const key of overridden.split(",")) {
		// intlResponse was captured before the access-token cookie refresh below, so it always carries
		// a "cookie" entry in this list (next-intl forwards every header it read, not just the one it
		// changed) whose value predates that refresh - replaying it would silently drop the fresh
		// access token from the request this same render sees. `request`'s own cookie mutations must
		// win, so cookie is intentionally excluded here.
		if (key === "cookie") {
			continue;
		}
		const value = intlResponse.headers.get(`x-middleware-request-${key}`);
		if (value !== null) {
			headers.set(key, value);
		}
	}
	return headers;
}

// Splits a resolved, locale-prefixed pathname (e.g. `/en/settings`) into its locale and the
// un-prefixed pathname the pre-i18n auth guard logic below was written against (e.g. `/settings`).
function splitLocale(pathname: string): { locale: string; pathname: string } {
	const [, maybeLocale, ...rest] = pathname.split("/");
	if (hasLocale(routing.locales, maybeLocale)) {
		return { locale: maybeLocale, pathname: `/${rest.join("/")}` };
	}
	return { locale: routing.defaultLocale, pathname };
}

// Deliberately does not clear the auth cookies. Refresh tokens are single-use, so two overlapping
// requests after the access token expires both spend the same cookie - one wins, one 401s here.
// Deleting cookies on that loss would race the winner's fresh Set-Cookie and force a real
// re-login (F-04). A genuinely dead token just fails the next refresh too and the user signs in then.
function redirectToSignIn(request: NextRequest, locale: string): NextResponse {
	return NextResponse.redirect(new URL(`/${locale}/sign-in`, request.url));
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
