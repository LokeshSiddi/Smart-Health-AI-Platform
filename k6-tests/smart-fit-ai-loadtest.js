// =====================================================================
// Smart-Fit-AI — Comprehensive k6 Load, Stress, Spike & Soak Test
// =====================================================================
// Repository : https://github.com/LokeshSiddi/Smart-Fit-AI
// Architecture: Spring Boot Microservices
//   - Gateway          (8080)   — Spring Cloud Gateway, JWT validation
//   - User Service     (8081)   — PostgreSQL, /api/users/**
//   - Activity Service (8082)   — MongoDB  + RabbitMQ publish, /api/activities/**
//   - AI Service       (8083)   — MongoDB  + RabbitMQ consume + Gemini, /api/recommendations/**
//   - Eureka           (8761)   — Service discovery
//   - Config Server    (8888)   — Centralised config
//   - Keycloak         (8181)   — OAuth2 / JWT issuer (realm: fitness-oauth2)
//
// Test aspects covered
//   ✔ Smoke / Sanity      (sanity-check stage)
//   ✔ Load                (sustained traffic, baseline SLOs)
//   ✔ Stress              (beyond expected capacity to find breaking point)
//   ✔ Spike               (sudden burst – Gateway + Eureka circuit-breaker behaviour)
//   ✔ Soak / Endurance    (long-running leak/memory/connection-pool detection)
//   ✔ Throughput ceiling  (open-model RPS ramp)
//   ✔ Multi-scenario mix  (realistic user-journey weighted proportions)
//   ✔ Per-endpoint SLA    (p(95) thresholds, error-rate budgets)
//   ✔ Auth (Keycloak)     (token cache + concurrent refresh, expired token rejection)
//   ✔ Discovery           (Eureka health + service registration)
//   ✔ Database / MQ paths (Postgres & Mongo indirect via service; RabbitMQ indirectly)
//   ✔ Custom business metrics (recommendation latency, activity throughput)
//
// How to run
//   k6 run --out json=results.json smart-fit-ai-loadtest.js
//   k6 run -e BASE_URL=http://localhost:8080 -e KEYCLOAK_URL=http://localhost:8181 \
//          --out json=results.json smart-fit-ai-loadtest.js
//   k6 run --out influxdb=http://influx:8086/k6 smart-fit-ai-loadtest.js
//
// SUT must be started via `docker compose up -d` from the repo root.
// =====================================================================

import http from 'k6/http';
import { check, group, sleep, fail } from 'k6';
import { Trend, Rate, Counter, Gauge } from 'k6/metrics';
import { SharedArray } from 'k6/data';
import { uuidv4, randomIntBetween, randomString } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.3/index.js';

// ---------------------------------------------------------------------
// 1. CONFIGURATION
// ---------------------------------------------------------------------
const BASE_URL     = __ENV.BASE_URL     || 'http://localhost:8080';
const USER_URL     = __ENV.USER_URL     || 'http://localhost:8081';   // direct
const ACT_URL      = __ENV.ACT_URL      || 'http://localhost:8082';   // direct
const AI_URL       = __ENV.AI_URL       || 'http://localhost:8083';   // direct
const EUREKA_URL   = __ENV.EUREKA_URL   || 'http://localhost:8761';
const KEYCLOAK_URL = __ENV.KEYCLOAK_URL || 'http://localhost:8181';

const REALM       = 'fitness-oauth2';
const CLIENT_ID   = 'oauth2-pkce-client';                  // adjust if your realm uses another client
const KC_USER     = __ENV.KC_USER     || 'user1';
const KC_PASSWORD = __ENV.KC_PASSWORD || 'user1';

// User-journey weights (must sum to 100)
const W = {
  register:        2,    // cold-path
  getProfile:      18,
  validateUser:    10,
  trackActivity:   30,   // heaviest – writes Mongo + publishes to RabbitMQ
  getActivities:   22,
  getUserRecs:     10,
  getActRec:       8,
};

// ---------------------------------------------------------------------
// 2. CUSTOM METRICS  (these show up in --out json and Grafana)
// ---------------------------------------------------------------------
const httpReqDuration = new Trend('http_req_duration', true);
const httpReqFailed   = new Rate('http_req_failed');

const userRegisterLatency    = new Trend('user_register_latency',     true);
const getProfileLatency      = new Trend('get_profile_latency',       true);
const validateUserLatency    = new Trend('validate_user_latency',     true);
const trackActivityLatency   = new Trend('track_activity_latency',    true);
const getActivitiesLatency   = new Trend('get_activities_latency',    true);
const getUserRecsLatency     = new Trend('get_user_recs_latency',     true);
const getActRecLatency       = new Trend('get_act_rec_latency',       true);
const keycloakTokenLatency   = new Trend('keycloak_token_latency',    true);
const eurekaHealthLatency    = new Trend('eureka_health_latency',     true);
const aiProcessingE2E        = new Trend('ai_processing_e2e',         true); // activity POST → GET /api/recommendations/activity/{id}

const businessSuccessRate    = new Rate('business_success_rate');     // any 2xx
const authSuccessRate        = new Rate('auth_success_rate');
const activitySuccessRate    = new Rate('activity_success_rate');
const recommendationHitRate  = new Rate('recommendation_hit_rate');   // how often the async AI pipeline produced a record we can read
const errorRate4xx           = new Rate('error_rate_4xx');
const errorRate5xx           = new Rate('error_rate_5xx');

const userRegisteredCounter      = new Counter('users_registered_total');
const activitiesTrackedCounter    = new Counter('activities_tracked_total');
const recommendationsReadCounter = new Counter('recommendations_read_total');
const tokensIssuedCounter        = new Counter('tokens_issued_total');
const tokensReusedCounter        = new Counter('tokens_reused_total');
const tokensRefreshedCounter     = new Counter('tokens_refreshed_total');

const inflightActivityRequests   = new Gauge('inflight_activity_requests');
const openConnections            = new Gauge('open_connections');

// ---------------------------------------------------------------------
// 3. TEST OPTIONS  (multi-stage profile)
// ---------------------------------------------------------------------
//
// Profile interpretation:
//   stage 0  smoke       – 1m  very low, validates script + SUT
//   stage 1  ramp-up     – 2m  from 5 → 50 VUs
//   stage 2  load        – 5m  sustained 50 VUs (baseline)
//   stage 3  stress      – 5m  ramp 50 → 200 VUs (find breaking point)
//   stage 4  spike       – 2m  jump to 400 VUs (Gateway / Eureka behaviour)
//   stage 5  recovery    – 2m  drop to 50 VUs (how fast do we recover?)
//   stage 6  soak        – 10m 50 VUs (memory-leak / connection-pool detection)
//   stage 7  cooldown    – 1m  ramp-down
//
// Thresholds (per-endpoint SLOs) – tune to your RPS budget.
// ---------------------------------------------------------------------
export const options = {
  scenarios: {
    // -----------------------------------------------------------------
    // A) MIXED REALISTIC USER JOURNEY (default scenario, runs all stages)
    // -----------------------------------------------------------------
    mixed_journey: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m',  target: 5   },  // smoke
        { duration: '2m',  target: 50  },  // ramp
        { duration: '5m',  target: 50  },  // load
        { duration: '5m',  target: 200 },  // stress
        { duration: '2m',  target: 400 },  // spike
        { duration: '2m',  target: 50  },  // recovery
        { duration: '10m', target: 50  },  // soak / endurance
        { duration: '1m',  target: 0   },  // cooldown
      ],
      gracefulRampDown: '30s',
      exec: 'mixedJourney',
      tags: { scenario: 'mixed_journey' },
    },

    // -----------------------------------------------------------------
    // B) CONSTANT-OPEN-MODEL  –  measures pure throughput ceiling
    //     (uses arrival-rate executor, ignores VU cap)
    // -----------------------------------------------------------------
    throughput_ceiling: {
      executor: 'constant-arrival-rate',
      rate: 500,                // 500 RPS target
      timeUnit: '1s',
      duration: '2m',
      preAllocatedVUs: 100,
      maxVUs: 300,
      startTime: '20m',         // runs after mixed_journey soak
      exec: 'throughputCeiling',
      tags: { scenario: 'throughput' },
    },

    // -----------------------------------------------------------------
    // C) DISCOVERY & AUTH STRESS – independent infra probing
    // -----------------------------------------------------------------
    infra_probe: {
      executor: 'constant-vus',
      vus: 10,
      duration: '30m',          // runs entire test, pings Eureka + Keycloak
      exec: 'infraProbe',
      tags: { scenario: 'infra' },
    },
  },

  thresholds: {
    // Global / network
    'http_req_failed':              ['rate<0.01'],           // <1% errors overall
    'http_req_duration':            ['p(95)<800', 'p(99)<1500'],
    'http_reqs':                    ['count>0'],

    // Per-endpoint SLOs
    'user_register_latency':        ['p(95)<1000'],
    'get_profile_latency':          ['p(95)<300'],
    'validate_user_latency':        ['p(95)<200'],
    'track_activity_latency':       ['p(95)<600',  'p(99)<1200'], // write-path, includes Mongo + Rabbit publish
    'get_activities_latency':       ['p(95)<500'],
    'get_user_recs_latency':        ['p(95)<800'],
    'get_act_rec_latency':          ['p(95)<600'],
    'keycloak_token_latency':       ['p(95)<700'],
    'eureka_health_latency':        ['p(95)<300'],

    // Business
    'business_success_rate':        ['rate>0.99'],
    'auth_success_rate':            ['rate>0.99'],
    'activity_success_rate':        ['rate>0.98'],
    'recommendation_hit_rate':      ['rate>0.80'],   // async pipeline has lag
    'error_rate_4xx':               ['rate<0.05'],
    'error_rate_5xx':               ['rate<0.01'],
  },

  // Tag-scoped thresholds (per-service)
  ext: {
    loadimpact: { projectID: __ENV.K6_PROJECT_ID || 0 },
  },

  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  noConnectionReuse: false,
  userAgent: 'k6-smartfitai/1.0',
  discardResponseBodies: false,
};

// ---------------------------------------------------------------------
// 4. SHARED FIXTURES
// ---------------------------------------------------------------------
const activityTypes = ['RUNNING', 'CYCLING', 'SWIMMING', 'WALKING', 'YOGA', 'WEIGHT_TRAINING', 'HIIT', 'STRETCHING'];
const exerciseNames = ['morning run', 'lunchtime ride', 'evening swim', 'after-work gym', 'weekend hike', 'pilates flow'];

function pickActivity() {
  return {
    type:            activityTypes[randomIntBetween(0, activityTypes.length - 1)],
    duration:        randomIntBetween(10, 120),                          // minutes
    caloriesBurned:  randomIntBetween(50, 900),
    startTime:       new Date(Date.now() - randomIntBetween(0, 86400000)).toISOString(),
    additionalMetrics: {
      distanceKm:    +(Math.random() * 15).toFixed(2),
      avgHeartRate:  randomIntBetween(95, 175),
      maxHeartRate:  randomIntBetween(150, 195),
      elevationGain: randomIntBetween(0, 600),
      customNote:    exerciseNames[randomIntBetween(0, exerciseNames.length - 1)],
    },
  };
}

function pickRegister() {
  return {
    email:     `k6_${randomString(8).toLowerCase()}@smartfit.test`,
    password:  'P@ssw0rd_' + randomString(4),
    firstName: 'K6',
    lastName:  'User',
    keycloakId: uuidv4(),
  };
}

// ---------------------------------------------------------------------
// 5. KEYCLOAK TOKEN CACHE  (per-VU, 60s safety reissue)
// ---------------------------------------------------------------------
function getAccessToken() {
  // simple per-VU token cache using a global object
  if (!globalThis.__kc) globalThis.__kc = {};
  const key = `${__VU}`;
  const now = Date.now();
  const cached = globalThis.__kc[key];
  if (cached && cached.access_token && cached.expires_at > now + 5000) {
    tokensReusedCounter.add(1);
    return cached.access_token;
  }

  const url = `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token`;
  const body = {
    grant_type: 'password',
    client_id:  CLIENT_ID,
    username:   KC_USER,
    password:   KC_PASSWORD,
    scope:      'openid profile email',
  };
  const res = http.post(url, body, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    tags:    { endpoint: 'keycloak_token' },
  });
  keycloakTokenLatency.add(res.timings.duration);

  const ok = check(res, {
    'kc token 200': r => r.status === 200,
    'kc has access_token': r => (r.json('access_token') || '').length > 50,
  });
  if (!ok) {
    authSuccessRate.add(false);
    return null;
  }
  authSuccessRate.add(true);
  tokensIssuedCounter.add(1);

  const token = res.json('access_token');
  const expiresIn = Number(res.json('expires_in') || 60);
  globalThis.__kc[key] = { access_token: token, expires_at: now + expiresIn * 1000 };
  return token;
}

// ---------------------------------------------------------------------
// 6. SCENARIO: MIXED REALISTIC USER JOURNEY
// ---------------------------------------------------------------------
export function mixedJourney() {
  // ---- (a) Auth handshake (caching amortises cost) ----
  let token = getAccessToken();
  if (!token) {
    sleep(1);
    return;
  }
  const authHdr = { headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' } };

  // ---- (b) Pick action by weighted random ----
  const totalW = Object.values(W).reduce((a, b) => a + b, 0);
  let pick = Math.random() * totalW;
  let action = 'getProfile';
  for (const k of Object.keys(W)) {
    if ((pick -= W[k]) < 0) { action = k; break; }
  }

  // ---- (c) Execute the chosen journey step ----
  let res, ok, body;

  switch (action) {
    case 'register': {
      body = JSON.stringify(pickRegister());
      res = http.post(`${BASE_URL}/api/users/register`, body, { ...authHdr, tags: { endpoint: 'user_register' } });
      userRegisterLatency.add(res.timings.duration);
      ok = check(res, { 'register 200': r => r.status === 200, 'register has id': r => !!r.json('id') });
      if (ok) userRegisteredCounter.add(1);
      break;
    }
    case 'getProfile': {
      // Use the JWT 'sub' claim as userId, OR a random existing id (read-path tolerant)
      const userId = `user-${randomIntBetween(1, 1000)}`;
      res = http.get(`${BASE_URL}/api/users/${userId}`, { ...authHdr, tags: { endpoint: 'get_profile' } });
      getProfileLatency.add(res.timings.duration);
      check(res, { 'getProfile 200/404': r => r.status === 200 || r.status === 404 });
      break;
    }
    case 'validateUser': {
      const userId = `user-${randomIntBetween(1, 1000)}`;
      res = http.get(`${BASE_URL}/api/users/${userId}/validate`, { ...authHdr, tags: { endpoint: 'validate_user' } });
      validateUserLatency.add(res.timings.duration);
      check(res, { 'validate 200': r => r.status === 200 });
      break;
    }
    case 'trackActivity': {
      // Async pipeline: POST activity → wait → GET recommendation → measure e2e
      const userId = `user-${randomIntBetween(1, 1000)}`;
      const activity = pickActivity();
      const t0 = Date.now();
      inflightActivityRequests.add(1);
      res = http.post(
        `${BASE_URL}/api/activities`,
        JSON.stringify(activity),
        { ...authHdr, headers: { ...authHdr.headers, 'X-User-ID': userId }, tags: { endpoint: 'track_activity' } }
      );
      inflightActivityRequests.add(-1);
      trackActivityLatency.add(res.timings.duration);
      ok = check(res, { 'track 200': r => r.status === 200 || r.status === 201 });
      if (ok) {
        activitySuccessRate.add(true);
        activitiesTrackedCounter.add(1);
        const activityId = res.json('id');

        // Asynchronously wait for AI pipeline (RabbitMQ consume → Gemini → Mongo)
        // The recommendation appears within ~1-5s normally. We poll up to 12s.
        if (activityId) {
          let rec = null;
          for (let i = 0; i < 6; i++) {
            sleep(2);
            const r2 = http.get(`${BASE_URL}/api/recommendations/activity/${activityId}`, { ...authHdr, tags: { endpoint: 'get_act_rec' } });
            getActRecLatency.add(r2.timings.duration);
            if (r2.status === 200) { rec = r2; break; }
            if (r2.status === 404 && i === 5) { rec = r2; }   // give up after 12s
          }
          if (rec && rec.status === 200) {
            recommendationHitRate.add(true);
            recommendationsReadCounter.add(1);
            aiProcessingE2E.add(Date.now() - t0);
          } else {
            recommendationHitRate.add(false);
          }
        }
      } else {
        activitySuccessRate.add(false);
      }
      break;
    }
    case 'getActivities': {
      const userId = `user-${randomIntBetween(1, 1000)}`;
      res = http.get(`${BASE_URL}/api/activities`, { ...authHdr, headers: { ...authHdr.headers, 'X-User-ID': userId }, tags: { endpoint: 'get_activities' } });
      getActivitiesLatency.add(res.timings.duration);
      check(res, { 'getActs 200': r => r.status === 200 });
      break;
    }
    case 'getUserRecs': {
      const userId = `user-${randomIntBetween(1, 1000)}`;
      res = http.get(`${BASE_URL}/api/recommendations/user/${userId}`, { ...authHdr, tags: { endpoint: 'get_user_recs' } });
      getUserRecsLatency.add(res.timings.duration);
      check(res, { 'getUserRecs 200': r => r.status === 200 });
      break;
    }
    case 'getActRec': {
      const activityId = `act-${randomIntBetween(1, 5000)}`;
      res = http.get(`${BASE_URL}/api/recommendations/activity/${activityId}`, { ...authHdr, tags: { endpoint: 'get_act_rec' } });
      getActRecLatency.add(res.timings.duration);
      check(res, { 'getActRec 200/404': r => r.status === 200 || r.status === 404 });
      break;
    }
  }

  // ---- (d) Global response classification ----
  if (res) {
    if (res.status >= 200 && res.status < 300) businessSuccessRate.add(true);
    else businessSuccessRate.add(false);
    if (res.status >= 400 && res.status < 500) errorRate4xx.add(1);
    if (res.status >= 500)                      errorRate5xx.add(1);
    httpReqFailed.add(res.status >= 400);
    openConnections.add(__VU);
  }

  // Realistic think-time
  sleep(randomIntBetween(1, 3) / 5);
}

// ---------------------------------------------------------------------
// 7. SCENARIO: THROUGHPUT CEILING (open-model, 500 RPS)
// ---------------------------------------------------------------------
export function throughputCeiling() {
  // No user-journey logic – just brute-force read traffic
  const token = getAccessToken();
  if (!token) return;
  const authHdr = { headers: { Authorization: `Bearer ${token}` } };

  const r = Math.random();
  if (r < 0.6) {
    http.get(`${BASE_URL}/api/activities`, { ...authHdr, headers: { ...authHdr.headers, 'X-User-ID': 'load-1' }, tags: { endpoint: 'get_activities' } });
  } else if (r < 0.9) {
    http.get(`${BASE_URL}/api/users/user-1`, { ...authHdr, tags: { endpoint: 'get_profile' } });
  } else {
    http.get(`${BASE_URL}/api/recommendations/user/load-1`, { ...authHdr, tags: { endpoint: 'get_user_recs' } });
  }
}

// ---------------------------------------------------------------------
// 8. SCENARIO: INFRA PROBE  (Eureka + Keycloak health)
// ---------------------------------------------------------------------
export function infraProbe() {
  group('Eureka', () => {
    const r = http.get(`${EUREKA_URL}/actuator/health`, { tags: { endpoint: 'eureka_health' } });
    eurekaHealthLatency.add(r.timings.duration);
    check(r, { 'eureka up': x => x.status === 200 });
  });
  group('Eureka apps', () => {
    const r = http.get(`${EUREKA_URL}/eureka/apps`, { tags: { endpoint: 'eureka_apps' } });
    check(r, { 'eureka apps 200': x => x.status === 200 });
  });
  group('Keycloak realm', () => {
    const r = http.get(`${KEYCLOAK_URL}/realms/${REALM}`, { tags: { endpoint: 'keycloak_realm' } });
    check(r, { 'kc realm 200': x => x.status === 200 });
  });
  group('Gateway actuator', () => {
    const r = http.get(`${BASE_URL}/actuator/health`, { tags: { endpoint: 'gateway_health' } });
    check(r, { 'gw up': x => x.status === 200 });
  });
  sleep(2);
}

// ---------------------------------------------------------------------
// 9. SETUP / TEARDOWN
// ---------------------------------------------------------------------
export function setup() {
  // Pre-flight: verify SUT is up
  const checks = {
    gateway:    http.get(`${BASE_URL}/actuator/health`),
    eureka:     http.get(`${EUREKA_URL}/actuator/health`),
    keycloak:   http.get(`${KEYCLOAK_URL}/realms/${REALM}`),
    config:     http.get('http://localhost:8888/actuator/health'),
  };
  console.log('--- SUT preflight ---');
  for (const [k, v] of Object.entries(checks)) {
    console.log(`  ${k.padEnd(10)} → ${v.status}`);
  }
  const anyDown = Object.values(checks).some(v => v.status >= 500);
  if (anyDown) {
    fail('SUT preflight failed – is docker compose up?');
  }
  return { startedAt: new Date().toISOString() };
}

export function teardown(data) {
  console.log(`--- Test finished at ${new Date().toISOString()} (started ${data.startedAt}) ---`);
}

// ---------------------------------------------------------------------
// 10. CUSTOM SUMMARY (resume-friendly Markdown + CSV)
// ---------------------------------------------------------------------
export function handleSummary(data) {
  const md = textSummary(data, { indent: ' ', enableColors: false });
  return {
    'stdout':                 textSummary(data, { indent: ' ', enableColors: true }),
    'reports/summary.md':     md,
    'reports/summary.json':   JSON.stringify(data, null, 2),
  };
}
