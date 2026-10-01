# AgroTrends — Development Log

Read this first at the start of every working session. Newest entry on top.
The step-by-step plan and progress are in `docs/ROADMAP.md`; the reasoning is in `docs/PLAN.md`.

---

## Where we are

- Vision: a Medium for agricultural knowledge with AI (`docs/PLAN.md`). Estimated 30–35 % there.
- Phase 0 (secrets/tokens/admin) and Phase 1 (correctness/authorization) are being committed as a stack of
  small step branches on `feat/hardening-base` (code was written and verified 2026-10-01; 19 unit tests
  and ~60 live HTTP checks against a throwaway pgvector database passed).
- Current branch history: `main` ← `feature/rag-impl` (RAG groundwork) ← `docs/conventions-and-plan` ←
  `feat/hardening-base` ← step branches.

## Next up

1. Merge the Phase 0/1 step branches serially into `feat/hardening-base`, then base → `main`.
2. **Nayeem:** roadmap 0.4 — revoke keys, rotate the DB password, purge git history.
3. Phase 2 starts with 2.1 (response DTOs) and 2.2 (real pagination): everything after builds on them.
4. Phase 3.1 (replace the shut-down embedding model) is urgent for blog create/update — consider pulling it
   forward ahead of the rest of Phase 2.

## Open items

| Item | Needs | Blocks |
|---|---|---|
| Leaked Gemini keys / DB password in git history | Nayeem (revoke, rotate, then purge) | roadmap 0.4 |
| Create a local `.env` from `.env.example` (DB_PASSWORD, GEMINI_API_KEY, JWT_SECRET) — the app no longer starts without them | Nayeem | running the app locally |
| `text-embedding-004` shut down 2026-01-14: blog create/update likely fails at the embedding step | decision on the replacement model; test with a real key | roadmap 3.1 |
| Frontend must stop sending `userId` / `authorUserId`, use `/api/auth/refresh-token`, and handle 401 vs 403 | frontend | frontend integration |
| `/api/user/id/{id}` still returns the full `User` entity (e-mail, mobile, roles) to any signed-in user | roadmap 2.1 | public profiles |
| `contextLoads` fails without a local Postgres | roadmap 4.4 | CI |
| No SMTP configured: reset links are written to the log (dev only) | an SMTP account | production password reset |
| The audit PDF (`AgroTrends-Code-Audit.pdf`) is intentionally not committed | — | — |

---

## 2026-10-01 (roadmap 1.9)

**Done**
- `AuditorAwareImpl`: anonymous / unauthenticated -> `SYSTEM`. `AuthUtil` is a static helper only; unused
  `LoggedInEmail/UserId/User` (looked up by name, but the JWT subject is the e-mail) removed.

## 2026-10-01 (roadmap 1.8)

**Done**
- `POST /api/auth/forgot-password` `{email}` (always 200) and `POST /api/auth/reset-password` `{token,newPassword}`.
  Token: 256-bit random, only the SHA-256 hash stored in `password_reset_token`, 15 min, single use.
- `MailService`: `SmtpMailService` when `spring.mail.host` is set, else `LoggingMailService` (logs the link at WARN).
  Link = `{app.frontendUrl}/reset-password?token=...` — the frontend needs a page for it.
- Verified live: unknown e-mail -> 200, weak password does not burn the token, reuse -> 400, old sessions dead.

**Known limitation**
- Dev mode logs the reset link; production needs `MAIL_HOST` etc. (see `.env.example`). The legacy, fully
  commented `ForgetPasswordService` and its orphan OTP DTOs are still in the tree (roadmap 4.7).

## 2026-10-01 (roadmap 1.7)

**Done**
- `UserService.update` acts on the caller; e-mail/mobile uniqueness checked on change (was only on sign-up).
- `POST /api/user/change-password` `{currentPassword,newPassword}`: wrong current -> 400 (5 tries/15 min then 429),
  same password -> 400, policy via `CommonUtils.getInvalidPasswordMessage`; clears `mustChangePassword`;
  revokes all sessions/refresh tokens, so the client must sign in again (verified live).
- `UserService.revokeAllCredentials`, `setNewPassword`; `deleteUser` removes the user's sessions and refresh tokens.

**Breaking API changes**
- Removed `userId` from `UpdateUserRequest`; `PUT /api/user/update` is now validated (`@Valid`).

## 2026-10-01 (roadmap 1.6)

**Done**
- Blog create: author = the caller's `Author` row, else 403 `Only registered authors can publish blogs.`;
  update/delete owner-or-admin (verified live). Dead commented `@PreAuthorize` lines removed.
- `DELETE /api/categories/id/{id}/delete` now requires `CATEGORY_DELETE`.

**Breaking API changes**
- Removed `authorUserId` from `CreateBlogRequest`. Blog create/update/delete take optional `?lang`.

**Known limitation**
- Blog delete/update do not yet remove or replace the blog's vectors (roadmap 3.3), and embedding currently
  depends on the shut-down model (roadmap 3.1).

## 2026-10-01 (roadmap 1.5)

**Done**
- Comments: author = caller; update/delete owner-or-admin (verified live: other user -> 403, owner -> 200).
- `/api/comments/reply` now reads `?lang` (it read a non-standard `Accept-Language` request *parameter*).
- `CommentServiceTest` grows to 4 (create as caller, refused non-owner delete).

**Breaking API changes**
- Removed `userId` from `CreateCommentRequest`, `ReplyCommentRequest`, `UpdateCommentRequest`.
- `DELETE /api/comments/id/{id}` takes optional `?lang`.

## 2026-10-01 (roadmap 1.4)

**Done**
- `AuthorizationService`: `currentPrincipal`, `currentUserId`, `isAdmin` (RoleType ADMIN/SUPER_ADMIN),
  `assertOwnerOrAdmin` (403 `You do not have permission...`). Anonymous callers -> 401.
- Questions/answers: author = caller; update/delete owner-or-admin. Verified live: another user's edit/delete -> 403.
- `AuthorizationServiceTest` (4).

**Breaking API changes**
- Removed `userId` from `CreateQuestionRequest`, `CreateAnswerRequest`, `ReplyToAnswerRequest`, `UpdateAnswerRequest`.
- `DELETE /api/answers/delete/id/{id}` now takes optional `?lang`. Request fields are `@NotNull/@NotBlank` (400 on missing).

## 2026-10-01 (roadmap 1.3)

**Done**
- Filter checks the session row; sign-out / password change / e-mail change really end a token's life
  (verified live: a signed-out token that was valid for the rest of its hour now gets 401).
- `POST /api/auth/refresh-token` `{refreshToken}`: rotating, single-use, atomic consume; reusing an old one -> 401.
  `deleteByCredentialId` (deleted only expired tokens) replaced by `deleteAllByUserId`; the stray copy-paste
  text in the expiry message is gone.
- `AttemptLimiter` (in-memory fixed window); `AuthService.authenticate` shared by consumer and admin sign-in;
  failures -> 401 `Invalid email or password.` (identical for unknown e-mail / wrong password / wrong portal);
  lockout -> 429. Client IP is `request.getRemoteAddr()` — set `server.forward-headers-strategy` behind a proxy.
- Tests: `AttemptLimiterTest` (3), `JwtAuthenticationFilterTest` (4).

**Breaking API changes**
- Sign-in failures are 401 with one message (were 404/401/403 with distinct messages).
- Refresh endpoint is `/api/auth/refresh-token`; refresh tokens are single-use — store the new one each time.
- Access token lifetime is 15 minutes, so clients must refresh.

**Known limitation**
- Limiter state is per instance and lost on restart (roadmap 4.8).

## 2026-10-01 (roadmap 1.2)

**Done**
- `GlobalExceptionHandler`: generic message + `{errorId}` payload, full exception logged with that id;
  `@Order(LOWEST_PRECEDENCE)`. `ExceptionAuthHandlingController`: `@Order(HIGHEST_PRECEDENCE + 1)`, adds an
  `AccessDeniedException` -> 403 handler. `ExceptionHandlingController`: no more `getLocalizedMessage()` payloads.
- `AuthenticationExceptionHandler` (entry point): 401 JSON `Authentication required or token is invalid.`

**Breaking**
- Unauthenticated requests are now **401** (were 403); bad sign-in is 401 (was 403).

## 2026-10-01 (roadmap 1.1)

**Done**
- `CommentService.mapToCommentResponse`: root comments map `parentCommentId = null` instead of throwing.
  Create and list of top-level comments worked for the first time (verified live: create, list, reply).
- `CommentServiceTest` (2): root comment and reply map correctly.

## 2026-10-01 (roadmap 0.3)

**Done**
- `InitialDataLoader` -> `ApplicationReadyEvent`; admin e-mail/password from `app.admin.*` (random if unset,
  logged once at WARN). `AppConstants` is now a final utility class with final fields.
- New `User.mustChangePassword` (nullable column, null = false); carried on `CustomUserDetails`.
  Enforced by the JWT filter in 1.3.
- Legacy `admin@gmail.com` / `123456` accounts are detected on startup and reset to a random password.
- `User.password` is `@JsonIgnore` — hashes no longer leak in sign-in, `/me`, `/id/{id}`, `/paginated`.

**Verified live** against a throwaway pgvector database: generated password works, `123456` is rejected.

## 2026-10-01 (roadmap 0.2)

**Done**
- `JwtUtil(@Value app.jwt.secret)`: startup fails if the secret is missing or < 32 bytes. `SecurityConstants.SECRET`
  removed. Every token gets a random `jti`, so tokens issued in the same second are distinct.
- Access token lifetime 1 h -> 15 min.
- `JwtUtilTest` (4): short/missing secret, round trip, forged-secret rejection, distinct tokens.

**Breaking**
- Every existing token is invalid after deploy (new secret) — intended.

## 2026-10-01 (roadmap 0.1)

**Done**
- `application.properties`: DB password, Gemini key and JWT secret come from env vars (`DB_PASSWORD`,
  `GEMINI_API_KEY`, `JWT_SECRET`) with no defaults; `.env` is imported via `spring.config.import`.
- `.env.example` documents every variable; `.env` and `*-local.properties` are git-ignored.
- Removed the commented MySQL password, the `org.example.bankingManagementApplication` logger, Security
  DEBUG and `show-sql`.

**Known limitation**
- The old keys and password are still in git history until roadmap 0.4 (revoke, rotate, purge) is done.

## 2026-10-01 (audit, research and plan)

**Done**
- Read the 2026-09-30 audit (4/10: strong structure, critical security defects, RAG write-only).
- Ran primary-source research on Spring AI 1.1.2 RAG, pgvector, Gemini embeddings, Spring Security,
  Flyway/Testcontainers → `docs/research/rag-and-platform-findings.md`.
- Wrote `docs/PLAN.md`, `docs/ROADMAP.md` (42 steps, 5 phases), the `.claude/skills/` conventions and
  `CLAUDE.md`, ported from the delivery-app project's working style.

**Decisions** — see `docs/PLAN.md` §4.

**Surprises from the research**
- `text-embedding-004` was shut down 2026-01-14; Spring AI 1.1.2 still defaults to it and its model enum
  does not list the replacements (`gemini-embedding-001`, `gemini-embedding-2`).
- pgvector ids are UUIDs; delete-by-blog needs a metadata filter, not ids.
- Spring Security has no rate limiting or lockout counter of its own.
