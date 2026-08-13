# Smart-Fit-AI — k6 Performance Test Suite

A **production-grade load / stress / spike / soak / smoke** testing harness for
[Smart-Fit-AI](https://github.com/LokeshSiddi/Smart-Fit-AI), built with
[Grafana k6](https://k6.io).

It exercises every public endpoint of every Spring Boot microservice through
the API Gateway (with a real OAuth2 / JWT handshake against Keycloak), plus
direct infra probes against Eureka. Results are written to InfluxDB, visualised
in Grafana, and a resume-ready Markdown report is auto-generated.

---

## 🏗️ Architecture under test

```
                ┌──────────────────────────────────────────┐
                │           Smart-Fit-AI stack             │
                │                                          │
   k6 ─────────▶│  API Gateway (8080)  ─┬─▶ User Svc (8081, PostgreSQL)
                │                       ├─▶ Activity Svc (8082, Mongo + RabbitMQ publish)
                │  Eureka (8761)        └─▶ AI Svc (8083, Mongo + RabbitMQ consume + Gemini)
                │  Config Svc (8888)
                │  Keycloak (8181, realm: fitness-oauth2)
                └──────────────────────────────────────────┘
```

| Service | Port | DB / Queue | Purpose |
|---|---|---|---|
| API Gateway | 8080 | — | Spring Cloud Gateway, JWT validation, CORS |
| User Service | 8081 | PostgreSQL | `/api/users/{id}`, `/register`, `/{id}/validate` |
| Activity Service | 8082 | MongoDB + RabbitMQ | `/api/activities` (POST publishes to `activity.queue`) |
| AI Service | 8083 | MongoDB + RabbitMQ + Gemini | `/api/recommendations/user/{id}`, `/activity/{id}` |
| Eureka | 8761 | — | Service discovery |
| Config Server | 8888 | — | Centralised config |
| Keycloak | 8181 | — | OAuth2 / JWT issuer (realm `fitness-oauth2`) |

---

## 📂 Files in this folder

| File | Purpose |
|---|---|
| `smart-fit-ai-loadtest.js` | **Main script** — 8-stage ramping profile with mixed user-journey, throughput-ceiling and infra-probe scenarios |
| `smoke.js` | 5 VU · 30 s pre-deploy sanity check |
| `spike.js` | 5 → 500 VU burst, validates Gateway + Eureka behaviour under sudden load |
| `soak.js` | 50 VU · 1 h, detects memory leaks, connection-pool exhaustion, RabbitMQ consumer-lag growth |
| `keycloak-token.js` | Constant 200 RPS to Keycloak — sizes the auth server |
| `eureka-discovery.js` | 20 VU constant load against Eureka registry |
| `docker-compose.test.yml` | One-shot runner: k6 + InfluxDB + Grafana dashboard |
| `grafana/dashboards/smart-fit-ai.json` | Pre-built Grafana dashboard |
| `parse_results.py` | Converts `summary.json` → resume-friendly Markdown table |
| `reports/` | Auto-created; contains `summary.json` + `summary.md` after each run |

---

## 🚀 Quick start

### 1. Start the SUT (Smart-Fit-AI itself)
```bash
git clone https://github.com/LokeshSiddi/Smart-Fit-AI.git
cd Smart-Fit-AI
cp .env.example .env       # fill in your GEMINI_API_KEY
docker compose up -d       # brings up postgres, mongo, rabbitmq, keycloak, eureka, config, gateway, user/activity/ai svcs
```

### 2. Install k6
```bash
# macOS
brew install k6
# Linux
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update && sudo apt-get install k6
```

### 3. Run individual tests
```bash
# Smoke (30 s)
k6 run smoke.js

# Spike
k6 run spike.js

# Soak (1 h)
k6 run soak.js

# Full ramping profile (≈28 minutes)
k6 run --summary-export=reports/summary.json smart-fit-ai-loadtest.js
```

### 4. Run with the dashboard (InfluxDB + Grafana)
```bash
docker compose -f docker-compose.test.yml --profile grafana up -d influxdb grafana
docker compose -f docker-compose.test.yml --profile load    up k6-load
```
- Grafana → http://localhost:3000 (admin / admin)
- Open dashboard **Smart-Fit-AI — k6 Load Test**

### 5. Generate the resume report
```bash
k6 run --summary-export=summary.json smart-fit-ai-loadtest.js
python3 parse_results.py summary.json > resume-metrics.md
```

---

## 🧪 What the main test actually measures

The main script (`smart-fit-ai-loadtest.js`) defines **three scenarios** running in parallel:

### A. `mixed_journey` — realistic weighted user behaviour
Each VU picks one of 7 journey steps with these weights (sum = 100):

| Action | Weight | What it hits |
|---|---|---|
| `register` | 2 | `POST /api/users/register` (Postgres write) |
| `getProfile` | 18 | `GET /api/users/{id}` (Postgres read) |
| `validateUser` | 10 | `GET /api/users/{id}/validate` |
| `trackActivity` | 30 | `POST /api/activities` → Mongo write + Rabbit publish |
| `getActivities` | 22 | `GET /api/activities` (Mongo read) |
| `getUserRecs` | 10 | `GET /api/recommendations/user/{id}` (Mongo read) |
| `getActRec` | 8 | `GET /api/recommendations/activity/{id}` (Mongo read) |

Stages:
```
0  smoke       1 m    5   VUs    (sanity)
1  ramp        2 m    50  VUs
2  load        5 m    50  VUs    (baseline SLOs)
3  stress      5 m    200 VUs    (find breaking point)
4  spike       2 m    400 VUs    (Gateway/Eureka burst)
5  recovery    2 m    50  VUs    (recovery speed)
6  soak        10 m   50  VUs    (leak detection)
7  cooldown    1 m    0   VUs
```

### B. `throughput_ceiling` — open-model 500 RPS
Constant-arrival-rate executor hits the gateway with **500 req/s** for 2 minutes
to find the *real* ceiling independent of think-time.

### C. `infra_probe` — Eureka + Keycloak + Gateway health
10 VUs running the entire test, pinging:
- `GET /actuator/health` on Eureka, Gateway
- `GET /realms/fitness-oauth2` on Keycloak
- `GET /eureka/apps` to validate every service is `UP`

---

## 📊 Custom metrics emitted

The script exports ~25 custom metrics, all visible in Grafana:

| Metric | Type | Meaning |
|---|---|---|
| `http_req_duration` | trend | standard k6 latency |
| `http_req_failed` | rate | overall failure rate |
| `user_register_latency` | trend | SLO for register endpoint |
| `get_profile_latency` | trend | SLO for profile read |
| `validate_user_latency` | trend | SLO for user validation |
| `track_activity_latency` | trend | SLO for activity POST (Mongo + Rabbit) |
| `get_activities_latency` | trend | SLO for activity list |
| `get_user_recs_latency` | trend | SLO for AI recs by user |
| `get_act_rec_latency` | trend | SLO for AI recs by activity |
| `keycloak_token_latency` | trend | SLO for auth |
| `eureka_health_latency` | trend | SLO for service discovery |
| **`ai_processing_e2e`** | trend | **End-to-end: activity POST → recommendation GET** (async pipeline) |
| `business_success_rate` | rate | any 2xx response |
| `auth_success_rate` | rate | Keycloak 200s |
| `activity_success_rate` | rate | POST /api/activities 2xx |
| `recommendation_hit_rate` | rate | async pipeline success within 12 s |
| `error_rate_4xx` | rate | client errors |
| `error_rate_5xx` | rate | server errors |
| `users_registered_total` | counter | synthetic sign-ups created |
| `activities_tracked_total` | counter | activities written |
| `recommendations_read_total` | counter | AI recs read back |
| `tokens_issued_total` | counter | JWTs minted |
| `tokens_reused_total` | counter | JWT cache hits (proves the per-VU token cache works) |
| `inflight_activity_requests` | gauge | concurrent in-flight POSTs |

---

## 🎯 SLO thresholds (per-endpoint)

These are baked in; tweak in `smart-fit-ai-loadtest.js → options.thresholds`:

| Endpoint | p(95) target | Error budget |
|---|---|---|
| `POST /api/users/register` | < 1000 ms | < 1% |
| `GET  /api/users/{id}` | < 300 ms | < 1% |
| `GET  /api/users/{id}/validate` | < 200 ms | < 1% |
| `POST /api/activities` | < 600 ms (p99 < 1200) | < 1% |
| `GET  /api/activities` | < 500 ms | < 1% |
| `GET  /api/recommendations/user/{id}` | < 800 ms | < 1% |
| `GET  /api/recommendations/activity/{id}` | < 600 ms | < 1% |
| `POST Keycloak /token` | < 700 ms | < 1% |
| `GET  Eureka /actuator/health` | < 300 ms | < 1% |
| **Global** | p95 < 800 ms, p99 < 1500 ms | < 1% |

---

## 📝 Resume-ready snippet

After running, paste this in your portfolio / LinkedIn / CV:

> **Performance-tested a 7-service Spring Boot + Spring Cloud + RabbitMQ + PostgreSQL + MongoDB + Keycloak fitness platform with k6.**
> Designed and executed 8-stage ramping load (5 → 400 VUs), 500 RPS throughput-ceiling, 1-hour soak, and spike tests across 7 endpoints, with per-VU Keycloak token caching, RabbitMQ async-pipeline end-to-end latency tracking, and 25+ custom metrics. Built an InfluxDB + Grafana dashboard for live visualisation and an auto-generated Markdown report. Validated per-endpoint SLOs (e.g. `POST /api/activities` p95 < 600 ms, p99 < 1200 ms) and identified the activity-write path as the bottleneck under stress.

---

## 🛠️ Troubleshooting

| Symptom | Fix |
|---|---|
| `401 Unauthorized` everywhere | Set `KC_USER` / `KC_PASSWORD` env vars to a real user in the `fitness-oauth2` realm. Default in scripts is `testuser` / `testpass`. |
| `404` on `/actuator/health` from user/activity/AI svc | These services only expose `health,info`; the gateway permits all `/actuator/**`. Direct hits work too. |
| `connection refused: localhost:8181` | Keycloak isn't up — wait for `docker compose ps` to show `healthy`. |
| RabbitMQ consumer lag | Check `docker logs smartfit_aiservice` — Gemini API call may be throttled; reduce `W.trackActivity` weight. |
| k6: `ERRO[0001] memory` | Bump `K6_NO_THRESHOLDS=1` and `K6_VUS=400` carefully; default heap is 1 GB. |
| Tests pass but `recommendation_hit_rate < 0.8` | The async pipeline (Rabbit → AI → Gemini → Mongo) has 5–15 s lag under load; increase the poll loop in `mixedJourney`. |

---

## 📜 License

MIT — feel free to copy this harness into your own Spring Boot / microservices
projects and adapt the endpoint table to your own routes.
