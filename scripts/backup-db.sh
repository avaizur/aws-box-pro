#!/bin/bash
# ============================================================
# backup-db.sh — Backup SQLite DB to S3
# Run manually or add to crontab:
#   0 2 * * * /opt/myproject/scripts/backup-db.sh >> /opt/myproject/logs/backup.log 2>&1
# ============================================================

DB_PATH="/opt/myproject/data/app.db"
BUCKET="${S3_BUCKET_NAME:-my-pilot-bucket}"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_KEY="backups/sqlite/app_backup_${TIMESTAMP}.db"

echo "[$(date)] Starting SQLite backup..."

# Copy the DB file locally first (safe even if API is writing)
cp "$DB_PATH" "/tmp/app_backup_${TIMESTAMP}.db"

# Upload to S3
aws s3 cp "/tmp/app_backup_${TIMESTAMP}.db" "s3://$BUCKET/$BACKUP_KEY"

# Clean up temp
rm -f "/tmp/app_backup_${TIMESTAMP}.db"

echo "[$(date)] Backup complete → s3://$BUCKET/$BACKUP_KEY"
