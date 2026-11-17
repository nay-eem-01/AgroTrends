# AgroTrends deployment: Lightsail, host Nginx, GitHub Actions

## Current checkpoint (2026-11-17)

Reported by Nayeem: Lightsail Ubuntu 24.04, Sydney, 2 GB RAM/2 GB swap, static IP `52.65.208.166`, Docker and
Compose, host Nginx, DNS, Let's Encrypt HTTPS and renewal dry-run work. The API's public health is UP. Frontend
build/files are served by Nginx; Google sign-in needs `VITE_GOOGLE_CLIENT_ID` in the frontend build.
The AWS account is Free with $100 credits and a plan deadline of **2027-05-17**. The VM is $12/month before
extras/taxes. This replaces the old Azure/Caddy instructions. Hosting after the free-plan deadline needs action.

Supabase/profile writes, R2 upload/display (`agrotrends-files`), Brevo delivery and earlier Docker login are verified.
Docker-specific R2/Brevo acceptance and real backup/upload/isolated restore remain pending. WSL backup
authentication is unresolved. Do not claim a verified backup. Monitor AWS credit usage and VM disk space.

## Branches and activation

Nayeem's 2026-11-17 decision: **`staging` is the live deploy branch for both repos**.
Keep feature step → feature base → `development` → `staging` promotion PRs. Do not deploy from `main` or
`production`. Protect `staging` with reviewed PRs and the `checks` job. A successful merge triggers a deployment;
failed checks leave the live version unchanged. `workflow_dispatch` supports a manual run **with staging selected**.
A manual run from any other branch runs checks but cannot deploy. No additional required environment approval is
needed if the intention is deployment immediately after a merge.

Create a GitHub **Environment named `staging`** in each repo under Settings → Environments. Restrict its deployment
branches to `staging`. Configure these values separately in both repos:

| Kind | Name | Value |
|---|---|---|
| Variable | `DEPLOY_HOST` | `52.65.208.166` |
| Variable | `DEPLOY_USER` | `ubuntu` |
| Variable | `DEPLOY_KNOWN_HOSTS` | Full verified ED25519 known_hosts line for this IP, not just the fingerprint |
| Secret | `DEPLOY_SSH_KEY` | A dedicated Actions SSH private key; never the personal Lightsail key |

Generate a dedicated Ed25519 key on your own PC, with a unique filename and no passphrase for unattended Actions.
Enter the private part only in GitHub's secret editor, never in chat or Git. Add the public `.pub` line to the VM's
`~/.ssh/authorized_keys` with the `restrict` prefix (disables forwarding and PTY, while allowing deployment commands).
Keep the personal key so you can still log in interactively.

Verify the VM's key through an already trusted session:

```bash
sudo ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub
```

Expected fingerprint from the initial setup:
`SHA256:CVe4PtACewz/yDSAWPSee6G4eyBoUykiQ34Kg0TvtuU`.
On your PC, `ssh-keyscan -t ed25519 52.65.208.166` obtains the public line. Save to a temporary file and run
`ssh-keygen -lf <file>`; only copy it to `DEPLOY_KNOWN_HOSTS` if its fingerprint matches the trusted VM output.
The workflow enforces host-key checking, never accepting an arbitrary key on first use.

The deployment key logs in as `ubuntu`, which has passwordless sudo and therefore broad server access. It is
separate from the personal key, but is **not** a least-privilege deployment user. Limit who can edit staging,
workflow files, and environment secrets. The VM needs outbound HTTPS and SSH 22 reachable by the GitHub-hosted
runner; the current Lightsail SSH rule permits this. Do not install a self-hosted runner on this small live VM.

Backend and frontend workflows run independently. For incompatible API changes, deploy a backward-compatible
backend first, then the frontend. Their locks prevent overlapping releases within each service. GitHub may coalesce
pending runs; the intention is convergence on the latest staging version, not deploying every intermediate commit.

## Backend runtime

- CI runs Java 21 unit tests (`Asia/Dhaka`), excluding the known DB-dependent context test, and script regression tests.
- GitHub builds the image from the staging commit and pushes `ghcr.io/nay-eem-01/agrotrends` with a unique run tag.
- The VM deploys the returned **immutable digest**, using `deploy/compose.image.yml` with project name `backend`.
- Only `127.0.0.1:8080` is published. The existing `backend_uploads` volume is reused. Caddy is not started.
- Secrets stay at `/home/ubuntu/agrotrends/backend/.env` (mode 600). This file is never uploaded by CI or printed.
- Release scripts live in `~/agrotrends/backend-releases/<commit>-<run>-<attempt>`; source checkout state is not altered.
- `~/agrotrends/backend-deploy/current` records the successful deployment, `previous` the preceding release.

Keep the verified DB/session-pooler, R2 and Brevo values in `.env`, with these public settings:

```dotenv
BACKEND_URL='https://api.agrotrends.ahmednayeem.tech'
FRONTEND_URL='https://app.agrotrends.ahmednayeem.tech'
CORS_ORIGINS='https://app.agrotrends.ahmednayeem.tech'
R2_BUCKET='agrotrends-files'
```

The backend Google client ID must match the frontend. `INITIAL_ADMIN_PASSWORD` seeds a new admin; it does not
reset an existing one. `API_DOMAIN` is no longer consumed by Compose. Never read/print/commit `.env`.

### GHCR access on the VM, before first deployment

Packages initially may be private. In a trusted server terminal run:

```bash
sudo docker login ghcr.io -u nay-eem-01
```

Enter a suitable GitHub token with `read:packages` at the password prompt, never in the command line or chat.
Grant the token access to the package as required by the account/org. The deployment uses root's Docker credential
store because it invokes `sudo docker`. Alternatively make this source-public package public in GitHub's package
settings once it exists; then no VM registry token is needed. Publishing in CI uses the built-in `GITHUB_TOKEN`.

### Health and rollback

The script downloads the image and validates Compose before replacing the running container. It waits up to roughly
8 minutes (60 attempts, each with a 5-second probe and 3-second delay), requiring JSON `status=UP` locally. On a
startup or local health failure, it restarts the previous actual image and checks health again. A failed deployment
still fails the workflow. A separate final workflow probe checks the public HTTPS API.

**Automatic rollback restores app code only.** `spring.jpa.hibernate.ddl-auto=update` can change Supabase during
startup. Those changes are not undone, and can make an old image incompatible. Resolve backups/isolated restore
before promoting schema-changing work. Backend restarts have brief downtime; this is not a blue/green rollout.

Manual rollback, after at least two successful automated deployments:

```bash
previous=$(readlink -f ~/agrotrends/backend-deploy/previous)
bash "$previous/deploy.sh" "$(cat "$previous/image")"
```

Keep previous images/releases for rollback. Do not use Docker prune with `-a` blindly. Retention is manual for now;
monitor `docker system df` and `df -h`. Initial first-release rollback can restore the existing local-build image,
but it is not represented by a previous automated release directory.

## Nginx and frontend prerequisite

Nginx remains a host service and keeps Certbot's certificates. `deploy/nginx.conf` is a reference matching this
installation, not something Actions overwrites. Back up `/etc/nginx/sites-available/agrotrends` before changing it.
Never copy certificate private keys into a repo. Certbot's current certificate covers both hostnames.

Before frontend automation, make an initial link preserving today's working frontend:

```bash
sudo ln -s /var/www/agrotrends /var/www/agrotrends-current
```

In **only the app HTTPS server block**, change the root to `/var/www/agrotrends-current`. Keep the API proxy and
Certbot settings. Serve `/deployment-version.txt` without caching and use SPA fallback for frontend routes; see the
reference config. Test `sudo nginx -t`, then reload Nginx. The first automated frontend release will switch the link.
Do not rerun `ln -s` if the link already exists; inspect its destination instead.

## Manual build and recovery

For emergency source builds, `docker-compose.yml` builds `app` locally, binds loopback and has no Caddy. Run from
the backend checkout with its private `.env`. Routine automated releases use the digest-based Compose instead.
Do not switch branches or pull over the existing VM changes until they are safely preserved in a reviewed PR.
Feature branches go into their feature base, then development, then staging; no direct staging commits.

## Acceptance and monitoring still to complete

- Login and refresh persistence, Google sign-in, profile writes, image upload/display/replace/delete through the live Docker backend.
- Brevo password-reset delivery and completion; inspect logs locally without sharing tokens/reset links.
- `deploy/backup.sh` with the real session-pooler credentials, R2 backup copy and restore on a separate disposable DB.
  Never restore onto the live database as a test; never enable the schedule before successful verification.
- Uptime alerts on `https://api.agrotrends.ahmednayeem.tech/actuator/health`, credit and disk monitoring.
- Unit checks here do not prove a real VM deployment or backup works. Activate workflows only after the documented
  SSH/GHCR/Nginx/environment setup and run both staging deployments once under observation.
