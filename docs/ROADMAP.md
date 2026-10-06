# AgroTrends — Feature Roadmap

What gets built, in order, and how far we are. The reasoning is in `docs/PLAN.md`; the running log is
`docs/DEV_LOG.md`. Update this file in the same PR that finishes a step.

Each step is sized to be **one small PR** (see the `git-workflow` skill). A step that grows past ~10–15 files
gets split here first.

**Legend:** ✅ done · 🔄 in progress · ⬜ not started · ⏸ deferred / needs the owner

**Progress:** 16 of 48 steps done

---

## Phase 0 — Stop the bleeding

Secrets, tokens and the default admin.

| # | Step | Status |
|---|---|---|
| 0.1 | Move every secret to environment variables; `.env.example`; ignore `.env` | ✅ |
| 0.2 | JWT secret injected and validated at startup; 15-minute access tokens with a unique `jti` | ✅ |
| 0.3 | No default admin credentials — generated/env password, forced change, legacy default auto-locked | ✅ |
| 0.4 | **Nayeem:** revoke the five Gemini keys, rotate the DB password, purge git history (`filter-repo`/BFG) and force-push | ⬜ |

## Phase 1 — Correct and authorized

Make the existing API trustworthy. Breaking API changes are recorded in the DEV_LOG.

| # | Step | Status |
|---|---|---|
| 1.1 | Fix the top-level comment NullPointerException (+ regression test) | ✅ |
| 1.2 | Error handling: no exception detail in responses; 401/403 as JSON; advice ordering | ✅ |
| 1.3 | Sessions and tokens: sign-out revokes, refresh-token rotation, hardened sign-in (generic 401, rate limit) | ✅ |
| 1.4 | Identity from the token + owner-or-admin: questions and answers (`AuthorizationService`) | ✅ |
| 1.5 | Identity from the token + owner-or-admin: comments | ✅ |
| 1.6 | Identity from the token + owner-or-admin: blogs (author profile required), category-delete privilege | ✅ |
| 1.7 | Profile edits act on the caller only; uniqueness checks; change-password; revoke credentials on change/delete | ✅ |
| 1.8 | Forgot / reset password by e-mail link (`MailService`: SMTP or dev logger) | ✅ |
| 1.9 | Audit: anonymous requests stamped `SYSTEM`; `AuthUtil` cleanup | ✅ |
| 1.10 | Default sort field `creationDate` (was a non-existent `createdDate`); stop serializing lazy `Blog.comments` | ✅ |

## Phase 2 — Medium core

Everything that makes it a publishing platform. Each step one small PR.

| # | Step | Status |
|---|---|---|
| 2.1 | Response DTOs for User, Blog, Category (and a clamped `pageSize`); stop returning entities | ⬜ |
| 2.1b | Answer / Comment / Question responses: `createdAt`, `updatedAt` and the author's display name (never `createdBy`, which holds the e-mail); one date format | ⬜ |
| 2.2 | Real database pagination for blogs by category/author/user; `sortBy` allowlist -> 400 | ⬜ |
| 2.3 | Blog `status` (DRAFT/PUBLISHED), slug, reading time, `published_at`; public lists show PUBLISHED only; `status` in the vector metadata; unpublish removes the vectors | ⬜ |
| 2.4 | Tags/topics (many-to-many) and filtering by tag | ⬜ |
| 2.5 | `Blog.content` `@Lob` -> `TEXT`; full-text search endpoint | ⬜ |
| 2.6 | Image upload (cover + inline) behind a storage port; size/type limits | ⬜ |
| 2.7 | Claps on blogs (one user, many claps capped) and counts | ⬜ |
| 2.8 | Bookmarks / reading lists (private; 404 for others) | ⬜ |
| 2.9 | Follow authors and topics | ⬜ |
| 2.10 | Public author profile endpoint (bio, specialities, posts, counts) | ⬜ |
| 2.11 | Home feed: following, trending, latest | ⬜ |
| 2.12 | Agriculture metadata on blogs and questions (crop, season, region, soil) + filters | ⬜ |

## Phase 3 — AI that cites

The differentiator. See `docs/research/rag-and-platform-findings.md`.

| # | Step | Status |
|---|---|---|
| 3.1 | Replace the shut-down embedding model (`gemini-embedding-2`, 768 dims); verify end to end | ✅ |
| 3.2 | Index with metadata (`blogId`, `authorId`, `categoryId`, `title`) + `TokenTextSplitter` chunking (~800 tokens); `status` is added with 2.3 | ✅ |
| 3.3 | Replace vectors on blog update, delete them on blog delete; one-off re-index (`AI_REINDEX_ON_STARTUP`); unpublish is handled in 2.3 | ✅ |
| 3.4 | Retrieval advisor on the chat client (`QuestionAnswerAdvisor`), PUBLISHED only | ⬜ |
| 3.5 | Citations: `/api/ai/ask` returns the source posts | ⬜ |
| 3.6 | Per-user daily quota, prompt length cap, timeout; token usage on `AiAnswer` | ⬜ |
| 3.7 | `GET /api/ai/history` (private to the caller) | ⬜ |
| 3.8 | AI helpers: suggested tags, summary, related posts, labelled AI draft for unanswered questions | ⬜ |

## Phase 4 — Ship it

Operability and regression safety.

| # | Step | Status |
|---|---|---|
| 4.1 | `dev` / `prod` profiles; `ddl-auto=validate`; CORS origins from a property | ⬜ |
| 4.2 | Flyway baseline + migrations for the existing schema | ⬜ |
| 4.3 | `docker-compose.yml` with `pgvector/pgvector:pg16`; Dockerfile | ⬜ |
| 4.4 | Testcontainers (pgvector): `contextLoads` passes; `@WebMvcTest` slices for auth and ownership | ⬜ |
| 4.5 | GitHub Actions: build + test + gitleaks | ⬜ |
| 4.6 | README: prerequisites, env vars, run, endpoint table | ⬜ |
| 4.7 | Housekeeping: rename `constatnt`, delete dead Thymeleaf templates and orphan OTP DTOs, fix `AuditModel` date format, unify `TranslationService` injection | ⬜ |
| 4.8 | Rate-limit store: move `AttemptLimiter` to a shared store if running more than one instance | ⬜ |

## Phase 5 — Notifications (Kafka)

Notification events go through Kafka (decision 10 in `docs/PLAN.md`). Rebuilt on `development`; the old
`feature/kafka-impl` branch is reference only (it was built on the stale `dev` branch, takes `userId` from
the path, publishes inside the transaction and trusts every package for JSON).
Needs 2.9 (follows) for the "new post from an author you follow" event.

| # | Step | Status |
|---|---|---|
| 5.1 | Kafka in `docker-compose.yml` (KRaft, no ZooKeeper); `spring-kafka`; topics declared in code; bootstrap servers from env; JSON trusted packages limited to our event package | ⬜ |
| 5.2 | Notification events (records with ids only): comment on your blog, reply to your comment, answer on your question, new post from a followed author; published **after commit** | ⬜ |
| 5.3 | Consumer: `Notification` entity, idempotent on event id, no self-notifications, retries + dead-letter topic | ⬜ |
| 5.4 | `/api/notifications` for the caller only (identity from the token, 404 for others'): list, unread count, mark one / all read | ⬜ |
| 5.5 | Tests: unit tests for event -> notification rules; Testcontainers Kafka integration test (after 4.4) | ⬜ |
| 5.6 | Delivery beyond the inbox (e-mail digest / push / WebSocket) | ⏸ |
