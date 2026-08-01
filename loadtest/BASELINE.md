# 개선 전 베이스라인 (2026-08-01)

측정 환경: 로컬 Windows, postgres/redis Docker, `./gradlew bootRun`(HIBERNATE_STATS=true), k6 in Docker.
시나리오: `api_baseline.js`, 10 VU × 2분, 총 53,850 요청, 실패율 0%.
시드: 여행 1개, 유저 4명, 결제 200건(+정산 200/정산유저 800), 기록 50건(+이미지 100).

## 엔드포인트별 응답시간 (k6)

| 엔드포인트 | avg | p95 | p99 | max |
| --- | ---: | ---: | ---: | ---: |
| GET /travel | 9.2ms | 11.9ms | 14.4ms | 36ms |
| GET /travel/userdetail | 18.1ms | 22.4ms | 26.9ms | 79ms |
| GET /payment (전체 200건) | 28.6ms | 35.1ms | 41.5ms | 136ms |
| GET /payment (날짜별 20건) | 18.7ms | 23.4ms | 28.0ms | 89ms |
| GET /payment/detail | 22.3ms | 27.5ms | 33.2ms | 102ms |
| GET /record (날짜별 5건) | 35.6ms | 44.0ms | 50.5ms | 117ms |

## 요청당 JDBC statement 수 (Hibernate 세션 통계)

모든 요청에 JWT 필터의 유저 조회 세션(+1 쿼리)이 별도로 붙음.

| 엔드포인트 | 본 처리 쿼리 수 | 원인 |
| --- | ---: | --- |
| GET /travel | 1 | (EAGER travelImage가 join으로 처리됨) |
| GET /travel/userdetail | 7 | travelUser 4명 × 개별 user 조회 + 프로필 이미지 |
| GET /payment (200건) | 10 | 결제 1 + category 4 + travelUser 4 lazy 로딩 |
| GET /payment/detail | 10 | 정산 → 정산유저 → travelUser → user 다층 lazy |
| GET /record (5건) | 20 | 기록 1 + 기록별 이미지목록 5 + 이미지 lazy 10 + travelUser |

**중요**: 시드가 유저 4명·카테고리 4종뿐이라 세션 1차 캐시가 N+1을 흡수해 쿼리 수 상한이 낮게 나옴.
실서비스처럼 항목마다 다른 연관 엔티티면 쿼리 수는 목록 크기에 비례해 증가함
(예: record 날짜별 5건 → 20쿼리, 즉 건당 4쿼리 꼴).

## 인덱스 현황

`pg_indexes` 확인 결과 **모든 테이블에 PK 인덱스만 존재**. `payments(travel_id)`, `travelrecords(travel_id)` 등
FK/조회조건 인덱스 전무 → 현재 200건에선 seq scan도 0.06ms지만 데이터 증가 시 선형 악화.

## 해석

- 로컬은 DB 왕복이 ~0.5ms라 N+1 영향이 절대치로는 작게 보임. 운영(EC2↔DB, 왕복 수 ms)에서는
  쿼리 수가 곧 지연시간이므로 **요청당 쿼리 수가 핵심 지표**.
- 가장 무거운 패턴: record 조회(건당 4쿼리), payment 목록/상세(카디널리티 늘면 2N+1).
- JWT 필터가 모든 요청에 DB 세션 + 쿼리 1개를 고정으로 추가.
