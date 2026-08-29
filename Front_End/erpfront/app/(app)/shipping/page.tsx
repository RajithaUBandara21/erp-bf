import { ModulePlaceholder } from "@/components/shell/ModulePlaceholder";
import { MODULE_REGISTRY } from "@/lib/modules";

const entry = MODULE_REGISTRY.find((m) => m.id === "shipping")!;

export default function ShippingPage() {
	return <ModulePlaceholder label={entry.label} description={entry.description} />;
}
