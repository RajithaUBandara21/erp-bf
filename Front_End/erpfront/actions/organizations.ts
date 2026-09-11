"use server";

import { revalidatePath } from "next/cache";
import { getTranslations } from "next-intl/server";
import { redirect } from "@/i18n/navigation";
import { authedFetch } from "@/lib/api";
import type { OrganizationDetail } from "@/types/organizations";

const ORGANIZATIONS_PATH = "/settings/organizations";

export interface CreateOrganizationState {
	error?: string;
	fieldErrors?: { name?: string };
	created?: boolean;
}

// `locale` is a bound first argument (see components/settings/CreateOrganizationForm.tsx) - a Server
// Action has no reliable way to read the current locale on its own, so the client passes what it
// already knows via useLocale(). No redirect on success: there is no org detail page and the caller
// stays in their current org, so the form just collapses in place.
export async function createOrganization(
	locale: string,
	_prev: CreateOrganizationState,
	formData: FormData,
): Promise<CreateOrganizationState> {
	const name = String(formData.get("name") ?? "").trim();
	if (!name) {
		const t = await getTranslations({ locale, namespace: "settings.organizations" });
		return { fieldErrors: { name: t("form.nameRequired") } };
	}

	// authedFetch sets no Content-Type of its own; this POST has a body, so pass it explicitly.
	const result = await authedFetch<OrganizationDetail>("/api/organizations", {
		method: "POST",
		headers: { "Content-Type": "application/json" },
		body: JSON.stringify({ name }),
	});

	if (result.success) {
		revalidatePath(ORGANIZATIONS_PATH);
		return { created: true };
	}

	if ("unauthorized" in result) {
		redirect({ href: "/sign-in", locale });
	}

	if (result.status === 400 && result.fieldErrors?.name) {
		return { fieldErrors: { name: result.fieldErrors.name } };
	}

	// 403 (role changed mid-session) / 409 (at limit) / network - the backend sentence, shown untranslated.
	return { error: result.error };
}
