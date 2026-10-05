---
name: git-workflow
description: Git and project-tracking conventions for AgroTrends - branch flow (phase base branch + serial step branches), small conventional commits, no attribution, what Claude may commit/push, and keeping docs/DEV_LOG.md and docs/ROADMAP.md current. Use at the start of every session and before any commit, branch or PR.
---

# Git workflow and dev log (AgroTrends)

Ported from the delivery-app project so both repos work the same way.

## Start of every session

Read `docs/DEV_LOG.md` ("Where we are", "Next up", "Open items") and `docs/ROADMAP.md`, then
`git status` and the current branch. Do not re-derive what the log already says.

## Branches

Long-lived, protected in spirit (nobody commits to them directly - only PRs):

| Branch | Purpose |
|---|---|
| `development` | integration: all finished features land here first; tested here |
| `staging` | pre-production: what passed testing on `development`, soaked before release |
| `production` | what is live |

`main` is the pre-2026-10-01 snapshot (the hardening work was merged there). **No new work goes to `main`.**
`dev` is an older unrelated branch; ignore it.

Feature flow:

1. Take a **feature base branch** from `development`: `feat/<feature>-base`
   (e.g. `feat/drafts-and-slugs-base`).
2. Each roadmap **step** is its own branch, taken from the **previous step's branch** (the first from the
   base), so stacked PRs never conflict. Name: `feat/<step>`, `fix/<step>`, `docs/<topic>`, `chore/<topic>`.
3. Step branches PR **serially into the feature base branch**.
4. When the feature is done, the base PRs into **`development`**. Test there.
5. If fine, **`development` -> `staging`** (promotion PR). Test there.
6. If fine, **`staging` -> `production`** (release PR).

Rules: never branch from or PR into `main`; never skip a stage; never merge a base into `staging` or
`production` directly. Use merge commits (or rebase-and-merge), not squash, for stacked step PRs.
Docs-only branches follow the same path (branch off `development`, PR into `development`).

## Commits

- Conventional prefixes with an optional scope: `feat(auth): ...`, `fix(comments): ...`, `docs: ...`,
  `test(authz): ...`, `refactor:`, `chore:`.
- Subject: imperative, <= 72 chars, says *what changed for a reader*. Body (when the why isn't obvious)
  says why - the audit finding, the decision, the trade-off.
- **One coherent change per commit that builds and passes tests.** A regression test ships in the same
  commit as its fix.
- **Small PRs.** If a step would touch more than ~10-15 files, split it (and split the roadmap row first).
- Author identity only (nay-eem-01). **No `Co-Authored-By` and no Claude/AI attribution** in commits or PRs.
- Never commit: `.env`, keys, tokens, the audit PDF or other binaries unless asked.

## What Claude may do

- Commit when asked. Push only when asked. Open PRs only when asked (`gh` may not be installed -
  the user opens PRs on GitHub).
- Never force-push, `reset --hard`, `clean -fd`, rewrite published history or delete branches without
  an explicit instruction naming the action. History purges for leaked secrets are the user's call.

## Dev log and roadmap - in the same PR as the step

When a roadmap step finishes:
1. Tick it in `docs/ROADMAP.md` and update the progress count.
2. Add a dated entry (newest first) to `docs/DEV_LOG.md`: **Done**, **Decisions**, **Breaking API
   changes** (if any), **Known limitations**.
3. Refresh "Where we are", "Next up" and "Open items".

A tiny `docs: tick roadmap x.y` commit on the step branch is the norm.
