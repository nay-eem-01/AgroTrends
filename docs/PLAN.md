# AgroTrends — Plan

**Status:** audit 2026-09-30, plan 2026-10-01, **direction reset 2026-10-13** after the product study
(`docs/research/product-study-2026-10.md`) and a council review. Progress is tracked in `docs/ROADMAP.md`;
how the features connect is in `docs/ARCHITECTURE.md`.

---

## 1. What we are building

> **Where Bangladesh's young agriculture community asks, answers and learns. AI answers instantly, a verified
> expert confirms.**

Questions and answers are the core job; articles (blogs) are the long-form knowledge the AI learns from and
cites. Bangla and English are both first-class. The AI is not the moat (free chatbots answer well enough);
the moat is **local, cited, expert-checked** answers and a community whose best answers make the AI better.

Questions every design decision must keep answerable: **who** wrote **what**, in **which state**, **who may
change it**, **who checked it**, and **what the AI used** to answer.

## 2. Who it is for

| Group | Role on the platform |
|---|---|
| **Agri students and fresh graduates** (primary) | First askers and answerers; build a public track record; tomorrow's experts |
| Young agri professionals (agronomists, vets, fisheries, SAAOs) | Verified experts; later course teachers and paid answerers |
| Urban/rooftop growers, hobbyists, agri entrepreneurs | Askers; later Academy buyers |
| Farmers | Served through their children, students and field workers, not directly at launch |

Why students first: smartphones, literacy, concentrated on a few campuses and in Facebook groups, and a weekly
reason to return. Assets: the AgroTrends Facebook page (~1,000 followers) and two team members from an
agricultural university who can write and recruit experts.

## 3. Business domains

| Domain | Owns | At launch |
|---|---|---|
| Identity & profiles | One account type, e-mail confirmation, Google sign-in, sessions, public profile | ✅ |
| Knowledge (articles) | Blogs, drafts, tags, claps, bookmarks, follows, feeds | ✅ (exists) |
| Community Q&A | Questions with topic/agri metadata/photos, answers, votes, accepted answer | ✅ |
| Expertise & reputation | Reputation ledger per topic, Top Contributor, expert applications, Verified Expert badges | ✅ |
| AI advisor | RAG over articles + vetted answers, stored AI answers on questions, expert review queue | ✅ |
| Trust & safety | Reports, moderation, hiding, bans, disclaimers, rate limits | ✅ |
| Engagement | In-app notifications | ✅ |
| Academy | Courses, cohorts, enrolment | Later (tested by hand first) |
| Consultations | Paid priority questions, then appointments | Later |
| Payments | bKash / SSLCommerz | Later |

## 4. Where we are (2026-10-13)

Phases 0–3 are on `development`: a hardened API, the Medium-style publishing core, and RAG with citations.
Google sign-in is merged. **Not deployed, zero users.** The launch needs Phases 4–8 (`docs/ROADMAP.md`).

## 5. Trust model (launch)

**Reputation** (per topic, plus a total), written to a `ReputationEvent` ledger so every point can be traced
and reversed:

| Event | Points |
|---|---|
| Answer upvoted | +10 |
| Question upvoted | +5 |
| Answer accepted | +15 (answerer), +2 (asker) |
| Question or answer downvoted | −2 |
| Post removed by a moderator | everything it earned is reversed |

Votes on your own posts are refused. Downvoting needs 50 points.

**Badges**

| Badge | How | Unlocks |
|---|---|---|
| Verified Expert (per topic) | Application with proof (degree, staff ID, faculty page, licence), admin approves | Expert answers; confirm / correct / flag AI answers in those topics |
| Top Contributor (per topic) | Automatic: 250 points and 5 accepted answers in the topic | Badge; can flag AI answers |

**What the AI learns from:** published articles, accepted answers, expert answers and expert-confirmed AI
answers. Never unchecked AI answers, flagged or hidden content, or drafts.

## 6. Launch and the 8-week test

1. Build Phases 5 → 6 → 7 → 8 with unit tests, then Phase 4 (ship it). Deploy and live-test only once all of
   5–8 are done.
2. Before launch: 30–50 seeded Bangla articles, 5–10 invited and verified experts.
3. Launch through the Facebook page and 2–3 university groups.
4. Weekly numbers, targets by week 8: ≥ 30 weekly active askers/answerers, ≥ 80 % of questions with a human
   answer in 24 h, ≥ 70 % of AI answers reviewed in 48 h, ≥ 10 verified experts, ≥ 10 paying students in a
   hand-run Academy course (Google Meet + bKash).
5. **Week 8:** targets hit → build the Academy and paid questions into the app. Fewer than 30 weekly askers
   and no paying students → stop adding features and rethink the audience or the offer.

Revenue order after the test: Academy → paid priority questions → appointments → labelled sponsorship. An NGO
white-label advisor only if a partner approaches us. Sponsors never influence AI answers.

## 7. Decisions taken

Settled unless Nayeem reopens them.

| # | Decision | Choice | Why |
|---|---|---|---|
| 1 | Secrets | Env vars + git-ignored `.env`; no defaults; fail at startup | The audit found five Gemini keys, the DB password and the JWT secret in git. |
| 2 | Access / refresh tokens | 15-min access JWT validated against a session row; opaque single-use refresh token, rotated | Sign-out and revocation must actually work. |
| 3 | Ownership failures | **403** for public content, **404** for private data | Public content's existence is no secret; drafts/bookmarks/AI history must not leak existence. |
| 4 | Where ownership is enforced | In the **service** (`AuthorizationService.assertOwnerOrAdmin`) | Cannot be forgotten by a new endpoint; unit-testable. |
| 5 | Brute-force protection | In-memory `AttemptLimiter`, no new dependency | Enough for one instance; revisit before scaling out. |
| 6 | Password reset | E-mail link, 256-bit token, hash stored, 15 min, single use | No SMS provider; link flow needs only SMTP. |
| 7 | Seeded admin | Generated or env password, forced change on first sign-in | Default credentials are a full takeover. |
| 8 | ~~Publishing rights: only `AUTHOR` sign-ups~~ | **Superseded by 11** | Locked readers and Google sign-ups out of writing. |
| 9 | API-breaking changes | Allowed before launch; each recorded in the DEV_LOG | No external clients yet. |
| 10 | ~~Notifications via Kafka~~ | **Superseded by 13** | A broker to run for zero users. |
| 11 | Account model (2026-10-13) | One account type; a profile for everyone; any user with a confirmed e-mail may ask, answer, vote, publish and use the AI | Simple onboarding; e-mail confirmation stops quota farming and spam. |
| 12 | Topics (2026-10-13) | The existing categories are the topics for questions, reputation and expert badges | One taxonomy across articles, Q&A and expertise. |
| 13 | Notifications (2026-10-13) | In-process `@TransactionalEventListener(AFTER_COMMIT)` writing `Notification` rows | Same API as the Kafka design, nothing to operate. `shortcut:` move to a broker or outbox when a second consumer or instance exists. |
| 14 | Expert verification (2026-10-13) | Per topic, by admin review of proof; revocable; launch experts hand-picked | A badge must mean something from day one. |
| 15 | AI answers on questions (2026-10-13) | Generated and stored for every new question, labelled unchecked until an expert acts; only confirmed/corrected ones reach RAG | Instant help without polluting the AI's knowledge with unchecked text. |
| 16 | Reputation storage (2026-10-13) | Append-only `ReputationEvent` ledger; totals derived (cached per user and topic) | Every point traceable and reversible on unvote or moderation. |

## 8. Risks

| Risk | Mitigation |
|---|---|
| No answerers: experts don't show up | Recruit 5–10 before launch with written commitments; they get badges, profiles and first offer on paid work |
| Harmful advice (pesticide doses) | Unchecked label, standing disclaimer, report button, expert flagging, badge revocation |
| Empty platform on day one | 30–50 seeded articles; launch only through channels we control |
| AI cost abuse | E-mail confirmation, per-user and per-IP daily caps |
| Gaming reputation (vote rings, self-votes) | Self-votes refused, downvote threshold, moderator reversal via the ledger |
| Chat model availability changes | Model is one property; `AiModelConfigTest` blocks closed ids |
| Prompt injection through shared content | Only published/vetted content retrievable; retrieved text framed as untrusted; citations |
| Leaked keys/passwords in git history | Roadmap 0.4 (Nayeem): rotate, then purge |
| In-memory rate limiter is per-instance | Roadmap 4.8 before running replicas |
