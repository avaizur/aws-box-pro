# Version Control & Work Separation Strategy

## Overview
This project is structured to support a dual-path strategy:
1.  **TEAM VIEW (SPLASH_ONLY)**: A professional branding-only demonstration for stakeholders.
2.  **PILOT VIEW (FULL_PILOT)**: The integrated solution including S3, Java Spring Boot, and AI analysis.

## 1. UI Mode Toggle
The application mode is controlled by the following file:
`c:\Users\aahmad56\Downloads\aws-proj-17326\frontend\src\config.js`

- To show the **Team Branding** only: Set `APP_MODE = 'SPLASH_ONLY';`
- To show the **Full Hard-Work Solution**: Set `APP_MODE = 'FULL_PILOT';`

## 2. Dual-Backend Strategy (EC2 vs Lambda)
We have implemented two ways to serve the AI analysis engine to optimize for cost and flexibility:
1.  **EC2 (Flask)**: Always-on, low-latency for frequent use.
2.  **Lambda (Serverless)**: Pay-as-you-go, cost-optimized for infrequent pilot tasks.

### How to Toggle Backend
Backend selection is controlled in `java-api/src/main/resources/application.properties`:
- For **EC2 Mode**: Set `ai.service.type=http`
- For **Lambda Mode**: Set `ai.service.type=lambda`

## Project Milestones
- **Infrastructure (AWS)**: Provisioned EC2 + New Lambda integration.
- **Frontend (React)**: High-quality UI with mode-toggling support.
- **Backend (Spring Boot)**: Support for Dual-Backend orchestration.
- **AI Service (Python)**: Available as both Flask App (EC2) and Lambda Function.
