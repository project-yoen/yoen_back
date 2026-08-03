# 개선 후 재측정 결과 (2026-08-01)

측정 조건은 `BASELINE.md`와 동일: 같은 시드 데이터, 같은 시나리오(k6 10 VU × 2분), 로컬 환경.
개선 후 총 50,502 요청, 실패 0.02%(15건 — 앱 로그에 에러 없음, Docker k6→host 네트워크 일시 오류로 판단).

## 응답시간 비교 (p95 / avg)

| 엔드포인트 | 개선 전 p95 | 개선 후 p95 | 개선 전 avg | 개선 후 avg | p95 변화 |
| --- | ---: | ---: | ---: | ---: | ---: |
| GET /travel | 11.9ms | 11.6ms | 9.2ms | 8.2ms | -3% |
| GET /travel/userdetail | 22.4ms | 16.9ms | 18.1ms | 12.6ms | **-25%** |
| GET /payment (전체 200건) | 35.1ms | 28.2ms | 28.6ms | 20.6ms | **-20%** |
| GET /payment (날짜별) | 23.4ms | 19.4ms | 18.7ms | 14.4ms | **-17%** |
| GET /payment/detail | 27.5ms | 24.4ms | 22.3ms | 18.5ms | **-11%** |
| GET /record (날짜별) | 44.0ms | 20.9ms | 35.6ms | 15.7ms | **-52%** |

## 요청당 JDBC statement 수 비교

| 엔드포인트 | 개선 전 | 개선 후 | 비고 |
| --- | ---: | ---: | --- |
| JWT 필터 유저 조회 | 매 요청 1 | 캐시 히트 시 0 | Redis 캐시 (TTL 10분) |
| GET /travel/userdetail | 7 | 3 | fetch join |
| GET /payment (200건) | 10 | 3 | fetch join |
| GET /payment/detail | 10 | 6 | paymentId 기준 일괄 조회 |
| GET /record (날짜별 5건) | 20 | 4 | 이미지 일괄 조회 |

**핵심**: 개선 전엔 쿼리 수가 목록 크기·연관 엔티티 다양성에 비례해 증가했지만(시드가 유저 4명이라 낮게 측정된 것),
개선 후엔 **목록 크기와 무관하게 고정**. 운영(DB 왕복 수 ms)에서는 로컬보다 격차가 훨씬 커짐.

## 적용된 개선 (커밋 단위)

1. N+1 제거: `@EntityGraph`/`JOIN FETCH`, EAGER 이미지→LAZY, `default_batch_fetch_size=100`
2. JWT 필터 유저 조회 Redis 캐싱 (변경 시 무효화 포함)
3. 조회 패턴 기반 인덱스 9개 (`payments(travel_id,type,pay_time)` 등 — 기존엔 PK 인덱스뿐)
4. GCS 업로드를 트랜잭션 첫 DB 접근 앞으로 이동(커넥션 미점유) + 대표이미지 HTTP 재다운로드를 GCS 서버사이드 복사로 교체

이미지 업로드 경로(4번)는 k6 시나리오에 없어 수치엔 안 잡히지만, 기록 생성 시 이미지 1장 기준
"업로드 2왕복 + 다운로드 1왕복 + 재업로드 2왕복" → "업로드 2왕복 + 복사 2메타데이터 호출(바이트 전송 없음)"로 감소.

## 보류한 항목과 이유

- **OSIV(open-in-view=false)**: `AuthService.checkTravelUserRoleByPayment`의 `pm.getTravel()` 등
  비트랜잭션 서비스 메서드의 lazy 접근이 많아, 전 서비스에 `@Transactional(readOnly=true)` 감사 없이 끄면
  LazyInitializationException 위험. 단위 테스트(mock)로는 검출 불가 → 별도 작업으로 분리 권장.
- **Hikari 풀 조정**: 10 VU 부하에서 `hikaricp_connections_pending` 0 → 현재 기본값(10)으로 충분.
  운영에서 Grafana의 HikariCP 패널(pending/acquire time)에 병목이 보일 때 조정.
- **페이지네이션**: 프론트 API 스펙 변경이 필요해 별도 논의 필요.
- **미사용 MyBatis 의존성 제거**: 성능 무관 정리 항목 (부팅 로그에 mapper 없음 경고 뜨는 중).

## 검증

- `./gradlew test` 전체 통과 (197 tests)
- 동일 시드·시나리오 재측정으로 전/후 비교
- 앱 로그 에러 0건 (부하 중)
