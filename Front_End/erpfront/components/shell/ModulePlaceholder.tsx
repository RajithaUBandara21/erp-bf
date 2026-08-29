import { getTranslations } from "next-intl/server";

interface ModulePlaceholderProps {
	moduleId: string;
}

// Shared stub content for every not-yet-built module route (build-plan 5-18). Each module's real
// feature replaces this page when it's built. Label/description are looked up by `moduleId` from
// the `modules.*` message catalog (see lib/modules.ts's MODULE_REGISTRY for the id list).
export async function ModulePlaceholder({ moduleId }: ModulePlaceholderProps) {
	const t = await getTranslations();
	const label = t(`modules.${moduleId}.label`);
	const description = t(`modules.${moduleId}.description`);

	return (
		<div className="flex flex-1 flex-col items-center justify-center gap-2 p-6 text-center">
			<h1 className="text-xl font-semibold">{t("modulePlaceholder.notBuilt", { label })}</h1>
			<p className="text-[13px] text-muted">{description}</p>
		</div>
	);
}
