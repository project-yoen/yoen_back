# YOEN 백엔드 성능 개선 리포트

- **작성일**: 2026-08-01
- **브랜치**: `perf/api-latency`
- **대상**: YOEN 백엔드 (Spring Boot 3.4 / JPA / PostgreSQL / Redis / Firebase Storage, EC2 1GB + Cloudflare)

---

## 1. 요약 (TL;DR)

"API 응답이 느리다"는 체감 문제를 **측정 → 원인 분석 → 개선 → 재측정**의 순서로 해결했다.
원인은 하나가 아니라 **애플리케이션 레벨 4가지 + 인프라 레벨 1가지**가 겹쳐 있었다.

| # | 문제 | 개선 결과 |
|---|---|---|
| 1 | N+1 쿼리 + 불필요한 EAGER 로딩 | 요청당 쿼리 수 최대 20개 → 4개 (목록 크기 비례 → **고정**) |
| 2 | JWT 인증 필터가 매 요청 DB 조회 | 캐시 히트 시 쿼리 **0개** |
| 3 | DB 인덱스 전무 (PK만 존재) | 조회 패턴 기반 인덱스 9개 추가 (데이터 성장 대비) |
| 4 | 트랜잭션 안에서 느린 외부 I/O(이미지 업로드) | 커넥션 점유 제거 + 불필요한 재다운로드 왕복 삭제 |
| 5 | **(최대 원인)** Cloudflare가 한국 트래픽을 미국으로 라우팅 | 운영 응답 **600~900ms → 40~50ms (약 15배)** |

로컬 부하 테스트(k6, 10 VU × 2분) 기준 엔드포인트별 p95는 최대 **52% 개선**
(여행기록 조회 44ms → 21ms), 운영 환경은 인프라 수정만으로 **15배** 개선.

---

## 2. 접근 방법: 추측하지 말고 측정하기

성능 문제에서 가장 흔한 실수는 "느린 것 같은 부분"을 감으로 고치는 것이다.
이번 작업은 코드를 고치기 전에 **측정 인프라부터** 만들었다.

### 2.1 사용한 측정 도구

| 도구 | 무엇을 재는가 | 왜 필요한가 |
|---|---|---|
| **k6** (부하 테스트) | 엔드포인트별 응답시간 분포(avg/p95/p99) | "느리다"를 숫자로 바꿔줌. 개선 전/후를 같은 조건으로 비교 |
| **Hibernate 세션 통계** | 요청당 실행된 SQL 개수 | N+1을 정량적으로 확인. `HIBERNATE_STATS=true`로 활성화 |
| **p6spy** | 실행된 SQL 원문 + 개별 실행시간 | 어떤 쿼리가 느린지 확인 (`developmentOnly` 의존성 → 운영 jar에 미포함) |
| **Prometheus + Grafana** | URI별 p95/p99, HikariCP 커넥션 상태 | 지속적 관찰. `YOEN API Latency` 대시보드 추가 |
| **curl 타이밍 분해** | DNS/TCP/TLS/TTFB 단계별 시간 | 운영 지연이 네트워크 문제인지 서버 문제인지 분리 |

> **p95란?** 요청 100개 중 95번째로 느린 요청의 응답시간. 평균은 소수의 느린 요청을
> 가려버리기 때문에, 사용자 체감을 대표하는 지표로 p95/p99를 쓴다.

### 2.2 측정 환경

- 로컬: Windows + Docker(postgres 16, redis 7) + `bootRun`, k6는 Docker 이미지로 실행
- 시드 데이터: 여행 1개, 유저 4명, 결제 200건(정산 200 + 정산유저 800), 기록 50건(이미지 100장)
  — **N+1은 데이터가 있어야 드러나므로** 시드가 필수다 (`loadtest/seed.sql`)
- 시나리오: 로그인 → 여행 목록 → 참여자 상세 → 결제 목록(전체/날짜별) → 결제 상세 → 기록 목록
- 재현 방법은 `loadtest/README.md` 참고

---

## 3. 개선 1: N+1 쿼리 제거

### 3.1 문제와 원인 — N+1이란 무엇인가

JPA에서 연관 엔티티(`@ManyToOne` 등)는 기본적으로 **지연 로딩(LAZY)** 이 권장된다.
지연 로딩이란 "연관 객체를 실제로 사용하는 순간에 DB에서 조회"하는 방식이다.

문제는 **목록을 조회한 뒤 반복문에서 연관 객체에 접근**할 때 생긴다:

```java
// PaymentService — 개선 전
List<Payment> pmList = paymentRepository.findAllByTravel...(...);  // 쿼리 1번
return pmList.stream().map(payment ->
    new PaymentSimpleResponseDto(
        payment.getCategory().getCategoryName(),      // ← 결제마다 카테고리 조회 쿼리
        payment.getTravelUser().getTravelNickname(),  // ← 결제마다 참여자 조회 쿼리
        ...
    )).toList();
```

결제가 N건이면 `목록 1번 + 카테고리 N번 + 참여자 N번 = 2N+1번` 쿼리가 나간다.
이것이 **N+1 문제**다. 쿼리 하나는 빨라도(수 ms) DB 왕복은 횟수만큼 누적되고,
특히 운영처럼 앱과 DB 사이 네트워크가 있는 환경에서는 왕복당 수 ms가 곱해진다.

**발견된 위치** (모든 목록 조회가 같은 패턴이었다):

- `PaymentService.getPaymentByTravelIdAndDate()` 등 결제 목록 4개 메서드
- `PaymentService.getDetailPayment()` — 정산마다 정산유저 목록 쿼리, 정산유저마다 참여자 조회 (**다층 N+1**)
- `RecordService.getTravelRecordsByDate()` — 기록마다 이미지 목록 쿼리 + 이미지마다 lazy 로딩
- `TravelService.getDetailTravelUser()` — 참여자마다 `userRepository.getReferenceById()` 개별 호출

**추가 문제: EAGER 즉시 로딩.** `Travel.travelImage`, `User.profileImage`가
`FetchType.EAGER`(즉시 로딩)로 되어 있었다. EAGER는 "이 엔티티를 조회하면 연관 엔티티도
무조건 함께 조회"라는 뜻이라, 이미지가 필요 없는 조회(예: 인증 필터의 유저 조회)에서도
매번 이미지 조인이 따라붙었다.

### 3.2 해결 방법

**① fetch join / `@EntityGraph`** — "목록과 연관 엔티티를 한 방 쿼리로 함께 가져와라"라고
JPA에 명시하는 방법. JOIN으로 한 번에 가져오므로 반복문에서 접근해도 추가 쿼리가 없다.

```java
// PaymentRepository — 개선 후
@EntityGraph(attributePaths = {"category", "travelUser"})
List<Payment> findAllByTravelAndTypeAndPayTimeBetweenAndIsActiveTrue(...);
```

**② IN 절 일괄 조회** — 다층 구조(결제→정산→정산유저)는 fetch join이 복잡해지므로,
자식 전체를 한 번에 가져와서 메모리에서 그룹핑하는 방식으로 바꿨다:

```java
// 정산별로 N번 조회하던 것을 → paymentId 기준 1번 조회 후 Map으로 그룹핑
List<SettlementUser> all = settlementUserRepository.findAllWithTravelUserByPaymentId(paymentId);
Map<Long, List<...>> bySettlementId = all.stream().collect(groupingBy(...));
```

**③ EAGER → LAZY 전환** — 이미지는 필요한 조회에서만 fetch join으로 가져오도록 변경.

**④ 전역 안전망: `default_batch_fetch_size=100`** — 놓친 lazy 로딩이 있어도
1건씩이 아니라 `WHERE id IN (?, ?, ... 100개)` 형태로 묶어서 조회하게 하는 설정.
N+1이 발생해도 N/100+1로 완화된다.

### 3.3 개선 결과 (Hibernate 세션 통계로 요청당 SQL 수 측정)

| 엔드포인트 | 개선 전 | 개선 후 |
|---|---:|---:|
| GET /travel/userdetail (참여자 상세) | 7 | **3** |
| GET /payment (결제 200건) | 10 | **3** |
| GET /payment/detail (결제 상세) | 10 | **6** |
| GET /record (날짜별 기록 5건) | 20 | **4** |

응답시간(k6 p95): 기록 조회 44.0ms → **20.9ms (-52%)**, 결제 전체 목록 35.1ms → **28.2ms (-20%)**

> **중요한 교훈 — 로컬 수치의 함정.** 시드 유저가 4명뿐이라 Hibernate의 1차 캐시(같은
> 세션에서 이미 조회한 엔티티는 재조회하지 않음)가 N+1을 흡수해서, 개선 전 수치도 실제보다
> 작게 측정됐다. 실서비스처럼 항목마다 연관 엔티티가 다르면 쿼리 수는 목록 크기에 비례해
> 커진다. **개선의 본질은 "쿼리 수가 목록 크기와 무관하게 고정"된 것**이다.

---

## 4. 개선 2: JWT 인증 필터의 요청당 DB 조회 제거

### 4.1 문제와 원인

토큰 인증 방식에서는 매 요청의 `Authorization` 헤더에 담긴 JWT를 필터가 검증한다.
기존 코드는 토큰 검증 후 **매 요청마다 DB에서 유저를 조회**했다:

```java
// JwtAuthenticationFilter — 개선 전
String userId = jwtProvider.getUserIdFromToken(token);
User user = userService.findById(Long.parseLong(userId));  // ← 모든 API 요청마다 DB 조회
```

로그인한 사용자의 **모든 요청에 DB 왕복 1회가 고정으로 추가**되는 구조다.
게다가 개선 전에는 `User.profileImage`가 EAGER라 이미지 조인까지 함께 실행됐다.

### 4.2 해결 방법 — Redis 캐싱 (그리고 조심해야 했던 함정)

이미 리프레시 토큰 저장용으로 쓰던 Redis에 유저 요약 정보를 캐싱했다:

- 캐시 키 `user_cache:{userId}`, TTL 10분
- 캐시 미스 → DB 조회 후 캐시에 적재, 캐시 히트 → DB 조회 생략
- 유저 정보가 **바뀌는 곳**(프로필 수정, 프로필 이미지 변경)에서 캐시 **무효화(evict)**
- Redis 장애 시에도 요청이 죽지 않도록 캐시 실패는 경고 로그만 남기고 DB 조회로 폴백

**함정 1 — 캐시에 뭘 담을 것인가.** User 엔티티를 통째로 캐싱하면 비밀번호 해시가 Redis에
저장되고(보안 문제), 캐시에서 복원한 객체를 실수로 `save()`하면 오래된 값으로 DB를
덮어쓸 수 있다. 그래서 **비밀번호/이미지를 뺀 읽기 전용 스냅샷(`CachedUser` record)** 만 담았다.

**함정 2 — detached 엔티티 저장 금지.** 캐시에서 복원한 User는 JPA가 관리하지 않는
detached 상태다. 이걸 `userRepository.save()`하면 캐시에 없던 필드(비밀번호 등)가
null로 덮어써질 수 있다. 그래서 유저를 **수정하는** 두 메서드(`updateUser`,
`saveProfileUrl`)는 반드시 DB에서 관리 엔티티를 다시 조회한 뒤 수정하도록 바꿨다.

### 4.3 개선 결과

같은 토큰으로 연속 호출 시 Hibernate 세션 통계:

```
1번째 호출: 유저 조회 세션(1 SQL) + 본 처리 세션(3 SQL)   ← 캐시 미스
2번째 호출: 본 처리 세션(3 SQL)만 실행                    ← 캐시 히트, 유저 DB 조회 소멸
```

전체 API의 기본 비용이 DB 왕복 1회씩 줄었다. 참여자 상세 p95 22.4ms → 16.9ms(-25%) 등
전 엔드포인트에 공통으로 기여.

---

## 5. 개선 3: DB 인덱스 추가

### 5.1 문제와 원인 — 인덱스가 하나도 없었다

인덱스는 책의 색인과 같다. 색인이 없으면 원하는 내용을 찾기 위해 책 전체를 넘겨봐야
하고(풀 스캔), DB도 마찬가지로 테이블 전체를 읽는다.

`pg_indexes`로 확인한 결과 **모든 테이블에 PK(기본키) 인덱스만 존재**했다. 두 가지 오해가
겹친 결과로 보인다:

1. **PostgreSQL은 외래키(FK)에 인덱스를 자동으로 만들지 않는다.** (MySQL InnoDB는 만들어줌)
2. `ddl-auto=update`는 엔티티에 선언된 것만 반영한다 — `@Index` 선언이 없었으므로 아무것도 안 만듦.

즉 `WHERE travel_id = ?` 같은 대표적인 조회 조건이 전부 풀 스캔 대상이었다.

### 5.2 해결 방법 — 조회 패턴 기반으로 선언

실제 쿼리의 WHERE 절을 분석해 엔티티에 `@Index`를 선언했다 (`ddl-auto=update`가 기동 시 생성):

| 테이블 | 인덱스 | 근거가 된 조회 |
|---|---|---|
| payments | (travel_id, type, pay_time) | 여행별+타입별+날짜범위 결제 목록 |
| travelrecords | (travel_id, record_time) | 여행별+날짜범위 기록 목록 |
| travelusers | (travel_id, user_id), (user_id) | 여행별 참여자 / 유저의 여행 목록 |
| settlements | (payment_id) | 결제별 정산 |
| settlementusers | (settlement_id) | 정산별 정산유저 |
| travelrecordimages / paymentimages | (travelrecord_id) / (payment_id) | 기록/결제별 이미지 |
| exchangerates | (created_at) | 최신 환율 top-1 조회 |

복합 인덱스는 **컬럼 순서가 중요**하다. `(travel_id, type, pay_time)`은
"travel_id 동일 조건 → type 동일 조건 → pay_time 범위 조건" 순으로 좁혀가는 쿼리 패턴과 일치시킨 것.

### 5.3 개선 결과 — 지금은 미미, 미래를 위한 것

로컬 200행에서는 `EXPLAIN ANALYZE` 결과 플래너가 여전히 풀 스캔을 선택했다(0.06ms).
**이건 정상이다** — 테이블이 작으면 인덱스를 타는 것보다 전체를 읽는 게 더 싸다고 DB가
판단한다. 인덱스의 가치는 데이터가 수만~수십만 행으로 커졌을 때 조회 시간이 선형으로
나빠지는 것을 막는 데 있다. `SET enable_seqscan=off`로 인덱스가 사용 가능함은 검증했다.

---

## 6. 개선 4: 트랜잭션 안의 느린 외부 I/O 분리

### 6.1 문제와 원인 — 커넥션 풀과 트랜잭션의 관계

DB 커넥션은 비싸서 미리 만들어두고 돌려쓴다(커넥션 풀, HikariCP 기본 **10개**).
`@Transactional` 메서드는 트랜잭션 동안 커넥션 1개를 점유하는데, 그 안에서 느린 작업을
하면 **커넥션을 쥔 채로 기다리는 시간**이 생긴다. 동시에 요청 10개가 이러면 풀이
고갈되어, 11번째 요청부터는 커넥션을 기다리느라 전체 API가 밀린다.

발견된 문제 (여행기록 생성 `createTravelRecord`, `@Transactional`):

1. **이미지 업로드가 트랜잭션 안에서 실행** — 이미지 1장당 GCS 왕복 2회(업로드 + 다운로드
   토큰 메타데이터 설정), 여러 장이면 순차 실행. 그동안 DB 커넥션 점유.
2. **더 심각한 것: 방금 올린 이미지를 HTTP로 다시 다운로드해서 재업로드.** 여행 대표
   이미지를 설정하는 `saveImageByUrl()`이 최대 30초 타임아웃의 블로킹 다운로드 +
   재업로드 2왕복을 트랜잭션 안에서 수행했다. 이미지 1장 생성에 **외부 왕복 5회**.

### 6.2 해결 방법

**① 업로드를 첫 DB 접근 앞으로 이동.** Hibernate는 트랜잭션이 시작돼도 실제 첫 SQL을
실행할 때까지 커넥션을 잡지 않는다(**지연 획득**). 이를 이용해 업로드를 메서드 맨 앞
(DB 접근 전)으로 옮기면, 느린 업로드 동안 커넥션을 점유하지 않는다.

```java
@Transactional
public TravelRecordResponseDto createTravelRecord(...) {
    // 느린 GCS 업로드를 첫 DB 접근 전에 수행 → 업로드 동안 DB 커넥션 미점유
    List<Image> images = (files != null && !files.isEmpty()) ? imageService.saveImages(user, files) : List.of();
    Travel tv = travelRepository.getReferenceById(dto.travelId());  // 여기서부터 커넥션 사용
    ...
}
```

**② 재다운로드 제거 → GCS 서버사이드 복사.** 스토리지 안에서 객체를 복사하는
`Blob.copyTo()`를 쓰면 **바이트가 앱 서버를 거치지 않는다**. 다운로드+재업로드(파일
크기에 비례하는 시간)가 메타데이터 호출 2번(수십 ms)으로 바뀐다.

### 6.3 개선 결과

이미지 1장 포함 기록 생성 기준, 요청 스레드가 감당하는 외부 I/O:

| | 개선 전 | 개선 후 |
|---|---|---|
| 외부 왕복 | 업로드 2 + 다운로드 1 + 재업로드 2 = **5회** (파일 크기 비례) | 업로드 2 + 복사 2 = **4회** (복사는 바이트 전송 없음) |
| DB 커넥션 점유 중 I/O | 전부 | **없음** |

이 경로는 k6 시나리오에 없어 수치화되지 않았지만, 동시 사용자가 늘었을 때
풀 고갈로 인한 전면 지연을 예방하는 구조적 개선이다.

---

## 7. 개선 5 (인프라, 최대 효과): Cloudflare 라우팅 문제

### 7.1 문제 — "EC2에 보내면 훨씬 느리다"

로컬 개선 후에도 운영 서버(api-yoen.com)는 훨씬 느렸다. 이때 서버 탓으로 추측하지 않고
**경로를 단계별로 분해해서 측정**한 것이 핵심이었다.

> **측정 개념.** `curl -w`로 요청의 각 단계 시간을 잴 수 있다:
> DNS 조회 → TCP 연결(이 시간 ≈ 네트워크 왕복시간 RTT) → TLS 핸드셰이크 →
> **TTFB**(Time To First Byte: 첫 응답 바이트까지 — 네트워크 + 서버 처리 포함).
> "TTFB − 네트워크 시간 = 서버 처리 시간"으로 분리할 수 있다.

같은 `/actuator/health`(아무 일도 안 하는 엔드포인트)를 세 경로로 측정:

| 경로 | TTFB | 의미 |
|---|---:|---|
| EC2 서버 내부에서 nginx 경유 | **~10ms** | 서버 자체는 빠르다 |
| 내 PC → EC2 IP 직접 (Cloudflare 우회) | **~35ms** | 한국↔서울 네트워크도 빠르다 (RTT 8ms) |
| 내 PC → api-yoen.com (실사용 경로) | **600~900ms** | ← 범인은 이 구간 |

DB 조회가 있는 `/user/exists`도 동일 패턴 → **지연은 쿼리 문제가 아니라 모든 요청에
똑같이 붙는 경로 비용**이라는 결론.

### 7.2 원인 — Cloudflare 무료 플랜의 한국 트래픽 라우팅

`api-yoen.com`의 DNS가 Cloudflare IP(104.21.x, 172.67.x)를 반환하고 있었다. 즉 도메인이
Cloudflare **프록시(주황 구름)** 를 통과 중이었다. 문제는 한국 주요 ISP가 Cloudflare
무료 플랜 트래픽을 서울 PoP이 아닌 **미국 서부 PoP으로 라우팅**하는 것으로 악명 높다는
점(피어링 비용 문제). 실제 요청 경로:

```
한국 사용자 → 미국 Cloudflare PoP → 서울 EC2 → 미국 PoP → 한국 사용자
```

태평양을 두 번 왕복하며 요청마다 +550~850ms. Cloudflare 엣지까지의 RTT만 135~190ms였다
(서울 PoP이면 5~20ms여야 정상).

### 7.3 해결 — 프록시 해제 (DNS only)

Cloudflare 대시보드에서 A 레코드를 주황 구름 → **회색 구름(DNS only)** 으로 변경.
사전 확인 사항: origin의 nginx가 이미 **유효한 Let's Encrypt 인증서**를 갖고 있어
프록시를 꺼도 HTTPS가 즉시 정상 동작함을 확인했다 (Cloudflare 전용 origin 인증서였다면
브라우저 오류가 났을 것).

**트레이드오프**: origin IP가 노출되어 Cloudflare의 DDoS 방어·IP 은닉을 잃는다.
보완: 보안그룹 인바운드를 80/443(+22는 내 IP만)으로 제한, `/actuator` nginx 차단 유지(기존 완료).

### 7.4 개선 결과

| 엔드포인트 | 변경 전 | 변경 후 |
|---|---:|---:|
| /actuator/health | 600~940ms (첫 요청 2.1s) | **37~56ms** |
| /user/exists (DB 조회) | ~600ms | **42~48ms** |

**요청당 약 15배 개선.** 앱 코드는 한 줄도 안 바꾸고 얻은 결과로, 이번 작업에서 가장
효과가 컸다. "느리면 코드부터 의심하지 말고 경로를 분해해서 측정하라"의 실례.

### 7.5 부차 발견: EC2 메모리 스왑

서버 점검 중 발견: RAM 911MB 중 **618MB가 스왑에 상주**(앱 JVM + postgres + redis +
nginx + alloy가 1GB에 동거). 지금은 warm 상태라 빨랐지만, 트래픽이 뜸한 뒤 첫 요청은
스왑된 메모리를 디스크에서 다시 읽느라(page-in) 수백 ms 느려질 수 있다 — 첫 측정에서
2.1초가 나온 유력한 이유. CPU는 여유(idle 93%+, steal 0)로 크레딧 고갈은 아니었다.

**권장**: 앱 컨테이너에 힙 상한 명시(`-XX:MaxRAMPercentage`), 장기적으로 2GB 인스턴스 검토.

---

## 8. 이번에 하지 않은 것과 그 이유

성능 작업은 "할 수 있는 것"이 아니라 "측정이 정당화하는 것"만 해야 한다. 아래는 의도적 보류.

| 항목 | 보류 이유 |
|---|---|
| **OSIV 끄기** (`open-in-view=false`) | OSIV는 HTTP 응답이 끝날 때까지 DB 세션을 열어두는 Spring 기본 설정. 끄면 커넥션을 일찍 반납해 좋지만, 현재 코드에 **트랜잭션 밖에서 lazy 로딩하는 서비스 메서드가 다수** 있어(예: `AuthService.checkTravelUserRoleByPayment`의 `pm.getTravel()`) 그냥 끄면 `LazyInitializationException`으로 런타임 장애가 난다. Mock 단위 테스트로는 검출 불가 → 전 서비스 `@Transactional(readOnly=true)` 감사와 통합 테스트를 갖춘 별도 작업으로 분리 |
| **Hikari 풀 확대** | 부하 테스트에서 커넥션 대기(`pending`) 0 → 현재 기본값(10)으로 충분. 근거 없는 튜닝은 하지 않음 |
| **페이지네이션** | 결제/기록 목록이 무제한 반환이라 데이터 성장 시 필요하지만, **프론트 API 스펙 변경**이 필요해 별도 논의 대상 |
| **MyBatis 의존성 제거** | 매퍼가 하나도 없는 미사용 의존성(기동 로그에 경고 출력 중). 성능 무관 정리 항목 |

---

## 9. 전체 결과 종합

### 로컬 (k6 10 VU × 2분, 동일 시드·시나리오 전/후 비교)

| 엔드포인트 | p95 전 → 후 | 요청당 SQL 전 → 후 |
|---|---|---|
| GET /travel | 11.9 → 11.6ms | 1 → 1 (+JWT 캐시 효과) |
| GET /travel/userdetail | 22.4 → **16.9ms (-25%)** | 7 → 3 |
| GET /payment (200건) | 35.1 → **28.2ms (-20%)** | 10 → 3 |
| GET /payment (날짜별) | 23.4 → **19.4ms (-17%)** | — |
| GET /payment/detail | 27.5 → **24.4ms (-11%)** | 10 → 6 |
| GET /record (날짜별) | 44.0 → **20.9ms (-52%)** | 20 → 4 |

- 총 5만+ 요청, 실패율 0% 수준, 전체 단위 테스트 197건 통과
- 모든 요청에서 JWT 필터 DB 조회(세션 1개 + SQL 1개) 추가 소멸

### 운영 (api-yoen.com)

- 응답시간 **600~900ms → 40~50ms (약 15배)** — Cloudflare 프록시 해제
- 현재 운영 배포본은 개선 전 코드이므로, `perf/api-latency` 배포 시 앱 레벨 개선이 추가로 얹힘

---

## 10. 남긴 자산 (재사용 방법)

| 자산 | 위치 | 용도 |
|---|---|---|
| k6 시나리오 + 시드 | `loadtest/` (`README.md`에 실행법) | 성능 회귀 확인, 배포 전/후 비교 |
| 측정 기록 | `loadtest/BASELINE.md`, `RESULTS.md`, `PROD_FINDINGS.md` | 이번 작업의 원본 데이터 |
| Grafana 대시보드 | `monitoring/grafana/dashboards/yoen-api-latency.json` | URI별 p95/p99, HikariCP 상시 관찰 |
| SQL 가시성 | `HIBERNATE_STATS=true` 환경변수 + p6spy(bootRun 전용) | N+1 재발 확인 |

### 커밋 이력 (`perf/api-latency`)

```
64d0c16 측정 인프라 추가: API 지연 대시보드, SQL 로깅, k6 부하테스트 + 베이스라인
7ce1f03 N+1 쿼리 제거: fetch join/EntityGraph 적용, 이미지 EAGER→LAZY 전환
5dbf735 JWT 필터 유저 조회 Redis 캐싱 (요청당 DB 조회 제거)
6154e2c 조회 패턴 기반 인덱스 추가 (@Index, ddl-auto=update로 생성)
1ea4fe3 GCS I/O를 트랜잭션 DB 접근과 분리, 대표이미지 재다운로드 제거
4ff5216 개선 후 재측정 결과 기록 (RESULTS.md)
ba2ad59 운영(EC2) 응답 지연 원인 분석 기록
43fac6f Cloudflare 프록시 해제 후 재측정 결과 반영 (600~900ms → 40~50ms)
```

---

## 11. 배운 것 정리 (초보 개발자를 위한 체크리스트)

1. **측정 없이 고치지 마라.** 개선 전 수치(베이스라인)가 없으면 "좋아졌다"를 증명할 수 없다.
2. **응답시간을 분해하라.** 네트워크(RTT/TLS)와 서버 처리(TTFB−네트워크)를 나눠 보면
   범인이 코드인지 인프라인지 바로 갈린다. 이번 최대 병목은 코드가 아니라 DNS 설정이었다.
3. **쿼리는 "몇 ms"보다 "몇 번"을 먼저 보라.** 목록 크기에 비례해 쿼리 수가 늘면 N+1이다.
   개선 목표는 "요청당 쿼리 수 고정".
4. **JPA 기본값을 알아야 한다.** EAGER의 전염성, OSIV 기본 on, PostgreSQL의 FK 인덱스
   비자동 생성 — 셋 다 몰라서 생긴 문제였다.
5. **트랜잭션 안에서 외부 API를 호출하지 마라.** DB 커넥션은 공유 자원이다.
6. **캐시는 무효화까지 설계해야 완성이다.** 그리고 캐시에서 복원한 엔티티를 절대 save()하지 마라.
7. **작은 테스트 데이터는 문제를 숨긴다.** 1차 캐시, 풀 스캔 선택 등 — 시드 데이터의
   크기와 다양성이 측정의 신뢰도를 결정한다.
