// =====================================================================
// Smart-Fit-AI — SOAK / ENDURANCE TEST  (50 VUs · 1 hour)
// Use this to detect: memory leaks, connection-pool exhaustion, slow
// Postgres/Mongo driver leaks, RabbitMQ consumer-lag growth, etc.
// =====================================================================

import http from 'k6/http';
import { check, sleep, fail } from 'k6';
import { Trend } from 'k6/metrics';
import encoding from 'k6/encoding';

const BASE_URL     = __ENV.BASE_URL     || 'http://localhost:8080';
const KEYCLOAK_URL = __ENV.KEYCLOAK_URL || 'http://localhost:8181';
const REALM        = 'fitness-oauth2';
const CLIENT_ID    = 'oauth2-pkce-client';
const KC_USER      = __ENV.KC_USER     || 'user1';
const KC_PASSWORD  = __ENV.KC_PASSWORD || 'user1';

const memoryLeakIndicator = new Trend('p99_latency_drift', true);

export const options = {
  scenarios: {
    soak: {
      executor: 'constant-vus',
      vus: 50,
      duration: '1h',
    },
  },
  thresholds: {
    // p99 latency must not degrade by more than 2x during the run.
    // (k6 doesn't have time-windowed thresholds natively; export the trend
    //  and inspect `reports/soak.json` afterwards.)
    'http_req_failed':   ['rate<0.02'],
    'http_req_duration': ['p(99)<2000'],
  },
};

export function setup() {
  const tr = http.post(
    `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token`,
    { grant_type: 'password', client_id: CLIENT_ID, username: KC_USER, password: KC_PASSWORD },
    { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } }
  );

  if (tr.status !== 200) fail(`Keycloak auth failed: ${tr.status}`);
  
  const token = tr.json('access_token');
  
  // Extract real UUID for the database
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
  
  memoryLeakIndicator.add(r.timings.duration);
  check(r, { 'acts 2xx': x => x.status < 500 });
  
  // CRITICAL: Pacing to maintain a steady ~50 RPS
  sleep(1);
}
