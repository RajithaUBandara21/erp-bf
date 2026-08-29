import { ModulePlaceholder } from "@/components/shell/ModulePlaceholder";
import { MODULE_REGISTRY } from "@/lib/modules";

const entry = MODULE_REGISTRY.find((m) => m.id === "suppliers")!;

export default function SuppliersPage() {
	return <ModulePlaceholder label={entry.label} description={entry.description} />;
}
