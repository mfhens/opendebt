import { expect, type APIRequestContext, type Page } from '@playwright/test';
import { randomUUID } from 'crypto';

export const CITIZEN_BASE = 'http://localhost:8086/borger';

const PERSON_REGISTRY_BASE = 'http://localhost:8090/person-registry';
const KEYCLOAK_BASE = process.env.E2E_KEYCLOAK_URL ?? 'http://localhost:8080';
const KEYCLOAK_ADMIN_USERNAME = process.env.E2E_KEYCLOAK_ADMIN_USERNAME ?? 'admin';
const KEYCLOAK_ADMIN_PASSWORD = process.env.E2E_KEYCLOAK_ADMIN_PASSWORD ?? 'admin';
const KEYCLOAK_REALM = 'opendebt';
const KEYCLOAK_CLIENT_ID = 'opendebt-citizen-portal';
const KEYCLOAK_CITIZEN_ROLE = 'CITIZEN';
const CPR_ATTRIBUTE = 'cpr';
const CPR_CLAIM_NAME = 'dk:gov:saml:attribute:CprNumberIdentifier';
const CPR_MAPPER_NAME = 'citizen-cpr-claim';

type KeycloakClientRepresentation = {
  id: string;
  clientId: string;
};

type KeycloakRoleRepresentation = {
  id: string;
  name: string;
};

type KeycloakUserRepresentation = {
  id: string;
  username?: string;
};

export type CitizenPortalUser = {
  keycloakUserId: string;
  username: string;
  password: string;
  cpr: string;
  personId: string;
};

export const SEEDED_CITIZENS = {
  lars: {
    cpr: '0503581234',
    personId: 'd0000000-0000-0000-0000-000000000001',
  },
  mads: {
    cpr: '0208741234',
    personId: 'd0000000-0000-0000-0000-000000000002',
  },
  emma: {
    cpr: '2209961234',
    personId: 'd0000000-0000-0000-0000-000000000003',
  },
  jens: {
    cpr: '3012571234',
    personId: 'd0000000-0000-0000-0000-000000000004',
  },
} as const;

type ProvisionCitizenOptions = {
  username?: string;
  password?: string;
  cpr?: string;
};

let cachedAdminAccessToken: string | undefined;
let cachedCitizenClient: KeycloakClientRepresentation | undefined;
let cachedCitizenRole: KeycloakRoleRepresentation | undefined;
let cprMapperEnsured = false;
let generatedCitizenIndex = 0;

function buildGeneratedIdentity(): { username: string; password: string; cpr: string } {
  generatedCitizenIndex += 1;
  const cprSuffix = `${Date.now()}${generatedCitizenIndex}`.slice(-4);
  return {
    username: `citizen-${randomUUID().slice(0, 8)}`,
    password: process.env.E2E_CITIZEN_PASSWORD ?? 'citizen123',
    cpr: `010101${cprSuffix}`,
  };
}

async function getAdminAccessToken(request: APIRequestContext): Promise<string> {
  if (cachedAdminAccessToken) {
    return cachedAdminAccessToken;
  }

  const response = await request.post(
    `${KEYCLOAK_BASE}/realms/master/protocol/openid-connect/token`,
    {
      form: {
        client_id: 'admin-cli',
        grant_type: 'password',
        username: KEYCLOAK_ADMIN_USERNAME,
        password: KEYCLOAK_ADMIN_PASSWORD,
      },
    },
  );
  expect(response.ok(), 'Keycloak admin token request').toBeTruthy();

  const body = (await response.json()) as { access_token?: string };
  expect(body.access_token, 'Keycloak admin access token').toBeTruthy();
  cachedAdminAccessToken = body.access_token;
  return cachedAdminAccessToken!;
}

async function getCitizenClient(
  request: APIRequestContext,
  adminToken: string,
): Promise<KeycloakClientRepresentation> {
  if (cachedCitizenClient) {
    return cachedCitizenClient;
  }

  const response = await request.get(
    `${KEYCLOAK_BASE}/admin/realms/${KEYCLOAK_REALM}/clients?clientId=${KEYCLOAK_CLIENT_ID}`,
    {
      headers: {
        Authorization: `Bearer ${adminToken}`,
      },
    },
  );
  expect(response.ok(), 'Load citizen portal Keycloak client').toBeTruthy();

  const clients = (await response.json()) as KeycloakClientRepresentation[];
  const client = clients.find((entry) => entry.clientId === KEYCLOAK_CLIENT_ID);
  expect(client, `Keycloak client ${KEYCLOAK_CLIENT_ID}`).toBeTruthy();
  cachedCitizenClient = client;
  return cachedCitizenClient!;
}

async function getCitizenRole(
  request: APIRequestContext,
  adminToken: string,
): Promise<KeycloakRoleRepresentation> {
  if (cachedCitizenRole) {
    return cachedCitizenRole;
  }

  const response = await request.get(
    `${KEYCLOAK_BASE}/admin/realms/${KEYCLOAK_REALM}/roles/${KEYCLOAK_CITIZEN_ROLE}`,
    {
      headers: {
        Authorization: `Bearer ${adminToken}`,
      },
    },
  );
  expect(response.ok(), `Load Keycloak role ${KEYCLOAK_CITIZEN_ROLE}`).toBeTruthy();
  cachedCitizenRole = (await response.json()) as KeycloakRoleRepresentation;
  return cachedCitizenRole;
}

async function ensureCitizenCprMapper(request: APIRequestContext): Promise<void> {
  if (cprMapperEnsured) {
    return;
  }

  const adminToken = await getAdminAccessToken(request);
  const client = await getCitizenClient(request, adminToken);
  const existingResponse = await request.get(
    `${KEYCLOAK_BASE}/admin/realms/${KEYCLOAK_REALM}/clients/${client.id}/protocol-mappers/models`,
    {
      headers: {
        Authorization: `Bearer ${adminToken}`,
      },
    },
  );
  expect(existingResponse.ok(), 'Load citizen portal protocol mappers').toBeTruthy();

  const existingMappers = (await existingResponse.json()) as Array<{ name?: string }>;
  const alreadyExists = existingMappers.some((mapper) => mapper.name === CPR_MAPPER_NAME);
  if (!alreadyExists) {
    const createResponse = await request.post(
      `${KEYCLOAK_BASE}/admin/realms/${KEYCLOAK_REALM}/clients/${client.id}/protocol-mappers/models`,
      {
        headers: {
          Authorization: `Bearer ${adminToken}`,
        },
        data: {
          name: CPR_MAPPER_NAME,
          protocol: 'openid-connect',
          protocolMapper: 'oidc-usermodel-attribute-mapper',
          config: {
            'user.attribute': CPR_ATTRIBUTE,
            'claim.name': CPR_CLAIM_NAME,
            'jsonType.label': 'String',
            'id.token.claim': 'true',
            'userinfo.token.claim': 'true',
            'access.token.claim': 'true',
          },
        },
      },
    );
    expect(createResponse.ok(), 'Create citizen CPR protocol mapper').toBeTruthy();
  }

  cprMapperEnsured = true;
}

async function findKeycloakUser(
  request: APIRequestContext,
  adminToken: string,
  username: string,
): Promise<KeycloakUserRepresentation | undefined> {
  const response = await request.get(
    `${KEYCLOAK_BASE}/admin/realms/${KEYCLOAK_REALM}/users?username=${encodeURIComponent(username)}&exact=true`,
    {
      headers: {
        Authorization: `Bearer ${adminToken}`,
      },
    },
  );
  expect(response.ok(), `Find Keycloak user ${username}`).toBeTruthy();

  const users = (await response.json()) as KeycloakUserRepresentation[];
  return users.find((user) => user.username === username);
}

async function createKeycloakCitizenUser(
  request: APIRequestContext,
  adminToken: string,
  username: string,
  password: string,
  cpr: string,
): Promise<string> {
  const createResponse = await request.post(
    `${KEYCLOAK_BASE}/admin/realms/${KEYCLOAK_REALM}/users`,
    {
      headers: {
        Authorization: `Bearer ${adminToken}`,
      },
      data: {
        username,
        enabled: true,
        emailVerified: true,
        firstName: 'Test',
        lastName: 'Citizen',
        attributes: {
          [CPR_ATTRIBUTE]: [cpr],
        },
      },
    },
  );
  expect(createResponse.status(), `Create Keycloak citizen user ${username}`).toBe(201);

  const createdUser = await findKeycloakUser(request, adminToken, username);
  expect(createdUser?.id, `Created Keycloak user id for ${username}`).toBeTruthy();

  const passwordResponse = await request.put(
    `${KEYCLOAK_BASE}/admin/realms/${KEYCLOAK_REALM}/users/${createdUser?.id}/reset-password`,
    {
      headers: {
        Authorization: `Bearer ${adminToken}`,
      },
      data: {
        type: 'password',
        temporary: false,
        value: password,
      },
    },
  );
  expect(passwordResponse.ok(), `Reset password for ${username}`).toBeTruthy();

  const role = await getCitizenRole(request, adminToken);
  const roleMappingResponse = await request.post(
    `${KEYCLOAK_BASE}/admin/realms/${KEYCLOAK_REALM}/users/${createdUser?.id}/role-mappings/realm`,
    {
      headers: {
        Authorization: `Bearer ${adminToken}`,
      },
      data: [
        {
          id: role.id,
          name: role.name,
        },
      ],
    },
  );
  expect(roleMappingResponse.ok(), `Assign ${KEYCLOAK_CITIZEN_ROLE} role to ${username}`).toBeTruthy();

  return createdUser!.id;
}

async function lookupOrCreatePersonId(
  request: APIRequestContext,
  cpr: string,
): Promise<string> {
  const response = await request.post(`${PERSON_REGISTRY_BASE}/api/v1/persons/lookup`, {
    data: {
      identifier: cpr,
      identifierType: 'CPR',
      role: 'PERSONAL',
    },
  });
  expect(response.ok(), `Lookup person_id for CPR ${cpr}`).toBeTruthy();

  const body = (await response.json()) as { personId?: string };
  expect(body.personId, `person_id for CPR ${cpr}`).toBeTruthy();
  return body.personId!;
}

export async function provisionCitizenPortalUser(
  request: APIRequestContext,
  options: ProvisionCitizenOptions = {},
): Promise<CitizenPortalUser> {
  await ensureCitizenCprMapper(request);

  const generatedIdentity = buildGeneratedIdentity();
  const username = options.username ?? generatedIdentity.username;
  const password = options.password ?? generatedIdentity.password;
  const cpr = options.cpr ?? generatedIdentity.cpr;

  const adminToken = await getAdminAccessToken(request);
  const existingUser = await findKeycloakUser(request, adminToken, username);
  expect(existingUser, `Keycloak username ${username} must be unique for petition026 E2E`).toBeFalsy();

  const keycloakUserId = await createKeycloakCitizenUser(request, adminToken, username, password, cpr);
  const personId = await lookupOrCreatePersonId(request, cpr);

  return {
    keycloakUserId,
    username,
    password,
    cpr,
    personId,
  };
}

export async function deleteCitizenPortalUser(
  request: APIRequestContext,
  keycloakUserId: string,
): Promise<void> {
  const adminToken = await getAdminAccessToken(request);
  await request.delete(
    `${KEYCLOAK_BASE}/admin/realms/${KEYCLOAK_REALM}/users/${keycloakUserId}`,
    {
      headers: {
        Authorization: `Bearer ${adminToken}`,
      },
    },
  );
}

export async function loginCitizenIfPrompted(
  page: Page,
  citizen: Pick<CitizenPortalUser, 'username' | 'password'>,
): Promise<void> {
  if (!page.url().includes('/realms/') && (await page.locator('#kc-form-login').count()) === 0) {
    return;
  }

  await page.locator('input[name="username"]').waitFor({ state: 'visible', timeout: 30_000 });
  await page.locator('input[name="username"]').fill(citizen.username);
  await page.locator('input[name="password"]').fill(citizen.password);

  const submitButton = page.locator(
    '#kc-form-login input[type="submit"], #kc-form-login button[type="submit"], input#kc-login',
  );
  await expect(submitButton.first(), 'Keycloak submit button').toBeVisible({ timeout: 30_000 });

  await Promise.all([
    page.waitForURL(/(localhost|127\.0\.0\.1):8086\/borger\//, { timeout: 60_000 }),
    submitButton.first().click(),
  ]);
}

export async function authenticateCitizenPortal(
  page: Page,
  citizen: Pick<CitizenPortalUser, 'username' | 'password'>,
  relativePath = '/min-gaeld',
): Promise<void> {
  await page.goto(`${CITIZEN_BASE}${relativePath}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60_000,
  });
  await loginCitizenIfPrompted(page, citizen);
}
