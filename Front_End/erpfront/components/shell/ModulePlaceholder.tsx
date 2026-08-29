interface ModulePlaceholderProps {
	label: string;
	description: string;
}

// Shared stub content for every not-yet-built module route (build-plan 5-18). Each module's real
// feature replaces this page when it's built.
export function ModulePlaceholder({ label, description }: ModulePlaceholderProps) {
	return (
		<div className="flex flex-1 flex-col items-center justify-center gap-2 p-6 text-center">
			<h1 className="text-xl font-semibold">{label} isn&apos;t built yet</h1>
			<p className="text-[13px] text-muted">{description}</p>
		</div>
	);
}
