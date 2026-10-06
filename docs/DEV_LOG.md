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
- Old `dev` branch reviewed and deleted (2026-10-06). Phase 3 started early (base `feat/ai-rag-base`): 3.1–3.5 done; `/api/ai/ask` answers from the platform's posts and returns them as sources.

## Next up

1. Merge `feat/rag-citations` into `feat/ai-rag-base` (Phase 3 base, off `development`).
   Next step: 3.6 (quota, prompt length cap, timeout; token usage) — answers take up to 91 s.
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
| Any database that still holds `text-embedding-004` vectors: start once with `AI_REINDEX_ON_STARTUP=true` | whoever owns that DB | retrieval on old data |
| Client errors (400/401) are logged at ERROR by `ExceptionHandlingController` — noisy | roadmap 4.7 | — |
| Frontend can show `sources` from `/api/ai/ask` as links to `/api/blogs/id/{blogId}` | frontend | — |
| Frontend must stop sending `userId` / `authorUserId`, use `/api/auth/refresh-token`, and handle 401 vs 403 | frontend | frontend integration |
| `contextLoads` fails without a local Postgres | roadmap 4.4 | CI |
| No SMTP configured: reset links are written to the log (dev only) | an SMTP account | production password reset |
| Hotfix path is not defined yet (proposal: branch off `production`, PR into `production`, then back-merge into `development`) | Nayeem to confirm | — |
| Gemini chat calls fail with "API key not valid" since 2026-10-06 evening (embeddings still work); `.env` key is 53 chars (a key is 39) — check the line for quotes/comments | Nayeem | live checks of 3.6–3.8 |
| After deploying 2.3a: start once with `AI_REINDEX_ON_STARTUP=true` (old chunks have no `status`, so retrieval ignores them) | whoever deploys | AI answers on existing posts |
| The audit PDF (`AgroTrends-Code-Audit.pdf`) is intentionally not committed | — | — |

---

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
