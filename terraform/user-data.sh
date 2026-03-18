#!/bin/bash
# =============================================================
# user-data.sh — Cloud-init script that runs ONCE on first boot
# Terraform passes this to EC2 via the user_data argument.
#
# Purpose: prepare the OS so the first SCP deploy "just works".
# It does NOT deploy the actual application code — that is done
# separately using scripts/deploy.sh from your local machine.
# =============================================================
set -e
exec > /var/log/user-data.log 2>&1

echo "=== EC2 User-Data: Starting first-boot setup ==="

# ── Update system ─────────────────────────────────────────────
dnf update -y

# ── Install Java 17 ───────────────────────────────────────────
dnf install -y java-17-amazon-corretto

# ── Install Python 3 + pip ────────────────────────────────────
dnf install -y python3 python3-pip

# ── Install Node.js (for 'serve' — serves React static build) ─
dnf install -y nodejs
npm install -g serve

# ── Install Nginx ─────────────────────────────────────────────
dnf install -y nginx
systemctl enable nginx

# ── Install SQLite ────────────────────────────────────────────
dnf install -y sqlite

# ── Install AWS CLI v2 ────────────────────────────────────────
curl -s "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "/tmp/awscliv2.zip"
unzip -q /tmp/awscliv2.zip -d /tmp
/tmp/aws/install
rm -rf /tmp/awscliv2.zip /tmp/aws

# ── Create project directory structure ────────────────────────
mkdir -p /opt/myproject/{frontend/build,java-api,python-ai,nginx,data,uploads,logs,scripts}
chmod -R 755 /opt/myproject

# ── Initialise SQLite database file ───────────────────────────
sqlite3 /opt/myproject/data/app.db "
  CREATE TABLE IF NOT EXISTS analysis_requests (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    input_text    TEXT,
    file_name     TEXT,
    s3_key        TEXT,
    status        TEXT DEFAULT 'pending',
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP
  );
  CREATE TABLE IF NOT EXISTS analysis_results (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    request_id      INTEGER NOT NULL,
    summary         TEXT,
    word_count      INTEGER,
    classification  TEXT,
    processing_ms   INTEGER,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (request_id) REFERENCES analysis_requests(id)
  );
" 2>/dev/null || true

echo "=== EC2 User-Data: Setup complete ==="
