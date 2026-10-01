# AgroTrends backend

A "Medium for agricultural knowledge, with AI": farmers, agronomists and students publish blogs, ask and
answer questions, and get AI answers grounded in the platform's own posts (RAG). Spring Boot 3.5 / Java 21,
PostgreSQL + pgvector, Spring AI (Gemini). The React frontend lives in `../AGROTRENDS- FrontEnd`.

## Start of every session

1. Read `docs/DEV_LOG.md` (where we are / next up / open items) and `docs/ROADMAP.md`.
2. `git status` and check the branch. Follow the `git-workflow` skill.

## Where things are

- `docs/PLAN.md` - vision, current state, decisions, gaps vs Medium, risks
- `docs/ROADMAP.md` - phased steps, one small PR each, with status
- `docs/DEV_LOG.md` - dated log of what was done and decided
- `docs/research/` - primary-source research notes (Spring AI RAG, Gemini embeddings, ...)
- `.claude/skills/` - `coding-conventions`, `api-conventions`, `security-checklist`, `git-workflow`
- `.env.example` - every environment variable the app reads

## Run and test

```bash
cp .env.example .env            # fill DB_PASSWORD, GEMINI_API_KEY, JWT_SECRET (>= 32 chars)
./mvnw spring-boot:run          # needs PostgreSQL with the pgvector extension
./mvnw -B test -Dtest='!AgriculturalBlogApplicationTests'   # unit tests; contextLoads needs a DB until Phase 4
```

Swagger UI: `/swagger-ui.html`. Never read, print or commit `.env`.

## Rules that matter most

- Caller identity comes from the token, never from a request body (`security-checklist`).
- Mutations are owner-or-admin, asserted in the service.
- Return DTOs, not entities. No secrets in code or config. No exception detail in responses.
- Lombok accessors and `@RequiredArgsConstructor`; no hand-written boilerplate (`coding-conventions`).
- Branch flow: `development` -> feature base -> serial step PRs -> base into `development` -> `staging` -> `production`.
  Never work on or merge into `main`. Small conventional commits, no attribution (`git-workflow`).
- Every fix ships with a regression test; update `docs/ROADMAP.md` and `docs/DEV_LOG.md` in the same PR.
