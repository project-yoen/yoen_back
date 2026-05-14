# Integration Test Plan

## Purpose
- 팀원이 나눠 맡은 통합 테스트 작업 중 내 담당 범위를 명확히 정리한다.
- PostgreSQL과 Redis를 각각 Testcontainers로 검증하되, 처음부터 두 저장소를 한 테스트에 섞지는 않는다.
- CI에서 `./gradlew test`만으로 유닛 테스트와 통합 테스트가 함께 안정적으로 실행되게 한다.

## Working Branch
- 현재 작업 브랜치: `jeonghoon`
- 이번 계획 문서와 이후 구현은 사용자가 별도로 바꾸지 않는 한 `jeonghoon` 브랜치에서 진행한다.

## Assigned Scope
- `PostgresIntegrationTestSupport`
  - PostgreSQL Testcontainers 공통 설정
  - 테스트 DB 이름은 `testdb`
- `RedisIntegrationTestSupport`
  - Redis Testcontainers 공통 설정
- `TravelRecordRepositoryIntegrationTest`
  - 여행 기록 Repository 쿼리 검증
  - 날짜 범위 조회 검증
- `TravelJoinCodeRedisDaoIntegrationTest`
  - 참여 코드 Redis 저장/조회/삭제/TTL 검증
- `JoinServiceIntegrationTest`
  - 참여 신청, 중복 신청 방지, 승인/거절 흐름 검증
  - PostgreSQL은 실제 Testcontainer 사용
  - Redis DAO는 mock 처리

## Explicitly Out Of Scope For This Round
- PostgreSQL + Redis를 둘 다 실제로 붙이는 전체 Join flow 테스트
- Firebase/GCS 연동 테스트
- Controller/API 레벨 통합 테스트
- 전체 `@SpringBootTest` context 복구
- GitHub Actions 배포/CD 구성
- 자동 merge 설정

## Testing Strategy
- 저장소별 통합 테스트를 먼저 분리한다.
- PostgreSQL 테스트는 JPA Repository와 DB 상태 변화에 집중한다.
- Redis 테스트는 Redis DAO와 key/value/TTL 동작에 집중한다.
- `JoinServiceIntegrationTest`는 서비스가 PostgreSQL 상태를 제대로 바꾸는지 검증한다.
- `JoinServiceIntegrationTest`에서 Redis는 참여 코드 -> travelId 변환만 mock으로 고정한다.

## Why Not Full Flow Yet
- PostgreSQL + Redis를 한 번에 붙이면 실패 원인이 넓어진다.
- 처음 통합 테스트를 학습하는 단계에서는 테스트 하나가 검증하는 외부 시스템을 선명하게 잡는 편이 좋다.
- 전체 flow 테스트는 나중에 happy path 1~2개만 추가하는 것이 적당하다.
- 이번 라운드에서는 전체 flow 테스트를 문서상 명시적으로 제외한다.

## Current Project Observations
- Gradle에는 PostgreSQL 드라이버가 있지만 Testcontainers 의존성이 아직 없다.
- Redis starter는 있지만 Redis Testcontainers 설정은 없다.
- `src/test/resources` 디렉터리와 테스트 전용 properties 파일이 아직 없다.
- 운영 설정은 `.env`에 의존한다.
- `YoenBackApplicationTests`의 전체 context 테스트는 현재 비활성화되어 있다.
- CI는 pull request 대상 `main`에서 `./gradlew test`를 실행한다.
- 기존 테스트는 Mockito 기반 유닛 테스트 중심이다.

## Professional Practice Notes
- 운영 DB와 같은 PostgreSQL에서 Repository 동작을 검증하는 것은 H2보다 실무에 가깝다.
- Redis DAO도 실제 Redis에서 TTL, key 삭제, 양방향 mapping을 검증하는 편이 좋다.
- Testcontainers 이미지는 `latest`보다 버전을 고정하는 편이 CI 재현성에 좋다.
- 통합 테스트는 느려질 수 있으므로 처음에는 저장소별 테스트와 핵심 서비스 테스트로 나눈다.
- 통합 테스트에서 발견되는 서비스 로직 버그는 테스트 기대값을 팀과 먼저 맞춘 뒤 수정하는 것이 좋다.

## Step 1. Gradle Testcontainers Dependencies

### Target File
- `build.gradle`

### Add Dependencies
- `org.testcontainers:junit-jupiter`
- `org.testcontainers:postgresql`

### Recommended Dependency Shape
```gradle
testImplementation 'org.testcontainers:junit-jupiter'
testImplementation 'org.testcontainers:postgresql'
```

### Redis Dependency Note
- Redis는 공식 전용 Testcontainers module 없이 `GenericContainer`로 충분하다.
- `GenericContainer`는 Testcontainers core에 포함되므로 `junit-jupiter` 의존성으로 사용할 수 있다.
- 필요하면 나중에 `com.redis:testcontainers-redis` 같은 별도 라이브러리를 검토할 수 있지만, 첫 단계에서는 표준 `GenericContainer`가 더 단순하다.

### Notes
- Spring Boot dependency management가 Testcontainers 버전을 관리할 수 있는지 확인한다.
- 버전 충돌이 있으면 Testcontainers BOM 도입을 검토한다.
- 현재 `./gradlew test` 하나로 전체 테스트를 돌리는 CI 구조는 유지한다.

## Step 2. Test Profile Properties

### Target File
- `src/test/resources/application-test.properties`

### Purpose
- 테스트가 로컬 `.env`에 의존하지 않게 만든다.
- Testcontainers가 주입하는 datasource/redis 설정을 우선 사용하게 한다.
- Hibernate가 테스트 컨테이너 안에 스키마를 만들고 테스트 종료 후 버리게 한다.

### Recommended Properties
```properties
spring.jpa.hibernate.ddl-auto=create
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false
spring.test.database.replace=none

spring.data.redis.repositories.enabled=false
spring.data.redis.host=localhost
spring.data.redis.port=6379

jwt.secret=test-jwt-secret-key-for-integration-tests

firebase.config.path=test-firebase.json
firebase.storage.bucket=test-bucket
```

### Notes
- PostgreSQL 테스트는 `@DataJpaTest` 중심으로 작성하면 Firebase 설정을 로드하지 않을 수 있다.
- Redis DAO 테스트는 전체 Spring Boot context보다 작은 테스트 context를 직접 구성하는 편이 안전하다.
- `@SpringBootTest`를 사용하면 FirebaseConfig가 초기화되며 실패할 수 있으므로 이번 범위에서는 피한다.

## Step 3. PostgresIntegrationTestSupport

### Target File
- `src/test/java/com/yoen/yoen_back/support/PostgresIntegrationTestSupport.java`

### Responsibility
- PostgreSQL Testcontainer를 한 곳에서 관리한다.
- Spring datasource property를 동적으로 등록한다.
- PostgreSQL 기반 통합 테스트 클래스들이 상속해서 같은 설정을 재사용하게 한다.

### Recommended Annotations
- `@Testcontainers`
- `@ActiveProfiles("test")`

### Recommended Container Settings
```java
@Container
static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test");
```

### Recommended Dynamic Properties
```java
@DynamicPropertySource
static void registerPostgresProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
}
```

### Notes
- Testcontainers는 Docker가 필요하다.
- CI의 GitHub-hosted Ubuntu runner는 보통 Docker 사용이 가능하다.
- 공통 support class에는 테스트 데이터 생성 헬퍼를 너무 많이 넣지 않는다.

## Step 4. RedisIntegrationTestSupport

### Target File
- `src/test/java/com/yoen/yoen_back/support/RedisIntegrationTestSupport.java`

### Responsibility
- Redis Testcontainer를 한 곳에서 관리한다.
- Redis host/port를 테스트 context에 제공한다.
- Redis DAO 통합 테스트가 실제 Redis에 붙게 한다.

### Recommended Container Settings
```java
@Container
static final GenericContainer<?> REDIS =
        new GenericContainer<>("redis:7-alpine")
                .withExposedPorts(6379);
```

### Recommended Dynamic Properties
```java
@DynamicPropertySource
static void registerRedisProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.host", REDIS::getHost);
    registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
}
```

### Important Bean Note
- 현재 `TravelJoinCodeRedisDao`는 `RedisTemplate<String, String>`을 주입받는다.
- 현재 `RedisConfig`는 `RedisTemplate<String, Object>` bean을 만든다.
- Spring의 generic bean matching에서 문제가 생길 수 있으므로 Redis DAO 통합 테스트에서는 테스트 전용 `RedisTemplate<String, String>` bean을 명시적으로 제공하는 방식을 우선 검토한다.

### Recommended Redis Test Context
- 전체 `@SpringBootTest` 대신 작은 context를 구성한다.
- 예시 방향:
  - `@ExtendWith(SpringExtension.class)`
  - `@ContextConfiguration(classes = {TravelJoinCodeRedisDao.class, RedisIntegrationTestSupport.RedisTestConfig.class})`
  - 테스트 전용 `RedisConnectionFactory`
  - 테스트 전용 `RedisTemplate<String, String>`

### Cleanup Strategy
- 각 테스트 전후로 Redis key를 삭제하거나 `flushDb()`를 호출한다.
- 테스트가 서로 같은 key를 공유하지 않도록 code/travelId 값을 테스트마다 다르게 둔다.

## Step 5. TravelRecordRepositoryIntegrationTest

### Target File
- `src/test/java/com/yoen/yoen_back/repository/travel/TravelRecordRepositoryIntegrationTest.java`

### Recommended Test Type
- `@DataJpaTest`
- `class TravelRecordRepositoryIntegrationTest extends PostgresIntegrationTestSupport`

### Required Repositories
- `TravelRecordRepository`
- `TravelRepository`
- `TravelUserRepository`
- `UserRepository`

### Required Test Data
- `User`
- `Travel`
- `TravelUser`
- 여러 개의 `TravelRecord`

### Test Data Setup Notes
- `User`는 `email`, `password`, `gender`, `birthday`가 필수다.
- `Travel`은 `travelName`, `numOfPeople`, `numOfJoinedPeople`가 필수다.
- `TravelRecord`는 `travel`, `travelUser`, `title`, `recordTime`이 필수다.
- `BaseEntity.isActive`는 기본값이 `true`지만, inactive 케이스는 명시적으로 `false`로 바꾼다.

### Planned Test Cases
- `findByTravelAndIsActiveTrue_existingActiveRecords_returnsOnlyActiveRecords`
  - 같은 여행의 active 기록만 조회되는지 검증한다.
  - inactive 기록은 제외되어야 한다.

- `findByTravel_TravelIdAndIsActiveTrue_existingActiveRecords_returnsOnlyMatchingTravelRecords`
  - `Travel` 객체가 아니라 `travelId` 기준으로 조회되는지 검증한다.
  - 다른 여행의 기록은 제외되어야 한다.

- `findByTravelRecordIdAndIsActiveTrue_activeRecord_returnsRecord`
  - active 기록 ID 조회가 성공하는지 검증한다.

- `findByTravelRecordIdAndIsActiveTrue_inactiveRecord_returnsEmpty`
  - inactive 기록은 ID가 맞아도 조회되지 않아야 한다.

- `findAllByTravelAndRecordTimeBetweenAndIsActiveTrue_recordsAcrossDates_returnsRecordsInsideRange`
  - 날짜 범위 안에 있는 active 기록만 조회되는지 검증한다.
  - 범위 밖 기록, 다른 여행 기록, inactive 기록은 제외되어야 한다.

- `findAllByTravelAndRecordTimeBetweenAndIsActiveTrue_recordAtBoundary_documentsCurrentBetweenBehavior`
  - `Between`의 경계 포함 여부를 문서화하는 테스트다.
  - Spring Data JPA `Between`은 일반적으로 시작/끝 경계를 모두 포함한다.
  - 현재 `RecordService.getTravelRecordsByDate()`는 `start`부터 `start.plusDays(1)`까지 조회하므로 다음 날 00:00 기록까지 포함될 수 있다.

### Important Risk To Discuss
- 하루 조회를 반열린 구간 `[start, nextDay)`으로 기대한다면 현재 Repository 메서드명 `Between`은 맞지 않을 수 있다.
- 수정 후보:
  - `findAllByTravelAndRecordTimeGreaterThanEqualAndRecordTimeLessThanAndIsActiveTrue`
  - 또는 `@Query`로 `>= start and < end` 명시
- 이 변경은 Repository 메서드명과 Service 호출부를 바꾸므로 팀 합의 후 진행한다.

## Step 6. TravelJoinCodeRedisDaoIntegrationTest

### Target File
- `src/test/java/com/yoen/yoen_back/dao/redis/TravelJoinCodeRedisDaoIntegrationTest.java`

### Recommended Test Type
- `class TravelJoinCodeRedisDaoIntegrationTest extends RedisIntegrationTestSupport`
- 전체 Spring Boot context를 띄우지 않는 작은 Spring test context

### Test Target
- `TravelJoinCodeRedisDao`

### No Database Required
- 이 테스트는 Redis DAO 자체를 검증한다.
- PostgreSQL Repository나 JPA Entity는 필요 없다.
- DB mock도 만들 필요가 없다.

### Planned Test Cases
- `saveBidirectionalMapping_validCodeAndTravelId_savesBothDirections`
  - `joinCode:{code}` -> `travelId`
  - `travelCode:{travelId}` -> `code`
  - 양방향 조회가 모두 성공해야 한다.

- `existsCode_existingCode_returnsTrue`
  - 저장된 code에 대해 `existsCode`가 true를 반환해야 한다.

- `existsTravelId_existingTravelId_returnsTrue`
  - 저장된 travelId에 대해 `existsTravelId`가 true를 반환해야 한다.

- `deleteByCode_existingCode_deletesBothDirections`
  - code 기준 삭제 후 `joinCode:{code}`와 `travelCode:{travelId}`가 모두 없어져야 한다.

- `deleteByTravelId_existingTravelId_deletesBothDirections`
  - travelId 기준 삭제 후 양쪽 key가 모두 없어져야 한다.

- `getExpirationTime_existingCode_returnsFutureTime`
  - 저장된 code의 만료 시간이 현재보다 미래여야 한다.

- `getExpirationTime_missingCode_returnsEmpty`
  - 없는 code는 empty를 반환해야 한다.

### TTL Notes
- 현재 DAO의 TTL은 3일이다.
- 테스트에서 정확히 3일 전체를 비교하면 실행 시간 때문에 flaky할 수 있다.
- 만료 시간이 현재보다 미래인지, 그리고 대략 2일 23시간 이상 남았는지 정도로 느슨하게 검증한다.

## Step 7. JoinServiceIntegrationTest

### Target File
- `src/test/java/com/yoen/yoen_back/service/JoinServiceIntegrationTest.java`

### Recommended Test Type
- `@DataJpaTest`
- `@Import({JoinService.class, TravelService.class})`
- `class JoinServiceIntegrationTest extends PostgresIntegrationTestSupport`

### Required Real Repositories
- `TravelJoinRequestRepository`
- `TravelRepository`
- `TravelUserRepository`
- `UserRepository`

### Required Mock Collaborators
- `TravelJoinCodeRedisDao`
- `CommonService`
- `ImageService`

### Why Redis DAO Is Mocked Here
- Redis 자체는 `TravelJoinCodeRedisDaoIntegrationTest`에서 실제 Redis로 검증한다.
- 이 테스트의 핵심은 JoinService가 PostgreSQL 상태를 어떻게 바꾸는지다.
- 참여 코드 해석은 mock으로 고정해서 서비스/DB 로직을 선명하게 검증한다.

### Test Data Setup Notes
- 여행 생성 시 `numOfPeople`과 `numOfJoinedPeople`를 명확히 지정한다.
- 여행 생성자/작성자는 `TravelUser`로 미리 저장한다.
- 참여 신청자는 별도 `User`로 저장한다.
- Redis mock은 참여 코드 `"ABC123"`을 여행 ID 문자열로 반환하게 한다.

### Planned Test Cases
- `requestToJoinTravel_validCode_savesJoinRequest`
  - Redis mock이 code -> travelId를 반환한다.
  - 신청자가 아직 신청하지 않았고 참여자도 아니면 `TravelJoinRequest`가 저장된다.
  - 저장된 요청은 `isAccepted=false`, `isActive=true`여야 한다.

- `requestToJoinTravel_existingActiveRequest_doesNotCreateDuplicateRequest`
  - 같은 여행/사용자에 active 요청이 이미 있으면 추가 저장되지 않아야 한다.
  - DB에 active 요청이 1개만 남는지 검증한다.

- `requestToJoinTravel_alreadyJoinedUser_doesNotCreateRequest`
  - 신청자가 이미 `TravelUser`로 참여 중이면 요청이 생성되지 않아야 한다.

- `requestToJoinTravel_invalidCode_throwsInvalidJoinCodeException`
  - Redis mock이 empty를 반환하면 예외가 발생해야 한다.
  - DB에는 요청이 저장되지 않아야 한다.

- `acceptJoinRequest_availableCapacity_acceptsRequestAndCreatesTravelUser`
  - 참여 요청을 승인하면 요청은 `isAccepted=true`, `isActive=false`가 된다.
  - 새 `TravelUser`가 생성된다.
  - `Travel.numOfJoinedPeople`가 1 증가한다.

- `acceptJoinRequest_alreadyJoined_throwsIllegalStateException`
  - 이미 같은 여행에 참여 중인 사용자의 요청 승인 시 예외를 검증한다.
  - 이때 요청 상태가 어떻게 남아야 하는지는 현재 코드와 기대 정책이 다를 수 있다.

- `acceptJoinRequest_fullCapacity_throwsIllegalStateException`
  - 여행 정원이 가득 찬 경우 예외가 발생해야 한다.
  - `TravelUser`는 생성되지 않아야 한다.
  - 요청 상태가 변경되는 현재 동작은 팀과 확인이 필요하다.

- `rejectJoinRequest_existingRequest_marksInactiveAndRejected`
  - 거절 시 요청은 `isAccepted=false`, `isActive=false`가 된다.
  - 거절된 요청은 active 목록 조회에서 제외되어야 한다.

### Important Service Logic Risk
- 현재 `JoinService.acceptJoinRequest()`는 요청을 먼저 승인/비활성 처리하고 저장한 뒤, 중복 참여/정원 초과를 검사한다.
- `@Transactional`이 없는 상태라면 예외가 발생해도 앞선 save가 커밋될 수 있다.
- 통합 테스트에서 이 문제가 드러날 가능성이 있다.
- 실무적으로는 검증을 먼저 하고 성공할 때만 상태를 바꾸는 순서가 더 안전하다.
- 이 로직 수정은 테스트 추가와 별도 PR 또는 같은 PR 내 별도 커밋으로 분리하는 것을 추천한다.

## Step 8. CI Considerations

### Current CI
- `.github/workflows/ci.yml`
- pull request 대상 `main`
- `./gradlew test`

### Expected Behavior After Integration Tests
- 별도 CI job 없이 기존 `Run tests` 단계에서 PostgreSQL/Redis 통합 테스트까지 함께 실행된다.
- Docker 기반 Testcontainers가 실행되므로 CI 시간이 늘어날 수 있다.

### Possible CI Failures
- Docker daemon 사용 불가
- Testcontainers 이미지 pull 실패
- PostgreSQL container startup timeout
- Redis container startup timeout
- 잘못된 test profile 설정으로 `.env` 값을 요구하는 경우
- `@SpringBootTest` 사용 시 Firebase/Redis 설정 초기화 실패

### Mitigation
- PostgreSQL은 `@DataJpaTest` 기반으로 범위를 좁힌다.
- Redis는 작은 Spring test context로 범위를 좁힌다.
- `@ActiveProfiles("test")`를 support class에 둔다.
- datasource/redis 값은 `@DynamicPropertySource`로 주입한다.
- Firebase/GCS는 이번 범위에서 실제 bean 초기화를 피한다.

## Step 9. Implementation Order

- [x] ~~`build.gradle`에 Testcontainers 의존성 추가~~
- [x] ~~`src/test/resources/application-test.properties` 추가~~
- [x] ~~`PostgresIntegrationTestSupport` 추가~~
- [x] ~~`RedisIntegrationTestSupport` 추가~~
- [x] ~~`TravelRecordRepositoryIntegrationTest` 기본 데이터 저장 테스트 작성~~
- [x] ~~`TravelRecordRepositoryIntegrationTest` active/inactive 조회 테스트 작성~~
- [x] ~~`TravelRecordRepositoryIntegrationTest` 날짜 범위 조회 테스트 작성~~
- [x] ~~`TravelJoinCodeRedisDaoIntegrationTest` 양방향 저장/조회 테스트 작성~~
- [x] ~~`TravelJoinCodeRedisDaoIntegrationTest` exists 테스트 작성~~
- [x] ~~`TravelJoinCodeRedisDaoIntegrationTest` delete 테스트 작성~~
- [x] ~~`TravelJoinCodeRedisDaoIntegrationTest` TTL 테스트 작성~~
- [x] ~~`JoinServiceIntegrationTest` 테스트 slice 구성~~
- [x] ~~`JoinServiceIntegrationTest` 참여 신청 테스트 작성~~
- [x] ~~`JoinServiceIntegrationTest` 중복 신청 방지 테스트 작성~~
- [x] ~~`JoinServiceIntegrationTest` 승인 성공 테스트 작성~~
- [x] ~~`JoinServiceIntegrationTest` 거절 성공 테스트 작성~~
- [x] ~~정원 초과/이미 참여 중 예외 테스트 작성~~
- [x] ~~현재 코드와 기대 정책이 충돌하는 테스트는 팀 논의 항목으로 표시~~
- [x] ~~`./gradlew test` 로컬 실행~~
- [ ] PR에서 CI 결과 확인

## Local Verification Notes
- `./gradlew compileTestJava` 실행 성공
- Docker Engine 29.x와 Spring Boot 3.4.7 기본 Testcontainers 1.20.6 조합에서 Docker discovery 실패 확인
- `ext['testcontainers.version'] = '1.21.4'`로 Testcontainers 버전을 올린 뒤 Redis 단일 통합 테스트 성공
- `DOCKER_HOST=unix:///Users/jeonghoon/.docker/run/docker.sock ./gradlew test --no-daemon` 실행 성공

## Suggested Review Size
- 1차 PR: Testcontainers 의존성 + `PostgresIntegrationTestSupport` + `RedisIntegrationTestSupport`
- 2차 PR: `TravelRecordRepositoryIntegrationTest`
- 3차 PR: `TravelJoinCodeRedisDaoIntegrationTest`
- 4차 PR: `JoinServiceIntegrationTest`
- 5차 PR: 통합 테스트에서 드러난 서비스 로직 수정

## Naming Convention
- 테스트 클래스명은 대상 클래스명 뒤에 `IntegrationTest`를 붙인다.
- 테스트 메서드는 기존 유닛 테스트 스타일을 유지한다.
- 형식: `methodName_condition_expectedResult`

## Open Questions For Team
- 하루 단위 여행 기록 조회는 끝 경계를 포함해야 하는가?
- 승인 실패 시 참여 요청 상태가 그대로 남아야 하는가, 아니면 비활성화되어야 하는가?
- 서비스 통합 테스트에서 `TravelService`를 실제 bean으로 쓸지, 일부만 mock 처리할지?
- 통합 테스트를 `./gradlew test`에 항상 포함할지, 나중에 별도 Gradle task로 분리할지?
- PostgreSQL + Redis 전체 Join flow 테스트는 어느 시점에 happy path만 추가할지?

## Completion Criteria
- PostgreSQL Testcontainers가 `testdb`로 실행된다.
- Redis Testcontainers가 실제 Redis key/value/TTL 동작을 검증한다.
- 테스트는 로컬 `.env`, 로컬 PostgreSQL, 로컬 Redis, Firebase credential 없이 실행된다.
- `TravelRecordRepositoryIntegrationTest`가 PostgreSQL 기준 Repository 쿼리를 검증한다.
- `TravelJoinCodeRedisDaoIntegrationTest`가 Redis DAO의 저장/조회/삭제/TTL을 검증한다.
- `JoinServiceIntegrationTest`가 Redis mock + PostgreSQL real repository 조합으로 주요 참여 흐름을 검증한다.
- PostgreSQL + Redis 전체 flow 테스트는 이번 라운드에서 추가하지 않는다.
- `./gradlew test`가 로컬과 GitHub Actions에서 통과한다.
