$KEY = "C:\Users\aahmad56\Downloads\aws-keys\aws-proj-17326.pem"
$IP = "3.9.188.77"
Write-Host ">>> Starting Deployment to $IP..."

# 1. Transfer the new Frontend build (Splash Only)
Write-Host ">>> Transferring frontend assets..."
scp -i $KEY -o StrictHostKeyChecking=no -r frontend/build/* ec2-user@${IP}:/opt/myproject/frontend/build/

# 2. Restart the app service to apply changes
Write-Host ">>> Restarting services on server..."
ssh -i $KEY -o StrictHostKeyChecking=no ec2-user@${IP} "sudo systemctl restart ai-doc-analysis.service"

Write-Host ">>> ✅ Deployment Complete! Visit: http://$IP"
