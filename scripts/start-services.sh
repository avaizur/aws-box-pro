#!/bin/bash
# =============================================================
# start-services.sh — Restart all application services on EC2.
# Run this on the EC2 instance (called by deploy.sh remotely,
# or run manually after SSH-ing in).
#
# Services managed:
#   - React frontend  (port 3000, served by 'serve')
#   - Java Spring Boot API (port 8080)
#   - Python Flask AI service (port 5000, via gunicorn)
#   - Nginx (reverse proxy, port 80)
# =============================================================
set -e

REMOTE_DIR="/opt/myproject"
LOG_DIR="$REMOTE_DIR/logs"
JAR_NAME="java-api-1.0.0.jar"

mkdir -p "$LOG_DIR"

echo "--- Stopping existing processes ---"
pkill -f "$JAR_NAME"     2>/dev/null || true
pkill -f "gunicorn"      2>/dev/null || true
pkill -f "serve.*build"  2>/dev/null || true
sleep 2

# ── Java API (port 8080) ──────────────────────────────────────
echo "--- Starting Java API (port 8080) ---"
nohup java -jar "$REMOTE_DIR/java-api/$JAR_NAME" \
    --spring.datasource.url="jdbc:sqlite:$REMOTE_DIR/data/app.db" \
    --ai.service.url="http://localhost:5000" \
    --app.s3.bucket-name="${S3_BUCKET_NAME:-ai-doc-analysis-awsboxapp-london}" \
    --logging.file.name="$LOG_DIR/java-api.log" \
    > "$LOG_DIR/java-api-console.log" 2>&1 &
echo "  Java API started (PID $!)"

# Wait a moment for Java to start before Python
sleep 2

# ── Python Flask AI service (port 5000) ──────────────────────
echo "--- Starting Python AI service (port 5000) ---"
cd "$REMOTE_DIR/python-ai"
nohup gunicorn -w 2 -b 0.0.0.0:5000 app:app \
    > "$LOG_DIR/python-ai.log" 2>&1 &
echo "  Python AI started (PID $!)"

# ── React Frontend (port 3000) ───────────────────────────────
echo "--- Starting React frontend (port 3000) ---"
nohup serve -s "$REMOTE_DIR/frontend/build" -l 3000 \
    > "$LOG_DIR/frontend.log" 2>&1 &
echo "  React frontend started (PID $!)"

# ── Nginx ────────────────────────────────────────────────────
echo "--- Reloading Nginx ---"
sudo systemctl restart nginx
echo "  Nginx restarted"

echo ""
echo "==================================================="
echo "  All services started. Logs in: $LOG_DIR"
echo ""
echo "  Health checks:"
echo "    curl http://localhost:8080/api/health"
echo "    curl http://localhost:5000/health"
echo "==================================================="
