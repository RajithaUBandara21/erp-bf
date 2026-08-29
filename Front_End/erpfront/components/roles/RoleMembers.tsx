"use client";

import { useState, useTransition } from "react";
import { useLocale } from "next-intl";
import { assignMember, unassignMember, type RoleFormState } from "@/actions/roles";
import type { RoleMember, UserSummary } from "@/types/roles";
import { initials } from "@/components/roles/RoleMembersList";

// i18n keys (build-plan 4): members, members_empty, add_member, add, adding, remove, removing,
// no_users_to_add, select_user_placeholder

interface RoleMembersProps {
	roleId: string;
	members: RoleMember[];
	/** Tenant users offered in the "add member" picker (already-assigned users are filtered out here). */
	candidates: UserSummary[];
	canEdit: boolean;
}

/** Interactive Members panel: list, remove a member, add one from the tenant directory. */
export function RoleMembers({ roleId, members, candidates, canEdit }: RoleMembersProps) {
	const locale = useLocale();
	const [error, setError] = useState<string | null>(null);
	const [selectedId, setSelectedId] = useState("");
	const [pending, startTransition] = useTransition();

	const memberIds = new Set(members.map((member) => member.userId));
	const addable = candidates.filter((user) => !memberIds.has(user.id));

	function run(action: () => Promise<RoleFormState>) {
		setError(null);
		startTransition(async () => {
			const result = await action();
			if (result?.error) {
				setError(result.error);
			} else {
				setSelectedId("");
			}
		});
	}

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
							<div className="min-w-0 flex-1">
								<div className="text-[13px] font-semibold">{member.fullName}</div>
								<div className="truncate text-xs text-muted">{member.email}</div>
							</div>
							{canEdit && (
								<button
									type="button"
									onClick={() => run(() => unassignMember(locale, roleId, member.userId))}
									disabled={pending}
									className="min-h-9 rounded-[5px] border border-border bg-surface px-2.5 py-1.5 text-xs font-semibold text-danger hover:border-danger hover:bg-danger-bg disabled:cursor-not-allowed disabled:opacity-55"
									aria-label={`Remove ${member.fullName} from this role`}
								>
									Remove
								</button>
							)}
						</li>
					))}
				</ul>
			)}

			{canEdit && (
				<div className="mt-4 border-t border-border pt-4">
					<div className="flex items-center gap-2">
						<select
							value={selectedId}
							onChange={(event) => setSelectedId(event.target.value)}
							disabled={pending || addable.length === 0}
							className="min-h-9 flex-1 rounded-[5px] border border-border bg-bg px-2.5 py-2 text-[13px] text-text focus:border-accent focus:outline-none disabled:cursor-not-allowed disabled:opacity-55"
							aria-label="Choose a user to add to this role"
						>
							<option value="">Add a user...</option>
							{addable.map((user) => (
								<option key={user.id} value={user.id}>
									{user.fullName} ({user.email})
								</option>
							))}
						</select>
						<button
							type="button"
							onClick={() => run(() => assignMember(locale, roleId, selectedId))}
							disabled={pending || !selectedId}
							className="min-h-9 rounded-[5px] bg-accent px-3.5 py-2 text-[13px] font-semibold text-accent-ink hover:bg-accent-hover disabled:cursor-not-allowed disabled:opacity-55"
						>
							{pending ? "Adding..." : "Add"}
						</button>
					</div>
					{addable.length === 0 && (
						<p className="mt-2 text-[11px] text-faint">No other users to add.</p>
					)}
				</div>
			)}

			{error && <p className="mt-3 text-[11px] text-danger">{error}</p>}
		</section>
	);
}
