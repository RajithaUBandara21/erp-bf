import { createNavigation } from "next-intl/navigation";
import { routing } from "@/i18n/routing";

const navigation = createNavigation(routing);

export const { Link, usePathname, useRouter, getPathname } = navigation;

// Re-typed with an explicit `never` return: TypeScript doesn't reliably resolve next-intl's own
// deeply-conditional generic type for `redirect` to `never` at call sites, so code after a
// `redirect(...)` call was type-checking as reachable (breaking union narrowing on the branches
// that follow it). Wrapping it in a plain function TS can trust fixes that everywhere at once.
export function redirect(...args: Parameters<typeof navigation.redirect>): never {
	return navigation.redirect(...args);
}
