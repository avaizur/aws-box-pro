# Version Control & Work Separation Strategy

## Overview
This project is structured to support a dual-path strategy:
1.  **TEAM VIEW (SPLASH_ONLY)**: A professional branding-only demonstration for stakeholders.
2.  **PILOT VIEW (FULL_PILOT)**: The integrated solution including S3, Java Spring Boot, and AI analysis.

## How to Toggle Modes
The application mode is controlled by the following file:
`c:\Users\aahmad56\Downloads\aws-proj-17326\frontend\src\config.js`

- To show the **Team Branding** only: Set `APP_MODE = 'SPLASH_ONLY';`
- To show the **Full Hard-Work Solution**: Set `APP_MODE = 'FULL_PILOT';`

## Important Note for AI Assistants
Before making any changes to the UI or Backend integration, always check the `config.js` file and ask the USER: 
*"Which version of the project are we working on today? (Team Branding or Full Pilot?)"*

## Project Milestones
- **Infrastructure (AWS)**: Provisioned and stable.
- **Frontend (React)**: High-quality UI with mode-toggling support.
- **Backend (Spring Boot)**: Integration-ready orchestration layer.
- **AI Service (Python)**: Local and Bedrock engines configured.
