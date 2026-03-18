###############################################################
# outputs.tf — Useful values printed after 'terraform apply'
###############################################################

output "ec2_public_ip" {
  description = "Public IP address of the EC2 instance."
  value       = aws_instance.app_server.public_ip
}

output "ec2_public_dns" {
  description = "Public DNS of the EC2 instance (use in SCP/SSH commands)."
  value       = aws_instance.app_server.public_dns
}

output "s3_bucket_name" {
  description = "Name of the S3 bucket for uploads, exports, and backups."
  value       = aws_s3_bucket.app_bucket.bucket
}

output "iam_role_name" {
  description = "Name of the IAM role attached to the EC2 instance for S3 access."
  value       = aws_iam_role.ec2_s3_role.name
}

output "ssh_command" {
  description = "Ready-to-use SSH command to connect to the EC2 instance."
  value       = "ssh -i ~/.ssh/${var.ec2_key_name}.pem ec2-user@${aws_instance.app_server.public_dns}"
}

output "app_url" {
  description = "URL to access the application after deployment."
  value       = "http://${aws_instance.app_server.public_ip}"
}
