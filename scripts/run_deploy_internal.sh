#!/bin/bash
export PATH=$PATH:/c/Windows/System32/OpenSSH
export EC2_HOST="3.9.188.77"
export EC2_KEY="/c/Users/aahmad56/Downloads/aws-keys/aws-proj-17326.pem"
bash scripts/deploy.sh --skip-build
