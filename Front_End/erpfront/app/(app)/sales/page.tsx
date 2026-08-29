import { ModulePlaceholder } from "@/components/shell/ModulePlaceholder";
import { MODULE_REGISTRY } from "@/lib/modules";

const entry = MODULE_REGISTRY.find((m) => m.id === "sales")!;

export default function SalesPage() {
	return <ModulePlaceholder label={entry.label} description={entry.description} />;
}
