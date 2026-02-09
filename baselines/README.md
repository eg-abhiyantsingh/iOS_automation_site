# S3 Bucket Policy Baselines

**Jira Ticket:** ZP-774  
**Maintained by:** Abhiyant Singh, Pradip Chavda

## Overview

This directory contains the **expected S3 bucket policy definitions** for all 40 Terraform-managed buckets across 5 environments. These JSON files are the **single source of truth** used by the automated drift detection tests.

## Directory Structure

```
baselines/
├── README.md           ← This file
├── dev/                ← 8 baseline files for DEV environment
│   ├── eg-pz-dev-s3-asset-photos-ohio.json
│   ├── eg-pz-dev-s3-branding-ohio.json
│   ├── eg-pz-dev-s3-ir-photos-ohio.json
│   ├── eg-pz-dev-s3-frontend-ohio.json
│   ├── eg-pz-dev-s3-onboarding-jobs-ohio.json
│   ├── eg-pz-dev-s3-attachments-ohio.json
│   ├── eg-pz-dev-s3-reporting-jobs-ohio.json
│   └── eg-pz-dev-s3-slds-ohio.json
├── qa/                 ← 8 baseline files for QA environment
├── staging/            ← 8 baseline files for STAGING environment
├── prod/               ← 8 baseline files for PROD environment
└── bces-iq/            ← 8 baseline files for BCES-IQ environment
```

**Total: 40 JSON files** (8 services × 5 environments)

## Policy Types

### Branding Buckets (5 files — one per environment)
Two policy statements:
1. `PublicReadAccess` — Allows anyone to read objects (`s3:GetObject`)
2. `AllowAuthenticatedUploads` — Allows `cicd-devops` user to upload

### Non-Branding Buckets (35 files — seven per environment)
One policy statement:
1. `AllowAuthenticatedUploads` — Allows `cicd-devops` user to upload

## How to Update Baselines

When Terraform or the team makes an **intentional policy change**:

1. **Edit** the corresponding JSON file in `baselines/{env}/{bucket-name}.json`
2. **Verify** the JSON is valid: `cat baselines/prod/eg-pz-prod-s3-slds-ohio.json | jq .`
3. **Commit** the change to git
4. **Push** — the drift detection tests will now pass against the new expected policy

## Running the Tests

```bash
# All environments (42 tests)
mvn test -DsuiteXmlFile=testng-smoke.xml

# Single environment
mvn test -DsuiteXmlFile=testng-smoke-prod.xml

# Using groups
mvn test -DsuiteXmlFile=testng-smoke.xml -Dgroups=dev,qa
```

## AWS Configuration

| Setting | Value |
|---------|-------|
| Account ID | `165183897698` |
| Region | `us-east-2` (Ohio) |
| CICD User | `arn:aws:iam::165183897698:user/cicd-devops` |
| AWS Profile | `eg-pz-readonly` (override with `AWS_PROFILE` env var) |
