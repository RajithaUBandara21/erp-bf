// Leaf module: no `next/headers` or React imports, so `proxy.ts` can import it without pulling
// request-scoped APIs into the proxy.

export const REQUEST_TIMEOUT_MS = 5000;

/**
 * `fetch` with a default timeout. Node's `fetch` has none, so a backend that accepts the socket and
 * then stalls would hang the caller indefinitely. The abort rejects the promise like any other
 * network failure, which every call site already handles with a try/catch. A caller-supplied
 * `signal` wins and disables the default timeout.
 */
export function fetchWithTimeout(...args: Parameters<typeof fetch>): Promise<Response> {
	const [input, init] = args;
	if (init?.signal) {
		return fetch(input, init);
	}
	return fetch(input, { ...init, signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS) });
}
