---
name: security-checklist
description: Security rules for the AgroTrends backend - identity from the token, owner-or-admin checks, sessions and refresh tokens, sign-in hardening, password reset, secrets, what must never be logged or returned, RAG/AI abuse limits. Use before writing or changing any endpoint that reads or writes user-owned data, anything touching JWT/passwords/tokens, file upload, or the AI endpoints.
---

# Security rules (AgroTrends)

Non-negotiable. If a rule blocks a requirement, raise it - do not quietly work around it. Most of these
exist because the 2026-09-30 audit found the opposite in the code (see `docs/PLAN.md`).

## Identity and authorization

1. **The acting user comes from the token, never from input.** `AuthorizationService.currentUserId(lang)`.
   A DTO field that names the caller (`userId`, `authorUserId`, `authorId`) is a defect - delete it.
2. **Mutations are owner-or-admin**: `AuthorizationService.assertOwnerOrAdmin(ownerUserId, lang)` in the
   *service*, not only a controller `@PreAuthorize`. A role check lets every user through to every row.
3. Privilege checks (`@PreAuthorize("hasAuthority('CATEGORY_DELETE')")`) are for admin-style actions and
   must name the privilege of *that* resource (not copy-paste `BLOG_DELETE`). Every privilege used must
   exist in `AppConstants.PERMISSIONS`.
4. **403 vs 404.** Public content (blogs, questions, comments): not-yours is **403** - the row is visible
   to everyone anyway. Private data (drafts, bookmarks, a user's e-mail, AI history): not-yours is **404**,
   via a scoped query (`findByIdAndUserId`), so existence isn't confirmed.
5. Only users with an author profile can publish. Roles are never chosen by the client.
6. Never return an entity that contains `password`, other users' e-mail/mobile, roles or privileges to a
   non-admin. (`User.password` is `@JsonIgnore` as a backstop, not as the design.)

## Tokens and sessions

- Access JWT: 15 minutes, type claim `access`, unique `jti`. Refresh token: opaque random, 10 days,
  **single use** (rotated on refresh), stored server-side.
- A JWT is accepted only if its `UserSession` row is active. Sign-out, password change/reset and e-mail
  change deactivate sessions and delete refresh tokens (`UserService.revokeAllCredentials`).
- The JWT secret is `JWT_SECRET`, >= 32 bytes, no default; `JwtUtil` refuses to start without it.
- A malformed/expired/forged token never produces a 500: the filter leaves the request unauthenticated
  and the entry point answers 401.
- Accounts flagged `mustChangePassword` can reach only `/api/user/change-password`, `/api/user/me`,
  `/api/auth/sign-out`.

## Sign-in and password flows

- Verify credentials **first**, then check role/portal. Every failure returns the same 401
  `Invalid email or password.` - no 404 for unknown e-mail, no distinct "wrong portal".
- Throttle with `AttemptLimiter`: sign-in 5/15 min per e-mail and 20/15 min per IP; forgot-password
  3/h per e-mail and 10/h per IP; change-password 5/15 min per user. Count attempts for unknown
  accounts too. (In-memory = single instance; move to a shared store before scaling out.)
- Forgot-password always answers 200. Reset tokens: 256-bit random, only the SHA-256 hash stored,
  15 minutes, single use, killed on use. Password policy is checked *before* the token is consumed.
- Password policy: `CommonUtils.getInvalidPasswordMessage` (8-16, upper, digit, special) on sign-up,
  change and reset - one policy, every path.
- BCrypt only. Never store, log or return a password or hash.

## Secrets and config

- Env vars / git-ignored `.env`. No default for any secret; missing => startup failure.
- `.env`, `*-local.properties` stay in `.gitignore`. Run gitleaks before pushing (CI in Phase 4).
- Seeded admin: `INITIAL_ADMIN_PASSWORD` or a generated one logged once; always `mustChangePassword`.
- If a secret is ever committed: rotate it first, then purge history. Rotation is what protects you.

## Errors and logging

- Responses never contain stack traces, SQL, class names or `ex.getMessage()` of unexpected exceptions.
- Never log tokens, passwords, API keys or full request bodies (exception: the dev-only mail logger).

## AI endpoints

- `/api/ai/*` is authenticated, length-capped and quota'd per user (Phase 3). An unmetered LLM call is a
  billing hole.
- Blogs are embedded into the shared vector store: treat blog text as **untrusted input to the prompt**.
  Retrieval must carry metadata (author, status) and only `PUBLISHED` content may be retrieved.
- Deleting or unpublishing a blog deletes its vectors in the same transaction/flow.

## Checklist before opening a PR

- [ ] No caller identity in any request DTO or path
- [ ] Ownership asserted in the service for every mutation; test for "other user -> 403/404"
- [ ] New endpoint has `@SecurityRequirement` unless genuinely public
- [ ] No secret, token or hash in a response, log line or test fixture
- [ ] Regression test for any bug fixed
