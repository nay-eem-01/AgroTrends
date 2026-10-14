# AgroTrends — Feature Roadmap

What gets built, in order, and how far we are. The reasoning is in `docs/PLAN.md`; the running log is
`docs/DEV_LOG.md`. Update this file in the same PR that finishes a step.

Each step is sized to be **one small PR** (see the `git-workflow` skill). A step that grows past ~10–15 files
gets split here first.

**Legend:** ✅ done · 🔄 in progress · ⬜ not started · ⏸ deferred / needs the owner

**Progress:** 69 of 86 steps done

**Launch scope (decided 2026-10-13):** Phases 4–8 must all be done before the public launch. Order of work:
5 → 6 → 7 → 8 → 4 (deploy) → live test → launch. Nothing is deployed until Phases 5–8 are done. After launch, the
8-week test decides what comes from **Later** (see `docs/PLAN.md` §6).

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
| 2.1a | Response DTOs for Blog and Category (author shown as `{authorId, name}`); clamped `pageSize` | ✅ |
| 2.1b | Response DTOs for User: private `/me` and auth payloads (`UserResponse`), public `/id/{id}` (`{id, name}`), admin list | ✅ |
| 2.1c | Answer / Comment / Question responses: `authorName`, `createdAt`, `updatedAt` (ISO `Instant`; never `createdBy`, which holds the e-mail) | ✅ |
| 2.2 | Real database pagination for blogs by category/author; `sortBy` allowlist -> 400; author lists by Author id | ✅ |
| 2.3a | Blog `status` (DRAFT/PUBLISHED), `publishedAt`, publish/unpublish, drafts list; public lists PUBLISHED only; `status` in vector metadata + retrieval filter | ✅ |
| 2.3b | Blog slug (stable, Unicode-friendly) and reading time; `GET /api/blogs/slug/{slug}` | ✅ |
| 2.4 | Tags (many-to-many, normalised, max 5 per blog), `GET /api/blogs/all/tag/{tagName}`, `GET /api/tags?q=` | ✅ |
| 2.5 | `Blog.content` / `AiAnswer.aiAnswer` `@Lob` -> `TEXT` (startup patch before Hibernate); full-text search `GET /api/blogs/search?q=` | ✅ |
| 2.6 | Image upload (`POST /api/images`, authors, JPEG/PNG/WebP by magic bytes, 5 MB) behind an `ImageStorage` port; local disk served at `/uploads/**` | ✅ |
| 2.7 | Claps on published blogs (max 50 per reader, not own), `clapCount` on blogs; `/api/blogs/id/{blogId}/claps` | ✅ |
| 2.8 | Bookmarks: private reading list (`PUT`/`DELETE /api/blogs/id/{blogId}/bookmark`, `GET /api/bookmarks`) | ✅ |
| 2.9 | Follow authors and topic tags (`/api/authors/{authorId}/follow`, `/api/tags/{tagName}/follow`, `/api/me/following/*`) | ✅ |
| 2.10 | Public author profile `GET /api/authors/{authorId}` (bio, specialities, post/follower counts) and `PUT /api/authors/me` | ✅ |
| 2.11 | Home feed: `/api/feed/latest`, `/api/feed/following` (authors + tags), `/api/feed/trending` (14 days, most clapped) | ✅ |
| 2.12a | Agriculture metadata on blogs (crop, season, region, soil) + filters on `/api/blogs/all` | ✅ |
| 2.12b | Agriculture metadata on questions + filters on `/api/questions/all` | ✅ |
| 2.13 | Public reading: anonymous GET on published content (blogs, feeds latest/trending, search, tags, categories, authors, comments, questions, answers); every write still needs sign-in | ✅ |
| 2.14 | `GET /api/authors/me` (prefill the author profile editor) and `authorId` on `UserResponse` (null for readers) | ✅ |
| 2.15a | Google ID-token verifier (`spring-security-oauth2-jose`, Google JWKS, `iss`/`aud`/expiry checks); `GOOGLE_CLIENT_ID` | ✅ |
| 2.15b | `POST /api/auth/google`: find by Google id, else link by e-mail, else create; `emailVerifiedAt`; password and mobile optional | ✅ |

## Phase 3 — AI that cites

The differentiator. See `docs/research/rag-and-platform-findings.md`.

| # | Step | Status |
|---|---|---|
| 3.1 | Replace the shut-down embedding model (`gemini-embedding-2`, 768 dims); verify end to end | ✅ |
| 3.2 | Index with metadata (`blogId`, `authorId`, `categoryId`, `title`) + `TokenTextSplitter` chunking (~800 tokens); `status` is added with 2.3 | ✅ |
| 3.3 | Replace vectors on blog update, delete them on blog delete; one-off re-index (`AI_REINDEX_ON_STARTUP`); unpublish is handled in 2.3 | ✅ |
| 3.4 | Retrieval advisor on the chat client (`RetrievalAugmentationAdvisor`, top 5, similarity ≥ 0.7); PUBLISHED-only filter lands with 2.3 | ✅ |
| 3.5 | Citations: `/api/ai/ask` returns `sources` (`blogId`, `title`) next to `answer` | ✅ |
| 3.6 | Per-user daily quota (20), question cap (1000 chars), 60 s Gemini timeout, 503 on AI failure; token usage on `AiAnswer` | ✅ |
| 3.7 | `GET /api/ai/history` (private to the caller, newest first, paged) | ✅ |
| 3.8a | AI helpers: related posts for a blog (`GET /api/blogs/id/{blogId}/related`) | ✅ |
| 3.8b | AI helpers: summary and suggested tags for an author's draft (`POST /api/ai/blog-assist`) | ✅ |
| 3.8c | AI helpers: labelled AI draft answer for an unanswered question (`POST /api/questions/id/{questionId}/ai-draft`) | ✅ |


## Phase 4 — Ship it

Operability and regression safety. Runs after Phases 5–8 are complete; then deploy and live-test before launch.

| # | Step | Status |
|---|---|---|
| 4.1 | `dev` / `prod` profiles; `ddl-auto=validate`; CORS origins from `app.frontendUrl` (audit B6); `server.forward-headers-strategy` so per-IP limits see the client IP behind the proxy | ⬜ |
| 4.2 | Flyway baseline + migrations for the existing schema; then delete `SchemaPatches` (audit O8) | ⬜ |
| 4.3 | `docker-compose.yml` with `pgvector/pgvector:pg16`; Dockerfile; `uploads/` on a named volume | ⬜ |
| 4.4 | Testcontainers (pgvector): `contextLoads` passes; `@WebMvcTest` slices for auth and ownership | ⬜ |
| 4.5 | GitHub Actions: build + test + gitleaks | ⬜ |
| 4.6 | README: prerequisites, env vars, run, endpoint table | ⬜ |
| 4.7 | Housekeeping: rename `constatnt`, fix `AuditModel` date format, log 4xx below ERROR (dead code moved to 5.4) | ⬜ |
| 4.8 | Rate-limit store: move `AttemptLimiter` to a shared store if running more than one instance | ⏸ |
| 4.9 | Spring Boot Actuator: health + basic metrics, health endpoint public, the rest admin-only | ⬜ |
| 4.10 | Backups: nightly `pg_dump` and `uploads/` copy, restore tested once | ⬜ |

## Phase 5 — Launch blockers

Code-audit fixes (`docs/research/product-study-2026-10.md` §6) and the account model. One account type for
everyone; a confirmed e-mail before any write.

| # | Step | Status |
|---|---|---|
| 5.1 | AI indexing never fails a blog save: catch + log in `DocumentService.indexBlog`/`reindexBlog`, the re-index runner heals (audit B1) | ✅ |
| 5.2 | System prompt: drop the "Bangla only if explicitly asked" sentence; test asserts it (audit B2) | ✅ |
| 5.3 | Delete the DB-driven i18n error stack (`ErrorCodeEntity`, `Language`, `LocalizedText`, `Translation` + services) and unused `ErrorCode` constants (audit O1, O3; fixes B4) | ✅ |
| 5.4 | Delete dead code: `dtos/` + ModelMapper, Thymeleaf, `ForgetPasswordService` and other orphans (audit O4–O6) | ✅ |
| 5.5 | One account type: sign-up takes no `userType`; the author profile becomes the user's profile, created on first need; any confirmed user may publish | ✅ |
| 5.6 | E-mail confirmation: token mailed at sign-up, `POST /api/auth/verify-email`, `POST /api/auth/resend-verification`; Google sign-in counts as confirmed | ✅ |
| 5.7 | Confirmed-e-mail gate on every write (questions, answers, comments, blogs, votes, images, AI): 403 `EMAIL_NOT_VERIFIED` | ✅ |
| 5.8 | Per-IP daily cap on `/api/ai/**` and `/api/auth/google` (audit B3 remainder) | ✅ |
| 5.9 | Server-side HTML sanitising (jsoup safelist) for blogs, questions and answers | ✅ |
| 5.10 | `BlogSummaryResponse` with a 200-character excerpt for every list endpoint (audit A1) | ✅ |
| 5.11 | `@EntityGraph` on list queries; `open-in-view=false` (audit A2) | ✅ |

## Phase 6 — Community Q&A and reputation

Stack Overflow-style trust. Topics are the existing categories. Point values are in `docs/PLAN.md` §5.

| # | Step | Status |
|---|---|---|
| 6.1 | Topic (category, required) and tags on questions; filter by topic and tag | ✅ |
| 6.2 | Photos on questions (up to 4 image URLs from `POST /api/images`, now open to every confirmed user) | ✅ |
| 6.3 | Full-text search over questions; `answerCount`; `unanswered` filter | ✅ |
| 6.4 | Votes on questions and answers: up/down, one per user, not on your own, atomic `score` | ✅ |
| 6.5 | Accepted answer: the asker picks one (can change it); accepted answer sorts first | ✅ |
| 6.6 | Reputation ledger: `ReputationEvent` rows per user and topic, written and reversed with votes and accepts; downvoting needs 50 points | ✅ |
| 6.7 | Reputation on profiles and posts: totals per topic on `GET /api/users/{id}/reputation`; author reputation + badges on question/answer responses | ✅ |
| 6.8 | Top Contributor badge, computed per topic (250 points and 5 accepted answers) | ✅ |

## Phase 7 — Verified experts and AI review

The trust layer. An expert is verified **per topic**; only verified experts confirm or correct AI answers.

| # | Step | Status |
|---|---|---|
| 7.1 | Expert application: `POST /api/experts/applications` (topics, field title, credential type, institution, proof images, links); `GET /api/experts/applications/me` | ✅ |
| 7.2 | Admin review: pending list, approve (grants an `ExpertBadge` per topic) / reject with reason, revoke a badge | ✅ |
| 7.3a | Stored AI answer on every new question, generated after commit (async), status `UNCHECKED`; responses carry a safety `disclaimer` | ✅ |
| 7.3b | Retire the on-demand `POST /api/questions/id/{id}/ai-draft` (replaced by 7.3a) | ✅ |
| 7.4 | Review queue for experts (their topics only): confirm / correct (creates an expert answer, AI answer `CORRECTED`) / flag (`FLAGGED`, hidden); admin can reopen | ✅ |
| 7.5 | "Ask an expert to check this": turn a private `/api/ai/ask` answer into a public question | ✅ |
| 7.6a | RAG over Q&A: index accepted answers, expert answers and confirmed AI answers (`type=answer`, `questionId`); remove on un-accept, delete, reopen or badge revoked (hide arrives with 8.4) | ✅ |
| 7.6b | Citations for answers: `sources` gain `type` and `questionId` | ⬜ |
| 7.7 | Question crop/season/region passed into the AI prompt | ⬜ |

## Phase 8 — Notifications and moderation

Replaces the old Kafka phase: in-process after-commit events, same API, no broker (`docs/PLAN.md` decision 13).
The old `feature/kafka-impl` branch can be deleted.

| # | Step | Status |
|---|---|---|
| 8.1 | `@TransactionalEventListener(AFTER_COMMIT)` writes `Notification` rows: answer on your question, your answer accepted, AI answer on your question checked, new review item in your topic, application decided, reply to your comment | ⬜ |
| 8.2 | `/api/notifications` for the caller only: list, unread count, mark one / all read | ⬜ |
| 8.3 | Reports: `POST /api/reports` (blog, question, answer, AI answer, comment, user; reason); one per user per target | ⬜ |
| 8.4 | Moderation: admin report queue, hide content (out of public lists and RAG, reputation reversed), ban user (sessions revoked), revoke expert badge | ⬜ |

## Later — after the 8-week launch test

Not counted in progress. Each one waits for the week-8 decision (`docs/PLAN.md` §6) and is tested by hand
before it is built.

| Item | Note |
|---|---|
| AgroTrends Academy (courses, cohorts, enrolment) | First course runs on Google Meet + bKash, no code |
| Paid priority questions, then expert appointments | Start as a bKash number on expert profiles |
| Payments (bKash / SSLCommerz) | Only once a paid stream is proven |
| Remove the `String lang` parameter from service signatures (audit O2) | Use `Accept-Language` / `LocaleContextHolder` when needed |
| Messenger bot, photo triage (Gemini vision), voice in/out | Product study §5 |
| Multi-turn AI chat; thumbs up/down on AI answers | |
| SEO: sitemap, OpenGraph, RSS; weekly e-mail digest | |
