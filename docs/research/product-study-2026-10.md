# AgroTrends — Product study and code audit

**Date:** 2026-10-11 · **Scope:** market and product fit, monetisation, AI roadmap feasibility, backend code audit
(ponytail lens: bugs, over-engineering, dead code, architecture) · **Assumed market:** Bangladesh first (the
product is Bangla + English).

---

## 0. The verdict in one screen

| Question | Short answer |
|---|---|
| Is the product good? | **The engineering is good; the product thesis is weak.** The backend is clean, tested (152 unit tests pass), secure by design, and already does what most competitors don't: AI answers grounded in local posts, with citations, in Bangla. |
| Does it solve a real problem? | **Yes, but not the one the name says.** The real problem is *"I have a farming problem right now and nobody trustworthy to ask"*. It is not *"I want to read long articles about farming"*. |
| Will users pay? | **Smallholder farmers: almost no.** Evidence from comparable markets shows steep price sensitivity and about half of farmers unwilling to pay at all. **Commercial growers, agri-entrepreneurs, input companies and NGOs: yes**, for specific outcomes. |
| Can it earn money as a side project? | **Not as "Medium for agriculture" with reader subscriptions.** It is possible as an *ask-an-expert + AI advisor* with B2B/NGO money or sponsorship, but only if you solve **distribution**, which is now the hardest problem and is not a coding problem. |
| Is it needed? | Some of it. A knowledge base that an AI cites is needed; a **blog-first reading app is not**. Reposition the blogs as the *fuel* for answers, not the product. |
| Biggest risk right now | **Zero users and not deployed.** Phase 4 (Docker, Flyway, CI, deploy) is untouched while 41 feature steps are done. The next feature is worth less than the first 50 real users. |

---

## 1. What the product offers today (honest inventory)

Backend `development` @ `054ca0c`, frontend `agrotrends-web` F0–F6 done (F6.7 notifications pending).

- **Publishing:** blogs with drafts, slugs, reading time, tags, images, categories, agriculture metadata (crop,
  season, region, soil), full-text search, claps, bookmarks, follows, public author profiles, home feeds
  (latest / following / trending).
- **Community Q&A:** questions with agri metadata, threaded answers and comments.
- **AI (the differentiator):** `/api/ai/ask` uses RAG over published blogs, returns citations, has a daily quota
  and history; plus related posts, draft-assist (summary + tags) and a labelled AI draft answer for unanswered
  questions.
- **Trust & safety:** token sessions with refresh rotation, owner-or-admin checks, rate-limited sign-in, password
  reset by mail.
- **Not there:** deployment, notifications, moderation, expert verification, email verification, answer
  voting / accepted answers, image-based diagnosis, voice.

**What it gives a user's time:** a reader gets curated, local, Bangla-capable articles. A person with a
problem gets an AI answer that cites local posts. A writer gets an audience (if one exists). Only the second
of those is clearly better than what they already use (Facebook groups, YouTube, the 16123 call centre,
dealers).

---

## 2. Market reality

### 2.1 Reach: who can even use a web app

- GSMA 2024: only **26 % of rural Bangladeshis own a smartphone**, 27 % use mobile internet; Bangladesh ranks
  near the bottom of 12 lower-middle-income countries. Illiteracy blocks 19 % of rural non-users.
  ([TBS](https://www.tbsnews.net/node/1013061), [Financial Express](https://thefinancialexpress.com.bd/national/bd-bottom-among-12-lmics-in-usage-of-phone-net))
- BBS ICT survey 2025–26: individual internet use **48.9 %**; **68.8 % of rural households** have at least one
  smartphone (often the son's or daughter's, not the farmer's).
  ([MEA Tech Watch](https://meatechwatch.com/?p=42188))

**Implication:** the smallholder farmer is mostly reached *through someone*: a child, a dealer, an extension
officer (SAAO), an NGO field worker. A text-heavy reading product reaches the intermediaries, not the farmer.

### 2.2 Reading is losing to answers and video

- Search clicks are collapsing: Pew (2025) found users click a result **8 %** of the time when an AI Overview
  is shown vs 15 % without; Chartbeat/Reuters Institute measured Google referrals to publishers **down 33 %**
  globally in the year to Nov 2025; zero-click searches rose 56 % → 69 % (Similarweb).
  ([Digiday](https://digiday.com/media/in-graphic-detail-ai-platforms-are-driving-more-traffic-but-not-enough-to-offset-zero-click-search/),
  [Campaign](https://campaignlive.com/article/1-click-through-rate-google-ai-overviews-killing-publishers/1927692),
  [Yahoo/Pew coverage](https://www.yahoo.com/news/google-ai-summaries-increase-frequency-223953043.html))
- Bangladeshi farmers who use YouTube report higher knowledge and income (2026 study, 276 farmers,
  Chattogram; correlational). Farmer-to-farmer video raised rice yields 15 % in a controlled study.
  ([J. Agric. Sci. 2026](https://asistent.ceon.rs/index.php/jas/article/view/64256),
  [Access Agriculture](https://Accessagriculture.org/sites/default/files/1._summary_Chowdhury_et_al_2011_ENGLISH_0.pdf))

**Implication:** your instinct is right. A blog platform that relies on SEO traffic is entering its worst
decade. But *being the source an AI cites* and *answering directly* are both getting more valuable.

### 2.3 Demand for trusted answers is large and under-served

- The government Krishi Call Centre (16123) handled **92,094 calls** in July 2025 – June 2026
  ([BSS](https://www.bssnews.net/special-stories/401845)). That is tiny next to roughly 16.5 million farm
  households: the demand exists, but the channel doesn't scale.
- Krishoker Janala (government) covers 1,000+ diseases of 120 plants, but it is a static lookup.
  ([South-South Galaxy](https://my.southsouth-galaxy.org/en/solutions/detail/krishoker-janala-farmers-window))
- Digital Green's FarmerChat (India, Ethiopia, Kenya...) proves the model you are building: generative AI
  grounded in trusted local content, voice-first, with expert review. OpenAI's estimate is that it cuts
  extension cost from **$35 to $0.35 per farmer**.
  ([AI Kosh](https://aikosh.indiaai.gov.in/home/use-cases/details/farmerchat_ai_powered_agricultural_advisory_at_scale.html),
  [Analytics India](https://analyticsindiamag.com/openai-is-helping-farmers-in-india-increase-crop-yields/))

### 2.4 Competitors and substitutes

| Substitute | What it does well | Where AgroTrends can win |
|---|---|---|
| Facebook gardening/farming groups (e.g. 87k-member plant groups) | Free, fast, social, where people already are | Searchable, organised, answers don't vanish in a feed; AI that cites |
| YouTube farming channels | Video, trust in faces, no literacy needed | Specific answers to *my* crop and *my* region |
| 16123 call centre / SAAO | Human, official | Available 24/7, scales, remembers history |
| Plantix (10 M users, 80 % India; free, funded by API sales and input retail; acquired by HELM AG 2023) | Photo diagnosis | Local Bangla knowledge, community, not tied to selling chemicals |
| Krishoker Janala | Official, offline | Conversational, current, community-updated |
| ChatGPT / Gemini apps | Already free and good in Bangla | **Local, cited, vetted** answers; a human expert behind them |

The last row is the existential one: a general chatbot answers "how do I treat rice blast" well enough for
free. AgroTrends only wins if its answers are **more local and more trustworthy** (verified experts, cited
local posts, region and season aware) and if it is **where the user already is**.
([Plantix: FAO](https://agritechobservatory.review.fao.org/en/plantix-crop-doctor),
[Dealroom](https://app.beta.dealroom.co/companies/peat_gmbh_plantix_app),
[Daily Star gardening groups](https://epfs.thedailystar.net/TDS/2022/03/26/Bangladesh/MAI/5_04/726fbe042ff1be93_04.pdf))

---

## 3. Will anyone pay? Can it be a side business?

### 3.1 Evidence on willingness to pay

- Ghana (Vodafone Farmers Club, IFPRI): **85 %** would subscribe at 1 GHC/month, **50 %** at 2 GHC, **19 %** at 3 GHC.
  Women's WTP is markedly lower. ([AgEcon](https://ageconsearch.umn.edu/record/277451?ln=en))
- Kenya (agro-weather SMS): just over half would pay; average WTP about **$0.58/month**, rising with farm size.
  A 2022 global survey: ~50 % of farmers unwilling to pay for digital tools.
  ([UC Davis MRR](https://basis.ucdavis.edu/sites/g/files/dgvnsk466/files/2024-01/MRR%20Evidence%20Insight%202024-01_Kenya%20Willingness%20to%20Pay.pdf))
- Medium itself needed **>1 million paying members** to reach its first profitable month (Aug 2024) after losing
  $2.6 M/month in 2022. ([TechCrunch](https://techcrunch.com/2025/07/11/mediums-ceo-explains-what-it-took-to-stop-losing-2-6m-monthly))
- Bangladeshi agritech that makes money (iFarmer, Agroshift) earns from **transactions**: produce sales, input
  sales, financing commission. It does not earn from content.
  ([Dhaka Tribune](https://www.dhakatribune.com/amp/business/271329/agritech-startup-ifarmer-raises-2.1-million),
  [Daily Star](https://thedailystar.net/tech-startup/news/agritech-startup-agroshift-raises-18m-the-largest-ever-pre-seed-funding-bangladesh-3151616),
  [B Briddhi landscape](https://www.sie-b.org/wp-content/uploads/2025/02/B-Briddhi-Bangladesh-Agritech-Landscape-Assessment.pdf))

### 3.2 What that means for revenue (realistic, side-project scale)

| Model | Who pays | Realistic? | Notes |
|---|---|---|---|
| Reader subscription ("Medium") | Farmers / readers | ❌ | Needs scale you won't get; WTP ≈ cents |
| Ads (AdSense) | Advertisers | ⚠️ weak | Low BD CPMs, and falling page views (§2.2) |
| **Sponsored / branded knowledge** | Seed, fertiliser, feed, agro-machinery firms | ✅ | Clearly labelled sponsor posts and sponsored expert Q&A days; must never steer AI answers |
| **Paid expert consult** (commission) | Commercial growers: poultry, fish, dairy, rooftop/nursery, export veg | ✅ | Farmer pays 200–500 BDT for a verified agronomist/vet answer within X hours; you take 20–30 % via bKash |
| **White-label AI advisor** | NGOs, extension projects, cooperatives, input companies | ✅ best | They already have content and budgets; you sell "your manuals → a cited Bangla chatbot" per project |
| Premium for agri-students / job seekers | Students | ⚠️ small | Exam prep and notes; low price, some volume |
| Data / insights (aggregated, anonymised question trends by region) | Input firms, researchers | ⚠️ later | Only with consent and scale |

**Side-project answer:** yes, it can earn, but from **B2B/NGO and commercial growers**, not reader
subscriptions. One NGO pilot at BDT 3–5 lakh is worth more than a year of ad revenue at this scale.

### 3.3 Unit cost of the AI (so pricing doesn't lose money)

One `/api/ai/ask` call is about 4–5 k input tokens (5 chunks × ~800 + prompt) and about 600 output tokens. At
current Flash list prices ($0.50–1.50 in / $3–9 out per M) that is about **$0.004–0.012 per answer**.
([pricing aggregator; verify on ai.google.dev](https://www.morphllm.com/gemini-api-pricing)). Cheap, **but** the
quota is per account and sign-up has no e-mail verification (§6 B3), so a bot can create accounts and spend
your key.

---

## 4. Where we fall behind, and what to offer instead

### 4.1 Gaps

1. **Wrong primary job.** The UX and the roadmap optimise for *reading and writing* (claps, trending,
   reading time). The paying and recurring job is *asking and getting a trustworthy answer fast*.
2. **No trust layer.** Anyone can pick `userType = AUTHOR`; there are no verified agronomists or vets, no
   accepted answers or voting, and no moderation. Bad pesticide advice can cause real harm.
3. **Q&A is not in the AI's knowledge.** Only blogs are embedded. The community's best answers, which are
   the most specific, local knowledge on the site, are invisible to RAG.
4. **Distribution not designed.** The product waits for people to come to a website. Farmers live in Facebook,
   Messenger, WhatsApp, YouTube and IMO.
5. **Not deployed.** No Docker/CI/Flyway/deploy (Phase 4 = 0 of 8).
6. **Cold start.** Medium-style feeds are empty without hundreds of good posts. Seed content is not planned.

### 4.2 Proposed positioning

> **"Ask a verified agri expert, get an answer in minutes, in Bangla. The AI answers first, citing local
> posts; a human expert confirms."**

Blogs stay, as the expert's long-form knowledge that the AI cites and the SEO/AI-crawler surface, but the home
screen becomes *Ask*, not *Read*.

**Beachhead users (in order):** (1) urban rooftop/terrace and nursery gardeners and hobby growers in Dhaka/
Chattogram. They are literate, have smartphones, are active in big Facebook groups, and some will pay.
(2) Small commercial poultry, fish and dairy farmers: losses are expensive, so they pay for fast vet/agronomist
answers. (3) Agri students and SAAOs, who become your experts and writers. Smallholder rice farmers come via
intermediaries and voice later.

---

## 5. AI roadmap feasibility (next target, not now)

| Feature | Feasible now? | How (least effort) | Cost / risk |
|---|---|---|---|
| **AI chat (RAG)** | ✅ Done | Add Q&A answers (accepted/expert) to the index; add region/season to the prompt from the question's agri metadata | ~$0.004–0.012/answer |
| **Standard-Bangla voice in → text answer** | ✅ Feasible | Send the recorded audio straight to Gemini (multimodal audio input); no separate ASR service | Audio input ~$1/M tokens; fine for standard Bangla |
| **Voice answer out (TTS)** | ✅ Feasible | Browser `speechSynthesis` first (free); a cloud TTS later | Bangla TTS voices are adequate, not great |
| **Regional dialects** (Chittagonian, Sylheti, Noakhali...) | ❌ Research-grade | The best 2025 dialect-aware model still has **WER 0.74** across 12 regions; foundation models "struggle heavily" ([BanglaTalk](https://arxiv.org/pdf/2510.06188), [Ben-10](https://arxiv.org/pdf/2510.23252v1)) | Don't promise it. Accept dialect speech with a "did you mean…" text confirmation step |
| **Crop disease detection from photos** | ⚠️ Feasible as *triage*, not diagnosis | Use Gemini vision with a strict prompt plus RAG over your disease posts; return "likely X, confirm with an expert" and route to a human. **Do not train your own CNN**: PlantVillage-trained models fall from ~94 % to **55 %** on field photos ([Plants 2022](https://doaj.org/article/f54bfb1b987b45afbdddaf7987f96563), [IEEE Access 2023](https://doaj.org/article/e81741e8485f44b0b4f4ef55b92c95d0)) | Wrong diagnosis means wrong spray: always pair with expert review; log photos (with consent) to build a real local dataset over time |
| Messenger/WhatsApp bot | ✅ Feasible, **highest leverage** | Same `/api/ai/ask` behind a Messenger webhook | Meta app review; per-conversation fees on WhatsApp |

**Order I'd do it:** Messenger bot → photo triage (Gemini vision) → voice in (standard Bangla) → TTS → dialects
(only if a partner funds data collection).

---

## 6. Code audit (ponytail)

Method: read the core services, config, security and exception path; dead-code sweep; ran
`./mvnw -B test -Dtest='!AgriculturalBlogApplicationTests'` → **152 tests, 0 failures**. ~10.4 k lines of
main Java, 75 endpoints. Overall: **solid layering and security, carrying a lot of inherited boilerplate
from a previous project**, plus a few real bugs on the AI write path.

### 6.1 Bugs (fix first)

| # | Where | Bug | Failure | Fix (smallest) |
|---|---|---|---|---|
| B1 | `service/BlogService.java:93,155,191` | Embedding is called inline after `save`, with no transaction and no catch | Gemini down or quota hit → blog **is saved** but the client gets a 500 → author retries → **duplicate posts**. On update, `reindexBlog` deletes old vectors first, so a failed re-embed leaves the post **missing from AI** until the next edit | Catch and log in `DocumentService.indexBlog`/`reindexBlog` (index is a cache, the post is the truth) and let `BlogReindexRunner` heal; or `@TransactionalEventListener(AFTER_COMMIT)` + `@Async`. Test: indexer throws → create still returns 200 |
| B2 | `config/AIConfig.java:28` | System prompt contradicts itself: "Bangla for a question in Bangla" then "Answer in Bangla **only if the user explicitly asks**" | A Bangla question with no explicit request can come back in English, which is the exact regression `fix/ai-prompt-language` targeted | Delete the last sentence; extend `AIConfigTest` to assert it is absent |
| B3 | `service/AuthService.java:58-76` | No e-mail verification and self-chosen `userType` (AUTHOR) | AI quota (20/day) is per account, so scripted sign-ups give unlimited Gemini spend on your key; anyone can publish as an "author" | Verify e-mail before AI/publish (reuse the password-reset token + `MailService`); add a per-IP daily cap on `/api/ai/**` with the existing `AttemptLimiter` |
| B4 | `service/ErrorCodeService.java:33` | `private TranslationService translationService;` is not `final`, so `@RequiredArgsConstructor` never injects it | The first time an `error_code` row exists with a matching language, **every error response throws an NPE** inside the exception handler | Moot if you delete the subsystem (O1); otherwise make it `final` |
| B5 | `service/AiService.java:57` | Quota check-then-call is not atomic | N parallel requests at 19/20 all pass, so a minor overspend | Acceptable now; note it with a `shortcut:` comment |
| B6 | `security/SecurityConfig.java:62` | CORS origin hard-coded to `http://localhost:5173`; `FRONTEND_URL` exists but is unused | Production frontend gets CORS errors on deploy | Read `app.frontendUrl` (part of roadmap 4.1) |

### 6.2 Over-engineering and dead code (delete beats add)

| # | What | Evidence | Action |
|---|---|---|---|
| O1 | **DB-driven i18n error system**: `ErrorCodeEntity`, `Language`, `LocalizedText`, `Translation` + their services/repos/requests (~600 lines) | No endpoint or seeder creates error codes; `ExceptionHandlingController.java:153` does a DB lookup on **every error** that always returns null; the translation branch would NPE (B4) | Delete the stack. If Bangla error messages are wanted, use Spring's `messages.properties` / `messages_bn.properties` (`MessageSource`), which is built in |
| O2 | `String lang` threaded through **178** method signatures | Only consumer is O1 | Replace with `LocaleContextHolder` / `Accept-Language` when (if) you add `MessageSource`; remove the params gradually, file by file |
| O3 | 96 of 175 `ErrorCode` constants unused (security questions, devices, mobile, OTP...) | Inherited from the banking template | Delete unused constants |
| O4 | `dtos/` package (6 classes) + ModelMapper dependency + `CommonUtils.mapEntity*` | DTOs only reference each other; responses are records with `from()` | Delete package, helper methods and the `modelmapper` dependency |
| O5 | Thymeleaf: 3 dependencies, 6 templates, `spring.thymeleaf.*` config | No `@Controller` in the codebase | Delete (roadmap 4.7 already lists templates) |
| O6 | `ForgetPasswordService` (fully commented out), `CustomSuccessHandler`, `FileStorageException`, `InitiateUpdatePasswordRequest`, `ResetPasswordFromSessionTokenRequest`, `OTPChannel` | Not referenced | Delete |
| O7 | **Phase 5: Kafka for notifications** | Zero users; adds a broker to run, monitor and pay for | Insert `Notification` rows in a `@TransactionalEventListener(AFTER_COMMIT)` in-process. Same API, no Kafka. `shortcut: in-process events, move to a broker when a second consumer or instance exists` |
| O8 | `SchemaPatches` hand-rolled migrations + `ddl-auto=update` | Works, but is a home-made Flyway | Do roadmap 4.2 (Flyway baseline) before the first real deploy, then delete `SchemaPatches` |

### 6.3 Architecture and performance

| # | Issue | Impact | Fix |
|---|---|---|---|
| A1 | List endpoints return the **full `content`** of every blog (`BlogResponse.java:32`) and compute reading time per row | Feed page of 20 long posts = hundreds of KB on rural 3G | `BlogSummaryResponse` (title, excerpt 200 chars, image, author, tags, claps, readingTime) for lists; full body only on `/id` and `/slug` |
| A2 | N+1 queries: `@ManyToOne` defaults to EAGER (category, author → user) and tags are EAGER; every list row triggers extra selects; relies on open-in-view | Fine at 10 users, slow at 10 k | `@EntityGraph(attributePaths={"author.user","category","tags"})` on list queries; set `spring.jpa.open-in-view=false` after that |
| A3 | JWT filter does 2–3 DB reads per authenticated request (user + session) | OK for one small VPS | Leave it; that's the price of real revocation. Cache later if needed |
| A4 | Images on local disk (`uploads/`) | Lost if the container/VM is rebuilt | Mount a volume in compose and back it up; the `ImageStorage` port makes S3/R2 a later swap |
| A5 | RAG indexes blogs only | Community answers can't be cited | Index accepted/expert answers with `type=answer` metadata; same `DocumentService` |
| A6 | Not deployable (no Dockerfile/compose/CI/profiles) | No users can arrive | Roadmap 4.1–4.5 **before any new feature** |

### 6.4 What is genuinely good (keep it)

Owner-or-admin in the service layer, identity from the token, real session revocation, refresh rotation,
secrets in env, published-only RAG filter, untrusted-context framing in the RAG prompt, citations, quota +
timeout + 503, slug design for Bangla titles, atomic clap counter, 152 focused unit tests, and the
docs/roadmap discipline.

---

## 7. Better ideas (ranked by value ÷ effort)

1. **Facebook Messenger bot on the existing `/api/ai/ask`.** Meet users where they are; the website becomes the
   archive the bot cites. Small code, biggest reach change.
2. **Verified experts + "expert confirmed" badge on AI answers.** `Author.verified` (admin sets it after
   checking a degree or SAAO/DLS ID); experts get notified of AI answers in their speciality and can confirm or
   correct. This is your moat against free ChatGPT.
3. **Accepted answers + upvotes on Q&A, then index accepted answers into RAG.** The community makes the AI
   smarter over time; that flywheel is the product.
4. **Seasonal, region-aware nudges.** You already store crop, season and region: "Aman transplanting in
   Rangpur this week: 3 things to check", sent via Messenger or e-mail. That is retention without a feed.
5. **Photo triage** with Gemini vision + expert routing (§5).
6. **White-label kit for NGOs:** "upload your manuals → get a cited Bangla advisor". Same RAG, a `tenant` tag in
   metadata. This is your best revenue path.
7. **Seed the content:** convert public-domain DAE/BARI/BRRI extension material (check licences) into posts so
   the AI has local sources on day one; invite 10–20 agri students as paid/credited writers.

---

## 8. What to do next (ordered)

1. **Fix B1, B2, B3** (small PRs, each with a regression test).
2. **Ship it:** roadmap 4.1–4.5 (profiles + CORS from env, Flyway, Docker Compose, Testcontainers, CI) and deploy
   to one cheap VPS with HTTPS. Rotate leaked keys (0.4) first.
3. **Delete O1–O6** (one housekeeping PR per item; pure deletions, tests stay green).
4. **Replace Phase 5 Kafka with in-process after-commit events** (O7).
5. **Validate demand before building more:** post in 5 large gardening/farming Facebook groups, offer free
   "ask an agronomist" for 4 weeks, and measure questions/week, repeat askers, and whether anyone accepts a
   200 BDT fast-answer offer. **Kill criterion:** fewer than 30 weekly active askers after 8 weeks → pivot to
   the NGO white-label path only.
6. Then: Messenger bot → verified experts → accepted answers into RAG → photo triage → voice.

---

## 9. Limits of this study

- No primary user research: no interviews or surveys of AgroTrends' target users. §8 step 5 is that research.
- Market figures come from secondary sources (press, NGO, academic); some are self-reported (a2i, Medium's CEO)
  or third-party pricing aggregators. Verify Gemini prices on `ai.google.dev` before setting prices.
- Code audit read the core paths in full and swept the rest; it is not a line-by-line review of all 241
  files. The frontend was not audited.

## 10. Missing features (checked against the code, 2026-10-11)

Google sign-in is now built (roadmap 2.15, branches pushed). Everything below was checked against the code on
`feat/google-signin-endpoint`. **P0** = needed before real users; **P1** = needed for the product to work as a
community; **P2** = growth and polish.

### Accounts and trust

| Pri | Missing | Why it matters |
|---|---|---|
| P0 | **E-mail verification at sign-up** (`emailVerifiedAt` now exists; only Google and password reset set it) | Unverified sign-ups can farm the AI quota (B3) and publish under any address |
| P0 | **"Become an author"** for an existing reader (`POST /api/authors/me`) | Today the author profile is only created at sign-up; every Google sign-up and every reader is locked out of writing |
| P1 | **Verified expert badge** (agronomist, vet, SAAO) with an admin review flow | The trust layer that beats a free chatbot (§4.1) |
| P1 | Report content and users; moderation queue; admin ban/hide | No way to remove spam or harmful advice today |
| P2 | Public reader profile (avatar, handle) | Readers who answer questions have no identity |

### Content

| Pri | Missing | Why it matters |
|---|---|---|
| P0 | **Server-side sanitisation of blog HTML** (or a markdown-only decision) | No sanitiser exists in the code (no jsoup/OWASP); stored XSS depends entirely on the frontend |
| P0 | List responses with an **excerpt**, not the full body (A1) | Feed payload size on rural mobile data |
| P1 | Author stats: views and reads per post | Writers stay when they see readers |
| P2 | Scheduled publishing, series/collections, edit history | Medium parity |
| P2 | SEO: `sitemap.xml`, OpenGraph/Twitter meta, RSS | Be the source that search engines and AI crawlers cite |

### Q&A (the core job; see §4.2)

| Pri | Missing | Why it matters |
|---|---|---|
| P1 | **Accepted answer + upvotes**, answer count, "unanswered" filter | Users can't tell which answer is right |
| P1 | **Photos on questions** (reuse `POST /api/images`) | Farmers describe crop problems with pictures, not words |
| P1 | Search over questions (blogs have full-text search, questions don't) | Avoid duplicate questions |
| P1 | **Index accepted/expert answers into RAG** (A5) | The community makes the AI smarter |
| P2 | Tags on questions (blogs have them) | Follow a topic across posts and questions |

### AI

| Pri | Missing | Why it matters |
|---|---|---|
| P1 | Thumbs up/down and "report wrong answer" on AI answers | Quality signal; catches harmful advice |
| P1 | Multi-turn chat (follow-up questions keep context) | `/api/ai/ask` is single-turn today |
| P1 | Send the question's region/season/crop with the prompt | Local answers are the differentiator |
| P2 | Photo triage, voice in/out (§5) | Next target, as you planned |

### Engagement

| Pri | Missing | Why it matters |
|---|---|---|
| P1 | Notifications (Phase 5; in-process events, not Kafka, see O7) | People return when someone answers them |
| P2 | Weekly e-mail digest and seasonal nudges by region | Retention without a feed habit |
| P2 | Share to Facebook/WhatsApp with good previews | Distribution is where farmers already are |

### Platform and operations

| Pri | Missing | Why it matters |
|---|---|---|
| P0 | **Deploy**: Docker/compose, Flyway, CI, `prod` profile, CORS from env (Phase 4) | Nobody can use it yet |
| P0 | Health checks and basic metrics (Spring Boot Actuator is not a dependency) | Know when it's down |
| P0 | Backups for Postgres and `uploads/` | Uploads live on local disk |
| P1 | Per-IP limit on `/api/ai/**` and `/api/auth/google` | AI cost control (B3) |
| P2 | Admin dashboard (users, posts, AI spend per day) | Run the business on numbers |
| P2 | Payments (bKash / SSLCommerz) | Only once a revenue model is chosen (§3.2) |

## Sources

- [TBS — Bangladesh lags in smartphone, internet usage (GSMA 2024)](https://www.tbsnews.net/node/1013061)
- [Financial Express — BD bottom among 12 LMICs](https://thefinancialexpress.com.bd/national/bd-bottom-among-12-lmics-in-usage-of-phone-net)
- [MEA Tech Watch — BBS ICT survey 2025–26](https://meatechwatch.com/?p=42188)
- [Digiday — AI platforms vs zero-click search](https://digiday.com/media/in-graphic-detail-ai-platforms-are-driving-more-traffic-but-not-enough-to-offset-zero-click-search/)
- [Campaign — AI Overviews click-through](https://campaignlive.com/article/1-click-through-rate-google-ai-overviews-killing-publishers/1927692)
- [Yahoo — Pew on AI summaries and zero clicks](https://www.yahoo.com/news/google-ai-summaries-increase-frequency-223953043.html)
- [J. Agric. Sci. 2026 — YouTube use by Bangladeshi farmers](https://asistent.ceon.rs/index.php/jas/article/view/64256)
- [Access Agriculture — Chowdhury et al. 2011, farmer video and rice yields](https://Accessagriculture.org/sites/default/files/1._summary_Chowdhury_et_al_2011_ENGLISH_0.pdf)
- [BSS — Krishi Call Centre 92,094 calls](https://www.bssnews.net/special-stories/401845)
- [South-South Galaxy — Krishoker Janala](https://my.southsouth-galaxy.org/en/solutions/detail/krishoker-janala-farmers-window)
- [AI Kosh — FarmerChat](https://aikosh.indiaai.gov.in/home/use-cases/details/farmerchat_ai_powered_agricultural_advisory_at_scale.html)
- [Analytics India — OpenAI/FarmerChat cost estimate](https://analyticsindiamag.com/openai-is-helping-farmers-in-india-increase-crop-yields/)
- [FAO Agritech Observatory — Plantix](https://agritechobservatory.review.fao.org/en/plantix-crop-doctor)
- [Dealroom — Plantix](https://app.beta.dealroom.co/companies/peat_gmbh_plantix_app)
- [Daily Star — Bangladeshi plant Facebook groups](https://epfs.thedailystar.net/TDS/2022/03/26/Bangladesh/MAI/5_04/726fbe042ff1be93_04.pdf)
- [AgEcon — IFPRI Ghana WTP for Farmers Club](https://ageconsearch.umn.edu/record/277451?ln=en)
- [UC Davis MRR — Kenya WTP for agro-weather advisories](https://basis.ucdavis.edu/sites/g/files/dgvnsk466/files/2024-01/MRR%20Evidence%20Insight%202024-01_Kenya%20Willingness%20to%20Pay.pdf)
- [TechCrunch — Medium profitability (2025)](https://techcrunch.com/2025/07/11/mediums-ceo-explains-what-it-took-to-stop-losing-2-6m-monthly)
- [TechCrunch — Medium first profitable month (2024)](https://techcrunch.com/2024/08/21/august-was-mediums-first-profitable-month-ever)
- [Dhaka Tribune — iFarmer raises $2.1 M](https://www.dhakatribune.com/amp/business/271329/agritech-startup-ifarmer-raises-2.1-million)
- [Daily Star — Agroshift pre-seed](https://thedailystar.net/tech-startup/news/agritech-startup-agroshift-raises-18m-the-largest-ever-pre-seed-funding-bangladesh-3151616)
- [B Briddhi — Bangladesh agritech landscape 2025](https://www.sie-b.org/wp-content/uploads/2025/02/B-Briddhi-Bangladesh-Agritech-Landscape-Assessment.pdf)
- [Gemini API pricing (aggregator)](https://www.morphllm.com/gemini-api-pricing)
- [BanglaTalk — Bengali regional dialect ASR (arXiv 2510.06188)](https://arxiv.org/pdf/2510.06188)
- [Ben-10 — dialect ASR corpus (arXiv 2510.23252)](https://arxiv.org/pdf/2510.23252v1)
- [Plants 2022 — maize field accuracy drop](https://doaj.org/article/f54bfb1b987b45afbdddaf7987f96563)
- [IEEE Access 2023 — lab vs field corn disease images](https://doaj.org/article/e81741e8485f44b0b4f4ef55b92c95d0)
