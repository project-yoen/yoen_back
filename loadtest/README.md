# YOEN 부하테스트 / 응답시간 측정

로컬에서 API 응답시간을 측정하고 개선 전/후를 비교하기 위한 도구 모음.

## 준비물

- Docker (postgres, redis, 모니터링 스택)
- [k6](https://k6.io/) (`winget install k6 --source winget` 또는 `choco install k6`)
- `.env` 세팅된 상태에서 앱 기동 가능해야 함

## 실행 순서

```powershell
# 1. 인프라 기동 (postgres, redis)
docker compose up -d

# 2. 앱 기동 (SQL 로깅 켜려면 HIBERNATE_STATS=true)
#    bootRun으로 실행해야 p6spy(SQL 로그)가 활성화됨 - 운영 jar에는 미포함
$env:HIBERNATE_STATS="true"
./gradlew bootRun

# 3. (선택) 모니터링 랩 기동 - Grafana http://localhost:3000
docker compose -f compose.monitoring.local.yaml up -d

# 4. 부하테스트 유저 등록 (최초 1회)
k6 run loadtest/seed_users.js

# 5. 시드 데이터 주입 (최초 1회, 컨테이너/DB 이름은 .env 값에 맞게)
docker exec -i <postgres-container> psql -U <DATABASE_USERNAME> -d <DATABASE_NAME> < loadtest/seed.sql

# 6. 측정 실행
k6 run loadtest/api_baseline.js
# 파라미터 조절:
k6 run -e VUS=10 -e DURATION=2m loadtest/api_baseline.js
```

## 보는 곳

- **k6 출력**: 엔드포인트별(`name` 태그) p95/p99 — 개선 전/후 비교의 기준 수치
- **Grafana `YOEN API Latency` 대시보드**: URI별 p95/p99, HikariCP 커넥션 상태 (pending이 뜨면 풀 고갈)
- **앱 로그**: p6spy가 쿼리별 실행시간을, `HIBERNATE_STATS=true`가 세션(≈요청)당 JDBC statement 수를 출력
  - 예: `... spent 12ms executing 41 JDBC statements` ← 요청 하나에 쿼리 41개면 N+1

## 측정 시나리오 (api_baseline.js)

setup에서 로그인(토큰 4개) 후, VU들이 반복 실행:

| 요청 | 노리는 병목 |
| --- | --- |
| `GET /travel` | 여행 목록 + EAGER 이미지 |
| `GET /travel/userdetail` | 유저별 개별 조회 N+1 |
| `GET /payment?travelId=` | 결제 전체 목록 N+1 (200건) |
| `GET /payment?travelId=&date=&type=` | 날짜별 결제 목록 N+1 |
| `GET /payment/detail?paymentId=` | 정산 다층 N+1 |
| `GET /record?travelId=&date=` | 기록별 이미지 조회 N+1 |

시드 데이터 규모: 여행 1개, 유저 4명, 결제 200건(정산 200건 + 정산유저 800건), 기록 50건(이미지 100장, 더미 URL).

## 결과 기록

- 개선 전: `BASELINE.md`
- 개선 후: `RESULTS.md` (같은 시드·같은 시나리오로 재측정해서 비교)
