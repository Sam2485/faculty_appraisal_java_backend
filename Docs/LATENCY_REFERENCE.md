# Faculty Appraisal Java Backend — Latency Reference

## Infrastructure

| Setting | Value |
|---|---|
| Platform | GCP Cloud Run (asia-south1) |
| Runtime | Java 21 + Spring Boot 3.5 |
| Database | Cloud SQL PostgreSQL (same region) |
| Execution Environment | 2nd Generation |
| Min Instances | 1 (always warm) |
| Memory / CPU | 512 MiB / 1 vCPU |
| Concurrency | 80 requests/instance |

---

## Cold Start (first request after deployment)

> Only happens once after a new deployment. Min instances = 1 means **no cold starts during normal usage**.

```
JVM + Spring Boot startup:    4 – 6 seconds
Cloud Run container spin-up:  ~500ms
─────────────────────────────────────────────
Total (deploy only):          ~5 – 7 seconds
After that: never again until next deploy
```

---

## Warm Request Latency

### Authentication

| Endpoint | P50 | P95 | P99 | Bottleneck |
|---|---|---|---|---|
| `POST /auth/login` | 150ms | 250ms | 400ms | BCrypt (intentionally slow) |
| `POST /auth/register` | 100ms | 180ms | 300ms | BCrypt + DB write |
| `GET /auth/me` | 10ms | 25ms | 50ms | JWT decode + 1 DB lookup |

### Faculty Appraisal

| Endpoint | P50 | P95 | P99 | Bottleneck |
|---|---|---|---|---|
| `GET /appraisal/status` | 15ms | 35ms | 60ms | 2 indexed DB queries |
| `GET /appraisal/snapshot` | 10ms | 25ms | 50ms | 1 indexed DB query |
| `PUT /appraisal/snapshot` | 20ms | 45ms | 80ms | 1–2 DB writes |
| `POST /appraisal/submit` | 250ms | 500ms | 900ms | 22 batch DELETEs + INSERTs |

### Dashboard

| Endpoint | P50 | P95 | P99 | Bottleneck |
|---|---|---|---|---|
| `GET /dashboard/subordinates` | 40ms | 80ms | 150ms | 3–4 batch DB queries |
| `GET /dashboard/faculty/{email}` | 20ms | 40ms | 80ms | 2 DB queries + JSON parse |

### Admin

| Endpoint | P50 | P95 | P99 | Bottleneck |
|---|---|---|---|---|
| `GET /admin/stats` | 150ms | 350ms | 600ms | Full table scan on faculty_profiles |
| `GET /admin/users` | 80ms | 150ms | 300ms | Filtered DB query |
| `GET /admin/submissions` | 100ms | 200ms | 400ms | JOIN across declarations + profiles |
| `GET /admin/export` | 300ms | 600ms | 1200ms | Full CSV build in memory |

### Non-Teaching

| Endpoint | P50 | P95 | P99 | Bottleneck |
|---|---|---|---|---|
| `GET /non-teaching/workflow` | 20ms | 40ms | 80ms | 2–3 DB queries |
| `POST /non-teaching/submit` | 80ms | 150ms | 280ms | Multi-table write |
| `PUT /non-teaching/review` | 60ms | 120ms | 220ms | Workflow state update |

---

## DB Query Speed (Cloud SQL, same region)

```
Indexed lookup (email + year):    2 – 5ms
Full table scan (small table):   10 – 30ms
Full table scan (large table):   50 – 300ms
Batch INSERT (saveAll, 50 rows): 10 – 25ms
Single INSERT:                    3 – 8ms
```

---

## Summary (Typical User Experience)

```
Login:               ~200ms   (BCrypt — cannot be faster by design)
Loading form:        ~15ms    (snapshot fetch)
Saving draft:        ~25ms    (snapshot upsert)
Submitting form:     ~350ms   (heaviest write operation)
Viewing dashboard:   ~50ms    (batch loaded)
Admin stats page:    ~200ms   (heaviest read operation)
```

---

## Optimizations Applied

| Optimization | Before | After |
|---|---|---|
| Hibernate batch inserts | 1 INSERT per row | 50 rows per batch |
| Document saves | 1 DB call per file | 1 batch call total |
| Reflection field lookup | Class hierarchy traversal every call | Cached after first lookup |
| Cache size | 500 entries | 2000 entries |
| DB idle connections | Dropped after 10 min | Kept alive with 60s ping |
| Cold starts | Every ~15 min of idle | Never (min instances = 1) |
| JVM startup | ~7s | ~5s (fast SecureRandom + no JMX) |
| Tomcat threads | 200 (default, wasteful) | 100 (right-sized for I/O-bound) |
| HikariCP idle timeout | 10 minutes | 5 minutes (suits Cloud Run) |

---

## Cloud Run Configuration

| Setting | Value | Reason |
|---|---|---|
| Min instances | 1 | Keeps one instance always warm — no cold starts |
| Max instances | 3 | Caps DB connections at 45 (15 pool × 3), within Cloud SQL's 100 limit |
| Concurrency | 80 | Matches Tomcat thread count |
| Memory | 512 MiB | JVM heap capped at 75% = ~384 MiB, sufficient for this workload |
| CPU | 1 vCPU | Adequate for I/O-bound JDBC workload |
| Execution environment | 2nd Generation | Better CPU, network, and Linux compatibility |
| Startup CPU boost | Enabled | Extra CPU during JVM startup reduces cold start time |
| Billing | Request-based | Cost-efficient; min=1 handles warmth without always-on CPU cost |

---

## JVM Flags (Dockerfile)

```
-XX:+UseContainerSupport      Reads cgroup limits instead of host RAM
-XX:MaxRAMPercentage=75.0     Caps heap at 75% of container RAM (~384 MiB)
-XX:InitialRAMPercentage=50.0 Pre-sizes heap to avoid GC pressure on startup
-Djava.security.egd=file:/dev/./urandom  Fast SecureRandom (no entropy blocking)
-Dspring.jmx.enabled=false    Disables unused JMX — saves ~50-100ms startup
-Dfile.encoding=UTF-8         Explicit encoding for consistent string handling
```

---

## Connection Pool (HikariCP)

```
Maximum pool size:   15 connections
Minimum idle:         2 connections
Connection timeout:  30 seconds
Idle timeout:         5 minutes
Max lifetime:        30 minutes
Keepalive time:      60 seconds   ← prevents Cloud SQL proxy from dropping idle connections
```

---

*Last updated: May 2026*
*Backend: faculty-appraisal-java-backend (Cloud Run, asia-south1)*
