# YOEN_BACK Agent Notes

## Project Context
- This is a Spring Boot backend for a travel expense and settlement application.
- The current learning goal is to introduce unit tests and PR-based CI in a realistic small-team workflow.
- Do not work directly on `main` for implementation tasks. Use a short-lived branch, preferably with the `codex/` prefix unless the user asks otherwise.

## Collaboration Rules
- The user is learning real-world workflow, so flag instructions that differ from common professional practice before acting.
- Prefer PR-based CI: tests run on pull requests, and merge is manual after checks pass.
- Do not introduce automatic merge unless the user explicitly asks after discussing branch protection and repository settings.
- Before broad changes, explain the intended scope in Korean and keep changes small enough to review.

## Current Technical Shape
- Java 17, Spring Boot 3.4.7, Gradle.
- Main dependencies include Spring Web, Security, Data JPA, Data Redis, WebFlux, MyBatis starter, Firebase Admin, Google Cloud Storage, PostgreSQL, JUnit 5, Spring Security Test.
- Main domains are `user/auth`, `travel`, `payment/settlement`, `record`, `join`, `image`, and `common`.
- Current test coverage is only the default `@SpringBootTest` context test.
- Runtime configuration depends on `.env` values for PostgreSQL, Redis, Firebase, and JWT.

## Testing Direction
- Start with isolated unit tests for pure or near-pure service logic before full Spring context tests.
- Avoid external services in unit tests: mock repositories, Redis DAOs, Firebase/GCS upload, WebClient-based exchange-rate calls, and security collaborators.
- Treat `PaymentService` settlement/shared-fund logic, `TravelService` joined-person counts, `JoinService` duplicate join prevention, `AuthService` token reissue rules, `UserService` login/register behavior, and `Formatter` parsing as early candidates.
- Add test configuration before adding many tests: test profile/properties, clear naming conventions, Mockito/JUnit patterns, and CI-friendly command.
- Do not let tests depend on local `.env`, real PostgreSQL, real Redis, Firebase credentials, or live exchange-rate APIs.

## CI Direction
- Use GitHub Actions on `pull_request` targeting `main`.
- First CI goal: run Gradle tests reliably.
- Branch protection should require the test check to pass before manual merge into `main`.
- Keep deployment/CD separate from the first CI milestone.
