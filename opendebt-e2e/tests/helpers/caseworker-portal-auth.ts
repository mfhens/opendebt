import type { Page } from '@playwright/test';

/** Base URLs for the demo/local stack (docker-compose or `./start-demo.ps1`). */
export const CW_BASE = 'http://localhost:8087/caseworker-portal';
export const CASE_SVC = 'http://localhost:8081/case-service';
export const DEBT_SVC = 'http://localhost:8082/debt-service';

/**
 * Log in as anna-jensen (role: CASEWORKER — full write access).
 *
 * hasWriteAccess() returns true for CASEWORKER and ADMIN only.
 */
export async function authenticateCaseworkerWritable(page: Page): Promise<void> {
  await page.goto(`${CW_BASE}/demo-login`, { waitUntil: 'domcontentloaded' });
  await page.selectOption('#caseworkerId', 'anna-jensen');
  await page.click('button[type="submit"]');
  await page.waitForURL(/caseworker-portal\/cases/, { timeout: 30_000 });
}

/**
 * Log in as erik-sorensen (role: SENIOR_CASEWORKER — read-only).
 *
 * hasWriteAccess() returns false for SENIOR_CASEWORKER, so readOnly=true in the
 * limitation panel and all write controls are hidden by Thymeleaf conditionals.
 */
export async function authenticateCaseworkerReadOnly(page: Page): Promise<void> {
  await page.goto(`${CW_BASE}/demo-login`, { waitUntil: 'domcontentloaded' });
  await page.selectOption('#caseworkerId', 'erik-sorensen');
  await page.click('button[type="submit"]');
  await page.waitForURL(/caseworker-portal\/cases/, { timeout: 30_000 });
}
