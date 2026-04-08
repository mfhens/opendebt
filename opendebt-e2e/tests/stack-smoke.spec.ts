import { test, expect } from '@playwright/test';

/**
 * Smoke checks against the docker-compose stack (CI: ports 8085–8087).
 * Uses the API request context so tests do not depend on browser rendering.
 */
const portals = [
  { name: 'creditor', port: 8085 },
  { name: 'citizen', port: 8086 },
  { name: 'caseworker', port: 8087 },
] as const;

for (const { name, port } of portals) {
  test(`${name} portal actuator health is UP`, async ({ request }) => {
    const response = await request.get(`http://127.0.0.1:${port}/actuator/health`);
    expect(response.ok(), `GET /actuator/health on port ${port}`).toBeTruthy();
    const body = await response.json();
    expect(body.status, `health status on port ${port}`).toBe('UP');
  });
}
