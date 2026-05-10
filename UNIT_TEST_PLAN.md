# Unit Test Plan

## Purpose
- Learn a realistic unit-test and PR-based CI workflow for this Spring Boot backend.
- Current owner scope: `AuthServiceTest`, `JoinServiceTest`, `RecordServiceTest`, and `FormatterTest`.
- Keep these tests as isolated unit tests first. Do not depend on PostgreSQL, Redis, Firebase, WebClient, or full Spring context.

## Working Rules
- Before implementing tests, reread this file and the target production file.
- Use JUnit 5 and Mockito with `@ExtendWith(MockitoExtension.class)` for service tests.
- Use direct assertions for `FormatterTest`; it should not need Mockito.
- Mock repositories, Redis DAOs, image services, token providers, and other collaborators.
- Prefer one behavior per test.
- Use test names shaped like `methodName_condition_expectedResult`.
- Verify side effects only when they are the behavior under test, such as save/delete calls.
- When an item is completed, mark it checked and strike it through, for example `- [x] ~~Completed item~~`.

## Target Files
- `src/test/java/com/yoen/yoen_back/service/AuthServiceTest.java`
- `src/test/java/com/yoen/yoen_back/service/JoinServiceTest.java`
- `src/test/java/com/yoen/yoen_back/service/RecordServiceTest.java`
- `src/test/java/com/yoen/yoen_back/common/utils/FormatterTest.java`

## Planned Order
- [x] ~~Create `FormatterTest`~~
- [x] ~~Create `AuthServiceTest` for token generation, login token issuing, reissue, and logout~~
- [x] ~~Extend `AuthServiceTest` for travel/payment/record/image/join-request role checks~~
- [x] ~~Create `JoinServiceTest`~~
- [x] ~~Create `RecordServiceTest` for lookup and record creation without images~~
- [x] ~~Extend `RecordServiceTest` for image creation, update, image deletion, and record deletion~~
- [x] ~~Run the relevant Gradle test command after tests are written~~
- [x] ~~Update CI workflow plan after local tests are stable~~

## CI Workflow Plan
- [x] ~~Add `.github/workflows/ci.yml`~~
- [x] ~~Run CI on pull requests targeting `main`~~
- [x] ~~Use Java 17 with Gradle cache~~
- [x] ~~Run `./gradlew test` as the first CI gate~~
- [ ] Configure GitHub branch protection to require the CI check before merging into `main`

## FormatterTest Plan
- [x] ~~`getDateTime_validIsoDateTime_returnsLocalDateTime`~~
- [x] ~~`getDateTime_null_returnsNull`~~
- [x] ~~`getDateTime_invalidFormat_throwsDateTimeParseException`~~
- [x] ~~`getDate_validIsoDate_returnsLocalDate`~~
- [x] ~~`getDate_null_returnsNull`~~
- [x] ~~`getDate_invalidFormat_throwsDateTimeParseException`~~
- [x] ~~`getTime_validIsoTime_returnsLocalTime`~~
- [x] ~~`getTime_null_returnsNull`~~
- [x] ~~`getTime_invalidFormat_throwsDateTimeParseException`~~

## AuthServiceTest Plan
- [x] ~~`generateAccessToken_validUserId_delegatesToJwtProvider`~~
- [x] ~~`generateRefreshToken_validUserId_delegatesToJwtProvider`~~
- [x] ~~`loginAndGetToken_validCredentials_generatesTokensAndStoresRefreshToken`~~
- [x] ~~`reissueTokens_invalidRefreshToken_throwsInvalidTokenException`~~
- [x] ~~`reissueTokens_missingStoredToken_throwsInvalidTokenException`~~
- [x] ~~`reissueTokens_mismatchedStoredToken_throwsInvalidTokenException`~~
- [x] ~~`reissueTokens_validRefreshToken_returnsNewTokensAndUpdatesRedis`~~
- [x] ~~`logout_validAccessToken_deletesRefreshToken`~~
- [x] ~~`checkTravelUserRoleByTravel_allowedRole_returnsTravelUser`~~
- [x] ~~`checkTravelUserRoleByTravel_missingTravel_throwsAccessDeniedException`~~
- [x] ~~`checkTravelUserRoleByTravel_missingTravelUser_throwsAccessDeniedException`~~
- [x] ~~`checkTravelUserRoleByTravel_userMismatch_throwsAccessDeniedException`~~
- [x] ~~`checkTravelUserRoleByTravel_disallowedRole_throwsAccessDeniedException`~~
- [x] ~~Add representative role-check tests for payment, record, payment image, travel record image, and join request paths~~

## JoinServiceTest Plan
- [x] ~~`getJoinCode_existingCode_returnsStoredCode`~~
- [x] ~~`getJoinCode_missingCode_createsAndStoresCode`~~
- [x] ~~`getJoinCode_missingTravel_throwsIllegalStateException`~~
- [x] ~~`getJoinCode_codeLookupFails_throwsInvalidJoinCodeException`~~
- [x] ~~`requestToJoinTravel_invalidCode_throwsInvalidJoinCodeException`~~
- [x] ~~`requestToJoinTravel_newRequester_savesJoinRequest`~~
- [x] ~~`requestToJoinTravel_existingActiveRequest_doesNotSave`~~
- [x] ~~`requestToJoinTravel_alreadyJoinedUser_doesNotSave`~~
- [x] ~~`getUniqueJoinCode_returnsSixCharacterAlphaNumericCode`~~
- [x] ~~`getCodeExpiredTime_existingCode_returnsExpirationTime`~~
- [x] ~~`getCodeExpiredTime_missingCode_throwsInvalidJoinCodeException`~~
- [x] ~~`getJoinRequestList_existingRequests_returnsResponseDtos`~~
- [x] ~~`acceptJoinRequest_availableCapacity_acceptsAndCreatesTravelUser`~~
- [x] ~~`acceptJoinRequest_alreadyJoined_throwsIllegalStateException`~~
- [x] ~~`acceptJoinRequest_fullCapacity_throwsIllegalStateException`~~
- [x] ~~`rejectJoinRequest_existingRequest_marksInactiveAndRejected`~~
- [x] ~~`getUserTravelJoinRequests_existingRequests_returnsTravelSummaries`~~
- [x] ~~`deleteUserTravelJoinRequest_existingRequest_marksInactiveAndRejected`~~

## RecordServiceTest Plan
- [x] ~~`getAllTravelRecordsByTravelId_existingRecords_returnsRepositoryResult`~~
- [x] ~~`getAllTravelRecordsByTravel_existingRecords_returnsRepositoryResult`~~
- [x] ~~`getTravelRecordsByDate_validDate_returnsRecordDtosWithImages`~~
- [x] ~~`createTravelRecord_withoutImages_savesRecordAndReturnsEmptyImageList`~~
- [x] ~~`createTravelRecord_missingTravelUser_throwsAccessDeniedException`~~
- [x] ~~`createTravelRecord_withImages_savesImagesAndRecordImageMappings`~~
- [x] ~~`createTravelRecord_withImagesAndNoTravelImage_setsTravelProfileImage`~~
- [x] ~~`updateTravelRecord_withoutImages_updatesFieldsAndReturnsEmptyImageList`~~
- [x] ~~`updateTravelRecord_withRemovedImages_deletesRequestedImages`~~
- [x] ~~`updateTravelRecord_withImages_savesNewImageMappings`~~
- [x] ~~`deleteTravelRecordImage_existingImage_deletesImageAndSoftDeletesMapping`~~
- [x] ~~`deleteTravelRecordImage_missingImage_throwsIllegalArgumentException`~~
- [x] ~~`deleteTravelRecord_existingRecord_deletesImagesAndSoftDeletesRecord`~~

## Known Testability Notes
- `JoinService` creates `SecureRandom` internally, so exact generated code values should not be asserted.
- `RecordService.updateTravelRecord()` assumes `removeImageIds()` is non-null. Use `List.of()` in tests unless this is refactored.
- `RecordService` image tests should mock `ImageService`; do not call Firebase or Google Cloud Storage.
- `AuthService` Redis behavior should be tested through `RefreshTokenRedisDao` mocks, not real Redis.
- Repository behavior itself is not covered by these unit tests. If needed later, add repository or integration tests separately.
