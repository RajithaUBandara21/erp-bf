// The session guard and topbar now live in the shared app-shell layout (app/(app)/layout.tsx);
// this layout only keeps the settings-specific max-width content wrapper.
export default function SettingsLayout({ children }: LayoutProps<"/settings">) {
	return <div className="mx-auto w-full max-w-[1100px] flex-1 p-6">{children}</div>;
}
