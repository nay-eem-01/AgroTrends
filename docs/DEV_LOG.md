# AgroTrends — Development Log

Read this first at the start of every working session. Newest entry on top.
The step-by-step plan and progress are in `docs/ROADMAP.md`; the reasoning is in `docs/PLAN.md`.

---

## Where we are

- **Direction reset 2026-10-13** (`docs/PLAN.md`): a Q&A-first community for young agri people; AI answers
  instantly, verified experts confirm; articles feed the AI. Students and young professionals first.
- Phases 0–3 and Google sign-in (2.15) are on `development`. **Not deployed, zero users.**
- **Launch = Phases 4–9** (`docs/ROADMAP.md`, 85 of 96 steps done): launch blockers and one account type with
  e-mail confirmation (5), shipping (4), votes + reputation (6), verified experts + AI review queue (7),
  in-process notifications + moderation (8), admin dashboard endpoints (9). The Kafka phase is dropped.
- How the features connect: `docs/ARCHITECTURE.md`. Partner-facing plan: a shared doc (link in the PR).
- **Branch model:** `development` -> `staging` -> `production`; feature base off `development`, serial step PRs into
  the base. `main` is frozen. See the `git-workflow` skill.

## Next up

1. Merge `docs/product-study` and `docs/launch-plan` into `development`.
2. Phases 5–8 are on `development` (PR #110). Phase 9 (admin dashboard) is done on `feat/admin-dashboard-base`:
   merge 9.1 → 9.2 → 9.3, then the base into `development`. Next: Phase 4 (ship it: profiles, Flyway, Docker,
   Testcontainers, CI), then deploy and live-test.
3. Then Phases 6, 7, 8; deploy (Phase 4) and live-test only after all of them.
4. **Team:** 30–50 seeded Bangla articles; 5–10 experts invited; pick the 6–8 launch topics (categories).
5. **Nayeem:** roadmap 0.4 (rotate keys, purge history); set `GOOGLE_CLIENT_ID`; make
   `development` the default branch; delete `feature/kafka-impl` (superseded by Phase 8).

## Open items

| Item | Needs | Blocks |
|---|---|---|
| Leaked Gemini keys / DB password in git history — also on `origin/dev` (`f06f456`) and `origin/feature/kafka-impl`, so the purge must cover those branches | Nayeem (revoke, rotate, then purge) | roadmap 0.4 |
| `feature/kafka-impl`: no longer needed (Phase 8 uses in-process events); delete it | Nayeem | — |
| Create a local `.env` from `.env.example` (DB_PASSWORD, GEMINI_API_KEY, JWT_SECRET) — the app no longer starts without them | Nayeem | running the app locally |
| Any database that still holds `text-embedding-004` vectors: start once with `AI_REINDEX_ON_STARTUP=true` | whoever owns that DB | retrieval on old data |
| Launch topics (categories), reputation numbers | team decision (partner doc) | 6.1 |
| Who reviews expert applications and reports: now a setting (9.3). Super-admin creates an ADMIN role with `EXPERT_REVIEW` / `CONTENT_MODERATE` / `USER_READ` and grants it | Nayeem | moderators other than the super-admin |
| Frontend: `sources[].type` is `blog` (link `/api/blogs/id/{blogId}`) or `answer` (link the question `questionId`); `ai-draft` is gone — poll `GET /api/questions/id/{id}/ai-answer` and show its `disclaimer` and `status` | frontend | — |
| Frontend must stop sending `userId` / `authorUserId`, use `/api/auth/refresh-token`, and handle 401 vs 403 | frontend | frontend integration |
| `contextLoads` fails without a local Postgres | roadmap 4.4 | CI |
| No SMTP configured: reset links are written to the log (dev only) | an SMTP account | production password reset |
| No SMS provider: phone codes are written to the log (dev only). Pick one (SSL Wireless, BulkSMSBD, Twilio…) | team | phone sign-up in production (10.5) |
| Hotfix path is not defined yet (proposal: branch off `production`, PR into `production`, then back-merge into `development`) | Nayeem to confirm | — |
| Gemini chat: `gemini-3.8-flash` failed, `gemini-3.5-flash` works (2026-10-30). Re-run the live checks that were blocked: 3.6–3.8, 7.3a generation, 7.6b answer citations, 7.7 farm details | Nayeem | — |
| `3aaabb4` (Google client id default, `APP_ENV_FILE`) was committed straight to `development`, not through a PR | — (note only) | — |
| After deploying 2.3a: start once with `AI_REINDEX_ON_STARTUP=true` (old chunks have no `status`, so retrieval ignores them) | whoever deploys | AI answers on existing posts |
| Deleting a user with comments/questions/answers fails on foreign keys: decide delete vs anonymise | Nayeem | account deletion |
| The audit PDF (`AgroTrends-Code-Audit.pdf`) is intentionally not committed | — | — |

---

## 2026-10-30 (answers and comments: relations and loading)

**Done**
- 4.12 Removed `Question.answers`, `Answer.replies` and `Comment.replies`: nothing read them except the two
  `viewReplies`, and their only other job was JPA cascade delete, which loads every child before deleting it.
  The `ManyToOne` side stays; `Answer.question` and `Answer.parentAnswer` get `@OnDelete(CASCADE)` (like
  `Comment` already had), and `SchemaPatches.CASCADE_ON_DELETE` rewrites the two existing foreign keys.
  Replies come from `findByParentAnswerIdAndHiddenFalseOrderByIdAsc` / `findByParentCommentIdAndHiddenFalseOrderByIdAsc`
  (oldest first; the old collections had no order at all).
- Indexes on `answers(question_id)`, `answers(answer_id)`, `comment(blog_id)`, `comment(parent_comment_id)`: Postgres does not index foreign keys, so the answer
  list, `Question.answerCount` (a subquery per question on every feed row) and replies scanned the whole table.
- `AnswerService.getAllByQuestionId` is `@Transactional(readOnly = true)`: the answers reuse the question loaded
  for the 404 check instead of loading it (and its user, roles, tags) a second time.

**Decisions**
- `answerCount` stays a `@Formula`: with the index it is one indexed count, and a stored counter would have to be
  kept in step by create, delete, hide, unhide, moderation and the cascade.

**Known limitations**
- Not measured: no local Postgres in this session. Answer's `ManyToOne`s are still eager (each answer pulls its
  user with roles and privileges, and its question); making them `LAZY` is the next lever, after counting queries
  with `spring.jpa.show-sql=true` on a real database.
- The FK patches run as an `ApplicationRunner`, after the web server starts: on an existing database a question
  delete in the first second after startup could still hit the old FK. Flyway (4.2) removes the window.
- The cascade itself has no database test yet; it needs Testcontainers (4.4).

---

## 2026-10-23 → 2026-10-30 (logging, Gemini model, config)

**Done**
- 4.11 `logback-spring.xml` replaces `logback.xml`. `CorrelationIdFilter` tags every request with
  `X-Correlation-Id`: it keeps a safe incoming id (`[A-Za-z0-9-]{8,64}`), otherwise makes a new UUID, returns it
  in the response header and logs it as `[cid=…]`. `LogbackMaskingPatternLayout` masks password/token/secret/
  credential JSON fields and `key=value` pairs, `Bearer` values, and JWT-shaped or `ya29.` tokens in every
  appender. Console and file appenders are async (queue 8192; it blocks rather than drops). Outside `prod` a
  rolling file goes to `${LOG_PATH:-~/logfiles/agrotrends}` (100 MB per file, 30 days, 5 GB cap).
- Removed `logging.file.path=@project.artifactId@`: it set `LOG_PATH`, so logs went into
  `./AgriculturalBlogApplication/` inside the repo. `*.log` and `logfiles/` are in `.gitignore` too.
- `AIConfig.googleGenAiClient` is a `static` bean that reads the key from `Environment`, so the
  "AIConfig created too early" warning is gone. Removed a leftover debug constructor that printed a stack trace.
- Chat model `gemini-3.8-flash` → `gemini-3.5-flash`: chat calls failed on 3.8.
- `app.google.client-id` falls back to the web client id (not a secret); `APP_ENV_FILE` points to another
  `.env` file.
- Tests: masking layout, correlation-id filter (safe/unsafe id, MDC cleared), no `logging.file.path`, chat model.

**Decisions**
- In `prod` (a container) logs go to stdout only; the file appender lives in `!prod` so a container with no
  writable home still starts.
- Masking is a safety net: sensitive DTOs still mask their own `toString`.

**Known limitations**
- The masking regexes match keys by name; a secret under an unrelated key (e.g. `"code"`) is not masked.

---

## 2026-10-17 (Phase 10: phone sign-up)

**Done**
- 10.1 Access tokens name the user id, not the e-mail, so an account needs no e-mail. Sign-in takes `identifier`
  (e-mail, `+8801712345678` or `01712345678`); the old `email` field still works. Throttling is per identifier.
  `SignInRequest.toString` no longer prints the password.
- 10.2 `POST /api/auth/sign-up`: `email`, `countryCode` + `mobileNumber`, or both (400 when neither, 400 for a bad
  number; a banned account's phone gets the ban). `PUT /api/user/update` adds, changes or removes either while one
  remains; a new one starts unconfirmed (a new e-mail gets its link). `phoneVerifiedAt`; `UserResponse` and
  `AdminUserResponse` gain `phoneVerified`. Writes need a confirmed e-mail or phone.
- 10.3 SMS codes: 6 digits, SHA-256 (salted with the user id) stored, 10 minutes, single use, dead after 5 wrong
  tries; 3 sends per number and 10 per IP an hour, 20 checks per IP per 15 minutes. Sent at sign-up and on a new or
  changed number (`ContactChangedEvent`, which now also sends the e-mail link). `POST /api/auth/verify-phone`
  `{countryCode, mobileNumber, code}`; `POST /api/auth/resend-phone-code` `{countryCode, mobileNumber}` (always 200).
  `SmsService` with a logging fallback while `SMS_PROVIDER` is unset (codes appear in the log as `[DEV SMS]`).
- 10.4 Forgot password by phone: `POST /api/auth/forgot-password/phone` `{countryCode, mobileNumber}` (always 200)
  and `POST /api/auth/reset-password/phone` `{countryCode, mobileNumber, code, newPassword}`; the policy is checked
  before the code is used, the code confirms the number, every session is signed out.
- Checked live (second instance on :8081, logged codes): phone-only sign-up, neither → 400, sign-in with `0171…` and
  `+880171…`, unconfirmed write → 403, wrong code → 400, right code → writes open, e-mail added later gets its link
  and signs in, phone removed while e-mail stays, both removed → 400, reset by SMS signs out the old token, duplicate
  phone → 400. The live run caught a wrong column name in the `user_email` NOT NULL patch (fixed, now tested).

**Decisions**
- Phone sign-up is confirmed by an SMS code (team choice 2026-10-17); a confirmed e-mail or phone opens writes.
- Phones are stored in E.164; a number without `+` is Bangladeshi (+880, leading 0 dropped).
- `createdBy` / `lastModifiedBy` and the moderator recorded on reports are now the user id, not the e-mail.
- Changing the e-mail no longer signs the user out (that was only because the e-mail was the token subject).

**Breaking API changes**
- Tokens issued before 10.1 stop working: users sign in again (zero users, so no migration).
- The 401 sign-in message is now "Invalid e-mail, phone number or password."
- `UserResponse.email` can be null (phone accounts). `PUT /api/user/update` with no e-mail removes it.
- The unconfirmed-write 403 message now reads "Confirm your e-mail address or phone number first: ..." (same
  "Confirm your e-mail" start the web app matches on).
## 2026-10-17 (quiet client errors)

**Done**
- `ExceptionHandlingController` logs an `ApplicationException` at ERROR only for 5xx; 4xx goes to DEBUG. The log was
  full of 404s that are normal answers: "This question has no AI answer" on every question view while AI answers
  are missing, and "Blog not found" twice per missing story (blog + comments requests).

---

## 2026-10-15 (Phase 9: admin dashboard endpoints)

**Done**
- 9.1 `GET /api/admin/users` (`q` inside name or e-mail, case-insensitive, `%`/`_` literal; `banned=true|false`;
  newest first) returns `AdminUserResponse` with `banned`, `bannedAt`, `banReason`, `roles`. `GET
  /api/admin/users/{id}`: question/answer/published-story counts (hidden posts included), open reports about
  the account, active expert topics (`{id, name}` for the revoke-badge button). Both `USER_READ`.
- 9.2 `GET /api/admin/stats`: users, new users (7 days), banned, questions, new questions (7 days), answers,
  published stories, open reports, pending expert applications, unchecked AI answers on visible questions.
  Any of `USER_READ`, `CONTENT_MODERATE`, `EXPERT_REVIEW`.
- 9.3 `GET /api/admin/privileges` (grantable only), `GET/POST/PUT /api/admin/roles`, `DELETE
  /api/admin/roles/{id}`, `POST/DELETE /api/admin/users/{userId}/roles/{roleId}`; `ROLE_*` privileges.
- Checked live: 9.1 11/11, 9.2 6/6 (counts equal SQL counts), 9.3 19/19 (a granted moderator's existing token
  moderates at once and loses it at once on removal; cannot manage roles; cannot be banned).

**Decisions**
- Only `ADMIN`-type roles are created, edited, deleted, granted or removed through the API; the seeded `USER` and
  `SUPER ADMIN` roles are fixed. `ROLE_*` privileges are never grantable, so role management stays with the
  super-admin and an admin role cannot create more admins.
- Privileges are read from the database on every request (`CustomUserDetailService`), so grants and removals
  apply without a new sign-in. Admin-portal sign-in needs an `ADMIN`/`SUPER_ADMIN` role, so a granted moderator
  can sign in there too.
- Stats are plain `count` queries, no cache: fine at launch size.

**Breaking API changes**
- `GET /api/user/paginated` removed (unused by the web app; its filters were ignored): use `GET /api/admin/users`.

**Known limitations**
- Holding an ADMIN role makes an account unbannable (8.4b rule); remove the role first.
- No audit trail of who granted which role beyond `lastModifiedBy` on the user row.

---

## 2026-10-14 (Phase 8 review fixes, round 3: hiding and restoring)

**Done** — review of 8.5a–c (15 findings), each with a regression test:
- `QuestionAiAnswer.statusBeforeHide`: the threshold hide remembers the status, a dismiss restores it
  (`unhide`), so a confirmed AI answer comes back confirmed and re-indexed; an expert's own flag (no
  remembered status) is never undone. (Was: only never-reviewed AI answers came back.)
- `closeAsActioned` keeps an existing snapshot (`coalesce`): the text saved at hide time survives an author
  deleting the hidden post before the admin confirms.
- `CommentService.findByIdWithException`: 404 unless the comment, its parent and its post (PUBLISHED) are
  visible; `AnswerService` also hides replies under a hidden answer.
- `POST /api/blogs/create` refuses `status: HIDDEN` (400); AI answers of hidden questions cannot be reported;
  `app.moderation.auto-hide-reports` is `@Min(1)` (0 stopped one report from being enough to hide anything:
  startup now fails).
- Moderation notices skip the user who caused them (moderator on their own post, author whose own report
  tipped the threshold).
- `Blog.status` uses `EnumNameConverter`; the boot-time `DROP CONSTRAINT` patch from 8.5a is removed.
- The banned-e-mail check moved into `UserService.validateEmail` (one lookup on sign-up); dead
  `AuthorizationService.isSignedInOwnerOrAdmin` removed.
- Checked live: 14/14 (confirmed AI answer hidden and restored with reviewer and vectors, snapshot kept after
  author delete, comment on hidden blog 404 for read/edit/report, HIDDEN create 400, banned sign-up 403;
  threshold 0 refuses to start) and every Phase 8 run again.

**Not changed, with reasons**
- Threshold race (two reports at the same instant can both count below, or both above, the threshold): the
  worst case is a hide one report late or a duplicate CONTENT_HIDDEN notice. Logged, not locked.
- Hiding a blog drops its vectors before commit: the vector store is the same Postgres, so it cannot be
  down while the report insert succeeds.
- One Hibernate `@Filter` for hidden rows instead of per-path checks: a larger change; the rule lives in each
  service's `findByIdWithException` and list query, pinned by tests.

## 2026-10-14 (Phase 8 review fixes, round 2)

**Done** — a second review (12 findings) of the first round of fixes:
- Reverted `assertOwnerAdminOrModerator`: it let any `CONTENT_MODERATE` holder delete any post, an admin's
  included, through the public endpoints with no snapshot, audit or notice. Public deletes are owner-or-admin
  again; `ModerationService` deletes through package-private `removeForModeration` in the four services.
- A banned account's token is treated as no token in `JwtAuthenticationFilter`: public reads keep working
  and everything else is a 401 (was a 403 on every request, sign-out and public reads included); the refresh
  flow then answers 403 with the ban. `unban` revokes credentials too, so a session created while the ban
  was being applied is not revived.
- An admin deciding their own application is not notified (the null actor had disabled the self-skip).
- Regression tests for the Google pre-link ban refusal and for `deliver` swallowing a failed store.
- Checked live: fix checks 16/16 (moderator 403 outside the queue, decision audited, banned token reads
  public pages and gets 401 elsewhere) and 8.4a, 8.4b, 8.5a–c again.

**Not changed, with reasons**
- "Expert decisions are not audited": verified live as a false alarm; the entity is dirtied after the
  conditional update, so auditing stamps `lastModifiedBy`.
- Ban via `isAccountNonLocked`: Spring checks it before the password, so a banned user would get the
  generic "invalid credentials" instead of the ban message.
- `docs/ROADMAP.md` for review fixes: there is no step row to tick; the dev log records them.

## 2026-10-14 (roadmap 8.5c)

**Done**
- `app.moderation.auto-hide-reports` (env `MODERATION_AUTO_HIDE_REPORTS`, default 3): when that many
  different users have open reports on a blog, question, answer, comment or AI answer,
  `ModerationService.hideForReview` hides it (blog → `HIDDEN`, others → `hidden`, AI answer → `FLAGGED`),
  drops it from the AI index, records its text as `snapshot` on the open reports, and sends the author
  `CONTENT_HIDDEN`. Users are never banned automatically.
- `POST /api/admin/reports/{id}/remove-content` (confirm): deletes the post for good, as in 8.4a.
- `POST /api/admin/reports/{id}/dismiss`: now dismisses every open report on that post (one decision per
  post) and restores it if it was hidden: back in lists and the AI index, author told `CONTENT_RESTORED`.
  Conditional: 409 if another moderator closed them first.
- Checked live: 28/28 (2 reports nothing, 3rd hides; snapshot on all three; vectors out and back; dismiss
  restores and closes all; confirm deletes and reverses reputation; blog hidden and restored; user reports
  never ban), and every Phase 8 run again.

**Decisions**
- Reputation stays while a post is hidden (it may come back) and goes when an admin confirms (delete).
- A dismissed AI answer goes back to `UNCHECKED` only if no expert had reviewed it; an expert-reviewed one
  stays hidden until an admin reopens it (safer for farming advice).

**Known limitations**
- Hiding a blog drops its vectors inside the transaction (like unpublish); a rollback right after would
  leave a published post out of AI answers until its next edit.
- Three accounts can hide any post until a moderator looks; e-mail confirmation and one report per user
  are the brakes. Watch the queue at launch.

## 2026-10-14 (roadmap 8.5b)

**Done**
- `hidden` (default false) on `Question`, `Answer`, `Comment`. `findByIdWithException` in their services
  answers 404 for hidden rows (and for answers on a hidden question), which covers single reads, edits,
  answering, replies, votes and accepts; delete reads the repository directly, so an author or a moderator
  can still delete a hidden post.
- Lists filter it: question list (specification), search, the author's questions, answers on a question,
  replies, comments on a post and their replies; `answerCount` counts visible answers only. The AI answer of
  a hidden question is a 404 and leaves the review queue. `AnswerIndexer` never indexes a hidden answer or
  an answer/AI answer on a hidden question.
- `Question.visibleAcceptedAnswerId` (`@Formula` subselect) replaces reading the accepted answer: found live
  before commit, reading `acceptedAnswer.hidden` hit a lazy proxy outside a session and made every question
  read with an accepted answer a 500.
- Checked live: 19/19 (+2 for visible accepts), and 8.2, 8.4a, 8.5a runs again.

## 2026-10-14 (roadmap 8.5a)

**Done** — moderation redesign (Nayeem, 2026-10-14): a post is hidden once enough users report it and deleted
only when an admin confirms the reports; 8.5a–c. This step sets the visibility rules:
- `BlogService.findVisibleBlog`: `PUBLISHED` for everyone; `DRAFT` for its author only (admins no longer);
  new `HIDDEN` for nobody, not even the author. A hidden post cannot be edited or republished (409
  `ERROR_UNDER_REVIEW`).
- `SchemaPatches.DROP_ENUM_CHECKS` drops `blogs_status_check` before Hibernate runs, so existing databases
  accept `HIDDEN` (new ones get a CHECK that already lists it).
- Comments: create, reply and the per-post list use `findVisibleBlog`. Fixed on the way: anyone could
  comment on, or list the comments of, someone else's draft by its id.
- Sign-up with a banned account's e-mail answers 403 with the ban (sign-in, Google and refresh already did).
- Checked live: 17/17.

**Breaking API changes**
- `GET /api/comments/blog/{blogId}` and comment create/reply answer 404 for a draft that is not yours.
- Admins no longer see other users' drafts.

**Known limitations**
- A banned person can still sign up with a different e-mail address.

## 2026-10-14 (Phase 8 review fixes)

**Done** — a phase-end review of 8.1a–8.4b (21 findings) led to these fixes, with regression tests:
- `AuthorizationService.assertOwnerAdminOrModerator` on the blog/question/answer/comment deletes: a moderator
  role without `ADMIN` (holding `CONTENT_MODERATE`) can remove content (was 403 and a rollback).
- Remove-content on a target that is already gone closes its reports as `ACTIONED` with "(already removed)":
  answers removed with their question, posts deleted by their author, AI answers already hidden. Targets are
  looked up with `EntityManager.find` so no exception marks the transaction rollback-only.
- Bans are enforced per request (`CustomUserDetails.banned`, refused in `JwtAuthenticationFilter`), closing
  the race with a sign-in in flight; Google sign-in refuses a banned account before linking it.
- `dismissIfOpen` and `closeAsActioned` are conditional and set `lastModifiedBy`/`lastModifiedDate` (bulk
  updates skip auditing).
- `ReviewService.hide` only changes the status: `reviewedBy` and any correction stay on record.
- Drafts cannot be reported (404, so their existence is not confirmed); AI-answer reports use
  `existsByQuestionIdAndStatusNot` instead of pulling in `AiService`.
- `APPLICATION_DECIDED` has no actor (admins stay anonymous, as moderators do). `deliver` catches failures,
  so an inline notification never fails a comment reply or stops the expert loop.
- `EnumNameConverter<E>` replaces three copied converters; `Report.targetType` and `status` are converted too.
  Databases created before this keep their old CHECK on those two columns (none deployed; drop by hand).
- `NOTIFICATIONS_TRANSPORT` added to `.env.example` (another value needs its implementation, or startup fails).
- Checked live: 13/13 fix checks (a non-admin moderator removes and is recorded; orphaned report closes;
  reviewer kept; hidden AI answer not reportable; dismiss audited; draft 404; banned live session 403) and
  every Phase 8 run again (12 + 12 + 14 + 11 + 25 + 12).

**Deferred, with reasons**
- A double-tapped report: 500 from the unique constraint (as votes; 4.7 error clean-up).
- An expert's correction still notifies the asker twice (logged in 8.1a).
- `CONTENT_REMOVED` on a removed blog or question links to a page that is gone; the UI shows the type.
- Replies under answers notify nobody (not in the 8.1 trigger list).
- Admin-only service methods rely on `@PreAuthorize`, as `CategoryService` does.
- One transaction per expert notification (`shortcut:` in `AiService.notifyReviewers`).

## 2026-10-14 (roadmap 8.4b)

**Done**
- `User.bannedAt`, `banReason`. `POST /api/admin/users/{userId}/ban {reason}` and `DELETE` on the same path
  (`CONTENT_MODERATE`). A ban revokes every session and refresh token; `AuthService.issueTokens` (the end of
  password, Google and refresh sign-in) refuses a banned account with 403 `ERROR_ACCOUNT_BANNED`, after the
  credentials check, so a wrong password still gets the generic 401. Admin accounts cannot be banned. Open
  reports about the user close as `ACTIONED` ("Banned: <reason>").
- Checked live: 12/12.

**Known limitations**
- A banned user's posts stay up; moderators remove them one by one through their reports.

## 2026-10-14 (roadmap 8.4a)

**Done**
- New privilege `CONTENT_MODERATE` (super-admin role). `GET /api/admin/reports?status=OPEN` (oldest first;
  reporter id and name), `POST /api/admin/reports/{id}/dismiss`, `POST /api/admin/reports/{id}/remove-content`.
- Remove = delete through the owners' delete paths (admin passes owner-or-admin): out of every list, AI
  vectors dropped, earned reputation gone (`ON DELETE CASCADE`), accepted mark cleared. All open reports on
  the target become `ACTIONED` with `snapshot` (the text, ≤ 2000 chars) as evidence; the author gets
  `CONTENT_REMOVED` with no actor. A reported AI answer is hidden (`FLAGGED`, `ReviewService.hide`, no
  reviewer recorded, asker not notified). A `USER` report answers 400: ban instead (8.4b).
- Checked live: 25/25 (an accepted, indexed answer reported twice: both closed, row, accept, 15 points and
  vectors gone, author told; dismiss leaves content; AI answer hidden; comment and question removal; 403
  for non-admins).
- Roadmap 8.4 split into 8.4a/8.4b.

**Decisions**
- Removal, not a restorable hide (decided 2026-10-14 with Nayeem): one small step instead of a `hidden`
  filter on every read path; the snapshot keeps the evidence.

**Known limitations**
- Not restorable. Removing a question also removes the answers others wrote on it.
- Moderators need an admin role: the delete paths check owner-or-admin, not the privilege.

## 2026-10-14 (roadmap 8.3)

**Done**
- `Report`: reporter (`ON DELETE CASCADE`), `targetType` (`BLOG`, `QUESTION`, `ANSWER`, `AI_ANSWER` by its
  question's id, `COMMENT`, `USER`), `targetId` (no foreign key: targets live in several tables), `reason`
  (`HARMFUL_ADVICE`, `SPAM`, `ABUSE`, `FALSE_CREDENTIALS`, `OTHER`; converted, no CHECK), `details` (≤ 500),
  `status` (`OPEN`, `ACTIONED`, `DISMISSED`). Unique per reporter and target.
- `POST /api/reports`: target must exist (404; a hidden or missing AI answer counts as missing), once per
  target (409), not yourself (400).
- Checked live: 11/11.

**Known limitations**
- Two simultaneous reports of the same target by one user: the unique constraint turns the second into a
  500 (as with votes; 4.7 error clean-up).

## 2026-10-14 (roadmap 8.2)

**Done**
- `GET /api/notifications?unreadOnly=&pageNo=&pageSize=` (newest first): `{id, type, targetType, targetId,
  actorId, actorName, read, createdAt}`; `GET /api/notifications/unread-count` → `{count}`;
  `POST /api/notifications/{id}/read` (404 for anyone else's); `POST /api/notifications/read-all` →
  `{updated}`. The first read time is kept.
- Checked live: 14/14.

**Known limitations**
- Polling only (no push/WebSocket); the frontend polls `unread-count`.

## 2026-10-14 (roadmap 8.1b)

**Done**
- `NEW_REVIEW_ITEM` to every active Verified Expert in the topic when an AI answer is stored (after
  generation, and from "ask the experts"); the asker is the actor, so an expert's own question does not
  notify them. `ExpertService.expertsIn(topicId)`.
- `APPLICATION_DECIDED` to the applicant on approve or reject (target `EXPERT_APPLICATION`, actor the admin).
- `COMMENT_REPLY` to the parent comment's author (target `BLOG`).
- Checked live: 12/12 (reply and self-reply, both decisions and no note on a refused second decision, each
  expert notified once, an expert's own question skipped); 8.1a run still 12/12.

**Known limitations**
- One row per expert per new question (`shortcut:` in `AiService.notifyReviewers`); a digest if topics get
  large.

## 2026-10-14 (roadmap 8.1a)

**Done**
- `notification` package: `NotificationPublisher` (the only way services notify), `NotificationMessage`
  (recipient, actor, type, target type and id — plain values any transport can serialize) and
  `SpringEventNotificationPublisher`, active for `app.notifications.transport=spring` (default; env
  `NOTIFICATIONS_TRANSPORT`): publishes an application event, stored by a `@TransactionalEventListener`
  after the commit (`fallbackExecution` for background work without a transaction).
- `NotificationService.store` (own `REQUIRES_NEW` transaction; skips self-notifications) writes
  `Notification` rows: recipient (`ON DELETE CASCADE`), actor (`SET NULL`), `type`, `targetType`
  (`QUESTION`, `BLOG`, `EXPERT_APPLICATION`), `targetId`, `readAt`.
- Triggers: `ANSWER_ON_YOUR_QUESTION` (asker), `ANSWER_ACCEPTED` (answerer), `AI_ANSWER_REVIEWED` (asker, on
  confirm/correct/flag; not on an admin reopen).
- Found live and fixed before commit: `columnDefinition` does not stop Hibernate's enum CHECK constraint;
  `type`/`targetType` use `AttributeConverter`s so new types need no schema change (test pins it).
- Checked live: 12/12 (no CHECK on the table, self-actions and a rolled-back answer notify nobody, re-accept
  does not duplicate, reopen is silent).
- Roadmap 8.1 split into 8.1a/8.1b.

**Decisions**
- Switching to Kafka or RabbitMQ later = one new `NotificationPublisher` implementation (its consumer calls
  `NotificationService.store`) plus `app.notifications.transport=kafka`; no caller changes. Not built now.

**Known limitations**
- In-process delivery: a crash between commit and store loses that notification; one instance only
  (`shortcut:` in `SpringEventNotificationPublisher`).
- An expert's correction notifies the asker twice: a new answer, and the AI answer reviewed.

## 2026-10-14 (Phase 7 review fixes)

**Done** — a phase-end review of 7.1–7.7 (17 findings) led to these fixes, each with a regression test:
- A self-accepted answer is not vetted (anyone could have fed the AI by answering and accepting their own
  question).
- The daily cap on AI answers for questions moved to `AttemptLimiter` (key `ai-question-answers:<userId>`),
  recorded before the model call: deleting questions no longer resets it, parallel questions cannot all pass,
  and failed calls count.
- Approve/reject is one conditional `UPDATE … WHERE status = PENDING` before any badge is granted: a second
  admin gets 409 (was: approved and rejected at once, or a 500).
- `QuestionService.update` publishes `QuestionChanged`: the question's answers and AI answer are re-synced
  (new title; expert vetting follows a topic change).
- Revoking a badge puts the AI answers that expert `CONFIRMED` in the topic back to `UNCHECKED` (and out of
  the index); deleting an expert's correction puts the corrected AI answer back to `UNCHECKED`.
- `indexingExecutor` waits up to 30 s for queued updates on shutdown.
- `CorrectAiAnswerRequest.content` (max 5000, was a 500 above that) and `AskExpertsRequest.title` carry
  `ErrorCode` messages. Review checks read expert topics directly instead of the reputation aggregation;
  a badge change looks up the expert's topics once, not per answer.
- Checked live: 17/17 fix checks (self-accept, topic move out and back, revoke reopens, correction delete
  reopens, 409 on a second decision, over-long correction 400) and all earlier Phase 7 runs again
  (15 + 31 + 37 + 16 + 23).

**Deferred, with reasons**
- Per-IP cap on the AI answers that questions trigger: an IP rule on `POST /api/questions/create` would
  block asking itself; accounts need a confirmed e-mail and each has its own daily cap. Revisit if abuse shows.
- Editing an accepted answer keeps it vetted with the new text (Stack Overflow keeps the accept too);
  reports and hiding (8.3, 8.4) are the remedy.
- Two simultaneous expert applications from one user can both be pending (harmless; admin decides each).
- Admin-only services (approve, reject, revoke, reopen) rely on `@PreAuthorize`, like `CategoryService`.
- Eager tags/users on review-queue and application pages load per row, as on the existing question lists.
- `AiService.ask` and `answerQuestion` keep separate response mapping; they differ in error handling.

**Known limitations**
- The AI-answer cap is in memory: a restart resets it (same as the other limits; 4.8).

## 2026-10-14 (roadmap 7.7)

**Done**
- The AI answer on a question ends its prompt with `Farm details: crop …; season kharif-2; region …; soil …`
  (only the fields the asker filled), after the 1000-character cap on the question text, so the details are
  never cut off and also steer retrieval.
- Unit-tested; not observable live until the Gemini chat key is fixed (same listener path as 7.3).

**Decisions**
- Private asks (`/api/ai/ask`) take no farm details: they have no structured fields; the asker writes them.

## 2026-10-14 (roadmap 7.6b)

**Done**
- `AiSourceResponse` is `{type, blogId, questionId, title}`: `type` `blog` (link to the post) or `answer`
  (link to the question whose checked answers were used). Several vetted answers to one question are one
  citation. Sources stored before (private asks, AI answers on questions) read back as `type: blog`.
- The RAG prompt labels answer excerpts "Community answer to: <question title>" and asks the model to name
  the posts or questions it used.
- Checked live: stored old-format and new-format sources round-trip through the API; 7.5 and 7.6a smoke
  runs still pass. A model call with answer citations awaits the Gemini chat key.

**Breaking API changes**
- Additive, but the frontend must branch on `sources[].type`: answer sources have `blogId: null` and a
  `questionId` (`/api/ai/ask`, `/api/ai/history`, `/api/questions/id/{id}/ai-answer`).

## 2026-10-14 (roadmap 7.6a)

**Done**
- Vetted answers reach the AI: the accepted answer, a direct answer (not a reply) by a Verified Expert in
  the question's topic, and an AI answer an expert `CONFIRMED`. Each is embedded as the question title plus
  the answer's plain text with `type=answer`, `questionId`, `answerId` or `aiAnswerId`, `categoryId` and
  `status=PUBLISHED`, so the existing retrieval filter finds them (related posts still match blogs only).
- Events `AnswerChanged`, `ExpertBadgeChanged`, `AiAnswerReviewed`, `QuestionDeleted`; `AnswerIndexer`
  handles them after the commit on a single `indexing-` thread (`AppConfig.indexingExecutor`;
  `spring.task.execution.mode=force` keeps Boot's executor for the AI answers), re-reads the current state
  and replaces the vectors. `AnswerService.create/update` and `QuestionService.delete` became transactional
  so their events are delivered.
- Found live and fixed before commit: a `readOnly` listener transaction made Postgres refuse the vector
  store's DELETE (nothing was indexed), and Spring refuses to start unless such a listener's transaction is
  `REQUIRES_NEW`; a reflection test pins both. The first version also ran in the request thread after the
  commit, holding two connections during the embedding call; it is async on one thread now.
- Checked live with real embeddings: 23/23 (accept/unaccept, edit replaces, badge grant/revoke re-sync,
  replies never, confirm/reopen, answer and question delete), no vector-store warnings.
- Roadmap 7.6 split into 7.6a/7.6b to keep each PR within 20 files.

**Decisions**
- After-commit and re-read instead of indexing inline: a rolled-back transaction (e.g. a lost correction
  race) cannot leave vectors for rows that do not exist.
- An expert's earlier answers in the topic are indexed when the badge is granted and dropped when it is
  revoked (unless accepted).

**Known limitations**
- Like posts, a failed embedding is logged and healed only by the next change to that answer
  (`shortcut:` in `DocumentService`); the startup re-index (`AI_REINDEX_ON_STARTUP`) covers posts only.
- Editing a question's title leaves the old title in its indexed answers until they change.

## 2026-10-14 (roadmap 7.5)

**Done**
- `POST /api/ai/history/{aiAnswerId}/ask-experts {categoryId, title?}`: the caller's own private ask (404
  for anyone else's) becomes a public question in that topic (title defaults to the first line, cut at 120
  characters); the stored answer becomes its `UNCHECKED` AI answer with its sources, so it appears in the
  experts' review queue. No second model call; the after-commit generator skips questions that already have
  an AI answer.
- `AiAnswer` gains `sources` (jsonb, `[]` for older rows) and `publishedQuestion` (`ON DELETE SET NULL`); a
  conditional update makes a double submit a 409 with nothing posted.
- `GET /api/ai/history` items gain `sources` and `publishedQuestionId` (additive).
- Checked live: 16/16 with seeded private asks (Gemini chat key still invalid): ownership 404, topic
  required, AI answer is the private one with its sources, second publish 409, deleting the question
  frees the ask again; no model call in the log.

**Known limitations**
- The question text is the plain ask text, so line breaks need `white-space: pre-line` in the UI.

## 2026-10-14 (roadmap 7.4)

**Done**
- `QuestionAiAnswer` gains `reviewedBy`, `reviewedAt` and `correction` (the expert's answer; both
  `ON DELETE SET NULL`).
- `GET /api/review-queue`: `UNCHECKED` AI answers in the caller's Verified Expert topics, oldest first, with
  the question title, text and topic (403 for non-experts).
- `POST /api/questions/id/{id}/ai-answer/confirm` and `.../correct {content}` (Verified Expert in the
  question's topic): correct posts the expert's answer on the question through `AnswerService.create` and
  links it as `correctionAnswerId`. `.../flag` (Verified Expert or Top Contributor in the topic) hides it.
  `POST /api/admin/questions/{id}/ai-answer/reopen` (`EXPERT_REVIEW`) puts any decision back to `UNCHECKED`.
- Every move is one conditional `UPDATE … WHERE status IN (…)`: a second reviewer gets 409, and a lost
  correction race rolls back the posted answer too.
- `QuestionAiAnswerResponse` gains `reviewerName`, `reviewedAt`, `correctionAnswerId`.
- Checked live: 37/37 with seeded AI answers (Gemini chat key still invalid): role checks (other topic,
  Top Contributor confirm, revoked expert all 403), double confirm 409, correction posted with the expert
  badge, flag hides, reopen restores, deleting a linked correction or the question works. Also fixed the 7.3
  smoke script, whose question-delete check had used a wrong path and passed vacuously; the cascade is now
  verified.

**Decisions**
- Only `UNCHECKED` answers can be confirmed, corrected or flagged; disagreements go through an admin reopen.
- Reopening a correction keeps the expert's answer as an ordinary answer.

**Known limitations**
- A flagged AI answer is 404 for everyone, admins included; moderators see it through the 8.4 queue.
- Reviewers are not notified of new items yet (8.1).

## 2026-10-14 (roadmap 7.3b)

**Done**
- Removed `POST /api/questions/id/{id}/ai-draft`, `AiDraftAnswerResponse`, `AiService.draftAnswer`,
  `AnswerService.hasAnswers` and `ERROR_QUESTION_ALREADY_ANSWERED`; the path left the per-IP AI rule.

**Breaking API changes**
- `POST /api/questions/id/{id}/ai-draft` is gone (404). The frontend reads
  `GET /api/questions/id/{id}/ai-answer` instead, polling briefly after posting a question.

## 2026-10-14 (roadmap 7.3a)

**Done**
- `QuestionAiAnswer` (one per question, `ON DELETE CASCADE`): answer text, `sources` (jsonb), `status`
  (`UNCHECKED`, `CONFIRMED`, `CORRECTED`, `FLAGGED` — all four created now so the column's CHECK never needs
  widening), token usage.
- `QuestionService.create` is now transactional and publishes `QuestionCreated`; `AiService.answerQuestion`
  (`@Async @TransactionalEventListener`, `@EnableAsync` in `AppConfig`) generates the answer after the commit
  with the same RAG chat client, from the title and the plain text of the question (capped at 1000 chars).
- `GET /api/questions/id/{id}/ai-answer` (public): `{id, questionId, answer, sources, status, disclaimer,
  createdAt}`; 404 until generated, when generation failed, or when flagged.
- Each asker gets at most `app.ai.daily-question-limit` (20) generated answers a day, counted separately
  from private asks; beyond that the question simply has no AI answer.
- Checked live: the listener runs on a task thread after the commit; with the current invalid Gemini chat key
  it logs the failure and the question stays usable (404 on ai-answer). The jsonb `sources` round-trip was
  checked with a seeded row; flagged → 404; deleting the question deletes its AI answer.
- Roadmap 7.3 split into 7.3a/7.3b to keep each PR within 20 files.

**Known limitations**
- A failed generation is not retried (`shortcut:` in `AiService.answerQuestion`).
- Editing a question does not regenerate its AI answer.
- End-to-end generation is unverified live until the Gemini chat key is fixed (open item).

## 2026-10-14 (roadmap 7.2)

**Done**
- `ExpertBadge`: one row per user and topic, with the field title; revoking stamps `revokedAt`, approving
  again clears it. `createdBy` / `lastModifiedBy` record which admin granted or revoked it.
- New privilege `EXPERT_REVIEW` (added to the super-admin role on startup):
  `GET /api/admin/expert-applications?status=PENDING` (oldest first, paged),
  `POST /api/admin/expert-applications/{id}/approve {categoryIds?, note?}` (all requested topics or a subset),
  `POST /api/admin/expert-applications/{id}/reject {note}` (reason required),
  `DELETE /api/admin/users/{userId}/expert-badges/{categoryId}` (revoke). Deciding twice is 409.
- `Badge.VERIFIED_EXPERT` in `authorBadges` on questions and answers in that topic (before
  `TOP_CONTRIBUTOR`). `GET /api/users/{id}/reputation` lists expert topics too, even with 0 points, and each
  topic gains `expertField` (e.g. "Veterinarian", null where not an expert).
- Checked live: 31/31 (403 for non-admins, subset approval, badge on answers/questions only in its topic,
  revoke and re-approve keep one row, applicant sees the note).

**Decisions**
- No separate public "expert badges" endpoint: the reputation endpoint already lists a profile's topics.
- Approving a subset does not record which topics were left out; the badges themselves are the record.

**Known limitations**
- Two admins deciding the same application at the same instant: the second badge insert fails with a 500
  (unique constraint). Rare; revisit with the 4.7 error clean-up.
- Decisions do not notify the applicant yet (8.1).

## 2026-10-14 (roadmap 7.1)

**Done**
- `ExpertApplication`: applicant, 1–3 topics, field title, credential type (`DEGREE`, `STAFF_ID`,
  `FACULTY_PAGE`, `LICENCE`, `OTHER`), institution, up to 4 proof images (must come from `POST /api/images`)
  and 3 `http(s)` links, `status` (`PENDING`, `APPROVED`, `REJECTED`), `reviewNote`.
- `POST /api/experts/applications` (at least one proof; 409 while one is still pending) and
  `GET /api/experts/applications/me` (the caller's own, newest first, paged).
- Checked live: 14/14 (anonymous 401, no proof / bad link / foreign image / bad credential type 400,
  unknown topic 404, second pending 409, other users see nothing).

**Decisions**
- Only one application waits at a time; after a decision the user may apply again (re-apply after rejection).

**Known limitations**
- Proof images live under the public `/uploads/` with unguessable names (`shortcut:` in `ExpertApplication`).
- Two simultaneous submissions can both be saved as pending; the admin decides each (harmless).
- Enum columns carry Hibernate `CHECK` constraints that `ddl-auto=update` never widens: adding a value to a
  stored enum needs a `SchemaPatches` entry until Flyway (4.2).

## 2026-10-13 (Phase 6 review fix)

**Done**
- Phase-end review of 6.1–6.8 found a lost-update race: editing a question or answer, or accepting an answer,
  saved the whole row including the `score` it had loaded, undoing a vote cast in between. `score` is now
  `updatable = false`; only the atomic `UPDATE … score + :delta` changes it (regression test `ScoreColumnTest`).
- Phase 6 live smoke after the fix: 59/59 against the local Postgres.

**Known limitations**
- `Blog.clapCount` has the same pattern (an edit could undo a clap made meanwhile); fix it the same way when
  articles are next touched.

## 2026-10-13 (roadmap 6.8)

**Done**
- Top Contributor badge (`Badge.TOP_CONTRIBUTOR`): 250 points and 5 accepted answers in a topic. Computed on
  read from the ledger (net accepts: a reversal row cancels its accept), so it comes and goes with the points.
- `GET /api/users/{id}/reputation` topics gain `acceptedAnswers` and `badges`. `QuestionResponse` and
  `AnswerResponse` gain `authorBadges`: the author's badges in that question's topic.
- One grouped query (`topicTotals`) now serves totals, profile topics and badges; `totalsFor`/`byTopic` removed.
- Checked live: 4 accepts no badge, the 5th with 250 points earns it, shown on the answer, not on the asker,
  unaccept takes it away. Full Phase 6 smoke run: 58/58.

**Decisions**
- Computed, not stored: nothing to keep in step, and moderation reversals (Phase 8) remove badges for free.
- Questions with no topic never count towards a badge.

**Known limitations**
- Top Contributors cannot flag AI answers yet; that permission comes with the review queue (Phase 7).

## 2026-10-13 (roadmap 6.7)

**Done**
- `GET /api/users/{userId}/reputation` (public): `{total, topics: [{categoryId, categoryName, points}]}`,
  highest topic first; a user with no events gets `{total: 0, topics: []}`.
- `authorReputation` on `QuestionResponse` and `AnswerResponse`. Lists (question pages, search, answers,
  replies) read every author's total with one grouped query; single responses with one small query.
- Checked live: endpoint totals equal the ledger, the topic is named, list and answer authors match.

**Decisions**
- Badges on responses arrive with the first badge (6.8), so the field has a real meaning from the start.

## 2026-10-13 (roadmap 6.6)

**Done**
- `ReputationEvent` ledger (decision 16): user, topic (the question's category), the question or answer that
  earned it, `reason`, `points`. `ReputationReason` holds the PLAN §5 numbers: question upvoted +5, answer
  upvoted +10, downvoted −2, answer accepted +15, accepting an answer +2.
- `ReputationService` is called by `VoteService` and `AnswerService`: a changed or withdrawn vote, a switched
  accept or an unaccept appends the reversal (same reason, negated points) and then the new event.
- Downvoting needs 50 points (403 `ERROR_DOWNVOTE_NEEDS_REPUTATION`); upvoting is open.
- Checked live: +5/+10, up→down nets −2, accept +15/+2, unaccept reverses, events carry the topic, deleting
  the answer removes what it earned.

**Decisions**
- Events are foreign-keyed to their post with `ON DELETE CASCADE`: deleting a post (by its author now, by a
  moderator in Phase 8) takes back exactly what it earned, with no extra code.
- No points for accepting your own answer; the voter earns or loses nothing.
- Totals are summed on read (`shortcut:` in `ReputationService`); cache per user and topic when reads get slow.

**Known limitations**
- Nobody has 50 points on day one, so nobody can downvote until upvotes and accepts accumulate (intended,
  as on Stack Overflow). Launch experts can be seeded points by hand if needed.
- Votes cast before 6.6 (dev data only) have no ledger rows.

## 2026-10-13 (roadmap 6.5)

**Done**
- `Question.acceptedAnswer` (`ON DELETE SET NULL`). `PUT /api/answers/id/{id}/accept` (asker only, 403 for
  anyone else; replies 400) replaces any earlier accept; `DELETE` on the same path takes it back.
- `QuestionResponse.acceptedAnswerId`, `AnswerResponse.accepted`.
- `GET /api/answers/question/{id}`: accepted first, then highest score, then oldest.
- Fixed before commit (found live): deleting the accepted answer failed with a 500 because the loaded question
  still pointed at it; `AnswerService.delete` now clears the accept in the same transaction (regression test).

**Breaking API changes**
- `GET /api/answers/question/{id}` returns direct answers only; replies were mixed in before and now come only
  from `GET /api/answers/replies/{answerId}` (matches `answerCount`).

**Decisions**
- Accepting your own answer is allowed (a self-answer can be the solution); 6.6 gives it no points.

## 2026-10-13 (roadmap 6.4)

**Done**
- `Vote` (one per user and question or answer; unique per pair; `ON DELETE CASCADE` from user, question and
  answer). `PUT /api/questions/id/{id}/vote?direction=UP|DOWN` and `PUT /api/answers/id/{id}/vote?direction=…`
  cast or change a vote; `DELETE` on the same paths withdraws it. Voting on your own post is 403.
- `score` column on questions and answers, moved by an atomic `UPDATE … SET score = score + :delta` (the clap
  pattern); shown on `QuestionResponse` and `AnswerResponse`. Votes answer `{score, myVote}`.
- `GET /api/questions/id/{id}/my-votes`: the caller's votes on the question and each answer, one query.
- Checked live: self-vote 403, idempotent repeat, up→down, withdraw, anonymous 401, deleting a voted thread.

**Known limitations**
- Two simultaneous first votes by the same user on the same post: the unique constraint rejects the second
  with a 500 instead of a clean answer (same as claps). Rare; handle with the 4.7 error clean-up.
- Downvotes are open to everyone until 6.6 adds the 50-point threshold.

## 2026-10-13 (roadmap 6.3)

**Done**
- `GET /api/questions/search?q=` (public): full-text over title and text, best match first, with the article
  search's `'simple'` configuration (Bangla and English alike; quoted phrases and `-exclusions`).
- `QuestionResponse.answerCount`: direct answers only (replies to answers are not counted).
- `GET /api/questions/all?unanswered=true`, combinable with the topic, tag and agri filters.
- Checked live: search, exclusion, blank query 400, answer vs reply counting, the unanswered filter.

**Decisions**
- `answerCount` is a Hibernate `@Formula` subquery, not a stored counter: nothing to keep in step and nothing to
  backfill. `shortcut:` one subquery per listed row; switch to a stored counter if question lists get slow.
- Search is written in HQL (`function('ts_match_vq', …)`) rather than native SQL, so the formula column loads.

**Known limitations**
- No search index yet (same as articles): a GIN index on the tsvector comes with the Flyway baseline (4.2).
- Moderated (hidden) answers still count until Phase 8 adds hiding.

## 2026-10-13 (roadmap 6.2)

**Done**
- Questions take `imageUrls`: up to four photos uploaded through `POST /api/images` (omitted on update keeps
  them). Stored in a `varchar[]` column with a `'{}'` default, so old rows read as an empty list.
- `ImageStorage.isStored(url)` and `ImageService.requireUploaded`: a URL from any other host is refused with
  400, so a question cannot embed a tracking pixel.
- Checked live (local Postgres): the column and `question_tags` are created by `ddl-auto`, foreign and 5+
  photo URLs are refused, the topic/tag filter returns the right question.

**Decisions**
- An array column, not a join table: four strings that are always read with the question.

**Known limitations**
- Article `imageUrl` is still unchecked; apply `requireUploaded` there when articles get the same review.

## 2026-10-13 (roadmap 6.1)

**Done**
- Questions have a topic (`category`, the same categories as articles — decision 12) and up to five tags
  (`question_tags`, the article tag table and normaliser). `GET /api/questions/all` filters by `categoryId` and
  `tag`, combined with the crop/season/region/soil filters.
- `MAX_TAGS_PER_BLOG` became `MAX_TAGS_PER_POST`; the category-required message no longer says "Blog".

**Breaking API changes**
- `POST /api/questions/create` and `PUT /api/questions/update` require `categoryId`; both accept `tags`
  (omitted on update keeps them). `QuestionResponse` gains `category` and `tags`.

**Known limitations**
- Questions asked before 6.1 have no topic (`category: null`); the column stays nullable until the Flyway
  baseline (4.2) can backfill or drop them.

## 2026-10-13 (roadmap 5.11)

**Done**
- `spring.jpa.properties.hibernate.default_batch_fetch_size=50` instead of per-query `@EntityGraph`: a fetch join
  on `tags` would page in memory. Measured live (show-sql): `GET /api/feed/latest` went from 11 statements for
  5 posts to 5 statements, and stayed at 5 with 8 posts (audit A2).
- `spring.jpa.open-in-view=false`. `CommentService` (lazy user/blog/parent) is `@Transactional`;
  `AnswerService.viewReplies` (lazy replies) is `@Transactional(readOnly = true)`.
- Fixed in passing: `AuthorService.profileOf` (5.5) attached a lazy `User` proxy, so blog create threw
  `LazyInitializationException` with open-in-view off. It now loads the user.
- **Live smoke test** on a local pgvector database (scratch script, not committed): sign-up → 403 on a write
  before confirmation → confirmation link from the dev mail log → verify → questions, answers, replies, blog
  draft/publish/update/by-id/by-slug, comments and replies, bookmarks, claps, follows, author profile, search,
  feeds, AI history. No 500s, no lazy-loading errors. Sanitising (5.9) and excerpts (5.10) checked on the live
  responses. 182 unit tests pass.

**Known limitations**
- `contextLoads` and integration tests still need a database in CI (roadmap 4.4); the smoke test was manual.
- The smoke test left test users, questions and posts in the local database.

---

## 2026-10-13 (roadmap 5.10)

**Done**
- `BlogSummaryResponse`: every blog list returns a plain-text `excerpt` (up to 200 characters, cut at a word,
  "…" appended) instead of `content` (audit A1). Covers `/api/blogs/all*`, `/api/blogs/search`,
  `/api/blogs/me/drafts`, `/api/feed/*` and `/api/bookmarks`. `GET /api/blogs/id/{id}` and `/slug/{slug}` keep
  the full `BlogResponse`.
- `SafeHtml.plainText` extracts readable text from HTML.
- Tests: `BlogSummaryResponseTest` (2). 182 unit tests pass.

**Breaking API changes**
- List items have `excerpt` and no `content`. The frontend's story cards should show `excerpt`; the editor must
  load a draft through `GET /api/blogs/id/{id}`.

---

## 2026-10-13 (roadmap 5.9)

**Done**
- `SafeHtml.clean` (jsoup 1.18.3, `Safelist.relaxed()`): keeps the TipTap editor's tags and images, removes
  scripts, event handlers, styles, frames and forms; links keep http(s), mailto or relative targets. Mirrors
  `agrotrends-web/src/lib/sanitize.ts`. Text with no tags is plain text and is stored unchanged.
- `Blog.content`, `Question.content`, `Answer.content`, `Comment.commentContent` are cleaned in
  `@PrePersist`/`@PreUpdate`, so every current and future write path is covered.
- Tests: `SafeHtmlTest` (4), `ContentSanitizingTest` (1). 180 unit tests pass.

**Known limitations**
- Content stored before this change is cleaned only when it is next edited (the frontend still sanitises on
  display).

---

## 2026-10-13 (roadmap 5.8)

**Done**
- `PaidEndpointRateLimitFilter`: per-IP caps across accounts. AI calls (`POST /api/ai/ask`,
  `POST /api/ai/blog-assist`, `POST /api/questions/id/*/ai-draft`, `GET /api/blogs/id/*/related`) share
  `app.ai.daily-ip-limit` (default 200 per day; generous for campus networks). `POST /api/auth/google`: 30 per
  hour. Over the cap: 429 "Too many requests from your network today…" (audit B3).
- `HttpResponse.writeError` writes the error envelope from filters; `JwtAuthenticationFilter` uses it too.
- Tests: `PaidEndpointRateLimitFilterTest` (3). 175 unit tests pass.

**Known limitations**
- Uses `request.getRemoteAddr()`: behind a reverse proxy every request has the proxy's IP. Roadmap 4.1 now
  includes `server.forward-headers-strategy`; deploy must not skip it.
- In-memory counters (single instance only, roadmap 4.8); a restart resets them.

---

## 2026-10-13 (roadmap 5.7)

**Done**
- `JwtAuthenticationFilter` refuses every non-GET request from a user without a confirmed e-mail with 403
  "Confirm your e-mail address first: …". Allowed before confirmation: `/api/auth/**`, `/api/user/update`,
  `/api/user/change-password`, `/api/user/delete`. Covers questions, answers, comments, blogs, claps, bookmarks,
  follows, images and AI in one place, next to the existing must-change-password rule (`refusal(...)`).
- `CustomUserDetails.emailVerified`; the seeded super-admin is created confirmed.
- Tests: two new `JwtAuthenticationFilterTest` cases. 172 unit tests pass.

**Breaking API changes**
- Unconfirmed accounts get 403 on writes. The frontend should show a "confirm your e-mail" banner when
  `UserResponse.emailVerified` is false, with a resend button (`POST /api/auth/resend-verification`).

**Known limitations**
- Admin accounts created before this change are unconfirmed and blocked from writes until they confirm through
  resend (dev databases only).

---

## 2026-10-13 (roadmap 5.6)

**Done**
- Sign-up mails a confirmation link (24 h, single use). `POST /api/auth/verify-email {token}` sets
  `emailVerifiedAt`; `POST /api/auth/resend-verification {email}` always answers 200 and is rate-limited
  (3 per e-mail, 10 per IP, per hour). Google sign-in and a completed password reset already confirm.
- `PasswordResetToken` became `EmailToken` with a `purpose` (`RESET_PASSWORD`, `VERIFY_EMAIL`); token issue,
  hashing, expiry and rate limiting moved to `EmailTokenService`, used by both flows. A mail failure at sign-up
  is logged, not thrown (the account exists; the user can resend).
- `ForgotPasswordRequest` renamed `EmailRequest` (same body), used by forgot-password and resend.
- Tests: `EmailTokenServiceTest` (5), `EmailVerificationServiceTest` (4). 170 unit tests pass.

**API changes (additive)**
- New: `POST /api/auth/verify-email`, `POST /api/auth/resend-verification`. The reset-link error now reads
  "This link is invalid or has expired." The frontend needs a `/verify-email?token=` page.

**Known limitations**
- Tokens move to a new `email_token` table; reset links issued before the deploy stop working (15-minute
  lifetime, so harmless). The old `password_reset_token` table stays until the Flyway baseline (4.2).
- Nothing is gated on confirmation yet; that is 5.7.

---

## 2026-10-13 (roadmap 5.5)

**Done**
- `AuthorService.profileOf(userId)` returns the user's profile and creates it on first need. Used by blog
  create, drafts and `GET`/`PUT /api/authors/me`, so every account can publish (PLAN decision 11).
- Image upload and blog assist lost their author-only gate (still signed-in only; 5.7 adds the confirmed-e-mail
  gate). `ERROR_AUTHOR_PROFILE_REQUIRED` is gone.
- Removed `UserType` (AUTHOR/CONSUMER), `User.userTypes`, `ProfessionalInfoRequest`, `AuthorCreateRequest` and
  the sign-up-time profile creation.
- Tests: `AuthorServiceTest` (2) new; author-gate tests removed; `GoogleSignInTest` updated. 161 unit tests pass.
- Split over `feat/single-account-type` and `feat/drop-user-type`.

**Breaking API changes**
- `POST /api/auth/sign-up` and `POST /api/auth/google`: `userType` and `professionalInfoRequest` are no longer
  read (sent values are ignored). Profile details go through `PUT /api/authors/me`.
- `UserResponse.userTypes` removed. `authorId` is null until the profile is first needed (first post, draft
  list, or profile editor), not "null for readers".
- `POST /api/blogs/create`, `GET /api/blogs/me/drafts`, `GET`/`PUT /api/authors/me`, `POST /api/images`,
  `POST /api/ai/blog-assist` no longer return 403 for non-authors.

**Known limitations**
- Existing databases keep the `users.user_types` column (unused). The Flyway baseline (4.2) should drop it.

---

## 2026-10-13 (roadmap 5.4)

**Done**
- Deleted the `dtos/` package, the ModelMapper bean and dependency, and `CommonUtils.mapEntity*` (audit O4).
- Deleted the commented-out `ForgetPasswordService` with its OTP types (`OTPChannel`,
  `InitiateForgetPasswordRequest`, `InitiateUpdatePasswordRequest`, `ResetPasswordFromSessionTokenRequest`,
  `SendVerificationCodeResponse`), plus `CustomSuccessHandler` and `FileStorageException` (audit O6).
- Removed Thymeleaf: three dependencies, six templates, `spring.thymeleaf.*` (audit O5).
- Split over `chore/delete-dead-code` and `chore/remove-thymeleaf`. 163 unit tests pass.

---

## 2026-10-13 (roadmap 5.3)

**Done**
- `ExceptionHandlingController` returns the `ErrorCode` message directly; it no longer queries `error_codes`
  on every error (audit O1). The latent NullPointerException in `ErrorCodeService` is gone with it (audit B4).
- Deleted the DB-driven localisation stack: `ErrorCodeEntity`, `Language`, `LocalizedText`, `Translation`,
  their repositories, services and request/response types, `CommonUtils.getLocalizedPageable`, and their
  `AppTables` entries. Deleted 100 unused `ErrorCode` constants (audit O3). 163 unit tests pass.
- Split over two branches to keep each PR under 20 files: `chore/delete-i18n-error-stack`,
  `chore/delete-i18n-entities`.

**Known limitations**
- Existing databases keep the empty `error_codes`, `languages`, `localized_texts` and `translations` tables
  (`ddl-auto=update` never drops). The Flyway baseline (4.2) should leave them out.
- The `lang` argument is still threaded through services and `ApplicationException`; removing it is audit O2
  (roadmap **Later**).

---

## 2026-10-13 (roadmap 5.2)

**Done**
- System prompt: removed "Answer in Bangla only if the user explicitly asks for it", which contradicted the
  language rule before it (audit B2). `AIConfigTest` asserts it stays gone. 163 unit tests pass.

---

## 2026-10-13 (roadmap 5.1)

**Done**
- `DocumentService.indexBlog` catches and logs an embedding failure instead of throwing (audit B1). Covers
  create, publish, update (`reindexBlog`) and `reindexAll`, which all go through it. Chunking stays outside
  the catch, so a bug in our own code still surfaces.
- Test: `DocumentServiceTest.embeddingFailureIsSwallowedSoTheSavedPostIsNotReportedAsFailed`. 163 unit tests pass.

**Known limitations**
- A post whose embedding failed stays out of AI answers until its next edit or a start with
  `AI_REINDEX_ON_STARTUP=true` (marked `shortcut:` in the code). On update, the old chunks are already deleted.

---

## 2026-10-13 (direction reset, launch plan)

**Done**
- Product study (`docs/research/product-study-2026-10.md`), then a council review of the founder's vision.
- `docs/PLAN.md` rewritten: positioning, audience, business domains, trust model, launch test, decisions 11–16.
- `docs/ROADMAP.md`: new Phases 5–8 (launch blockers, Q&A + reputation, experts + AI review, notifications +
  moderation); Phase 4 gains Actuator and backups; old Kafka Phase 5 dropped; **Later** list for post-launch.
- `docs/ARCHITECTURE.md`: every launch feature, data model and event flow with Mermaid diagrams.
- Partner-facing launch plan written as a shared doc (non-technical).

**Decisions**
- One account type; a confirmed e-mail before any write (supersedes decision 8).
- Categories are the topics for questions, reputation and expert badges.
- Reputation is an append-only ledger per topic; Top Contributor is computed; Verified Expert is admin-granted
  per topic and revocable.
- Every new question gets a stored AI answer, labelled unchecked; only expert-confirmed or corrected AI answers,
  accepted answers and expert answers reach RAG.
- Notifications are in-process after-commit events (supersedes decision 10, Kafka).
- Launch only after Phases 4–8; the 8-week test then decides on the Academy and paid questions.

**Known limitations**
- Reputation numbers and thresholds are proposals until the team confirms them.

---

## 2026-10-11 (roadmap 2.15b)

**Done**
- `POST /api/auth/google` `{idToken, userType?}` -> the usual `WebTokenResponse` (our JWT + refresh token). The
  account linked to the Google id; else the account with the same e-mail, now linked; else a new account
  (`USER` role, `userType` as at sign-up, default `CONSUMER`; `AUTHOR` also gets an author profile).
- `User`: `googleSubject` (unique), `emailVerifiedAt`; `password` and `mobileNumber` are now optional
  (`SchemaPatches` drops their NOT NULL on existing databases, idempotently).
- Linking an account whose e-mail was never verified drops its password and every session first: whoever
  registered that e-mail may not own it (account pre-hijacking). A completed password reset now sets
  `emailVerifiedAt` (the e-mailed link proves ownership), so such accounts keep their password when linked.
- Admin accounts are refused (same generic 401) **before** any linking; the admin portal stays password-only.
- Change-password on an account with no password: 409 with a hint to use "forgot password" (was a misleading
  "incorrect password"). E-mail change clears `emailVerifiedAt`.
- Fixed in passing: sign-up as `AUTHOR` without `professionalInfoRequest` threw a NullPointerException.
- `UserResponse` gains `hasPassword` and `emailVerified`.
- Tests: `GoogleSignInTest` (8), `UserResponseTest` extended. 162 unit tests pass. Live on a local DB:
  no client id -> 503, forged token -> 401, missing token -> 400.

**API changes (additive)**
- New endpoint `POST /api/auth/google`; new `UserResponse` fields `hasPassword`, `emailVerified`.

**Known limitations**
- Not tested with a real Google credential yet: needs a Google Cloud OAuth client id (`GOOGLE_CLIENT_ID`) and
  the frontend's Google button (Google Identity Services) sending the credential here.
- No per-IP rate limit on `/api/auth/google` (a forged token costs one local signature check).
- Google sign-up has no phone number; anything that later needs one must ask for it.

## 2026-10-11 (roadmap 2.15a)

**Done**
- `GoogleIdTokenVerifier` checks a Google Identity Services ID token locally: signature against Google's JWKS
  (keys cached by Nimbus), issuer `accounts.google.com`, audience = `app.google.client-id`, not expired.
  Returns `GoogleIdentity(subject, email, emailVerified, name)`; its `toString` leaves the e-mail out.
- New dependency `spring-security-oauth2-jose` (version from the Boot BOM). `GOOGLE_CLIENT_ID` in `.env.example`.
- `GoogleIdTokenVerifierTest` (3) with a local RSA key standing in for Google's.

**Decisions**
- ID-token flow (the browser gets the token from Google, the API verifies it), not the server-side
  authorization-code redirect: no client secret, no OAuth session state, and the API keeps issuing its own
  JWT + refresh token. Same design as habit-tracker.
- Blank client id = Google sign-in off (503), so the app still starts without it.

## 2026-10-08 (roadmap 2.14 — my author profile)

**Done**
- Asked for by the frontend (F1.4): the author profile editor had no way to load the current values.
- `GET /api/authors/me` -> `AuthorProfileResponse` for the caller; 403 for readers without an author profile (same as
  `PUT /api/authors/me`), 401 when anonymous.
- `UserResponse.authorId` (null for readers) on sign-up, sign-in, refresh, `/api/user/me` and the admin user list, so
  the client knows whether to show author features and can link to `/api/authors/{authorId}`.
- Tests: `UserResponseTest`, `AuthorProfileServiceTest`. Verified live with a new reader and a new author account.

**Breaking API changes**
- None; `authorId` is a new field. The frontend should regenerate its types (`npm run gen:api`).

---

## 2026-10-08 (roadmap 2.13 — public reading)

**Done**
- Decision (Nayeem): anyone can read; posting anything needs sign-in, like Medium.
- `SecurityConstants.PUBLIC_GET_MATCHERS`, permitted for **GET only**: `/api/blogs/all/**`, `/api/blogs/search`,
  `/api/blogs/id/*`, `/api/blogs/slug/*`, `/api/feed/latest`, `/api/feed/trending`, `/api/categories/all`,
  `/api/tags`, `/api/authors/*`, comments, questions and answers reads. `/api/blogs/all` moved here from the
  all-methods list.
- `AuthorizationService.signedInPrincipal()` / `signedInUserId()` / `isSignedInOwnerOrAdmin()`: anonymous is "nobody",
  not a 401. A draft read anonymously is a **404** (by id and by slug), as for a stranger. The author profile's
  `followedByMe` is false for anonymous visitors.
- Swagger: the token requirement removed from exactly those endpoints.
- Tests: `PublicReadMatchersTest` (which paths are public, which are not), `AuthorizationServiceTest`,
  `BlogServiceTest`, `AuthorProfileServiceTest`. Verified live: 14 anonymous reads 200, drafts 404, 13 private reads
  and writes 401.

**Decisions**
- Still signed-in: the following feed, drafts, bookmarks, my claps, follows lists, AI (`/api/ai/*`, AI drafts) and
  **related posts** — each related-posts call embeds text with a paid model, so opening it would be an unmetered bill.

**Known limitations**
- An expired token on a public GET is treated as anonymous (no 401), so `followedByMe` reads false until the client
  refreshes. The frontend should refresh the access token before it expires (15 minutes).
- No rate limit on public reads yet (Phase 4).

---

## 2026-10-08 (fix: clear 400 messages for bad parameters)

**Done**
- A query/path value that does not convert is a 400 naming the parameter; for an enum it lists the choices:
  `season=WINTER` -> "Invalid value for 'season'. Use one of: RABI, KHARIF_1, KHARIF_2, YEAR_ROUND.",
  `pageNo=abc` -> "Invalid value for 'pageNo'." The rejected value is never echoed.
- A missing required parameter -> "Missing required parameter 'q'."; any other 4xx from Spring MVC says
  "Invalid request." instead of "Server Error Occurred."
- Tests: `ExceptionHandlingControllerTest` (standalone MockMvc). Verified live on `/api/blogs/all`.

---

## 2026-10-07 (roadmap 2.12b — Phase 2 code complete)

**Done**
- Questions: `agri` (`AgriMetadata`) on create/update (omitted on update = keep) and in `QuestionResponse`;
  `GET /api/questions/all` takes optional `crop`, `season`, `region`, `soil` (`AgriSpecifications`).
- Tests: `QuestionServiceTest`. Verified live: create with agri info, filters by crop, season + region, region
  (no match), none.

## 2026-10-07 (roadmap 2.12a)

**Done**
- Roadmap 2.12 split into 2.12a (blogs) and 2.12b (questions).
- `@Embeddable AgriMetadata` (`agri_crop`, `agri_season`, `agri_region`, `agri_soil`); enums `CropSeason`
  (RABI, KHARIF_1, KHARIF_2, YEAR_ROUND — Bangladesh's seasons) and `SoilType`. `AgriInfo` record for requests,
  responses and filters (crop/region trimmed + lowercase, <= 60 chars).
- Blogs: `agri` on create/update (omitted on update = keep) and in `BlogResponse`. `GET /api/blogs/all` takes
  optional `crop`, `season`, `region`, `soil` (Spring Data `Specification`, `AgriSpecifications`).
- Tests: `AgriInfoTest`, `BlogServiceTest`. Verified live: create with agri info, filters by crop (any case),
  season + region, soil (no match), none (all 8).

**Decisions**
- Crop and region are free text (any crop, any district/upazila); season and soil are fixed lists so filters and
  the UI can offer them as choices.

## 2026-10-07 (roadmap 2.11)

**Done**
- `GET /api/feed/latest` (published, `publishedAt` desc), `GET /api/feed/following` (published posts whose author
  the caller follows **or** that carry a tag the caller follows; JPQL `EXISTS` subqueries, no duplicates; empty when
  following nothing) and `GET /api/feed/trending` (published in the last 14 days, `clapCount` desc then newest).
  All paged (`pageSize` clamped).
- `SchemaPatches`: published blogs from before 2.3a get `published_at = creation_date` (feeds sort by it).
- Tests: `BlogServiceTest` (sorts, 14-day window, caller-scoped following). Verified live: 6 old posts back-filled,
  the three feeds, following via a tag only (one post), following nothing (empty).

**Decisions**
- Trending = most claps among recent posts, using the all-time `clapCount`. A time-decayed score needs clap
  timestamps per day; revisit when there is real traffic.

## 2026-10-07 (roadmap 2.10)

**Done**
- `GET /api/authors/{authorId}` -> `AuthorProfileResponse {authorId, name, designation, occupation,
  workPlaceOrInstitution, specialities, bio, profileImageUrl, publishedPostCount, followerCount, followedByMe}` (no
  contact details). The author's posts stay at `GET /api/blogs/all/author/{authorId}`.
- `PUT /api/authors/me` (`UpdateAuthorProfileRequest`, bio <= 2000 chars): authors edit their own profile; others 403.
- `Author.bio` (TEXT) and `Author.profileImageUrl`: sign-up's `professionalStatement` and `profileImageUrl` were
  accepted and silently dropped; now stored.
- `AuthorProfileService` (avoids an `AuthorService` <-> `FollowService` cycle). Tests: `AuthorProfileServiceTest`.
  Verified live: edit own profile, counts (7 posts, 1 follower), `followedByMe` per caller, consumer edit -> 403.

## 2026-10-07 (roadmap 2.9)

**Done**
- `AuthorFollow` (`author_follows`) and `TagFollow` (`tag_follows`), unique per pair, all foreign keys
  `ON DELETE CASCADE`.
- `PUT`/`DELETE /api/authors/{authorId}/follow` (Author id as in `author.authorId`; yourself -> 400) and
  `PUT`/`DELETE /api/tags/{tagName}/follow` (normalised name; unknown -> 404). Idempotent.
- `GET /api/me/following/authors` (`AuthorSummaryResponse`) and `GET /api/me/following/tags` (names), newest first,
  paged. `FollowService.followerCount` for 2.10. `TagService.findByNameWithException`.
- Tests: `FollowServiceTest`, `CascadeOnDeleteTest`. Verified live: follow twice = one row, self-follow -> 400,
  tag by display name, unknown tag -> 404, both lists.

## 2026-10-07 (fix: deleting a commented blog)

**Done**
- Deleting a blog with comments failed (`comment.blog_id` had no `ON DELETE`; surfaced as a vague 400 from the SQL
  handler). `Comment.blog` and `Comment.parentComment` are `@OnDelete(CASCADE)`; `SchemaPatches.cascadeOnDelete`
  rewrites the existing constraints once at startup (idempotent).
- Tests: `CascadeOnDeleteTest`, `SchemaPatchesTest`. Verified live: constraints patched, blog with a comment deleted,
  comments gone, second start is a no-op.

**Known limitations**
- Deleting a *user* who has comments, questions or answers still fails on those foreign keys (account deletion
  needs its own decision: delete or anonymise their content).

## 2026-10-07 (roadmap 2.8)

**Done**
- `Bookmark` (`bookmarks`, unique user + blog, both foreign keys `ON DELETE CASCADE`).
- `PUT /api/blogs/id/{blogId}/bookmark`, `DELETE ...` (both idempotent; a draft that is not yours -> 404) and
  `GET /api/bookmarks` (caller's own, still-published only, most recently saved first). There is no endpoint that
  takes another user's id, so other people's lists cannot be addressed at all.
- Fix (2.7): `claps.blog_id` / `claps.user_id` are `ON DELETE CASCADE` — deleting a clapped blog failed on the
  foreign key. Test: `CascadeOnDeleteTest`.
- Tests: `BookmarkServiceTest`. Verified live: FKs created with cascade, double save = one row, list per user,
  draft -> 404, deleting a blog with a clap and a bookmark succeeds and removes both.

**Decisions**
- One reading list per user (bookmarks). Named lists can come later on the same table.

**Known limitations**
- Databases that ran 2.7 before this fix keep the old `claps` foreign keys (ddl-update does not change them):
  drop the `claps` table once (it is recreated) or alter the constraints.

## 2026-10-07 (roadmap 2.7)

**Done**
- `Clap` (`claps`, unique per blog + user, `clap_count` 1..50) and `Blog.clapCount` (default 0) kept in step by an
  atomic `UPDATE ... SET clap_count = clap_count + :delta`. `BlogResponse.clapCount`.
- `GET /api/blogs/id/{blogId}/claps` -> `{totalClaps, myClaps}`; `POST ...?count=1..50` adds (claps over the cap
  of 50 per reader are ignored, not refused); `DELETE` takes the caller's claps back. Own blog -> 403, draft -> 404,
  count outside 1..50 -> 400.
- Tests: `ClapServiceTest`. Verified live: 30 + 30 -> capped at 50, author -> 403, undo -> 0.

**Known limitations**
- Two first claps by the same reader at the same instant can hit the unique constraint (one gets a 400/500);
  acceptable for now.

## 2026-10-07 (roadmap 2.6)

**Done**
- `POST /api/images` (multipart `file`) -> `{url}`: authors only (403), JPEG/PNG/WebP detected by magic bytes
  (`ImageType`), max 5 MB (`ImageService.MAX_IMAGE_BYTES`; multipart limit 6 MB so the service answers; larger
  requests also get the 400 via `handleMaxUploadSizeExceededException`).
- Port `ImageStorage`; `LocalDiskImageStorage` writes `<uuid>.<ext>` under `app.storage.local-dir`
  (`STORAGE_DIR`, default `uploads`, git-ignored) and returns `app.storage.public-base-url` + name
  (`UPLOADS_PUBLIC_BASE_URL`, default `{backendUrl}/uploads`). `/uploads/**` is served read-only and public.
- Tests: `ImageStorageTest` (detection, storage), `ImageServiceTest` (author-only, SVG disguised as PNG, empty,
  oversized). Verified live: upload + public download (bytes identical, `nosniff`), SVG -> 400, 7 MB -> 400,
  consumer -> 403, `..%2f` traversal -> 400.

**Found and fixed while testing**
- A second `@ExceptionHandler(MaxUploadSizeExceededException)` made startup fail (ambiguous with
  `ResponseEntityExceptionHandler`); the shipped code overrides its hook instead.

**Known limitations**
- Local disk only works on one server; behind a load balancer use an object store (new `ImageStorage` impl).
- Images are stored as uploaded: no resizing and no EXIF/GPS stripping yet (a phone photo can reveal a farm's location).
- Unused uploads are never deleted.

## 2026-10-07 (roadmap 2.5)

**Done**
- `Blog.content` and `AiAnswer.aiAnswer`: `@Lob` (Postgres large object) -> `TEXT`. `SchemaPatches` converts existing
  databases at startup, **before** Hibernate (`SchemaPatchesOrder` = `EntityManagerFactoryDependsOnPostProcessor`):
  `oid` -> `text` via `convert_from(lo_get(...))`, frees the old large objects, `ai_answers.question` varchar -> text
  (replaces the manual `ALTER` from 3.6), and creates the GIN index `blogs_search_idx`. Idempotent.
- `GET /api/blogs/search?q=&pageNo&pageSize`: published blogs, `websearch_to_tsquery('simple', q)` over title +
  content, ranked by `ts_rank`; supports `"phrases"` and `-exclusions`; blank `q` -> 400.
- Tests: `SchemaPatchesTest`, `BlogServiceTest` (search). Verified live on the test DB in its pre-2.5 state:
  11 blog bodies and 8 AI answers converted with their text (incl. Bangla), large objects freed, second start is a
  no-op, searches for a word, two words, a phrase and an exclusion behave; Bangla tokens match in SQL.

**Found and fixed while testing**
- First attempt ran the patch after Hibernate: Hibernate's `ddl-auto=update` changed `oid` to `text` itself and
  copied the large-object **ids** ("24591") into the column. The test DB was restored from the still-existing
  large objects; the shipped patch runs before Hibernate. Never run an older build of this branch on real data.

**Decisions**
- `'simple'` text configuration: no stemming (so "disease" does not match "diseases"), but identical behaviour for
  Bangla and English. A per-language configuration can come later.
- The startup patch is a stop-gap until Flyway (4.2), which must start from the post-patch schema.

## 2026-10-07 (roadmap 2.4)

**Done**
- `Tag` (`tags.tag_name`, unique, normalised: trimmed, lowercase, single spaces) and `blog_tags` join table;
  `Blog.tags` eager (at most five per post).
- Create/update accept `tags` (max 5, each <= 40 chars, 400 otherwise). Omitted on update = keep current tags.
  `BlogResponse.tags` (sorted names).
- `GET /api/blogs/all/tag/{tagName}` (published only, case-insensitive) and `GET /api/tags?q=` (up to 20 names by
  prefix, for autocomplete).
- Tests: `TagServiceTest`, `BlogServiceTest` (keep/replace on update). Verified live: tags normalised and
  de-duplicated, filter by "RICE BLAST" works, suggestions for "ri", 6 tags -> 400.

**Decisions**
- Tags are free-form (authors create them by using them); 3.8b's AI suggestions feed straight into this field.

## 2026-10-07 (roadmap 2.3b)

**Done**
- `Blog.slug` (unique): `Slugs.base(title)` (NFKC, lowercase, keeps Unicode letters/marks/digits — Bangla titles
  stay readable; max 60 chars) + `-` + 6 random `[a-z0-9]`; retried on collision; never changes after creation.
- `GET /api/blogs/slug/{slug}` (same visibility as by id). `BlogResponse` adds `slug` and `readingTimeMinutes`
  (words / 200, at least 1; computed, not stored).
- `BlogSlugBackfillRunner` gives existing posts a slug at startup (idempotent).
- Verified live: 9 existing posts got slugs, unique index created, lookup by slug works, a Bangla title gave
  `ধানের-ব্লাস্ট-রোগ-9ivkqc`.

**Decisions**
- Random suffix instead of the database id: unique without a second save, and ids are not exposed in URLs.

## 2026-10-07 (roadmap 2.3a)

**Done**
- Roadmap 2.3 split into 2.3a (status) and 2.3b (slug, reading time).
- `Blog.status` (`BlogStatus` DRAFT/PUBLISHED, column default `'PUBLISHED'` so existing rows stay public) and
  `publishedAt` (first publish). `CreateBlogRequest.status` optional, default PUBLISHED (as before drafts existed).
- `POST /api/blogs/id/{id}/publish` and `/unpublish` (owner or admin; 409 if already in that state);
  `GET /api/blogs/me/drafts` (authors). Lists show PUBLISHED only; `GET /api/blogs/id/{id}` and `/related` give 404
  for someone else's draft (`AuthorizationService.isOwnerOrAdmin`).
- Vectors: only published posts are embedded (create/update/publish index; unpublish deletes; re-index deletes
  drafts' chunks). Chunks carry `status`; RAG retrieval and related posts filter on `status == 'PUBLISHED'`.
- Fix: `BlogRepositories` is `@Transactional(readOnly = true)`. Paged derived queries failed with "Large Objects
  may not be used in auto-commit mode" (`Blog.content` is a `@Lob`) — this hit the 2.2 category/author lists too.
- Verified live on the test DB: existing rows PUBLISHED; draft has no vectors and is missing from the public list;
  publish -> 1 chunk with `status: PUBLISHED`, publishing again -> 409; unpublish -> chunks gone; lists by
  category/author work.

**Breaking API changes**
- Blog payloads gain `status` and `publishedAt`. Lists exclude drafts; someone else's draft is a 404.

**Known limitations**
- Chunks embedded before this step have no `status` and are now invisible to retrieval: start once with
  `AI_REINDEX_ON_STARTUP=true` after deploying.
- Comments can still be posted on a draft by anyone who knows its id (comments are reworked later).

## 2026-10-07 (roadmap 2.2)

**Done**
- `BlogRepositories.findAllByCategory/findAllByAuthor(…, Pageable)`: real database paging (was load-all + `PageImpl`).
- `CommonUtils.getPageable(args, sortableFields, lang)`: `sortBy` outside the allowlist -> 400
  `ERROR_INVALID_SORT_FIELD`. Allowlists: blogs and questions `creationDate, lastModifiedDate, title`;
  categories `creationDate, categoryName`; admin user list `creationDate, name, email`. List endpoints take `lang`.
- Tests: `CommonUtilsTest` (400, allowed, empty), `BlogServiceTest` (author paging by Author id).

**Breaking API changes**
- `GET /api/blogs/all/author/{authorId}` takes the **Author** id (`author.authorId` in blog responses); it used to
  take the author's user id.
- An unknown `sortBy` is a 400 (was a 500).

**Known limitations**
- Admin-only lists (roles, languages, error codes) still accept any `sortBy`; they are admin-only and out of scope.

## 2026-10-07 (roadmap 2.1c)

**Done**
- `QuestionResponse`, `AnswerResponse`, `CommentResponse` gain `authorName` (the user's display name, never
  `createdBy`, which holds the e-mail), `createdAt`, `updatedAt` as ISO-8601 `Instant`s.
- Tests: `QuestionServiceTest` (name + 24-hour-safe timestamp), `CommentServiceTest` (name + timestamp).

**Breaking API changes**
- Question `createdAt`/`updatedAt` change from `"dd-MM-yyyy hh:mm:ss"` strings to ISO-8601 UTC.

## 2026-10-07 (roadmap 2.1b)

**Done**
- `UserResponse {id, name, email, mobileNumber, userTypes, roles (names), mustChangePassword, createdAt}` for the
  caller's own account: `/api/user/me`, `/api/auth/sign-up`, `sign-in`/`refresh-token` (`payload.user`), and the
  admin list `/api/user/paginated`.
- `/api/user/id/{id}` returns `PublicUserResponse {id, name}`. Closes the open item "returns the full User entity".
- Tests: `UserResponseTest`.

**Breaking API changes**
- `payload.user` in sign-in/refresh and the sign-up payload: roles are role-name strings (were role objects with
  privileges); audit fields replaced by `createdAt`. `/api/user/id/{id}` is now `{id, name}` only.

## 2026-10-07 (roadmap 2.1a — Phase 2 starts)

**Done**
- Phase 2 base `feat/medium-core-base`, branched from the end of Phase 3 (`feat/ai-draft-answer`) because Phase 2
  builds on its code (e.g. 2.3 puts `status` into the vector metadata). Merge Phase 3 into `development` first.
- Roadmap 2.1 split: 2.1a (Blog/Category), 2.1b (User); the old 2.1b (Q&A/comment timestamps) is now 2.1c.
- Blog endpoints return `BlogResponse {id, title, content, imageUrl, category {id, categoryName},
  author {authorId, name}, createdAt, updatedAt}`; category endpoints return `CategoryResponse {id, categoryName}`.
  `BlogService.getById` for the controller; `findByIdWithException` stays for other services.
- `CommonUtils.getPageable` clamps `pageSize` to 1..100 and `pageNo` to >= 0. `CommonUtils.toInstant`.
- Tests: `BlogResponseTest` (author by name only), `CommonUtilsTest` (clamping).

**Breaking API changes**
- Blog payloads: no `createdBy`/`lastModifiedBy`/`creationDate`/`lastModifiedDate`; `createdAt`/`updatedAt` are
  ISO-8601 UTC. `author` is `{authorId, name}` (was the whole `Author` with its `user`). `category` is
  `{id, categoryName}`. Category payloads lose the audit fields.

## 2026-10-07 (roadmap 3.8c — Phase 3 code complete)

**Done**
- `POST /api/questions/id/{questionId}/ai-draft` -> `{label: "AI draft - not reviewed by an expert", answer, sources}`.
  404 for an unknown question, 409 once it has any answer. Reuses `AiService.ask` (title + content, cut to 1000
  chars), so it counts against the caller's daily limit and shows in their history.
- `AnswerService.hasAnswers` (`existsByQuestionId`). Tests in `AiServiceTest`.
- Verified live: app starts with the new wiring; unknown question -> 404; new question -> 503 (chat key rejected).

**Decisions**
- The draft is returned to the caller only, not stored as an `Answer`: showing AI text as a community answer
  needs an "AI-generated" flag and moderation, which no step covers yet.

**Known limitations**
- Phase 3 chat features (3.4–3.8) need a live re-check once the Gemini chat key works (see Open items).

## 2026-10-07 (roadmap 3.8b)

**Done**
- `POST /api/ai/blog-assist` `{title, content}` -> `{summary, suggestedTags}` for the writing screen. Authors only
  (same rule as publishing: 403 without an `Author` profile), content cut at 12 000 characters, structured output
  via `ChatClient.entity(BlogAssistResponse.class)`, 503 when Gemini fails.
- `BlogAssistService` builds its own chat client (system prompt, no retrieval advisor) from the prototype
  `ChatClient.Builder`. `BlogAssistServiceTest` covers author-only, truncation, 503 and pass-through.

**Known limitations**
- Not verified live (chat key rejected). No per-user quota on this endpoint yet (author-only limits who can call it).
- Tags are suggestions only; they become real tags with 2.4.

## 2026-10-07 (roadmap 3.8a)

**Done**
- Roadmap 3.8 split into 3.8a (related posts), 3.8b (summary + suggested tags), 3.8c (AI draft answer).
- `GET /api/blogs/id/{blogId}/related?limit=` (1..10, default 5) -> `[{blogId, title}]`: vector search with the
  post's title + first 2000 characters, filter `blogId != this`, cosine >= 0.75
  (`app.ai.related-similarity-threshold`), one entry per post in similarity order.
- Verified live (embeddings work with the current key): "Rice blast in short" <-> "Stopping potato late blight"
  (0.76); the cow-feeding and drip-irrigation posts have no related post.

**Known limitations**
- Each call embeds the post's opening again (one embedding request per view); cache it if traffic grows.

## 2026-10-07 (roadmap 3.7)

**Done**
- `GET /api/ai/history?pageNo&pageSize`: the caller's own `{id, question, answer, askedAt}` (`askedAt` is an ISO
  `Instant`), newest first, Spring `Page` in `payload`. Identity from the token; no user id in the URL.
- `CommonUtils.clampedPageable` + `AppConstants.MAX_PAGE_SIZE = 100` (2.2 reuses them). The unused
  `AiService.getAllByUserId(…, userId)` and its unpaged repository method are gone.

**Known limitations**
- History does not include the sources of each answer (they are not stored).

## 2026-10-07 (roadmap 3.6)

**Done**
- `AiProperties` (`app.ai.*`): `daily-question-limit` (20, env `AI_DAILY_QUESTION_LIMIT`), `timeout` (60 s),
  `rag.top-k`, `rag.similarity-threshold`. Own Gemini `Client` bean with an HTTP timeout; `spring.ai.retry.max-attempts=2`.
- `AskQuestionRequest`: `@NotBlank`, `@Size(max = 1000)` (400). Quota: answered questions since local midnight;
  the 21st is a 429 before Gemini is called. Gemini failures -> 503 `ERROR_AI_UNAVAILABLE`, cause logged.
- `AiAnswer.promptTokens`/`completionTokens`; `ai_answers.question` mapped as `TEXT`. Unused `CreateAiResponseRequest` removed.
- Verified live without a working chat key: blank -> 400, 1001 chars -> 400, 21st question -> 429 with no
  Gemini call, failure -> 503 and nothing stored. Unit tests: `AiServiceTest` (quota, 503, token usage).

**Known limitations**
- Not verified live (chat key rejected since 2026-10-06 evening): the 60 s timeout actually applying (a 1 ms
  override did not reach the app, and the client's key fingerprint differed from the configured one — check that
  the chat model uses the `googleGenAiClient` bean) and token columns being filled.
- Existing databases need `ALTER TABLE ai_answers ALTER COLUMN question TYPE text;` once (ddl-auto=update does
  not change column types; Flyway arrives in 4.2).
- The quota counts per server-local day and is not atomic: parallel requests can exceed it by a few.

## 2026-10-06 (roadmap 3.5)

**Done**
- `/api/ai/ask` payload is now `AiAnswerResponse {answer, sources: [{blogId, title}]}`: one source per retrieved
  post (chunks of the same post collapse), in relevance order; `[]` when nothing matched.
- The chat call moved from `AiChatController` into `AiService.ask`; the caller id comes from
  `AuthorizationService.currentUserId` (token). `AiServiceTest` covers sources, de-duplication and storing the answer.
- Verified live: a rice + potato question returned both posts as sources; the mango question returned `[]`.

**API changes (additive)**
- `payload.sources` is new; `payload.answer` is unchanged.

**Decisions**
- `sources` lists the posts given to the model as context (what the answer was grounded on), not a parse of the
  titles the model happened to mention.

**Known limitations**
- `AskQuestionRequest.question` has no validation (blank/huge prompts reach Gemini) — 3.6.
- Latency up to 91 s measured; no timeout yet (3.6).

## 2026-10-06 (roadmap 3.4)

**Done**
- `RetrievalAugmentationAdvisor` is a default advisor on the chat client: top 5 chunks with cosine similarity
  >= 0.7 (`app.ai.rag.top-k`, `app.ai.rag.similarity-threshold`). Excerpts go in as `Post: <title>` blocks,
  framed as user-written reference material whose instructions are ignored; the model names the posts it used
  and answers in the question's language. With no match the question is passed through unchanged.
- New dependency `spring-ai-rag` (version from the Spring AI BOM).
- Fix: chat model `gemini-2.5-flash` -> `gemini-3.8-flash`. Google returns 404 "no longer available to new users"
  for the rotated key, so `/api/ai/ask` was a 500 before this step. `EmbeddingConfigTest` became `AiModelConfigTest`.
- Verified live: rice-blast question answered from and naming "Rice blast in short"; mango question (no post)
  answered generally; a Bangla question retrieved the English cow-feeding post and was answered in Bangla;
  a "what is the admin password" probe was refused.

**Decisions**
- `RetrievalAugmentationAdvisor` over `QuestionAnswerAdvisor`: its augmenter can pass a question through when no
  post matches (general advice still works) and takes a custom document formatter (titles for citations).
- Threshold 0.7 from measurement with gemini-embedding-2: question vs matching post 0.80–0.84, vs unrelated
  farming posts 0.60–0.66 (rice blast vs potato blight 0.73), off-topic ~0.55. Re-check on real data.
- `gemini-3.8-flash` was named by Google's own error message and confirmed working; Nayeem may prefer another model.

**Known limitations**
- Drafts are retrievable until 2.3 adds status and the filter.
- Answers took 12–67 s; no timeout yet (3.6). Sources are named in the text only; structured citations are 3.5.

## 2026-10-06 (roadmap 3.3)

**Done**
- `DocumentService.reindexBlog` deletes a blog's chunks (filter `blogId == id`) before indexing it again;
  `deleteBlog` runs after the blog row is deleted. `BlogService.update`/`delete` use them.
- One-off backfill: `AI_REINDEX_ON_STARTUP=true` (`app.ai.reindex-on-startup`) makes `BlogReindexRunner`
  re-embed every blog, 50 per page, then log the count. Off by default; documented in `.env.example`.
- Tests: `BlogServiceTest` (update replaces, delete order, a stranger's delete keeps vectors, re-index walks
  every page); `DocumentServiceTest` (delete-before-add, delete by filter).
- Verified live: re-index gave the 3.1 test blog full metadata; updating a 4-chunk blog left 1 new chunk;
  deleting a blog removed its chunks.

**Decisions**
- Backfill is a start-up switch, not an HTTP endpoint: no new API surface or privilege to secure, and it is
  rarely needed (only after a model change).
- Unpublish needs a blog status, so it moved into 2.3.

**Known limitations**
- Replace is delete-then-add, not atomic: if embedding fails mid-update, the blog has no vectors until the
  next update or a re-index. The re-index also runs while the app already serves requests.
- Vectors of blogs deleted before this change stay in `vector_store` (none in the fresh local DB).

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
