# Deploying the backend (one server, Docker)

One Linux server runs the app and Caddy (HTTPS) with `docker-compose.yml`; the database is **Supabase** (managed
PostgreSQL with pgvector). The same steps work on AWS EC2 now and on a VPS later; only section 3 is AWS-specific, and
moving servers never touches the database.

## 1. Before you start

- [ ] **Rotate the leaked keys** (roadmap 0.4): a new Gemini API key. Never reuse the old ones.
- [ ] **A domain** for the API, e.g. `api.agrotrends.com`. HTTPS needs one; a bare IP cannot get a certificate.
- [ ] **Cloudflare R2:** two buckets, `agrotrends-images` (public access on) and `agrotrends-backups` (private), each
      with its own "Object Read & Write" token scoped to that bucket.
- [ ] **Brevo:** verified sender domain and an SMTP key.
- [ ] **Google sign-in:** add the frontend's address under "Authorized JavaScript origins" of the OAuth client.

## 2. Supabase: the database

1. Sign up at supabase.com (GitHub sign-in works; no card on the free plan) and create an organization (Free).
2. **New project:**
   - Name: `agrotrends`.
   - Database password: **Generate a password** and save it in your password manager. It becomes `DB_PASSWORD`.
   - Region: **South Asia (Mumbai)** — the same as the EC2 server, so every query stays close.
   - Under the security options, **turn off the Data API**. The app talks to the database directly; with the Data
     API on, Supabase would also publish every table (users included) as a web API.
3. Wait until the project is ready (about 2 minutes).
4. **Database → Extensions:** search `vector` and enable it (schema `extensions`). The app needs it for the AI.
5. **Data API off check:** if you did not see the option at creation, open Project Settings, find **Data API**,
   and turn it off there.
6. **Connect** (the button at the top of the project) → connection string → method **Session pooler**. The direct
   connection is IPv6-only and EC2 has IPv4. From what it shows, note:
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

## 3. AWS: account and server

1. Sign up and pick the **Free plan**. Region: **Asia Pacific (Mumbai) `ap-south-1`**, the closest to Bangladesh.
2. **Billing → Budgets:** a monthly budget of $20 with an e-mail alert at 50 %. The free plan ends after 6 months or
   when the credits run out, and AWS then closes the account: move to the VPS (section 9) before that.
3. **EC2 → Launch instance:**
   - Ubuntu Server 24.04 LTS.
   - `t3.small` (2 GB) — or `t4g.small` (ARM, cheaper; every image here has an ARM build). Pick one marked
     "Free tier eligible". Not a 1 GB micro: the app and the database do not fit.
   - Key pair: create one and keep the `.pem` file safe.
   - Security group: SSH 22 from **My IP** only; HTTP 80 and HTTPS 443 from anywhere.
   - Storage: 30 GB gp3.
4. **EC2 → Elastic IPs:** allocate one and associate it with the instance, so the address survives a restart.
5. At your DNS provider: an **A record** `api` → the Elastic IP. On Cloudflare, set it to **DNS only** (grey
   cloud): the orange proxy hides visitors' IPs from the rate limits.

## 4. Prepare the server

```bash
ssh -i agrotrends.pem ubuntu@<elastic ip>

# 2 GB of swap: the Docker build and the JVM need headroom on a 2 GB server
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker ubuntu && exit        # sign in again so the group applies
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
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | from Supabase (section 2, step 6) |
| `JWT_SECRET` | `openssl rand -base64 48` |
| `GEMINI_API_KEY` | the new key |
| `FRONTEND_URL` | the frontend's address, e.g. `https://agrotrends.com` (used in e-mail links) |
| `CORS_ORIGINS` | only if the frontend has more than one address, comma-separated |
| `INITIAL_ADMIN_EMAIL`, `INITIAL_ADMIN_PASSWORD` | the super-admin (first start only; must be changed on first sign-in) |
| `GOOGLE_CLIENT_ID` | the OAuth web client id |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | Brevo |
| `R2_BUCKET`, `R2_ENDPOINT`, `R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY`, `UPLOADS_PUBLIC_BASE_URL` | images bucket |
| `BACKUP_R2_BUCKET`, `BACKUP_R2_ACCESS_KEY_ID`, `BACKUP_R2_SECRET_ACCESS_KEY` | backups bucket |

```bash
docker compose up -d --build                 # first build takes a few minutes
docker compose logs -f app                   # wait for "Started AgriculturalBlogApplication"
curl https://api.agrotrends.com/actuator/health     # {"status":"UP"}
```

Then sign in as the super-admin and change the password.

## 6. Backups

Supabase's free plan keeps none, so this nightly dump is the only backup.

```bash
./deploy/backup.sh                           # once by hand: writes backups/…dump and copies it to R2
crontab -e
# add:  15 3 * * * /home/ubuntu/AgroTrends/deploy/backup.sh >> /home/ubuntu/backup.log 2>&1
```

In the R2 dashboard, add a lifecycle rule on the backups bucket that deletes objects after 30 days.
**Test a restore once**, on a second free Supabase project (enable `vector` there first, point a copy of `.env`
at it), never on the live one: `./deploy/backup.sh restore backups/<file>.dump` replaces the app's tables.

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
3. Check health, then stop the old server and shut the AWS account down.

The database stays on Supabase and the images on R2, so nothing else moves.
