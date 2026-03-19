###############################################################
# variables.tf — Input variables for the pilot infrastructure
###############################################################

variable "aws_region" {
  description = "AWS region to deploy into."
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Short project name used for resource naming and tagging."
  type        = string
  default     = "ai-doc-analysis"
}

variable "environment" {
  description = "Environment label (e.g. pilot, dev, prod)."
  type        = string
  default     = "pilot"
}

variable "ec2_instance_type" {
  description = "EC2 instance type. t3.micro is Free Tier eligible."
  type        = string
  default     = "t3.micro"
}

variable "ec2_key_name" {
  description = "aws-proj-17326.pem"
  type        = string
  # No default — you must supply this in terraform.tfvars
}

variable "allowed_ssh_cidr" {
  description = "Your IP address in CIDR notation for SSH access. E.g. '1.2.3.4/32'."
  type        = string
  # No default — restrict SSH to your IP only, not 0.0.0.0/0
}

variable "s3_bucket_name" {
  description = "Globally unique S3 bucket name for uploads, exports, and backups."
  type        = string
  # No default — S3 bucket names must be globally unique; set in terraform.tfvars
}

variable "ami_id" {
  description = "AMI ID for Amazon Linux 2023 in your region. Check AWS Console for latest."
  type        = string
  default     = "ami-0c02fb55956c7d316"  # Amazon Linux 2023 us-east-1 (update if needed)
}
