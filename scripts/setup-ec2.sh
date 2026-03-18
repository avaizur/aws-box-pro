#!/bin/bash
# ============================================================
# setup-ec2.sh — First-time setup on a fresh Amazon Linux 2023
# Run ONCE on the EC2 instance after SSH-ing in:
#   chmod +x setup-ec2.sh && sudo ./setup-ec2.sh
# ============================================================
set -e

echo "==========================================="
echo "  EC2 First-Time Setup — Pilot Project"
echo "==========================================="

# ── Update system ────────────────────────────────────────────
dnf update -y -q

# ── Install Java 17 ──────────────────────────────────────────
echo "Installing Java 17..."
dnf install -y java-17-amazon-corretto -q

# ── Install Python 3 ─────────────────────────────────────────
echo "Installing Python 3 + pip..."
dnf install -y python3 python3-pip -q

# ── Install Node.js (for 'serve') ────────────────────────────
echo "Installing Node.js..."
dnf install -y nodejs -q
npm install -g serve -q

# ── Install Nginx ────────────────────────────────────────────
echo "Installing Nginx..."
dnf install -y nginx -q
systemctl enable nginx

# ── Install SQLite ───────────────────────────────────────────
dnf install -y sqlite -q

# ── Create project directory structure ───────────────────────
echo "Creating /opt/myproject directory structure..."
mkdir -p /opt/myproject/{frontend/build,java-api,python-ai,nginx,data,uploads,logs,scripts}
chmod -R 755 /opt/myproject

# ── Create SQLite DB file ────────────────────────────────────
sqlite3 /opt/myproject/data/app.db "SELECT 1;" > /dev/null
echo "SQLite DB initialized at /opt/myproject/data/app.db"

# ── Install AWS CLI v2 (for manual S3 ops) ───────────────────
echo "Installing AWS CLI v2..."
curl -s "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip -q awscliv2.zip
./aws/install --update
rm -rf awscliv2.zip aws/

# ── Install CloudWatch Agent (optional) ──────────────────────
echo "Installing CloudWatch Agent..."
dnf install -y amazon-cloudwatch-agent -q || echo "(CloudWatch install skipped)"

echo ""
echo "==========================================="
echo "  ✅  EC2 setup complete!"
echo "  Java:    $(java -version 2>&1 | head -1)"
echo "  Python:  $(python3 --version)"
echo "  nginx:   $(nginx -v 2>&1)"
echo "  AWS CLI: $(aws --version)"
echo "==========================================="
