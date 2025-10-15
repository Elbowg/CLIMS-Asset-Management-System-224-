import { test, expect } from '@playwright/test';

// Note: tests assume the frontend dev server is running and the backend is available at /api
// and that the default admin credentials (admin / Admin@123) work in the local dev environment.

test('create asset via UI', async ({ page }) => {
  // Go to app (frontend dev server)
  await page.goto('http://localhost:5173/');

  // Login
  await page.click('text=Login');
  await page.fill('input[name="username"]', 'admin');
  await page.fill('input[name="password"]', 'Admin@123');
  await page.click('button:has-text("Sign in")');
  await page.waitForURL('**/dashboard');

  // Navigate to Assets page
  await page.click('a:has-text("Assets")');
  await page.waitForURL('**/assets');

  // Open new asset form
  await page.click('a:has-text("Add Asset")');
  await page.waitForURL('**/assets/new');

  // Fill form
  const rand = Math.floor(Math.random() * 900) + 100;
  await page.fill('input[name="serialNumber"]', `E2E-SN-${rand}`);
  await page.fill('input[name="make"]', 'E2E-Make');
  await page.fill('input[name="model"]', 'E2E-Model');
  await page.fill('input[name="purchaseDate"]', '2025-10-15');
  await page.fill('input[name="warrantyExpiryDate"]', '2028-10-15');

  // Submit
  await page.click('button:has-text("Save")');

  // Back to list and expect the new asset to be present
  await page.waitForURL('**/assets');
  const text = await page.locator('table').innerText();
  expect(text).toContain('E2E-SN-');
});