# AgroTrends — Plan

**Status:** audit done 2026-09-30, plan written 2026-10-01 — progress is tracked in `docs/ROADMAP.md`.
**Source documents:** the code-audit report `AgroTrends-Code-Audit.pdf` (kept locally, not committed) and
`docs/research/rag-and-platform-findings.md` (primary-source research).

---

## 1. What we are building

> **Medium, but for agricultural knowledge — with AI.**

Farmers, agronomists, extension workers and students publish long-form posts, ask and answer questions,
follow people and topics, and get AI answers that are *grounded in the platform's own posts and cite them*.
Bangla and English are both first-class. The AI is the differentiator: a plain chatbot already exists
everywhere; an advisor that answers from vetted local knowledge and shows its sources does not.

Five questions every design decision must keep answerable: **who** wrote **what**, in **which state**
(draft/published), **who may change it**, and **what the AI used** to answer.

## 2. Where we are (2026-10-01)

Roughly **30–35 %** of the way to the vision (an estimate, not a measurement). After Phase 0–1 the
foundation is trustworthy; the product features and the AI are still ahead.

| Area | State |
|---|---|
| Architecture | Clean controller → service → repository layering; RBAC seeding; i18n error codes; audit superclass. **Good.** |
| Auth & security | Hardened in Phase 0–1 (see ROADMAP). Still open: rotate leaked keys, purge git history (Nayeem). |
| Content | Blog / category / comment / question / answer CRUD with threaded replies. No drafts, tags, slugs, search, images, reactions. |
| Social | None: no follows, bookmarks, claps, public profiles, feed, notifications. |
| AI | `/api/ai/ask` calls Gemini with **no retrieval**; blogs are embedded but never read back (write-only RAG). The embedding model in config (`text-embedding-004`) was **shut down 2026-01-14**, so blog create/update embedding is expected to fail. |
| Quality | Unit tests for the new auth/authz code; `contextLoads` needs a live Postgres; no CI, no migrations, no container. |

## 3. Gap vs Medium

| Medium capability | Here | Phase |
|---|---|---|
| Write, edit, delete a story | yes (rich text/markdown not decided) | — |
| Draft → publish, scheduled, unlisted | no | 2 |
| Slugs / pretty URLs, reading time | no | 2 |
| Topics / tags | categories only | 2 |
| Search | no | 2 |
| Cover + inline images | image URL string only; no upload | 2 |
| Claps / likes | no | 2 |
| Bookmarks / reading lists | no | 2 |
| Follow authors and topics, personalised feed | no | 2 |
| Public author profile page | partial (`Author` entity, no endpoint) | 2 |
| Responses (comments) | yes, threaded | — |
| Notifications | no | 5 (Kafka events) |
| **Agriculture-specific** (crop, season, region, soil) | no | 2 |
| **AI answers with citations** | chatbot only | 3 |

## 4. Decisions taken

Settled unless Nayeem reopens them. (Recorded 2026-10-01 while building Phase 0–1.)

| # | Decision | Choice | Why |
|---|---|---|---|
| 1 | Secrets | Env vars + git-ignored `.env`; no defaults; fail at startup | The audit found five Gemini keys, the DB password and the JWT secret in git. |
| 2 | Access / refresh tokens | 15-min access JWT validated against a session row; opaque single-use refresh token, rotated | Sign-out and revocation must actually work. |
| 3 | Ownership failures | **403** for public content, **404** for private data | Blogs/questions are public (existence is no secret); drafts/bookmarks/AI history must not leak existence. |
| 4 | Where ownership is enforced | In the **service** (`AuthorizationService.assertOwnerOrAdmin`), not only controller SpEL | Cannot be forgotten by a new endpoint; unit-testable. |
| 5 | Brute-force protection | In-memory `AttemptLimiter`, no new dependency | Enough for one instance; revisit (Redis/bucket4j) before scaling out. |
| 6 | Password reset | E-mail link, 256-bit token, hash stored, 15 min, single use; dev mail logger when no SMTP | No OTP/SMS provider exists; link flow needs only SMTP. |
| 7 | Seeded admin | Generated or env password, forced change on first sign-in; legacy `admin@gmail.com/123456` is auto-locked | Default credentials are a full takeover. |
| 8 | Publishing rights | Only users with an `Author` profile can create blogs | Matches the existing sign-up flow (`userType` AUTHOR). |
| 9 | API-breaking changes | Allowed during Phase 1; each recorded in the DEV_LOG | Identity moved from request body to token. |
| 10 | Notifications | Domain events published to **Kafka** after the DB transaction commits; a consumer writes `Notification` rows; the API reads them for the caller only | Decouples notifying from the request path and lets more consumers (e-mail, push, analytics) subscribe later. Trade-off: publish-after-commit can drop an event if the broker is down — acceptable for notifications; move to an outbox table if delivery must be guaranteed. (2026-10-05) |

## 5. Phases (detail in ROADMAP)

| Phase | Goal | Outcome |
|---|---|---|
| **0 — Stop the bleeding** | No secrets in the repo, no forgeable tokens, no default admin | Safe to keep developing |
| **1 — Correct and authorized** | Comments work, identity from token, ownership, real sessions, password lifecycle | The existing API is trustworthy |
| **2 — Medium core** | DTOs, real pagination, drafts/slugs, tags, search, images, claps, bookmarks, follows, feed, agri fields | A usable publishing platform |
| **3 — AI that cites** | Fix embeddings, chunk + metadata, retrieval advisor, citations, quotas, AI helpers | The differentiator works |
| **4 — Ship it** | Profiles, Flyway, Docker Compose, Testcontainers, CI + gitleaks, README, housekeeping | Deployable, regression-safe |
| **5 — Notifications** | Kafka events for comments, replies, answers, followed authors; consumer; caller-only inbox API | Users hear about activity on their content |

Phases 3 and 4 can overlap with the later part of Phase 2; Phase 2's DTO and pagination steps come first
because everything after builds on them.

## 6. Risks

| Risk | Mitigation |
|---|---|
| Embedding model `text-embedding-004` is shut down; Spring AI 1.1.2's model enum does not list the replacement | Phase 3.1: set `gemini-embedding-001`/`-2` as a plain string, set dimensions explicitly (768 recommended truncation), verify end to end, re-embed every blog. Research file has the sources. |
| Vector-space change breaks stored vectors; `initialize-schema` will not alter `vector(768)` | Re-embed script; keep dimensions constant or migrate the table. |
| Unmetered LLM calls | Per-user daily quota + prompt cap (3.5). |
| Prompt injection through blog text in the shared corpus | Only `PUBLISHED` content retrievable; treat retrieved text as untrusted; cite sources so users can check. |
| `Blog.content` is `@Lob` (stored as Postgres `oid`) — blocks SQL search | Convert to `TEXT` in a migration (2.5). |
| In-memory rate limiter is per-instance | Move to a shared store before running replicas. |
| Leaked keys/passwords remain in git history | 0.4 (Nayeem): rotate first, then `git filter-repo`/BFG + force-push. |
| Kafka adds a broker to run locally and in production | Compose service for dev (5.1); app must start and work without notifications if the broker is down; managed Kafka for production. |
