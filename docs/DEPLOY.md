# Deploying the backend (one server, Docker)

One Linux server runs the app and Caddy (HTTPS) with `docker-compose.yml`; the database is **Supabase** (managed
PostgreSQL with pgvector). The current target is an Azure VM. Moving servers never touches the database; older AWS instructions are
replaced below. Production infrastructure is not yet provisioned.

## Deployment checkpoint — 2026-11-16

Reported and verified by Nayeem locally (these are not checks run from this workspace):

- Supabase connection and profile writes work.
- R2 uploads and display work; the actual public image bucket is **`agrotrends-files`**.
- Brevo sender domain is authenticated and e-mail delivery works.
- Docker image builds, the container connects to Supabase, and health returns `UP`.
- Frontend login against the Docker backend works.

Still pending:

- Repeat image upload/display/deletion and Brevo delivery **through Docker**.
- Backup authentication fails in WSL. Deferred and unresolved: no successful real backup/restore is claimed.
- Provision the **Azure VM**, configure production DNS/HTTPS and monitoring.
- Image cleanup was absent at this checkpoint. The deployment-fixes work adds cleanup for removed/replaced
  saved images; verify it against `agrotrends-files` after deploying that code. Existing orphan objects are not swept.

## 1. Before you start

- [ ] **Rotate the leaked keys** (roadmap 0.4): a new Gemini API key. Never reuse the old ones.
- [ ] **A domain** for the API, e.g. `api.agrotrends.com`. HTTPS needs one; a bare IP cannot get a certificate.
- [ ] **Cloudflare R2:** two buckets, `agrotrends-files` (public access on) and `agrotrends-backups` (private), each
      with its own "Object Read & Write" token scoped to that bucket.
- [ ] **Brevo:** verified sender domain and an SMTP key.
- [ ] **Google sign-in:** add the frontend's address under "Authorized JavaScript origins" of the OAuth client.

## 2. Supabase: the database

1. Sign up at supabase.com (GitHub sign-in works; no card on the free plan) and create an organization (Free).
2. **New project:**
   - Name: `agrotrends`.
   - Database password: **Generate a password** and save it in your password manager. It becomes `DB_PASSWORD`.
   - Region: **South Asia (Mumbai)** — close to the planned server, so every query stays close.
   - Under the security options, **turn off the Data API**. The app talks to the database directly; with the Data
     API on, Supabase would also publish every table (users included) as a web API.
3. Wait until the project is ready (about 2 minutes).
4. **Database → Extensions:** search `vector` and enable it (schema `extensions`). The app needs it for the AI.
5. **Data API off check:** if you did not see the option at creation, open Project Settings, find **Data API**,
   and turn it off there.
6. **Connect** (the button at the top of the project) → connection string → method **Session pooler**. The direct
   connection is IPv6-only and an IPv4 VM needs the pooler. From what it shows, note:
   - host — like `aws-0-ap-south-1.pooler.supabase.com` (copy yours exactly; the prefix may differ)
   - port `5432` (session mode; **not 6543**, the transaction pooler breaks the app's prepared statements)
   - database `postgres`
   - user — `postgres.<project ref>`, with the dot and the project ref

   These become, in `.env`:

   ```
   DB_URL=jdbc:postgresql://<host>:5432/postgres?sslmode=require
   DB_USERNAME=postgres.<project ref>
   DB_PASSWORD=<the generated password>
   ```
7. Nothing else: the app creates its tables on the first start.

Free plan limits to know: **500 MB** of database (plenty for launch; watch it under Reports), **no backups**
(section 6 is the only copy), and a project with no activity for a week is paused (Restore it from the dashboard;
the running app and the nightly backup keep it active).

## 3. Azure: server (pending)

1. Create an Ubuntu 24.04 LTS Azure VM with at least 2 GB RAM; choose a region near the Supabase region.
   Confirm the subscription's actual price/credits and configure a budget alert before provisioning.
2. Use SSH key authentication. In the network security group allow SSH 22 from your IP only, and HTTP 80 / HTTPS
   443 publicly. Keep PostgreSQL and the app's internal port private.
3. Assign a static public IP. Point the API DNS A record to it. On Cloudflare use DNS only until the proxy and
   trusted-forwarded-IP configuration have been verified.
4. Record the chosen SSH username and IP locally; use them in section 4.

## 4. Prepare the server

```bash
ssh -i <private-key-path> <vm-user>@<static-public-ip>

# 2 GB of swap: the Docker build and the JVM need headroom on a 2 GB server
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker "$USER" && exit        # sign in again so the group applies
```

## 5. Configure and start

```bash
git clone https://github.com/nay-eem-01/AgroTrends.git && cd AgroTrends
git checkout production
cp .env.example .env && chmod 600 .env && nano .env
```

Set in `.env` (each is explained in `.env.example`):

| Key | Value |
|---|---|
| `API_DOMAIN` | `api.agrotrends.com` |
| `BACKEND_URL` | `https://api.agrotrends.com` (unsubscribe links in e-mails) |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | from Supabase (section 2, step 6) |
| `JWT_SECRET` | `openssl rand -base64 48` |
| `GEMINI_API_KEY` | the new key |
| `FRONTEND_URL` | the frontend's address, e.g. `https://agrotrends.com` (used in e-mail links) |
| `CORS_ORIGINS` | only if the frontend has more than one address, comma-separated |
| `INITIAL_ADMIN_EMAIL`, `INITIAL_ADMIN_PASSWORD` | the super-admin (first start only; must be changed on first sign-in) |
| `GOOGLE_CLIENT_ID` | the OAuth web client id |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | Brevo |
| `R2_BUCKET`, `R2_ENDPOINT`, `R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY`, `UPLOADS_PUBLIC_BASE_URL` | images bucket (`R2_BUCKET=agrotrends-files`) |
| `BACKUP_R2_BUCKET`, `BACKUP_R2_ACCESS_KEY_ID`, `BACKUP_R2_SECRET_ACCESS_KEY` | backups bucket |

```bash
docker compose up -d --build                 # first build takes a few minutes
docker compose logs -f app                   # wait for "Started AgriculturalBlogApplication"
curl https://api.agrotrends.com/actuator/health     # {"status":"UP"}
```

Then sign in as the super-admin and change the password.

## 6. Backups

Supabase's free plan keeps none. **WSL authentication currently fails; this procedure is not operationally verified.**
Resolve the pooler username/password authentication locally without printing credentials, run a dump and upload,
and prove restoration on a separate database before enabling the schedule. Earlier local stand-in checks do not
prove the real Supabase backup works.

```bash
./deploy/backup.sh                           # once by hand: writes backups/…dump and copies it to R2
crontab -e
# add:  15 3 * * * /home/<vm-user>/AgroTrends/deploy/backup.sh >> /home/<vm-user>/backup.log 2>&1
```

In the R2 dashboard, add a lifecycle rule on the backups bucket that deletes objects after 30 days.
**Test a restore once**, on a second free Supabase project (enable `vector` there first, point a copy of `.env`
at it), never on the live one: `./deploy/backup.sh restore backups/<file>.dump` replaces the app's tables.

Link previews for Facebook: share `https://api.agrotrends.com/share/questions/{id}` or `/share/stories/{slug}`.
They work as they are; no extra Caddy rule is needed.

## 7. Watch it

- A free uptime monitor (e.g. UptimeRobot) on `https://api.agrotrends.com/actuator/health`, alerting by e-mail.
- Logs: `docker compose logs --since 1h app`. They rotate at 5 × 20 MB.
- Database size: Supabase → Reports (free plan stops at 500 MB). Server disk: `df -h` now and then.

## 8. Release a new version

After `staging → production` is merged on GitHub:

```bash
cd ~/AgroTrends && git pull && docker compose up -d --build app
```

The API is down for about 30 seconds while the new container starts.

## 9. Move to a VPS later

1. On the new server: sections 4 and 5, with the same `.env` (copy it over `scp`, never through git or chat).
2. Point the `api` DNS record at the new server; Caddy gets a certificate on its own.
3. Check health, then stop the old server and decommission the old VM.

The database stays on Supabase and the images on R2, so nothing else moves.

## 10. Docker acceptance checks before production

- Sign in from the frontend, update a profile, restart the app, and verify the persisted profile.
- Upload a photo and cover; verify display from `agrotrends-files`. Replace/remove each and save; confirm that
  unreferenced old objects disappear from R2 while shared images remain. Deleting the origin object does not purge
  an already cached CDN/browser copy. Cleanup failures are logged and need manual retry; no historical orphan sweep.
- Request a password reset against Docker; verify actual Brevo delivery and complete the reset from the frontend.
- On an empty test database, verify the starter topics appear and a question/story can select one.
- Complete the unresolved backup/restore check, then Azure VM, DNS/HTTPS and uptime alerts.
