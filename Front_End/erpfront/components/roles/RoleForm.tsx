"use client";

import type { FormEvent, ReactNode } from "react";
import { useState, useTransition } from "react";
import { useLocale } from "next-intl";
import { useRouter } from "next/navigation";
import { Link } from "@/i18n/navigation";
import type { MatrixGroup } from "@/lib/permission-matrix";
import type { RoleDetail } from "@/types/roles";
import { createRole, updateRole, type RoleFormState } from "@/actions/roles";
import { PermissionMatrix } from "@/components/roles/PermissionMatrix";
import { RoleMembersList } from "@/components/roles/RoleMembersList";
import { RoleTypeBadge } from "@/components/roles/RoleTypeBadge";

// i18n keys (build-plan 4): permissions, role_info, role_name_label, description_col, scope_label,
// scope_hint, perm_legend, cancel, save_changes, saving, saved, new_role, create_role, creating

type RoleFormProps =
	| { mode: "edit"; role: RoleDetail; groups: MatrixGroup[]; headerActions?: ReactNode; membersSlot?: ReactNode }
	| { mode: "create"; groups: MatrixGroup[]; headerActions?: ReactNode; membersSlot?: ReactNode };

/** Create form, or editable detail for a custom role the caller may edit. Read-only roles use RoleDetailView. */
export function RoleForm(props: RoleFormProps) {
	const isEdit = props.mode === "edit";
	const role = props.mode === "edit" ? props.role : null;

	const router = useRouter();
	const locale = useLocale();
	const [name, setName] = useState(role?.name ?? "");
	const [description, setDescription] = useState(role?.description ?? "");
	const [codes, setCodes] = useState<string[]>(role?.permissionCodes ?? []);
	const [state, setState] = useState<RoleFormState>({});
	const [saved, setSaved] = useState(false);
	const [pending, startTransition] = useTransition();

	function edit<T>(setter: (value: T) => void) {
		return (value: T) => {
			if (saved) setSaved(false);
			setter(value);
		};
	}

	function onSubmit(event: FormEvent<HTMLFormElement>) {
		event.preventDefault();
		startTransition(async () => {
			const body = {
				name: name.trim(),
				description: description.trim() || null,
				permissionCodes: codes,
			};
			// createRole redirects on success and never resolves; updateRole returns {}.
			const result = role ? await updateRole(locale, role.id, body) : await createRole(locale, body);
			setState(result);
			setSaved(role != null && !result.error && !result.fieldErrors);
			if (role && !result.error && !result.fieldErrors) {
				router.refresh();
			}
		});
	}

	return (
		<form onSubmit={onSubmit}>
			<div className="mb-6 flex flex-wrap items-start justify-between gap-3">
				<div>
					<h1 className="text-xl font-semibold">{role ? role.name : "New role"}</h1>
					{role && (
						<div className="mt-1.5 flex items-center gap-2.5 text-[13px] text-muted">
							<RoleTypeBadge systemManaged={role.systemManaged} />
							<span>{role.memberCount === 1 ? "1 member" : `${role.memberCount} members`}</span>
						</div>
					)}
				</div>
				<div className="flex items-center gap-2">
					{saved && <span className="text-[13px] font-semibold text-success">Saved</span>}
					{props.headerActions}
					<Link
						href="/settings/roles"
						className="min-h-9 rounded-[5px] border border-border bg-surface px-3.5 py-2 text-[13px] font-semibold text-text hover:bg-surface-alt"
					>
						Cancel
					</Link>
					<button
						type="submit"
						disabled={pending}
						className="min-h-9 rounded-[5px] bg-accent px-3.5 py-2 text-[13px] font-semibold text-accent-ink hover:bg-accent-hover disabled:cursor-not-allowed disabled:opacity-55"
					>
						{submitLabel(isEdit, pending)}
					</button>
				</div>
			</div>

			{state.error && (
				<div className="mb-4 rounded-[5px] bg-danger-bg px-3 py-2.5 text-xs text-danger">{state.error}</div>
			)}

			<div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_260px] lg:items-start">
				<section className="rounded-lg border border-border bg-surface p-5 shadow-sm">
					<div className="mb-3 flex items-center justify-between gap-2">
						<h2 className="text-sm font-semibold">Permissions</h2>
						<span className="text-[11px] text-faint">Scope: entire tenant</span>
					</div>
					<PermissionMatrix groups={props.groups} selectedCodes={codes} onChange={edit(setCodes)} />
					<p className="mt-3 text-[11px] text-faint">
						View lets a user open records. Create, Edit, and Delete change them. Approve confirms documents
						such as orders and invoices.
					</p>
				</section>

				<div className="flex flex-col gap-4">
					<section className="rounded-lg border border-border bg-surface p-5 shadow-sm">
						<h2 className="mb-3 text-sm font-semibold">Role info</h2>
						<div className="mb-3 flex flex-col gap-1.5">
							<label htmlFor="role-name" className="text-xs font-semibold text-muted">
								Role name
							</label>
							<input
								id="role-name"
								value={name}
								onChange={(event) => edit(setName)(event.target.value)}
								className={`min-h-9 rounded-[5px] border bg-bg px-2.5 py-2 text-[13px] text-text focus:border-accent focus:outline-none ${
									state.fieldErrors?.name ? "border-danger" : "border-border"
								}`}
							/>
							{state.fieldErrors?.name && (
								<span className="text-[11px] text-danger">{state.fieldErrors.name}</span>
							)}
						</div>
						<div className="mb-3 flex flex-col gap-1.5">
							<label htmlFor="role-desc" className="text-xs font-semibold text-muted">
								Description
							</label>
							<textarea
								id="role-desc"
								rows={3}
								value={description}
								onChange={(event) => edit(setDescription)(event.target.value)}
								className="rounded-[5px] border border-border bg-bg px-2.5 py-2 text-[13px] text-text focus:border-accent focus:outline-none"
							/>
						</div>
						<div className="flex flex-col gap-1.5">
							<label htmlFor="role-scope" className="text-xs font-semibold text-muted">
								Scope
							</label>
							<input
								id="role-scope"
								value="Entire tenant"
								disabled
								className="rounded-[5px] border border-border bg-bg px-2.5 py-2 text-[13px] text-muted"
							/>
							<span className="text-[11px] text-faint">
								Branch and store level roles are planned for a later release.
							</span>
						</div>
					</section>

					{role && (props.membersSlot ?? <RoleMembersList members={role.members} />)}
				</div>
			</div>
		</form>
	);
}

function submitLabel(isEdit: boolean, pending: boolean): string {
	if (isEdit) return pending ? "Saving..." : "Save changes";
	return pending ? "Creating..." : "Create role";
}
