# Deploying the backend (one server, Docker)

One Linux server runs everything with `docker-compose.yml`: the app, PostgreSQL with pgvector, and Caddy for HTTPS.
The same steps work on AWS EC2 now and on a VPS later; only section 2 is AWS-specific.

## 1. Before you start

- [ ] **Rotate the leaked keys** (roadmap 0.4): new Gemini API key, new DB password. Never reuse the old ones.
- [ ] **A domain** for the API, e.g. `api.agrotrends.com`. HTTPS needs one; a bare IP cannot get a certificate.
- [ ] **Cloudflare R2:** two buckets, `agrotrends-images` (public access on) and `agrotrends-backups` (private), each
      with its own "Object Read & Write" token scoped to that bucket.
- [ ] **Brevo:** verified sender domain and an SMTP key.
- [ ] **Google sign-in:** add the frontend's address under "Authorized JavaScript origins" of the OAuth client.

## 2. AWS: account and server

1. Sign up and pick the **Free plan**. Region: **Asia Pacific (Mumbai) `ap-south-1`**, the closest to Bangladesh.
2. **Billing → Budgets:** a monthly budget of $20 with an e-mail alert at 50 %. The free plan ends after 6 months or
   when the credits run out, and AWS then closes the account: move to the VPS (section 8) before that.
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

## 3. Prepare the server

```bash
ssh -i agrotrends.pem ubuntu@<elastic ip>

# 2 GB of swap: the Docker build and the JVM need headroom on a 2 GB server
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker ubuntu && exit        # sign in again so the group applies
```

## 4. Configure and start

```bash
git clone https://github.com/nay-eem-01/AgroTrends.git && cd AgroTrends
git checkout production
cp .env.example .env && chmod 600 .env && nano .env
```

Set in `.env` (each is explained in `.env.example`):

| Key | Value |
|---|---|
| `API_DOMAIN` | `api.agrotrends.com` |
| `DB_PASSWORD` | new, long, random (`openssl rand -base64 24`) |
| `JWT_SECRET` | `openssl rand -base64 48` |
| `GEMINI_API_KEY` | the new key |
| `FRONTEND_URL` | the frontend's address, e.g. `https://agrotrends.com` (used in e-mail links) |
| `CORS_ORIGINS` | only if the frontend has more than one address, comma-separated |
| `INITIAL_ADMIN_EMAIL`, `INITIAL_ADMIN_PASSWORD` | the super-admin (first start only; must be changed on first sign-in) |
| `GOOGLE_CLIENT_ID` | the OAuth web client id |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | Brevo |
| `R2_BUCKET`, `R2_ENDPOINT`, `R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY`, `UPLOADS_PUBLIC_BASE_URL` | images bucket |
| `BACKUP_R2_BUCKET`, `BACKUP_R2_ACCESS_KEY_ID`, `BACKUP_R2_SECRET_ACCESS_KEY` | backups bucket |

Leave `DB_URL` unset: docker-compose points the app at its own database.

```bash
docker compose up -d --build                 # first build takes a few minutes
docker compose logs -f app                   # wait for "Started AgriculturalBlogApplication"
curl https://api.agrotrends.com/actuator/health     # {"status":"UP"}
```

Then sign in as the super-admin and change the password.

## 5. Backups

```bash
./deploy/backup.sh                           # once by hand: writes backups/…dump and copies it to R2
crontab -e
# add:  15 3 * * * /home/ubuntu/AgroTrends/deploy/backup.sh >> /home/ubuntu/backup.log 2>&1
```

In the R2 dashboard, add a lifecycle rule on the backups bucket that deletes objects after 30 days.
**Test a restore once** (it replaces the current data):
`docker compose exec -T db pg_restore -U postgres -d agrotrends --clean --if-exists < backups/<file>.dump`

## 6. Watch it

- A free uptime monitor (e.g. UptimeRobot) on `https://api.agrotrends.com/actuator/health`, alerting by e-mail.
- Logs: `docker compose logs --since 1h app`. They rotate at 5 × 20 MB.
- Disk: `df -h` now and then; the database and backups grow over time.

## 7. Release a new version

After `staging → production` is merged on GitHub:

```bash
cd ~/AgroTrends && git pull && docker compose up -d --build app
```

The API is down for about 30 seconds while the new container starts.

## 8. Move to a VPS later

1. On the new server: sections 3 and 4, with the same `.env` (copy it over `scp`, never through git or chat).
2. On the old server: `./deploy/backup.sh`, then `docker compose stop app` so nothing changes after the dump.
3. Copy the newest `backups/*.dump` to the new server and restore it (section 5).
4. Point the `api` DNS record at the new server; Caddy gets a certificate on its own.
5. Check health, then shut the AWS account down.

Images live in R2, so nothing else has to move.
