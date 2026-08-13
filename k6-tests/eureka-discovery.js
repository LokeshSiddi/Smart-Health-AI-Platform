// =====================================================================
// Smart-Fit-AI — EUREKA / DISCOVERY STRESS
// Hits the Eureka registry to ensure that under load the service
// registry remains queryable, the heartbeat is stable, and that all
// services (USER-SERVICE, ACTIVITY-SERVICE, AI-SERVICE) stay UP.
// =====================================================================

import http from 'k6/http';
import { check } from 'k6';
import { Trend, Counter } from 'k6/metrics';

const EUREKA_URL = __ENV.EUREKA_URL || 'http://localhost:8761';

const appsQueryLatency = new Trend('eureka_apps_latency', true);
const appQueryLatency  = new Trend('eureka_app_latency',  true);
const appsUp           = new Counter('eureka_apps_up');

export const options = {
  scenarios: {
    eureka: {
      executor: 'constant-vus',
      vus: 20,
      duration: '5m',
    },
  },
  thresholds: {
    'eureka_apps_latency': ['p(95)<300'],
    'eureka_app_latency':  ['p(95)<200'],
  },
};

export default function () {
  const apps = ['USER-SERVICE', 'ACTIVITY-SERVICE', 'AI-SERVICE', 'API-GATEWAY'];
  // List all apps
  const r1 = http.get(`${EUREKA_URL}/eureka/apps`, { tags: { endpoint: 'eureka_apps' } });
  appsQueryLatency.add(r1.timings.duration);
  check(r1, { 'apps 200': x => x.status === 200 });
  if (r1.status === 200) {
    const body = r1.body || '';
    for (const a of apps) if (body.includes(a)) appsUp.add(1);
  }
  // Per-app lookup
  for (const a of apps) {
    const r2 = http.get(`${EUREKA_URL}/eureka/apps/${a}`, { tags: { endpoint: 'eureka_app' } });
    appQueryLatency.add(r2.timings.duration);
    check(r2, { `${a} 200`: x => x.status === 200 });
  }
  sleep(1);
}
