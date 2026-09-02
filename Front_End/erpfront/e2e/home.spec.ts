import { expect, test } from "@playwright/test";

// Backend-free, like the rest of the suite: with no session cookie the proxy auth guard should
// redirect the module launcher at "/" to the localized sign-in page rather than render it.
test("visiting the home launcher without a session redirects to sign-in", async ({ page }) => {
  await page.goto("/");

  await expect(page).toHaveURL(/\/en\/sign-in$/);
  await expect(page.getByRole("heading", { name: "Sign in" })).toBeVisible();
});
