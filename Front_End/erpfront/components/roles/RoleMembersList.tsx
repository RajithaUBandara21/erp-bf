import type { RoleMember } from "@/types/roles";

// i18n keys (build-plan 4): members, members_empty
// Read-only member list. Step 8 adds the interactive add/remove variant.

export function RoleMembersList({ members }: { members: RoleMember[] }) {
	return (
		<section className="rounded-lg border border-border bg-surface p-5 shadow-sm">
			<h2 className="mb-3 text-sm font-semibold">Members</h2>
			{members.length === 0 ? (
				<p className="py-2 text-center text-xs text-muted">No users have this role yet.</p>
			) : (
				<ul className="flex flex-col gap-2">
					{members.map((member) => (
						<li key={member.userId} className="flex items-center gap-2.5">
							<span className="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-full bg-surface-alt text-[11px] font-bold text-muted">
								{initials(member.fullName)}
							</span>
							<div className="min-w-0">
								<div className="text-[13px] font-semibold">{member.fullName}</div>
								<div className="truncate text-xs text-muted">{member.email}</div>
							</div>
						</li>
					))}
				</ul>
			)}
		</section>
	);
}

export function initials(name: string): string {
	const letters = name
		.trim()
		.split(/\s+/)
		.map((word) => word[0] ?? "")
		.slice(0, 2)
		.join("")
		.toUpperCase();
	return letters || "?";
}
