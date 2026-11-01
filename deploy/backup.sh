#!/usr/bin/env bash
# Nightly database backup: a pg_dump of the app's tables (schema public) from the database in DB_URL, the last 7
# kept in backups/ next to docker-compose.yml, and a copy sent to Cloudflare R2 when BACKUP_R2_BUCKET is set in
# .env. Supabase's free plan keeps no backups, so this is the only one. Run from cron (see docs/DEPLOY.md):
#   15 3 * * * /home/ubuntu/AgroTrends/deploy/backup.sh >> /home/ubuntu/backup.log 2>&1
# Restore (replaces the app's tables; the vector extension must already be enabled):
#   deploy/backup.sh restore backups/<file>.dump
set -euo pipefail
cd "$(dirname "$0")/.."

# Read single keys rather than sourcing .env: a value with spaces or quotes must not run as shell.
env_value() { grep -E "^$1=" .env | tail -n 1 | cut -d= -f2- || true; }
# DB_URL is JDBC (jdbc:postgresql://host:port/db?sslmode=require); libpq takes the same URL without "jdbc:".
db_url="$(env_value DB_URL)"; db_url="${db_url#jdbc:}"
db_user="$(env_value DB_USERNAME)"; db_user="${db_user:-postgres}"
# The client must be at least the server's version; Supabase runs PostgreSQL 15 or 17.
pg() { docker run --rm -i -e PGUSER="$db_user" -e PGPASSWORD="$(env_value DB_PASSWORD)" -v "$PWD/backups:/backups" postgres:17 "$@"; }

if [ "${1:-}" = "restore" ]; then
  pg pg_restore -d "$db_url" --schema=public --clean --if-exists --no-owner --no-privileges "/backups/$(basename "$2")"
  echo "$(date -u) restored $2"
  exit 0
fi

mkdir -p backups
file="agrotrends-$(date -u +%Y-%m-%dT%H%M).dump"
pg pg_dump -d "$db_url" --schema=public -Fc -f "/backups/$file"
echo "$(date -u) wrote backups/$file ($(du -h "backups/$file" | cut -f1))"

ls -1t backups/*.dump | tail -n +8 | xargs -r rm --

bucket="$(env_value BACKUP_R2_BUCKET)"
if [ -n "$bucket" ]; then
  docker run --rm -v "$PWD/backups:/backups:ro" \
    -e AWS_ACCESS_KEY_ID="$(env_value BACKUP_R2_ACCESS_KEY_ID)" \
    -e AWS_SECRET_ACCESS_KEY="$(env_value BACKUP_R2_SECRET_ACCESS_KEY)" \
    -e AWS_DEFAULT_REGION=auto \
    amazon/aws-cli:2.27.0 s3 cp "/backups/$file" "s3://$bucket/db/$file" --endpoint-url "$(env_value R2_ENDPOINT)"
  echo "$(date -u) copied to R2 bucket $bucket"
fi
