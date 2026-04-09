import { test, expect } from '@playwright/test';

/**
 * Citizen portal landing page (petition022)
 *
 * Petition : petition022
 * Feature  : petitions/petition022-skyldnerportal-landing-page.feature
 * Spec     : petitions/specs/petition022-specs.yaml
 * Contract : petitions/petition022-skyldnerportal-landing-page-outcome-contract.md
 */

const CITIZEN = 'http://127.0.0.1:8086/borger';

test.describe('Citizen portal landing page', () => {
  test('Landing page is served at the portal root', async ({ page }) => {
    const response = await page.goto(`${CITIZEN}/`);
    expect(response?.status(), 'HTTP status').toBe(200);
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
    await expect(page.getByRole('heading', { level: 1 })).toContainText(/gæld|debt/i);
  });

  test('Landing page displays FAQ with 7 items', async ({ page }) => {
    await page.goto(`${CITIZEN}/`);
    await expect(page.locator('.skat-faq__item')).toHaveCount(7);
  });

  test('Landing page includes a link to MitID self-service', async ({ page }) => {
    await page.goto(`${CITIZEN}/`);
    const cta = page.locator('a.skat-button--primary').filter({ hasText: /gæld|debt/i });
    await expect(cta).toBeVisible();
    const href = await cta.getAttribute('href');
    expect(href, 'Mit debt overview URL').toMatch(/^https:\/\//);
  });

  test('Landing page explains interest accrues daily', async ({ page }) => {
    await page.goto(`${CITIZEN}/`);
    await expect(page.getByText(/dagligt|daily/i)).toBeVisible();
  });

  test('Landing page displays debt errors section', async ({ page }) => {
    await page.goto(`${CITIZEN}/`);
    await expect(page.getByRole('heading', { name: /fejl|errors/i })).toBeVisible();
  });

  test('Landing page supports Danish and English', async ({ page }) => {
    await page.goto(`${CITIZEN}/?lang=da-DK`);
    await expect(page.getByRole('heading', { level: 1 })).toContainText(/gæld/i);

    await page.goto(`${CITIZEN}/?lang=en-GB`);
    await expect(page.getByRole('heading', { level: 1 })).toContainText(/debt/i);
  });

  test('Accessibility statement page is served', async ({ page }) => {
    const response = await page.goto(`${CITIZEN}/was`);
    expect(response?.status(), 'HTTP status').toBe(200);
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
  });

  test('Landing page includes skip link and landmark roles', async ({ page }) => {
    await page.goto(`${CITIZEN}/`);
    await expect(page.locator('a.skat-skip-link[href="#main-content"]')).toBeVisible();
    await expect(page.locator('main#main-content[role="main"]')).toBeVisible();
  });

  test('Landing page includes language selector', async ({ page }) => {
    await page.goto(`${CITIZEN}/`);
    await expect(page.locator('nav.skat-language-selector')).toBeVisible();
  });

  test('Footer links to accessibility statement', async ({ page }) => {
    await page.goto(`${CITIZEN}/`);
    const link = page.locator('footer a[href="/borger/was"]');
    await expect(link).toBeVisible();
  });

  test('External URLs are configurable', async ({ page }) => {
    await page.goto(`${CITIZEN}/`);
    const links = page.locator('a[href^="https://"]');
    const count = await links.count();
    expect(count, 'at least one https link').toBeGreaterThan(0);
    for (let i = 0; i < count; i++) {
      const href = await links.nth(i).getAttribute('href');
      expect(href).toMatch(/^https:\/\//);
    }
  });
});
