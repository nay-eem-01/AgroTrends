# AgroTrends — Development Log

Read this first at the start of every working session. Newest entry on top.
The step-by-step plan and progress are in `docs/ROADMAP.md`; the reasoning is in `docs/PLAN.md`.

---

## Where we are

- Vision: a Medium for agricultural knowledge with AI (`docs/PLAN.md`). Estimated 30–35 % there.
- Phase 0 (except 0.4) and Phase 1 are **merged** (PR #19 into `main`, 2026-10-01).
- **Branch model (decided 2026-10-01):** `development` -> `staging` -> `production`, all created from `main`
  at `da19a32`. Feature work: base branch off `development`, serial step PRs into the base, base -> `development`
  (test) -> `staging` (test) -> `production`. `main` is frozen; no new work there. See the `git-workflow` skill.
- **Notifications will use Kafka** (decided 2026-10-05): Phase 5 in the roadmap, decision 10 in the plan.
- Old `dev` branch reviewed and deleted (2026-10-06). Phase 3 started early (base `feat/ai-rag-base`): 3.1 and 3.2 done.

## Next up

1. Merge `fix/embedding-model`, then `feat/rag-chunk-metadata`, into `feat/ai-rag-base` (Phase 3 base, off `development`).
   Next step: 3.3 (replace vectors on update, delete them on delete, backfill).
2. **Nayeem:** GitHub settings — make `development` the default branch; protect `development`, `staging`,
   `production` (PRs only, require review/CI once 4.5 lands).
3. **Nayeem:** roadmap 0.4 — revoke keys, rotate the DB password, purge git history.
4. Phase 2 starts (base `feat/medium-core-base` off `development`) with 2.1 (response DTOs) and 2.2 (real pagination): everything after builds on them.

## Open items

| Item | Needs | Blocks |
|---|---|---|
| Leaked Gemini keys / DB password in git history — also on `origin/dev` (`f06f456`) and `origin/feature/kafka-impl`, so the purge must cover those branches | Nayeem (revoke, rotate, then purge) | roadmap 0.4 |
| `feature/kafka-impl`: reference only for Phase 5; delete once Phase 5 is rebuilt | Nayeem | — |
| Create a local `.env` from `.env.example` (DB_PASSWORD, GEMINI_API_KEY, JWT_SECRET) — the app no longer starts without them | Nayeem | running the app locally |
| Blog update adds a second vector instead of replacing the old one | roadmap 3.3 | correct retrieval |
| Any database that still holds `text-embedding-004` vectors must clear `vector_store` and re-embed (no backfill yet) | roadmap 3.3 | retrieval on old data |
| Client errors (400/401) are logged at ERROR by `ExceptionHandlingController` — noisy | roadmap 4.7 | — |
| Frontend must stop sending `userId` / `authorUserId`, use `/api/auth/refresh-token`, and handle 401 vs 403 | frontend | frontend integration |
| `/api/user/id/{id}` still returns the full `User` entity (e-mail, mobile, roles) to any signed-in user | roadmap 2.1 | public profiles |
| `contextLoads` fails without a local Postgres | roadmap 4.4 | CI |
| No SMTP configured: reset links are written to the log (dev only) | an SMTP account | production password reset |
| Hotfix path is not defined yet (proposal: branch off `production`, PR into `production`, then back-merge into `development`) | Nayeem to confirm | — |
| The audit PDF (`AgroTrends-Code-Audit.pdf`) is intentionally not committed | — | — |

---

## 2026-10-06 (roadmap 3.2)

**Done**
- `DocumentService` splits each blog (title + content) with a `TokenTextSplitter` bean (`AIConfig.blogTextSplitter`,
  ~800 tokens) and stores `blogId`, `authorId`, `categoryId`, `title` on every chunk. `DocumentServiceTest` covers
  short posts, long posts and batches.
- Verified live: a ~13 kB post became 4 chunks, each with the metadata plus Spring AI's own `chunk_index`,
  `total_chunks` and `parent_document_id`.

**Decisions**
- `authorId` is the `Author` id (not the user id), matching `/api/blogs/all/author/...` and the future public profile.
- `status` metadata waits for 2.3 (blogs have no status yet); retrieval filters on it from then on.

**Known limitations**
- The blog indexed during the 3.1 check has only `blogId`; 3.3's backfill re-indexes it.

## 2026-10-06 (roadmap 3.1)

**Done**
- Embeddings: `gemini-embedding-2` with `dimensions=768` (matches `vector(768)`); `text-embedding-004` was shut
  down on 2026-01-14. `EmbeddingConfigTest` guards the model and the dimension match.
- Verified live on a fresh pgvector DB: sign-up as author -> create blog -> one `vector_store` row,
  `{"blogId": 1}`, 768 dims, norm 1.0 (gemini-embedding-2 normalises truncated vectors itself).
- Old `dev` branch deleted (local and origin).

**Decisions**
- `gemini-embedding-2` over `gemini-embedding-001`: Google's named replacement, no shutdown date, and it
  normalises at 768 dims (001 would need manual normalisation).
- Local DB for testing: Docker `pgvector/pgvector:pg16` named `agrotrends-db`, volume `agrotrends-pgdata`,
  bound to 127.0.0.1 with trust auth (dev only; becomes the compose service in 4.3).

**Known limitations**
- Re-embedding existing blogs is not automated (3.3). The local DB was empty, so nothing to re-embed.

## 2026-10-05 (old `dev` review, AI prompt fix, notifications plan)

**Done**
- Reviewed the 3 commits on the old `dev` branch against `development`. Already covered: `@EnableJpaAuditing`,
  `creationDate` default sort, top-level comment NPE. Dropped: `GET /api/user/get?email=` (any signed-in user
  could fetch anyone's full `User` by e-mail) and the secrets it re-committed.
- `fix(ai)`: the system prompt now answers English questions in English (salvaged from `dev`, with a test).

**Decisions**
- Notification events go through Kafka, published after commit; consumer writes `Notification` rows
  (plan decision 10, roadmap Phase 5). `feature/kafka-impl` is reference only.
- Timestamps on answer/comment/question responses become roadmap 2.1b: `createdAt`/`updatedAt` and the
  author's display name, never `createdBy` (it holds the e-mail).

## 2026-10-01 (branching model)

**Decisions**
- Long-lived branches `development`, `staging`, `production` created from `main` (`da19a32`) and pushed.
  No more merging to `main`.
- Feature flow: feature base off `development` -> step branches stacked serially -> step PRs into the base ->
  base PR into `development` -> test -> `development` -> `staging` -> test -> `staging` -> `production`.
- Assumption to confirm: the last hop is `staging` -> `production`, and hotfixes branch off `production`.

## 2026-10-01 (roadmap 1.10 — Phase 1 done)

**Done**
- Default sort `creationDate` (was `createdDate`, a property that doesn't exist). `Blog.comments` is `@JsonIgnore`
  (verified live: `/api/blogs/all` was 500 with a reply present, 200 after).

**Found while testing, not fixed here**
- `Blog.content` is `@Lob` and is stored as Postgres `oid` (roadmap 2.5).
- Entities are still returned directly for User/Blog/Category (roadmap 2.1).

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
