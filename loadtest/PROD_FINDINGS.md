# 운영(EC2) 응답 지연 원인 분석 (2026-08-01)

체감상 "EC2에 보내면 훨씬 느리다"의 원인을 경로 단계별로 분해 측정한 결과.

> **[해결됨]** 같은 날 Cloudflare 프록시를 DNS only로 변경 후 재측정:
> `/actuator/health` TTFB **600~940ms → 37~56ms**, `/user/exists` **~600ms → ~45ms** (약 15배 개선).
> TLS 핸드셰이크 포함 전체 연결 수립도 ~30ms로 정상화. 아래는 변경 전 분석 기록.

## 측정 결과 (`/actuator/health` TTFB 기준, 반복 샘플)

| 경로 | TTFB | 비고 |
| --- | ---: | --- |
| EC2 내부에서 nginx 경유 | **~10ms** | 서버 자체는 빠름 (warm 기준) |
| 내 PC → EC2 직접 (Cloudflare 우회) | **~32-49ms** | TCP RTT 8ms — 서울 리전다운 속도 |
| 내 PC → api-yoen.com (Cloudflare 경유, 실사용 경로) | **~600-900ms** (첫 요청 ~2.1s) | TCP RTT만 135-190ms |

`/user/exists`(DB 조회 포함)도 동일 패턴: 내부 ~10-14ms vs Cloudflare 경유 ~600ms.

## 결론: 병목은 앱도 서버도 아니고 **Cloudflare 프록시 라우팅**

- `api-yoen.com`은 Cloudflare 프록시(주황 구름)를 통과함 (DNS가 104.21.x/172.67.x = CF IP).
- 한국 주요 ISP는 Cloudflare 무료 플랜 트래픽을 서울 PoP이 아닌 **미국 서부 PoP으로 라우팅**하는 것으로 잘 알려져 있음 (피어링 비용 문제).
- 따라서 실제 경로가 `한국 → 미국 CF PoP → 서울 EC2 → 미국 CF PoP → 한국`이 되어
  요청당 **+550~850ms**가 순수 네트워크 우회 비용으로 추가됨.
- 직접 연결 시 34ms인 것이 증거: `curl --resolve api-yoen.com:443:43.200.189.31 https://api-yoen.com/actuator/health`

## 권장 조치 (효과 큰 순)

1. **Cloudflare 프록시 끄기**: CF 대시보드에서 api-yoen.com A 레코드를 주황 구름 → 회색 구름(DNS only)으로 변경.
   - origin의 nginx가 이미 유효한 Let's Encrypt 인증서(`CN=api-yoen.com`, 2026-09-16 만료)를 갖고 있어 즉시 동작.
   - 예상 효과: 실사용 응답시간 600-900ms → **~35-50ms** (약 15배)
   - 트레이드오프: origin IP 노출(CF의 DDoS 방어·IP 은닉 상실). 취미 서비스 수준에선 보안그룹에서 80/443/22만 열고
     `/actuator` nginx 차단(이미 되어 있음) 유지하면 일반적으로 수용 가능.
   - 대안: CF를 유지하고 싶으면 유료 플랜(한국 PoP 라우팅 개선) 검토.

2. **(부차) 스왑 상주 메모리**: RAM 911MB 중 스왑에 618MB 상주. 지금은 warm이라 빨랐지만,
   트래픽이 뜸한 뒤 첫 요청은 page-in으로 수백 ms 지연될 수 있음.
   - 앱 컨테이너에 `-XX:MaxRAMPercentage` 등 힙 상한 명시, 또는 인스턴스 업그레이드(2GB) 검토.
   - Grafana Cloud의 `node_memory_*` / swap 메트릭으로 콜드 지연과 스왑 활동의 상관 관찰 가능.

3. **앱 레벨 개선 배포**: `perf/api-latency` 브랜치의 N+1 제거·캐싱은 DB 왕복 수를 줄이므로
   네트워크 문제 해결 후 남는 서버 처리 시간을 더 줄여줌 (내부 10ms 중 일부).

## 참고

- CPU는 여유 (idle 93-99%, steal 0) — 크레딧 고갈 아님.
- 인증 API의 운영 측정은 생략: 병목이 모든 요청에 동일하게 걸리는 네트워크 비용임이 확인되어
  운영 DB에 테스트 데이터를 넣을 실익이 없음. 앱 레벨 차이는 로컬 BASELINE/RESULTS로 이미 측정됨.
- 현재 배포본은 개선 전 코드. 브랜치 배포 후 같은 방법으로 재측정하면 운영 전/후 비교 가능.
