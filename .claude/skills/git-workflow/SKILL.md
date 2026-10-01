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

- `main` is the integration branch; `feature/rag-impl` and `dev` are older long-lived branches.
- Each **roadmap phase** has a base branch: `feat/<phase>-base` (Phase 0+1: `feat/hardening-base`).
- Each **roadmap step** is its own branch, taken from the **previous step's branch** (not from `main`),
  so stacked PRs never conflict. Name: `feat/<step>`, `fix/<step>`, `docs/<topic>`, `chore/<topic>`.
- Step branches PR **serially into the phase base branch**; the base PRs into `main` when the phase is done.
- Never commit straight to `main`.

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
