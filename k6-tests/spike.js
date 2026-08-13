// =====================================================================
// Smart-Fit-AI — SPIKE TEST  (5 → 500 VUs in 10s, then back)
// Validates Gateway, Eureka, RabbitMQ, Mongo behaviour under sudden load.
// =====================================================================

import http from 'k6/http';
import { check, sleep, fail } from 'k6';
import encoding from 'k6/encoding';

const BASE_URL     = __ENV.BASE_URL     || 'http://localhost:8080';
const KEYCLOAK_URL = __ENV.KEYCLOAK_URL || 'http://localhost:8181';
const REALM        = 'fitness-oauth2';
const CLIENT_ID    = 'oauth2-pkce-client';
const KC_USER      = __ENV.KC_USER     || 'user1';
const KC_PASSWORD  = __ENV.KC_PASSWORD || 'user1';

export const options = {
  scenarios: {
    spike: {
      executor: 'ramping-vus',
      startVUs: 5,
      stages: [
        { duration: '10s', target: 100 },
        { duration: '30s', target: 100 },
        { duration: '10s', target: 5   },
        { duration: '30s', target: 5   },
      ],
      gracefulRampDown: '10s',
    },
  },
  thresholds: {
    'http_req_failed':   ['rate<0.05'],   // allow some failures during the burst
    'http_req_duration': ['p(95)<1500'],
  },
};

export function setup() {
  const tr = http.post(
    `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token`,
    { 
      grant_type: 'password', 
      client_id: CLIENT_ID, 
      username: KC_USER, 
      password: KC_PASSWORD 
    },
    { 
      headers: { 
        'Content-Type': 'application/x-www-form-urlencoded' 
      } 
    }
  );

  if (tr.status !== 200) fail(`Keycloak auth failed: ${tr.status}`);
  
  const token = tr.json('access_token');
  
  // Extract real UUID
  const payloadBase64 = token.split('.')[1]; 
  const payloadJson = JSON.parse(encoding.b64decode(payloadBase64, 'rawstd', 's'));

  return { token: token, userId: payloadJson.sub };
}

export default function (data) {
  const authHdr = { 
    headers: { 
      'Authorization': `Bearer ${data.token}`,
      'Content-Type': 'application/json',
      'X-User-ID': data.userId
    } 
  };

  const r = http.get(`${BASE_URL}/api/activities`, authHdr);
  check(r, { 'activities < 500': x => x.status < 500 });

  // CRITICAL: Prevent CPU starvation and connection exhaustion
  sleep(1);
}
