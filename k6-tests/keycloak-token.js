// =====================================================================
// Smart-Fit-AI — KEYCLOAK AUTH STRESS
// Stress the token endpoint itself: how many JWTs/sec can the realm serve?
// Useful before a big load test to size Keycloak CPU/memory.
// =====================================================================

import http from 'k6/http';
import { check } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const KEYCLOAK_URL = __ENV.KEYCLOAK_URL || 'http://localhost:8181';
const REALM        = 'fitness-oauth2';
const CLIENT_ID    = 'oauth2-pkce-client';
const KC_USER      = __ENV.KC_USER     || 'user1';
const KC_PASSWORD  = __ENV.KC_PASSWORD || 'user1';

const tokenLatency = new Trend('token_latency', true);
const tokenOk      = new Rate('token_ok');

export const options = {
  scenarios: {
    kc: {
      executor: 'constant-arrival-rate',
      rate: 200,
      timeUnit: '1s',
      duration: '3m',
      preAllocatedVUs: 50,
      maxVUs: 300,
    },
  },
  thresholds: {
    'token_latency': ['p(95)<500'],
    'token_ok':      ['rate>0.99'],
  },
};

export default function () {
  const r = http.post(
    `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token`,
    { grant_type: 'password', client_id: CLIENT_ID, username: KC_USER, password: KC_PASSWORD },
    { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } }
  );
  tokenLatency.add(r.timings.duration);
  tokenOk.add(r.status === 200);
  check(r, { 'token 200': x => x.status === 200 });
}
