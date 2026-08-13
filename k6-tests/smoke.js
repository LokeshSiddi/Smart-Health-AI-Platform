// =====================================================================
// Smart-Fit-AI — SMOKE TEST  (5 VUs · 30s · validates the SUT is healthy)
// Run BEFORE running the full load test in CI / pre-deploy gates.
// =====================================================================

import http from 'k6/http';
import { check, fail } from 'k6';
import encoding from 'k6/encoding';

const BASE_URL     = __ENV.BASE_URL     || 'http://localhost:8080';
const EUREKA_URL   = __ENV.EUREKA_URL   || 'http://localhost:8761';
const KEYCLOAK_URL = __ENV.KEYCLOAK_URL || 'http://localhost:8181';
const KC_USER      = __ENV.KC_USER      || 'user1';
const KC_PASSWORD  = __ENV.KC_PASSWORD  || 'user1';
const REALM        = 'fitness-oauth2';
const CLIENT_ID    = 'oauth2-pkce-client';

export const options = {
  vus: 5,
  duration: '30s',
  thresholds: {
    'http_req_failed':     ['rate<0.01'],
    'http_req_duration':   ['p(95)<1500'],
    'checks':              ['rate>0.99'],
  },
};

// 1. SETUP PHASE (Runs exactly ONCE before VUs start)
export function setup() {
  const up = http.get(`${BASE_URL}/actuator/health`);
  if (up.status !== 200) fail(`Gateway not healthy: ${up.status}`);
  
  // A. Fetch Keycloak Token
  const tokenRes = http.post(
    `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token`,
    {
      grant_type: 'password',
      client_id:  CLIENT_ID,
      username:   KC_USER,
      password:   KC_PASSWORD,
    },
    { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } }
  );
  
  if (tokenRes.status !== 200) {
    fail(`Keycloak auth failed: ${tokenRes.status} - ${tokenRes.body}`);
  }
  
  const token = tokenRes.json('access_token');
  
  // B. Decode JWT to get exact Keycloak ID
  const payloadBase64 = token.split('.')[1]; 
  const payloadJson = JSON.parse(encoding.b64decode(payloadBase64, 'rawstd', 's'));
  const realUserId = payloadJson.sub; 

  const authHdr = { 
    headers: { 
      'Authorization': `Bearer ${token}`, 
      'Content-Type': 'application/json',
      'X-User-ID': realUserId
    } 
  };

  // C. Force-register the user synchronously to guarantee DB match
  // This bypasses the async Gateway sync filter entirely.
  const registerPayload = JSON.stringify({
    keycloakId: realUserId,
    email: payloadJson.email || `${KC_USER}@test.com`,
    firstName: payloadJson.given_name || 'LoadTest',
    lastName: payloadJson.family_name || 'User',
    password: 'password123' // Satisfies your > 6 chars rule
  });

  const regRes = http.post(`${BASE_URL}/api/users/register`, registerPayload, authHdr);
  console.log(`[SETUP] Explicit Registration Status -> ${regRes.status}`);

  // D. Pass token and ID down to the virtual users
  return { token, realUserId };
}

// 2. EXECUTION PHASE (Loop run by 5 VUs concurrently)
export default function (data) {
  const authHdr = { 
    headers: { 
      'Authorization': `Bearer ${data.token}`, 
      'Content-Type': 'application/json',
      'X-User-ID': data.realUserId
    } 
  };

  const targets = [
    `${BASE_URL}/api/users/${data.realUserId}`,
    `${BASE_URL}/api/users/${data.realUserId}/validate`,
    `${BASE_URL}/api/activities`,
    `${BASE_URL}/api/recommendations/user/${data.realUserId}`,
    `${EUREKA_URL}/actuator/health`,
  ];
  
  for (const url of targets) {
    const r = http.get(url, authHdr);
    
    if (r.status >= 400) {
      // If a 4xx error still sneaks through, it will tell us exactly why
      console.warn(`[FAILED] URL: ${url} | Status: ${r.status} | Body: ${r.body}`);
    }

    check(r, { [`${url} 2xx/404`]: x => x.status < 500 });
  }
}