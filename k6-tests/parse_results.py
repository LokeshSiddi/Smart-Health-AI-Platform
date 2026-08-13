#!/usr/bin/env python3
"""
parse_results.py — Turn k6's summary.json into a beautiful Markdown
report suitable for pasting into a resume / LinkedIn / portfolio.

Usage:
    k6 run --summary-export=summary.json smart-fit-ai-loadtest.js
    python3 parse_results.py summary.json > resume-metrics.md
"""

import json
import sys
from pathlib import Path


def fmt(v, unit=""):
    if v is None:
        return "—"
    if isinstance(v, float):
        if v >= 1000:
            return f"{v:,.0f}{unit}"
        if v >= 10:
            return f"{v:.1f}{unit}"
        return f"{v:.3f}{unit}"
    return f"{v}{unit}"


def main(path: str) -> str:
    data = json.loads(Path(path).read_text())

    md = []
    md.append("# Smart-Fit-AI — Performance Test Report\n")
    md.append(f"_Generated from `{path}`_\n")

    # 1. Top-line
    metrics = data.get("metrics", {})
    root = data.get("root_group", {}) or {}
    checks = metrics.get("checks", {})

    def get(metric, key, default=None):
        m = metrics.get(metric, {})
        if not m:
            return default
        if "values" in m and key in m["values"]:
            return m["values"][key]
        return m.get(key, default)

    total_reqs = get("http_reqs", "count", 0)
    fail_rate  = get("http_req_failed", "rate", 0.0)
    dur        = get("iteration_duration", "avg", 0.0)
    p95        = get("http_req_duration", "p(95)", 0.0)
    p99        = get("http_req_duration", "p(99)", 0.0)
    rps        = get("http_reqs", "rate", 0.0)
    vus_max    = get("vus_max", "value", 0)

    md.append("## 🔑 Headline Numbers\n")
    md.append("| Metric | Value |")
    md.append("|---|---|")
    md.append(f"| Total HTTP requests        | **{fmt(total_reqs)}** |")
    md.append(f"| Throughput (avg RPS)       | **{fmt(rps, ' req/s')}** |")
    md.append(f"| Error rate (HTTP failures) | **{fmt(fail_rate * 100, ' %')}** |")
    md.append(f"| Latency p95                | **{fmt(p95, ' ms')}** |")
    md.append(f"| Latency p99                | **{fmt(p99, ' ms')}** |")
    md.append(f"| Peak virtual users         | **{fmt(vus_max)}** |")
    md.append("")

    # 2. Per-endpoint SLO table (the resume highlight)
    endpoint_metrics = [
        ("user_register_latency",     "POST /api/users/register"),
        ("get_profile_latency",       "GET  /api/users/{id}"),
        ("validate_user_latency",     "GET  /api/users/{id}/validate"),
        ("track_activity_latency",    "POST /api/activities"),
        ("get_activities_latency",    "GET  /api/activities"),
        ("get_user_recs_latency",     "GET  /api/recommendations/user/{id}"),
        ("get_act_rec_latency",       "GET  /api/recommendations/activity/{id}"),
        ("keycloak_token_latency",    "POST Keycloak /token"),
        ("eureka_health_latency",     "GET  Eureka /actuator/health"),
        ("ai_processing_e2e",         "End-to-end: activity POST → AI rec"),
    ]
    md.append("## 🎯 Per-Endpoint Performance\n")
    md.append("| Endpoint | avg | p(90) | p(95) | p(99) | max |")
    md.append("|---|---|---|---|---|---|")
    for key, label in endpoint_metrics:
        m = metrics.get(key, {})
        if not m:
            continue
        v = m.get("values", {})
        md.append(
            f"| `{label}` | {fmt(v.get('avg'),' ms')} | {fmt(v.get('p(90)'),' ms')} | "
            f"{fmt(v.get('p(95)'),' ms')} | {fmt(v.get('p(99)'),' ms')} | {fmt(v.get('max'),' ms')} |"
        )
    md.append("")

    # 3. Business KPIs
    md.append("## 📈 Business KPIs\n")
    md.append("| KPI | Value |")
    md.append("|---|---|")
    md.append(f"| Users registered (synthetic)    | **{fmt(get('users_registered_total','count'))}** |")
    md.append(f"| Activities tracked (synthetic)  | **{fmt(get('activities_tracked_total','count'))}** |")
    md.append(f"| Recommendations read (synthetic)| **{fmt(get('recommendations_read_total','count'))}** |")
    md.append(f"| JWT tokens issued               | **{fmt(get('tokens_issued_total','count'))}** |")
    md.append(f"| JWT tokens reused (cache hit)   | **{fmt(get('tokens_reused_total','count'))}** |")
    md.append(f"| Business success rate           | **{fmt(get('business_success_rate','rate',0)*100,' %')}** |")
    md.append(f"| Auth (Keycloak) success rate    | **{fmt(get('auth_success_rate','rate',0)*100,' %')}** |")
    md.append(f"| Activity success rate           | **{fmt(get('activity_success_rate','rate',0)*100,' %')}** |")
    md.append(f"| AI recommendation hit rate      | **{fmt(get('recommendation_hit_rate','rate',0)*100,' %')}** |")
    md.append(f"| 4xx error rate                  | **{fmt(get('error_rate_4xx','rate',0)*100,' %')}** |")
    md.append(f"| 5xx error rate                  | **{fmt(get('error_rate_5xx','rate',0)*100,' %')}** |")
    md.append("")

    # 4. SLO compliance
    md.append("## ✅ SLO Compliance\n")
    md.append("| Threshold | Result |")
    md.append("|---|---|")
    for t in data.get("thresholds", []):
        ok = "✅ pass" if t.get("ok") else "❌ fail"
        md.append(f"| `{t['name']} {t['threshold']}` | {ok} |")
    md.append("")

    md.append("---")
    md.append("_Tooling: k6 · InfluxDB · Grafana · Spring Boot Actuator · "
              "Spring Cloud Gateway · Eureka · Keycloak · RabbitMQ · PostgreSQL · MongoDB_\n")

    return "\n".join(md)


if __name__ == "__main__":
    path = sys.argv[1] if len(sys.argv) > 1 else "summary.json"
    print(main(path))
