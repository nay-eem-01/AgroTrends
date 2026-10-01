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
