# AI Document Analysis Service

**AWS Pilot Project · Workplace Learning · Single EC2 Instance**

A standalone workplace-style pilot project built on AWS.  
Users submit text or documents, and the system returns a structured analysis: summary, word count, and document classification.

This project is designed to be minimal, cheap, beginner-friendly, and easy to explain in interviews or internal discussions.

---

## What It Does

| Step | What Happens |
|---|---|
| User submits text or uploads a file | Via the React frontend (PDF, Word, Text supported) |
| Java API receives the request | Validates input, extracts text via Apache Tika |
| Java calls the Python AI service | Sends text via internal HTTP call |
| Python analyses the document | Returns: summary, classification, and PII detection |
| Java stores result in SQLite | Persists request + result (including sensitive data flags) |
| Java optionally uploads to S3 | Stores the original file or exported report |
| Frontend displays the result | Summary card with PII warnings and category badges |

---

## Architecture

```
         Browser / User
               │  HTTP :80
               ▼
   ┌───────────────────────────────────────┐
   │  Security Group (port 80 + 22)        │
   │  ┌────────────────────────────────┐   │
   │  │  EC2 t3.micro (Amazon Linux)   │   │
   │  │                                │   │
   │  │  ┌──────────────────────────┐  │   │
   │  │  │    Nginx  :80            │  │   │
   │  │  │  /         → :3000       │  │   │
   │  │  │  /api/*    → :8080       │  │   │
   │  │  │  /ai/*     → :5000       │  │   │
   │  │  └──────┬──────────┬────────┘  │   │
   │  │         │          │           │   │
   │  │  ┌──────▼──┐  ┌────▼───────┐  │   │
   │  │  │  React  │  │  Java API  │──┼───┼──► S3 Bucket
   │  │  │  :3000  │  │  :8080     │  │   │   uploads/
   │  │  └─────────┘  └────┬───────┘  │   │   exports/
   │  │                    │          │   │   backups/
   │  │               ┌────▼───────┐  │   │
   │  │               │ Python AI  │  │   │
   │  │               │  :5000     │  │   │
   │  │               └────────────┘  │   │
   │  │                               │   │
   │  │  SQLite: /opt/myproject/data/app.db│
   │  └────────────────────────────────┘  │
   └───────────────────────────────────────┘
```

**Terraform provisions** → EC2, Security Group, S3 Bucket, IAM Role  
**SSH/SCP scripts deploy** → JAR, Python, React build, Nginx config

---

## Project Structure

```
aws-proj-17326/
│
├── frontend/                   ← React web application
│   ├── src/
│   │   ├── App.jsx             ← Main UI: text input, file upload, results, history
│   │   └── App.css             ← Dark-mode professional styling
│   └── package.json
│
├── java-api/                   ← Spring Boot REST API (main orchestrator)
│   ├── pom.xml                 ← Dependencies: SQLite JDBC, AWS SDK v2, Spring Web/JPA
│   └── src/main/
│       ├── resources/
│       │   └── application.properties   ← SQLite path, AI service URL, S3 config
│       └── java/com/aidocanalysis/
│           ├── JavaApiApplication.java
│           ├── model/
│           │   ├── AnalysisRequest.java   ← 'analysis_requests' table
│           │   └── AnalysisResult.java    ← 'analysis_results' table
│           ├── repository/
│           │   ├── AnalysisRequestRepository.java
│           │   └── AnalysisResultRepository.java
│           ├── service/
│           │   ├── AiServiceClient.java   ← HTTP client → Python AI
│           │   └── S3Service.java         ← S3 upload/download
│           └── controller/
│               └── AnalysisController.java  ← All /api/* endpoints
│
├── python-ai/                  ← Flask AI processing service
│   ├── app.py                  ← /analyze endpoint: summary, word count, classification
│   ├── s3_helper.py            ← S3 utility (backup, upload, download)
│   └── requirements.txt
│
├── nginx/
│   └── nginx.conf              ← Reverse proxy routing
│
├── data/                       ← SQLite DB file lives here on EC2
├── uploads/                    ← Temporary local file staging
├── logs/                       ← All service logs
│
├── scripts/
│   ├── setup-ec2.sh            ← First-time EC2 setup (run once)
│   ├── deploy.sh               ← Your main deploy command (run from laptop)
│   ├── start-services.sh       ← Restart all services on EC2
│   └── backup-db.sh            ← Backup SQLite DB to S3
│
└── terraform/
    ├── versions.tf             ← Terraform + AWS provider version pins
    ├── main.tf                 ← EC2, Security Group, S3, IAM Role
    ├── variables.tf            ← Input variable definitions
    ├── outputs.tf              ← Public IP, DNS, SSH command, bucket name
    ├── terraform.tfvars        ← YOUR values (not committed to git)
    └── user-data.sh            ← Cloud-init script (runs once on first EC2 boot)

.github/
└── workflows/
    └── deploy.yml              ← GitHub Actions: build → deploy on push to main
```

---

## Getting Started

### Prerequisites

- AWS account
- Terraform ≥ 1.6 installed
- Java 17+ and Maven installed (local machine)
- Node.js 18+ and npm installed (local machine)
- An EC2 Key Pair created in your AWS region

### Step 1 — Configure Terraform

```bash
cd terraform

# Edit terraform.tfvars with your values:
#   ec2_key_name     = "your-key-pair-name"
#   allowed_ssh_cidr = "YOUR.IP.ADDRESS.HERE/32"  ← find at checkip.amazonaws.com
#   s3_bucket_name   = "ai-doc-analysis-yourname-2026"
```

### Step 2 — Provision Infrastructure

```bash
terraform init
terraform plan
terraform apply
```

After `apply`, Terraform prints:
```
ec2_public_ip   = "18.168.203.82"
ec2_public_dns  = "ec2-18-168-203-82.eu-west-2.compute.amazonaws.com"
s3_bucket_name  = "ai-doc-analysis-yourname-2026"
ssh_command     = "ssh -i ~/.ssh/your-key.pem ec2-user@18.168.203.82"
app_url         = "http://18.168.203.82"
```

The EC2 instance automatically runs `user-data.sh` on first boot, which installs Java, Python, Nginx, Node, SQLite, AWS CLI, and creates the directory structure.

### Step 3 — Deploy the Application

```bash
# From the project root:
export EC2_HOST="18.168.203.82"   # from terraform output
export EC2_KEY="~/.ssh/your-key.pem"

bash scripts/deploy.sh
```

`deploy.sh` will:
1. Build the React frontend (`npm run build`)
2. Build the Java JAR (`mvn clean package`)
3. SCP all files to EC2
4. Install Python dependencies on EC2
5. Reload Nginx and restart all three services

### Step 4 — Verify

```bash
# From outside (via Nginx):
curl http://18.168.203.82/api/health  # → {"status":"ok","service":"java-api"}
curl http://18.168.203.82/ai/health   # → {"status":"ok","service":"python-ai"}

# Open in browser:
http://18.168.203.82
```

---

## API Reference

### Java REST API (`/api/*`)

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/health` | Service health check |
| `POST` | `/api/analyze/text` | Submit plain text for analysis |
| `POST` | `/api/analyze/file` | Upload a document file for analysis |
| `GET` | `/api/analyze/history` | List all past analysis requests |
| `GET` | `/api/analyze/{id}` | Get result for a specific request |
| `POST` | `/api/analyze/{id}/export` | Export result as JSON to S3 |

**Example — Text analysis:**
```bash
curl -X POST http://18.168.203.82/api/analyze/text \
     -H "Content-Type: application/json" \
     -d '{"text": "Your document content here..."}'
```

**Response:**
```json
{
  "requestId": 1,
  "status": "completed",
  "summary": "Your document content here...",
  "wordCount": 5,
  "classification": "general",
  "processingMs": 12,
  "createdAt": "2026-03-17T17:40:11"
}
```

**Example — File upload:**
```bash
curl -X POST http://18.168.203.82/api/analyze/file \
     -F "file=@/path/to/document.txt"
```

### Python AI Service (`/ai/*`)

> **Note:** The Python AI service is called by the Java API internally. The frontend never calls it directly.

| Method | Path | Description |
|---|---|---|
| `GET` | `/ai/health` | Service health check |
| `POST` | `/ai/analyze` | Analyse text (called by Java API only) |

**Java → Python request:**
```json
{ "text": "Full document text content..." }
```

**Python → Java response:**
```json
{
  "summary": "First 1-3 sentences of the document...",
  "word_count": 142,
  "classification": "technical",
  "processing_ms": 18
}
```

**Classification labels:**

| Label | Meaning |
|---|---|
| `technical` | Code, specs, APIs, engineering content |
| `legal` | Contracts, clauses, compliance language |
| `financial` | Budgets, reports, revenue, cost data |
| `general` | Everything else |

---

## Database Schema (SQLite)

**File location on EC2:** `/opt/myproject/data/app.db`

### `analysis_requests` table

| Column | Type | Description |
|---|---|---|
| `id` | INTEGER PK | Auto-incremented request ID |
| `input_text` | TEXT | Raw text submitted by the user |
| `file_name` | TEXT | Original filename (if file uploaded; NULL otherwise) |
| `s3_key` | TEXT | S3 key of uploaded file (NULL if text-only) |
| `status` | TEXT | `pending` → `completed` or `failed` |
| `created_at` | DATETIME | When the request was received |

### `analysis_results` table

| Column | Type | Description |
|---|---|---|
| `id` | INTEGER PK | Auto-incremented result ID |
| `request_id` | INTEGER FK | Links to `analysis_requests.id` |
| `summary` | TEXT | AI-generated extractive summary |
| `word_count` | INTEGER | Total word count of input text |
| `classification` | TEXT | `technical`, `legal`, `financial`, or `general` |
| `processing_ms` | INTEGER | Time taken by Python AI service (ms) |
| `created_at` | DATETIME | When the result was stored |

**Inspect the DB directly on EC2:**
```bash
sqlite3 /opt/myproject/data/app.db
.tables
SELECT * FROM analysis_results LIMIT 5;
.quit
```

---

## S3 Usage

| Folder | Contents | When |
|---|---|---|
| `uploads/` | Original uploaded document files | When user uploads a file |
| `exports/` | JSON result exports | When user clicks "Export to S3" |
| `backups/` | SQLite `.db` backup files | On-demand or via nightly cron |

**No hard-coded access keys.** The EC2 IAM Role provides credentials automatically.

```bash
# Nightly backup via cron (add to EC2's crontab):
0 2 * * * S3_BUCKET_NAME=your-bucket /opt/myproject/scripts/backup-db.sh

# Manual CLI examples:
aws s3 ls s3://your-bucket/
aws s3 cp /opt/myproject/data/app.db s3://your-bucket/backups/manual.db
```

---

## Terraform Scope

Terraform manages **infrastructure only**. It does not deploy application code.

| File | Purpose |
|---|---|
| `versions.tf` | Terraform and AWS provider version pins |
| `main.tf` | EC2 instance, Security Group, S3 bucket, IAM role + policy + profile |
| `variables.tf` | Input variable definitions and defaults |
| `outputs.tf` | Prints EC2 IP/DNS, S3 bucket name, ready-to-use SSH command |
| `terraform.tfvars` | Your actual values (key name, IP, bucket name) — **do not commit** |
| `user-data.sh` | Cloud-init script: installs runtimes, creates directories, initialises SQLite schema |

**Terraform does NOT manage:**
- Route 53 / DNS
- RDS or any managed database
- Load balancers
- Auto Scaling
- Application code or deployments

---

## Manual Deployment Flow

```
Your Laptop                         EC2 Instance
──────────────────────────────      ─────────────────────────────────
terraform apply              →      EC2 launched, user-data.sh runs
                                    (install Java, Python, Nginx, SQLite)

npm run build                →      [local: frontend/build/ created]
mvn clean package            →      [local: java-api/target/*.jar created]

bash scripts/deploy.sh       →      SCP JAR, Python, React, Nginx config
                                    pip install -r requirements.txt
                                    nginx -t && systemctl reload nginx
                                    start-services.sh:
                                      - Java JAR (nohup)
                                      - Gunicorn (nohup)
                                      - serve :3000 (nohup)
                                      - Nginx reload
```

---

## Cost Estimate

| Resource | Configuration | Est. Monthly Cost |
|---|---|---|
| EC2 | t3.micro (Free Tier for 12 months) | $0 – $8.50 |
| EBS | 20 GB gp3 root volume | ~$1.60 |
| S3 | 5 GB storage + minimal API calls | ~$0.12 |
| Data Transfer | First 100 GB outbound free | $0 |
| CloudWatch Logs | 5 GB/month free tier | $0 |
| **Total** | | **~$2 – $14 / month** |

> 💡 Stop the EC2 instance when not using it — you only pay for running hours.

---

## Useful Commands

**On EC2 (after SSH-ing in):**

```bash
# Check health of all services
curl http://localhost:8080/api/health
curl http://localhost:5000/health

# View logs in real time
tail -f /opt/myproject/logs/java-api.log
tail -f /opt/myproject/logs/python-ai.log
tail -f /opt/myproject/logs/nginx-access.log

# Restart all services
sudo /opt/myproject/scripts/start-services.sh

# Inspect the database
sqlite3 /opt/myproject/data/app.db
  .tables
  SELECT * FROM analysis_requests;
  SELECT * FROM analysis_results;
  .quit

# Backup the database to S3
S3_BUCKET_NAME=your-bucket bash /opt/myproject/scripts/backup-db.sh

# Check Nginx status
sudo systemctl status nginx
sudo nginx -t    # test config syntax

# Check Application Service (Auto-start)
sudo systemctl status ai-doc-analysis
sudo journalctl -u ai-doc-analysis -f  # view service logs
```

**Terraform commands:**

```bash
cd terraform
terraform init          # first time only
terraform plan          # preview changes
terraform apply         # create/update infrastructure
terraform output        # print outputs (IP, DNS, S3 bucket)
terraform destroy       # tear down everything (saves cost)
```

---

## Environment Variables

Set on EC2 (in `/etc/environment` or as shell exports):

| Variable | Example | Used By |
|---|---|---|
| `S3_BUCKET_NAME` | `ai-doc-analysis-yourname-2026` | Java, Python, backup script |
| `AWS_REGION` | `us-east-1` | Java, Python |
| `SQLITE_DB_PATH` | `/opt/myproject/data/app.db` | Java (overrides default) |
| `AI_SERVICE_URL` | `http://localhost:5000` | Java (calls Python) |

Set on your local machine before running `deploy.sh`:

| Variable | Example |
|---|---|
| `EC2_HOST` | `ec2-54-x-x-x.compute-1.amazonaws.com` |
| `EC2_KEY` | `~/.ssh/my-key.pem` |

---

## GitHub and GitHub Actions

### Source Control Setup

This project is hosted on GitHub. The repository structure maps directly to the local project folder you already have.

```bash
# First time — push your local project to a new GitHub repo
git init
git add .
git commit -m "Initial commit: AI Document Analysis Service"
git branch -M main
git remote add origin https://github.com/your-username/ai-doc-analysis.git
git push -u origin main
```

> **Important:** Make sure `terraform.tfvars` is in your `.gitignore` before the first commit — it is by default in this project.

---

### GitHub Actions Workflow

**File:** [`.github/workflows/deploy.yml`](.github/workflows/deploy.yml)

The workflow triggers automatically on every push to the `main` branch. It runs on a free GitHub-hosted Ubuntu runner.

**What it does, step by step:**

| Step | Action |
|---|---|
| 1 | Checkout the repository code |
| 2 | Set up Node.js 20 and build the React frontend |
| 3 | Set up Java 17 (Corretto) and build the Java JAR via Maven |
| 4 | Run basic smoke checks (verify build artefacts exist, Python syntax check) |
| 5 | Load the EC2 SSH private key from GitHub Secrets into a temp file |
| 6 | SCP the JAR, Python files, React build, and Nginx config to EC2 |
| 7 | SSH into EC2: install Python deps, validate Nginx config, restart all services |
| 8 | Delete the temp SSH key file (cleanup always runs, even on failure) |
| 9 | Print a deployment summary with the live app URL |

---

### Required GitHub Secrets

Go to your repository → **Settings → Secrets and variables → Actions → New repository secret**

| Secret Name | Value | Where to find it |
|---|---|---|
| `EC2_HOST` | `ec2-54-x-x-x.compute-1.amazonaws.com` | `terraform output ec2_public_dns` |
| `EC2_USER` | `ec2-user` | Fixed for Amazon Linux |
| `EC2_SSH_KEY` | Full contents of your `.pem` private key file | Open `.pem` file in a text editor, copy everything including `-----BEGIN RSA PRIVATE KEY-----` |
| `S3_BUCKET_NAME` | `ai-doc-analysis-yourname-2026` | `terraform output s3_bucket_name` |

**How to add a secret:**
```
Repository → Settings → Secrets and variables → Actions
→ New repository secret → Name + Value → Add secret
```

> **Security note:** Secrets are never shown in workflow logs. GitHub masks them automatically. The SSH key is also deleted from the runner at the end of every job.

---

### Workflow vs. Manual deploy.sh — What\'s the Difference?

| | `deploy.sh` (local) | GitHub Actions workflow |
|---|---|---|
| **Trigger** | You run it manually | Runs automatically on push to `main` |
| **Builds** | On your laptop | On GitHub\'s free Ubuntu runner |
| **SSH key** | Your local `.pem` file | Stored in GitHub Secrets |
| **SCP** | From your laptop | From the GitHub runner |
| **Use case** | Quick fix without committing | Standard development flow |

Both approaches deploy exactly the same files in exactly the same way. The core architecture (single EC2, Nginx, SQLite) is completely unchanged.

---

### How to Trigger a Deployment

```bash
# Automatic — just push to main:
git add .
git commit -m "Update Python analysis logic"
git push origin main
# → GitHub Actions triggers automatically

# Manual — via GitHub UI:
# Go to Actions → Build and Deploy → Run workflow → Run workflow
```

Watch the deployment live: **GitHub → Actions tab → latest run → click to expand each step**.

---

## Future Enhancements (Later Phases)

> These are intentionally out of scope for the pilot. Listed here for future planning only.

- **Phase 2:** Replace keyword classification with a real ML model (scikit-learn or Hugging Face transformers)
- **Phase 2:** Add HTTPS using a self-signed certificate or AWS Certificate Manager
- **Phase 3:** Add user authentication (JWT or AWS Cognito)
- **Phase 3:** Move from SQLite to PostgreSQL via Amazon RDS (when concurrency is needed)
- **Phase 4:** Add a custom domain using Route 53
- **Phase 4:** Add a load balancer and second EC2 instance for high availability
- **Phase 5:** Containerise services with Docker and deploy via ECS

---

## What This Project Teaches

By building and deploying this project you will learn:

| Concept | Where |
|---|---|
| Launching and configuring EC2 | Terraform + user-data.sh |
| Security Groups (inbound rules) | Terraform main.tf |
| IAM Roles for EC2-to-S3 access | Terraform main.tf |
| Terraform basics (init/plan/apply/output) | terraform/ directory |
| Nginx as a reverse proxy | nginx/nginx.conf |
| Spring Boot REST API development | java-api/ |
| Connecting Java to SQLite | application.properties |
| Service-to-service HTTP communication | AiServiceClient.java |
| S3 file operations with AWS SDK | S3Service.java |
| Python Flask microservice | python-ai/app.py |
| SQLite database design | user-data.sh schema |
| Manual SSH/SCP deployment | scripts/deploy.sh |
| GitHub as a source control host | .github/ + README |
| GitHub Actions CI/CD basics | .github/workflows/deploy.yml |
| Storing secrets securely in GitHub | GitHub Secrets (not in code) |

---

## Design Principles

- **One EC2 instance** — keeps it simple and cheap
- **SQLite** — no database server to manage or pay for
- **IAM Role** — no hard-coded AWS credentials anywhere
- **Terraform for infrastructure, scripts for deployment** — clean separation of concerns
- **GitHub Actions for automated deploys** — push to main, deployment runs automatically
- **GitHub Secrets for credentials** — SSH key never stored in code
- **No Docker, no Kubernetes** — reduce complexity for learning purposes
