// i18n keys (build-plan 4): system_role, custom_role

export function RoleTypeBadge({ systemManaged }: { systemManaged: boolean }) {
	return systemManaged ? (
		<span className="inline-flex items-center rounded-full bg-surface-alt px-2.5 py-0.5 text-[11px] font-semibold text-muted">
			System
		</span>
	) : (
		<span className="inline-flex items-center rounded-full bg-accent px-2.5 py-0.5 text-[11px] font-semibold text-accent-ink">
			Custom
		</span>
	);
}
