import { expect, test } from "@playwright/test";

test("interim home page greets a signed-in user and links to settings", async ({ page }) => {
  await page.goto("/");
  await expect(page.getByRole("heading", { name: "Signed in" })).toBeVisible();
  await expect(page.getByRole("link", { name: "Go to settings" })).toHaveAttribute("href", "/settings");
});
