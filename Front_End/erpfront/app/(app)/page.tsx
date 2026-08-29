import { MODULE_REGISTRY } from "@/lib/modules";
import { can, fetchMyPermissions } from "@/lib/permissions";
import { decodeAccessTokenEmail } from "@/lib/jwt";
import { ModuleGrid, type ModuleTile } from "@/components/shell/ModuleGrid";

// Module launcher (build-plan 3b): the Odoo-style grid every remaining MVP module (5-18) is
// gated behind. Settings and Audit Logs (permissionResource-less or already-shipped modules)
// stay always-visible / permission-gated the same way their own pages already are.
export default async function Home() {
	const [perms, email] = await Promise.all([fetchMyPermissions(), decodeAccessTokenEmail()]);

	const tiles: ModuleTile[] = MODULE_REGISTRY.map((entry) => ({
		...entry,
		enabled: !entry.permissionResource || can(perms, `${entry.permissionResource}.view`),
	}));

	return (
		<div className="mx-auto w-full max-w-[1100px] flex-1 p-6">
			<div className="mb-4">
				<h1 className="mb-1 text-xl font-semibold">Modules</h1>
				{email && <p className="text-[13px] text-muted">Signed in as {email}</p>}
			</div>
			<ModuleGrid tiles={tiles} />
		</div>
	);
}
