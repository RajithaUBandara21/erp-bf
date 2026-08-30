"use client";

import type { FormEvent, ReactNode } from "react";
import { useState, useTransition } from "react";
import { useLocale, useTranslations } from "next-intl";
import { Link, useRouter } from "@/i18n/navigation";
import type { MatrixGroup } from "@/lib/permission-matrix";
import type { RoleDetail } from "@/types/roles";
import { createRole, updateRole, type RoleFormState } from "@/actions/roles";
import { PermissionMatrix } from "@/components/roles/PermissionMatrix";
import { RoleMembersList } from "@/components/roles/RoleMembersList";
import { RoleTypeBadge } from "@/components/roles/RoleTypeBadge";

type RoleFormProps =
	| { mode: "edit"; role: RoleDetail; groups: MatrixGroup[]; headerActions?: ReactNode; membersSlot?: ReactNode }
	| { mode: "create"; groups: MatrixGroup[]; headerActions?: ReactNode; membersSlot?: ReactNode };

/** Create form, or editable detail for a custom role the caller may edit. Read-only roles use RoleDetailView. */
export function RoleForm(props: RoleFormProps) {
	const isEdit = props.mode === "edit";
	const role = props.mode === "edit" ? props.role : null;

	const router = useRouter();
	const locale = useLocale();
	const t = useTranslations("roles");
	const tp = useTranslations("permissions");
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
					<h1 className="text-xl font-semibold">{role ? role.name : t("newRole")}</h1>
					{role && (
						<div className="mt-1.5 flex items-center gap-2.5 text-[13px] text-muted">
							<RoleTypeBadge systemManaged={role.systemManaged} />
							<span>{t("memberCount", { count: role.memberCount })}</span>
						</div>
					)}
				</div>
				<div className="flex items-center gap-2">
					{saved && <span className="text-[13px] font-semibold text-success">{t("saved")}</span>}
					{props.headerActions}
					<Link
						href="/settings/roles"
						className="min-h-9 rounded-[5px] border border-border bg-surface px-3.5 py-2 text-[13px] font-semibold text-text hover:bg-surface-alt"
					>
						{t("cancel")}
					</Link>
					<button
						type="submit"
						disabled={pending}
						className="min-h-9 rounded-[5px] bg-accent px-3.5 py-2 text-[13px] font-semibold text-accent-ink hover:bg-accent-hover disabled:cursor-not-allowed disabled:opacity-55"
					>
						{submitLabel(isEdit, pending, t)}
					</button>
				</div>
			</div>

			{state.error && (
				<div className="mb-4 rounded-[5px] bg-danger-bg px-3 py-2.5 text-xs text-danger">{state.error}</div>
			)}

			<div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_260px] lg:items-start">
				<section className="rounded-lg border border-border bg-surface p-5 shadow-sm">
					<div className="mb-3 flex items-center justify-between gap-2">
						<h2 className="text-sm font-semibold">{tp("heading")}</h2>
						<span className="text-[11px] text-faint">{tp("scopeNote")}</span>
					</div>
					<PermissionMatrix groups={props.groups} selectedCodes={codes} onChange={edit(setCodes)} />
					<p className="mt-3 text-[11px] text-faint">{tp("legend")}</p>
				</section>

				<div className="flex flex-col gap-4">
					<section className="rounded-lg border border-border bg-surface p-5 shadow-sm">
						<h2 className="mb-3 text-sm font-semibold">{t("info.heading")}</h2>
						<div className="mb-3 flex flex-col gap-1.5">
							<label htmlFor="role-name" className="text-xs font-semibold text-muted">
								{t("info.nameLabel")}
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
								{t("info.descriptionLabel")}
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
								{t("info.scopeLabel")}
							</label>
							<input
								id="role-scope"
								value={t("info.scopeValue")}
								disabled
								className="rounded-[5px] border border-border bg-bg px-2.5 py-2 text-[13px] text-muted"
							/>
							<span className="text-[11px] text-faint">{t("info.scopeHint")}</span>
						</div>
					</section>

					{role && (props.membersSlot ?? <RoleMembersList members={role.members} />)}
				</div>
			</div>
		</form>
	);
}

function submitLabel(isEdit: boolean, pending: boolean, t: ReturnType<typeof useTranslations>): string {
	if (isEdit) return pending ? t("saving") : t("save");
	return pending ? t("creating") : t("create");
}
