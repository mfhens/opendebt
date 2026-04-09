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
    const url = `http://127.0.0.1:${port}${contextPath}/actuator/health`;
    // Do not follow redirects: OAuth portals return 302 to Keycloak using the docker hostname
    // `keycloak`, which does not resolve on the GitHub Actions host (EAI_AGAIN).
    const response = await request.get(url, { maxRedirects: 0 });
    const status = response.status();

    if (status === 200) {
      const body = await response.json();
      expect(body.status, `health status on port ${port}`).toBe('UP');
      return;
    }

    // Non-dev profiles require login; same as `curl -sf` in CI (302 counts as "reachable").
    expect(status, `GET ${url}`).toBe(302);
    const location = response.headers().location ?? '';
    expect(location, 'redirect to OAuth login').toMatch(/oauth2|authorization/i);
  });
}
