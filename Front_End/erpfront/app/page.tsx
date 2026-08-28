import Link from "next/link";

// Interim landing page: sign-in / sign-up both redirect here. Build-plan 3b replaces this with the
// real Odoo-style module grid (and a shared cached session loader that also gates this route).
export default function Home() {
  return (
    <div className="flex flex-1 flex-col items-center justify-center gap-4 p-6 text-center">
      <h1 className="text-xl font-semibold">Signed in</h1>
      <Link href="/settings" className="text-sm text-accent underline">
        Go to settings
      </Link>
    </div>
  );
}
