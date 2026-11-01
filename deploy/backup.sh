#!/usr/bin/env bash
# Nightly database backup: a pg_dump next to docker-compose.yml, the last 7 kept, and a copy sent to Cloudflare R2
# when BACKUP_R2_BUCKET is set in .env. Run from cron (see docs/DEPLOY.md):
#   15 3 * * * /home/ubuntu/AgroTrends/deploy/backup.sh >> /home/ubuntu/backup.log 2>&1
# Restore:  docker compose exec -T db pg_restore -U postgres -d agrotrends --clean --if-exists < backups/<file>.dump
set -euo pipefail
cd "$(dirname "$0")/.."
# Read single keys rather than sourcing .env: a value with spaces or quotes must not run as shell.
env_value() { grep -E "^$1=" .env | tail -n 1 | cut -d= -f2- || true; }
db_user="$(env_value DB_USERNAME)"; db_user="${db_user:-postgres}"
bucket="$(env_value BACKUP_R2_BUCKET)"

mkdir -p backups
file="agrotrends-$(date -u +%Y-%m-%dT%H%M).dump"
docker compose exec -T db pg_dump -U "$db_user" -d agrotrends -Fc > "backups/$file"
echo "$(date -u) wrote backups/$file ($(du -h "backups/$file" | cut -f1))"

ls -1t backups/*.dump | tail -n +8 | xargs -r rm --

if [ -n "$bucket" ]; then
  docker run --rm -v "$PWD/backups:/backups:ro" \
    -e AWS_ACCESS_KEY_ID="$(env_value BACKUP_R2_ACCESS_KEY_ID)" \
    -e AWS_SECRET_ACCESS_KEY="$(env_value BACKUP_R2_SECRET_ACCESS_KEY)" \
    -e AWS_DEFAULT_REGION=auto \
    amazon/aws-cli:2.27.0 s3 cp "/backups/$file" "s3://$bucket/db/$file" --endpoint-url "$(env_value R2_ENDPOINT)"
  echo "$(date -u) copied to R2 bucket $bucket"
fi
