import { getLocale } from "next-intl/server";
import { Link, redirect } from "@/i18n/navigation";
import { authedFetch } from "@/lib/api";
import { can, fetchMyPermissions } from "@/lib/permissions";
import type { RoleSummary } from "@/types/roles";
import { RoleList } from "@/components/roles/RoleList";

// i18n keys (build-plan 4): roles_title, roles_intro, new_role, roles_forbidden, roles_load_error
export default async function RolesPage() {
	const [result, perms] = await Promise.all([
		authedFetch<RoleSummary[]>("/api/roles"),
		fetchMyPermissions(),
	]);

	if (!result.success && "unauthorized" in result) {
		redirect({ href: "/sign-in", locale: await getLocale() });
	}

	const forbidden = !result.success && result.status === 403;

	return (
		<div>
			<div className="mb-6 flex flex-wrap items-start justify-between gap-3">
				<div>
					<h1 className="text-xl font-semibold">Roles &amp; permissions</h1>
					<p className="mt-1 text-[13px] text-muted">
						Roles bundle permissions. Assign a role to a user to grant everything it allows.
					</p>
				</div>
				{result.success && can(perms, "role.create") && (
					<Link
						href="/settings/roles/new"
						className="min-h-9 rounded-[5px] bg-accent px-3.5 py-2 text-[13px] font-semibold text-accent-ink hover:bg-accent-hover"
					>
						New role
					</Link>
				)}
			</div>

			{result.success ? (
				<RoleList roles={result.data ?? []} />
			) : (
				<div className="rounded-lg border border-border bg-surface p-6 text-[13px] text-muted shadow-sm">
					{forbidden
						? "You don't have access to roles and permissions. Ask an administrator if you need it."
						: "We couldn't load roles right now. Please refresh to try again."}
				</div>
			)}
		</div>
	);
}
