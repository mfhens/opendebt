import { test, expect } from '@playwright/test';

/**
 * Smoke checks against the docker-compose stack (CI: ports 8085–8087).
 * Uses the API request context so tests do not depend on browser rendering.
 */
/** Must match server.servlet.context-path in each portal's application.yml */
const portals = [
  { name: 'creditor', port: 8085, contextPath: '/creditor-portal' },
  { name: 'citizen', port: 8086, contextPath: '/borger' },
  { name: 'caseworker', port: 8087, contextPath: '/caseworker-portal' },
] as const;

for (const { name, port, contextPath } of portals) {
  test(`${name} portal actuator health is UP`, async ({ request }) => {
    const response = await request.get(
      `http://127.0.0.1:${port}${contextPath}/actuator/health`,
    );
    expect(response.ok(), `GET /actuator/health on port ${port}`).toBeTruthy();
    const body = await response.json();
    expect(body.status, `health status on port ${port}`).toBe('UP');
  });
}
