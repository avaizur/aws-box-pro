"""
s3_helper.py — Utility for simple S3 operations from Python.
Uses boto3 with the EC2 IAM Role credential chain (no hard-coded keys).

Usage:
    from s3_helper import upload_file, download_file, backup_sqlite

    upload_file("local/path/file.txt", "remote/key.txt")
    download_file("remote/key.txt", "local/copy.txt")
    backup_sqlite("/opt/myproject/data/app.db")
"""

import boto3
import os
import shutil
import datetime

BUCKET = os.environ.get("S3_BUCKET_NAME", "my-pilot-bucket")
REGION = os.environ.get("AWS_REGION", "us-east-1")

s3 = boto3.client("s3", region_name=REGION)


def upload_file(local_path: str, s3_key: str) -> str:
    """Upload a local file to S3. Returns the s3:// URI."""
    s3.upload_file(local_path, BUCKET, s3_key)
    return f"s3://{BUCKET}/{s3_key}"


def download_file(s3_key: str, local_path: str) -> None:
    """Download a file from S3 to a local path."""
    s3.download_file(BUCKET, s3_key, local_path)


def backup_sqlite(db_path: str = "/opt/myproject/data/app.db") -> str:
    """
    Copy the SQLite DB file and upload it to S3 as a timestamped backup.
    Call this nightly via cron or manually before a deployment.
    """
    timestamp = datetime.datetime.utcnow().strftime("%Y%m%d_%H%M%S")
    backup_local = f"/tmp/app_backup_{timestamp}.db"
    shutil.copy2(db_path, backup_local)

    s3_key = f"backups/sqlite/app_backup_{timestamp}.db"
    uri = upload_file(backup_local, s3_key)
    os.remove(backup_local)
    print(f"[S3 Backup] Uploaded → {uri}")
    return uri


if __name__ == "__main__":
    # Quick test: backup the DB
    backup_sqlite()
