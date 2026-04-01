###############################################################
# main.tf — Core infrastructure for the AI Document Analysis pilot
#
# Provisions:
#   - EC2 instance (t3.micro)
#   - Security Group (SSH + HTTP)
#   - S3 Bucket (uploads, exports, backups)
#   - IAM Role + Policy (EC2 → S3 access, no hard-coded keys)
#   - IAM Instance Profile (attaches role to EC2)
#
# Does NOT provision:
#   - Route 53
#   - RDS / any managed database
#   - Load balancer
#   - Auto Scaling
#   - Multiple instances
###############################################################

# ── Data source: latest Amazon Linux 2023 AMI (optional override) ──────────
# Uncomment to auto-resolve AMI instead of using var.ami_id
# data "aws_ami" "amazon_linux_2023" {
#   most_recent = true
#   owners      = ["amazon"]
#   filter {
#     name   = "name"
#     values = ["al2023-ami-*-x86_64"]
#   }
# }

##############################################################
# SECURITY GROUP
##############################################################

resource "aws_security_group" "app_sg" {
  name        = "${var.project_name}-sg"
  description = "Allow SSH from your IP and HTTP from anywhere"

  # SSH — restricted to your IP only
  ingress {
    description = "SSH from admin IP"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.allowed_ssh_cidr]
  }

  # HTTP — open to the world (your app's public port)
  ingress {
    description = "HTTP from anywhere"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Allow all outbound (needed for pip install, package updates, S3 calls)
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_name}-sg"
  }
}

##############################################################
# IAM ROLE — EC2 can access S3 without hard-coded keys
##############################################################

resource "aws_iam_role" "ec2_s3_role" {
  name = "${var.project_name}-ec2-s3-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy" "ec2_s3_policy" {
  name = "${var.project_name}-ec2-s3-policy"
  role = aws_iam_role.ec2_s3_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "s3:PutObject",
          "s3:GetObject",
          "s3:DeleteObject",
          "s3:ListBucket"
        ]
        Resource = [
          aws_s3_bucket.app_bucket.arn,
          "${aws_s3_bucket.app_bucket.arn}/*"
        ]
      },
      # Allow Bedrock access
      {
        Effect = "Allow"
        Action = [
          "bedrock:InvokeModel"
        ]
        Resource = "arn:aws:bedrock:*:*:foundation-model/*"
      },
      # Optional: allow CloudWatch Logs agent
      {
        Effect = "Allow"
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ]
        Resource = "arn:aws:logs:*:*:*"
      }
    ]
  })
}

# Instance profile = the "wrapper" that attaches the role to the EC2 instance
resource "aws_iam_instance_profile" "ec2_profile" {
  name = "${var.project_name}-ec2-profile"
  role = aws_iam_role.ec2_s3_role.name
}

##############################################################
# S3 BUCKET — Uploads, exports, backups
##############################################################

resource "aws_s3_bucket" "app_bucket" {
  bucket = var.s3_bucket_name

  tags = {
    Name = var.s3_bucket_name
  }
}

# Block all public access — content is only accessed by the EC2 IAM role
resource "aws_s3_bucket_public_access_block" "app_bucket_block" {
  bucket = aws_s3_bucket.app_bucket.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# Optional: auto-delete files after 90 days to save costs during pilot
resource "aws_s3_bucket_lifecycle_configuration" "app_bucket_lifecycle" {
  bucket = aws_s3_bucket.app_bucket.id

  rule {
    id     = "expire-old-files"
    status = "Enabled"

    filter { prefix = "backups/" }

    expiration {
      days = 90
    }
  }
}

##############################################################
# EC2 INSTANCE — Hosts all application services
##############################################################

resource "aws_instance" "app_server" {
  ami                    = var.ami_id
  instance_type          = var.ec2_instance_type
  key_name               = var.ec2_key_name
  vpc_security_group_ids = [aws_security_group.app_sg.id]
  iam_instance_profile   = aws_iam_instance_profile.ec2_profile.name

  # Root volume — 20 GB is plenty for this pilot
  root_block_device {
    volume_size           = 20
    volume_type           = "gp3"
    delete_on_termination = true
  }

  # user-data.sh runs once on first boot to prepare the instance
  user_data = file("${path.module}/user-data.sh")

  tags = {
    Name = "${var.project_name}-server"
  }
}
