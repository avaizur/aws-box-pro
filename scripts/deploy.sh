#!/bin/bash
# =============================================================
# deploy.sh — AI Document Analysis Service
# Run from your LOCAL machine to deploy to EC2.
#
# Usage (local):
#   export EC2_HOST="ec2-XX-XX-XX-XX.compute-1.amazonaws.com"
#   export EC2_KEY="~/.ssh/my-key.pem"
#   bash scripts/deploy.sh
#
# Usage (skip build — called after CI already built artifacts):
#   export EC2_HOST="..."
#   export EC2_KEY="..."
#   bash scripts/deploy.sh --skip-build
#
# Note: GitHub Actions does NOT call this script directly.
# The workflow (.github/workflows/deploy.yml) replicates the
# SCP + SSH restart steps itself. This script is for local use.
# --skip-build exists as a convenience for advanced users who
# want to call this script from their own pipelines.
# =============================================================
set -e

# ── Parse flags ───────────────────────────────────────────────
SKIP_BUILD=false
for arg in "$@"; do
  case $arg in
    --skip-build) SKIP_BUILD=true ;;
  esac
done

# ── Config ────────────────────────────────────────────────────
EC2_HOST="${EC2_HOST:?Set EC2_HOST env var (e.g. from terraform output)}"
EC2_USER="${EC2_USER:-ec2-user}"
REMOTE_DIR="/opt/myproject"
JAR_NAME="java-api-1.0.0.jar"

# SSH key: prefer EC2_KEY_FILE (written by CI), fall back to EC2_KEY (.pem path)
if [ -n "$EC2_KEY_FILE" ]; then
  SSH_KEY="$EC2_KEY_FILE"
elif [ -n "$EC2_KEY" ]; then
  SSH_KEY="$EC2_KEY"
else
  echo "ERROR: Set either EC2_KEY (path to .pem) or EC2_KEY_FILE (temp key file)"
  exit 1
fi

echo "=============================================="
echo "  Deploying AI Document Analysis Service"
echo "  Target: $EC2_HOST"
echo "  Skip build: $SKIP_BUILD"
echo "=============================================="

# ── Step 1: Build React Frontend (skipped in CI) ─────────────
if [ "$SKIP_BUILD" = false ]; then
  echo ""
  echo ">>> [1/5] Building React frontend..."
  cd frontend
  npm install --silent
  npm run build --silent
  cd ..
  echo "    ✓ React build complete"
else
  echo ">>> [1/5] Skipping React build (--skip-build)"
fi

# ── Step 2: Build Java JAR (skipped in CI) ───────────────────
if [ "$SKIP_BUILD" = false ]; then
  echo ""
  echo ">>> [2/5] Building Java API..."
  cd java-api
  mvn clean package -DskipTests -q
  cd ..
  echo "    ✓ Java JAR built"
else
  echo ">>> [2/5] Skipping Java build (--skip-build)"
fi

# ── Verify artifacts exist before trying to SCP ──────────────
if [ ! -d "frontend/build" ]; then
  echo "ERROR: frontend/build/ not found. Run 'npm run build' first."
  exit 1
fi
if [ ! -f "java-api/target/$JAR_NAME" ]; then
  echo "ERROR: java-api/target/$JAR_NAME not found. Run 'mvn clean package' first."
  exit 1
fi

# ── Step 3: Create staging dir on EC2 ────────────────────────
echo ""
echo ">>> [3/5] Preparing EC2 directories..."
ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no \
    "$EC2_USER@$EC2_HOST" "mkdir -p /tmp/deploy /opt/myproject/{java-api,python-ai,frontend/build,scripts,logs}"

# ── Step 4: SCP all files ─────────────────────────────────────
echo ""
echo ">>> [4/5] Copying files to EC2..."
SCP="scp -i $SSH_KEY -o StrictHostKeyChecking=no"

$SCP "java-api/target/$JAR_NAME"          "$EC2_USER@$EC2_HOST:$REMOTE_DIR/java-api/$JAR_NAME"
$SCP python-ai/app.py python-ai/requirements.txt \
                                           "$EC2_USER@$EC2_HOST:$REMOTE_DIR/python-ai/"
$SCP -r frontend/build/.                  "$EC2_USER@$EC2_HOST:$REMOTE_DIR/frontend/build/"
$SCP nginx/nginx.conf                     "$EC2_USER@$EC2_HOST:/tmp/deploy/nginx.conf"
$SCP scripts/start-services.sh            "$EC2_USER@$EC2_HOST:$REMOTE_DIR/scripts/start-services.sh"

echo "    ✓ All files copied"

# ── Step 5: Remote install & restart ─────────────────────────
echo ""
echo ">>> [5/5] Installing dependencies and restarting services..."
ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no "$EC2_USER@$EC2_HOST" bash << 'REMOTE'
  set -e

  cd /opt/myproject/python-ai
  pip3 install -r requirements.txt -q
  echo "    ✓ Python dependencies installed"

  sudo cp /tmp/deploy/nginx.conf /etc/nginx/nginx.conf
  sudo nginx -t
  echo "    ✓ Nginx config valid"

  chmod +x /opt/myproject/scripts/start-services.sh
  sudo /opt/myproject/scripts/start-services.sh
REMOTE

echo ""
echo "=============================================="
echo "  ✅  Deployment complete!"
echo "  Visit: http://$EC2_HOST"
echo "  API:   http://$EC2_HOST/api/health"
echo "  AI:    http://$EC2_HOST/ai/health"
echo "=============================================="
