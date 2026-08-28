import { expect, test } from "@playwright/test";

// These paths short-circuit in the Server Action's client-side validation before any backend call,
// so the suite needs only the Next dev server (no Spring Boot / Postgres / Redis).

test("sign-in form shows a field error for each empty required field", async ({ page }) => {
  await page.goto("/sign-in");
  await page.getByRole("button", { name: "Sign in" }).click();

  await expect(page.getByText("Organization code is required.")).toBeVisible();
  await expect(page.getByText("Email is required.")).toBeVisible();
  await expect(page.getByText("Password is required.")).toBeVisible();
});

test("sign-up form flags a weak password, a mismatched confirmation, and unchecked terms", async ({ page }) => {
  await page.goto("/sign-up");

  await page.getByLabel("Organization name").fill("Northstar Retail");
  await page.getByLabel("Full name").fill("Nimal Perera");
  await page.getByLabel("Email").fill("owner@northstar-retail.com");
  await page.getByLabel("Password", { exact: true }).fill("weak");
  await page.getByLabel("Confirm password").fill("different-value");
  // The terms checkbox starts unchecked, which is the case under test.

  await page.getByRole("button", { name: "Create account" }).click();

  await expect(
    page.locator(".text-danger", {
      hasText: "Password must be at least 8 characters, with one number and one uppercase letter.",
    }),
  ).toBeVisible();
  await expect(page.getByText("Passwords do not match.")).toBeVisible();
  await expect(page.getByText("You must agree to the Terms of Service and Privacy Policy.")).toBeVisible();
});
