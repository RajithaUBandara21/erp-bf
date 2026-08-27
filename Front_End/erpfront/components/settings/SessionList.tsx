import type { SessionSummary } from "@/types/session";
import { SessionRow } from "@/components/settings/SessionRow";

export function SessionList({ sessions }: { sessions: SessionSummary[] }) {
	return (
		<div className="rounded-lg border border-border bg-surface shadow-sm">
			<ul className="flex flex-col gap-2">
				{sessions.map((session) => (
					<SessionRow key={session.id} session={session} />
				))}
			</ul>
		</div>
	);
}
