---
name: coding-conventions
description: Java/Spring coding conventions for the AgroTrends backend - package layout, layering, Lombok rules, constructor injection, DTO vs entity, error handling with ApplicationException/ErrorCode, configuration and secrets, logging, tests. Use whenever writing or changing Java code in this repo, before adding a service, entity, controller, DTO or config class.
---

# Coding conventions (AgroTrends)

Consistency beats local improvement: if the code already does something one way, follow it.
Where existing code deviates from a rule below, the deviation is listed in `docs/ROADMAP.md`
(Phase 4 housekeeping) and gets fixed there - do not fix it opportunistically inside an unrelated PR.

## Stack

Java 21, Spring Boot 3.5, Maven wrapper (`./mvnw`), PostgreSQL + pgvector, Spring Data JPA,
Spring Security 6 + JJWT, Spring AI (Google GenAI), springdoc-openapi, ModelMapper, Lombok.

## Layout

Single module, base package `com.project.agriculturalblogapplication`:

| Package | Holds |
|---|---|
| `controllers` | `@RestController`s - thin: bind, validate, delegate, wrap in `HttpResponse` |
| `service` | business rules, transactions, ownership checks |
| `repositories` | Spring Data interfaces |
| `entities` | JPA entities, all extending `AuditModel<String>` |
| `model.request` / `model.response` | request and response DTOs |
| `security` | JWT filter/util, `AuthorizationService`, `AttemptLimiter`, sessions |
| `exceptionHandler` | `ApplicationException` and the `@ControllerAdvice` classes |
| `mail` | `MailService` and its implementations |
| `config` | `@Configuration` classes, seeding (`InitialDataLoader`) |
| `constatnt` | constants, `ErrorCode` (the misspelling is known; rename is a roadmap step, do not "fix" it ad hoc) |

Layering is controller -> service -> repository -> entity. A controller never touches a repository.
A service may call another service, never another service's repository.

## Lombok - no hand-written boilerplate

- Entities and request DTOs: class-level `@Getter @Setter` (+ `@NoArgsConstructor`). **Never `@Data` on a
  JPA entity** - `equals/hashCode` over lazy relations is a trap.
- Constructor injection with `@RequiredArgsConstructor` on `private final` fields. Never a hand-written
  constructor that only assigns fields, and never `@Autowired` fields.
- A hand-written constructor is fine when it does real work (validates `app.jwt.secret`, a test clock
  seam) - say why in one line.
- Response DTOs are Java `record`s (new code).
- Keep hand-written accessors only where they have logic.

## Entities and DTOs

- **Return DTOs, never entities** from controllers (new code; existing `User`, `Blog`, `Category`
  endpoints are scheduled in Phase 2). Entities leak their object graph, lazy-load during
  serialization (this already broke `/api/blogs/all` once) and publish fields nobody chose to.
- Separate request and response classes: `CreateXRequest`, `UpdateXRequest`, `XResponse`.
- **A request DTO never carries the caller's identity** (`userId`, `authorUserId`). It comes from
  `AuthorizationService.currentUserId(lang)`. See the security skill.
- Enums are stored `@Enumerated(EnumType.STRING)`, never ordinal.
- Large text columns: `@Column(columnDefinition = "TEXT")`, not `@Lob` (Postgres maps `@Lob String` to `oid`).

## Errors

- Throw `ApplicationException(HttpStatus, ErrorCode.X, lang)`. The `ErrorCode` constant's value is the
  English message; `ErrorCodeService` swaps in a DB translation for `lang` when one exists.
- Add a constant to `ErrorCode` for every new failure; do not pass free text.
- Never put exception detail, SQL or class names in a message. Unexpected exceptions fall through to
  `GlobalExceptionHandler`, which logs them and returns an `errorId`.
- Status codes: see the api-conventions skill.

## Configuration and secrets

- Secrets come only from environment variables (local: the git-ignored `.env`, imported through
  `spring.config.import`). **No default value for a secret, in Java or in a committed properties
  file.** Missing secret => the app fails at startup.
- Non-secret tunables go in `application.properties` with a safe default.
- Prefer a `@ConfigurationProperties` class over scattered `@Value` for anything with 3+ related keys.
- Do not read or print `.env` contents.

## Logging

- SLF4J via `@Slf4j`. INFO for business events, WARN for security events (lockouts, seeded admin),
  ERROR only with the exception attached.
- **Never log**: passwords, JWTs, refresh tokens, reset tokens, API keys, full request bodies.
  The one deliberate exception is the dev-only `LoggingMailService`, which logs the reset link
  when no SMTP host is configured - and says so loudly.
- Production logging stays at INFO; `show-sql` and Spring Security DEBUG are for local debugging only.

## Tests

- JUnit 5 + Mockito + AssertJ/JUnit assertions. Unit tests need no Spring context and no database.
- Test what costs money when wrong: authorization/ownership, token and session lifecycle, anything with
  a state machine, every bug fixed (a regression test goes in the same commit as the fix).
- Integration tests run against real PostgreSQL via Testcontainers (Phase 4). Never H2: pgvector,
  `timestamptz` and Flyway migrations behave differently.
- Run: `./mvnw -B test` (until Phase 4 lands, `contextLoads` needs a local Postgres and is excluded
  with `-Dtest='!AgriculturalBlogApplicationTests'`).
