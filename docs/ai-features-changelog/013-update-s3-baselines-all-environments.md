# Changelog 013 — Update S3 Baseline Policies for All Environments

**Date**: 2026-04-15  
**Time**: ~20:30 IST  
**Prompt**: S3 bucket policy updated for QA — apply same pattern to dev, staging, prod, bces-iq  

---

## Summary

Updated **32 S3 baseline policy files** across 4 environments (dev, staging, prod, bces-iq) to include the new `DenyInsecureTransport` and `AllowCICDDevopsAccess` statements. QA was already updated in Changelog 011 — this brings the remaining environments into alignment.

---

## Background: What Are S3 Baselines?

The S3 drift detection system (`S3PolicyChecker.java`) compares **live AWS S3 bucket policies** against **baseline JSON files** stored in `baselines/{env}/`. If they differ, the test fails with "S3 Bucket Policy Drift Detected."

When AWS policies change (e.g., security team adds new statements), the baselines must be updated to match. Otherwise, every S3 drift test fails — not because of actual drift, but because the baseline is outdated.

### How Comparison Works

`S3PolicyChecker.normalizeJson()` normalizes both baseline and live JSON:
- **Sorts object keys** recursively (so key order doesn't matter)
- **Preserves array order** (so `Statement[0]`, `Statement[1]`, etc. must be in the same order)

This means:
- `{"Effect": "Allow", "Sid": "X"}` equals `{"Sid": "X", "Effect": "Allow"}` (key order irrelevant)
- `[Statement_A, Statement_B]` does NOT equal `[Statement_B, Statement_A]` (array order matters)

---

## What Changed in AWS

The security/DevOps team added two new policy statements to ALL S3 buckets across ALL environments:

### 1. DenyInsecureTransport
```json
{
  "Sid": "DenyInsecureTransport",
  "Effect": "Deny",
  "Principal": "*",
  "Action": "s3:*",
  "Resource": ["arn:aws:s3:::BUCKET", "arn:aws:s3:::BUCKET/*"],
  "Condition": {
    "Bool": { "aws:SecureTransport": "false" }
  }
}
```
**Purpose**: Blocks any HTTP (non-HTTPS) access to the bucket. This is an AWS security best practice — ensures all S3 traffic is encrypted in transit.

### 2. AllowCICDDevopsAccess
```json
{
  "Sid": "AllowCICDDevopsAccess",
  "Effect": "Allow",
  "Principal": { "AWS": "arn:aws:iam::165183897698:user/cicd-devops" },
  "Action": "s3:*",
  "Resource": ["arn:aws:s3:::BUCKET", "arn:aws:s3:::BUCKET/*"]
}
```
**Purpose**: Grants the CI/CD service account (`cicd-devops`) full S3 access to the bucket. This is broader than `AllowAuthenticatedUploads` (which only allows PutObject/PutObjectAcl) — needed for CI/CD operations like listing, deleting, and managing bucket contents.

---

## 4 Bucket Types with Different Structures

Not all buckets have the same policy. There are 4 distinct patterns:

### Type A: Standard Buckets (5 per environment = 20 files)
**Buckets**: asset-photos, ir-photos, attachments, reporting-jobs, slds  
**Statement order**: `AllowAuthenticatedUploads` → `DenyInsecureTransport` → `AllowCICDDevopsAccess`

### Type B: Branding Bucket (1 per environment = 4 files)
**Statement order**: `PublicReadAccess` → `AllowAuthenticatedUploads` → `DenyInsecureTransport` → `AllowCICDDevopsAccess`  
**Extra**: Has a `PublicReadAccess` statement allowing anyone to read branding assets (logos, themes).

### Type C: Frontend Bucket (1 per environment = 4 files)
**Statement order**: `AllowCloudFrontServicePrincipal` → `DenyInsecureTransport` → `AllowCICDDevopsAccess`  
**Different**: Uses CloudFront service principal instead of `AllowAuthenticatedUploads`. Each environment has a unique CloudFront distribution ID:
| Environment | CloudFront Distribution |
|---|---|
| dev | E2U2ML0PVSQ1MM |
| staging | E3Q9R4S2DB82ZW |
| prod | E3EBI07TXBMV7R |
| bces-iq | E9TJ761LFHDFU |
| qa | E406TGE9S91QX |

### Type D: Onboarding-Jobs Bucket (1 per environment = 4 files)
**Statement order**: `AllowAuthenticatedUploads` → `AllowCICDDevopsAccess` → `DenyInsecureTransport`  
**CRITICAL**: The `DenyInsecureTransport` statement is LAST (not second like other buckets). This matches how AWS returns the policy for this specific bucket. Since `S3PolicyChecker` preserves array order, this different order **must** be maintained or tests will fail.

---

## Files Changed (32 files)

### DEV Environment (8 files)
| File | Statements | Pattern |
|------|------------|---------|
| `baselines/dev/eg-pz-dev-s3-asset-photos-ohio.json` | 3 | Standard |
| `baselines/dev/eg-pz-dev-s3-ir-photos-ohio.json` | 3 | Standard |
| `baselines/dev/eg-pz-dev-s3-attachments-ohio.json` | 3 | Standard |
| `baselines/dev/eg-pz-dev-s3-reporting-jobs-ohio.json` | 3 | Standard |
| `baselines/dev/eg-pz-dev-s3-slds-ohio.json` | 3 | Standard |
| `baselines/dev/eg-pz-dev-s3-branding-ohio.json` | 4 | Branding |
| `baselines/dev/eg-pz-dev-s3-frontend-ohio.json` | 3 | Frontend |
| `baselines/dev/eg-pz-dev-s3-onboarding-jobs-ohio.json` | 3 | Onboarding |

### STAGING Environment (8 files)
| File | Statements | Pattern |
|------|------------|---------|
| `baselines/staging/eg-pz-staging-s3-asset-photos-ohio.json` | 3 | Standard |
| `baselines/staging/eg-pz-staging-s3-ir-photos-ohio.json` | 3 | Standard |
| `baselines/staging/eg-pz-staging-s3-attachments-ohio.json` | 3 | Standard |
| `baselines/staging/eg-pz-staging-s3-reporting-jobs-ohio.json` | 3 | Standard |
| `baselines/staging/eg-pz-staging-s3-slds-ohio.json` | 3 | Standard |
| `baselines/staging/eg-pz-staging-s3-branding-ohio.json` | 4 | Branding |
| `baselines/staging/eg-pz-staging-s3-frontend-ohio.json` | 3 | Frontend |
| `baselines/staging/eg-pz-staging-s3-onboarding-jobs-ohio.json` | 3 | Onboarding |

### PROD Environment (8 files)
| File | Statements | Pattern |
|------|------------|---------|
| `baselines/prod/eg-pz-prod-s3-asset-photos-ohio.json` | 3 | Standard |
| `baselines/prod/eg-pz-prod-s3-ir-photos-ohio.json` | 3 | Standard |
| `baselines/prod/eg-pz-prod-s3-attachments-ohio.json` | 3 | Standard |
| `baselines/prod/eg-pz-prod-s3-reporting-jobs-ohio.json` | 3 | Standard |
| `baselines/prod/eg-pz-prod-s3-slds-ohio.json` | 3 | Standard |
| `baselines/prod/eg-pz-prod-s3-branding-ohio.json` | 4 | Branding |
| `baselines/prod/eg-pz-prod-s3-frontend-ohio.json` | 3 | Frontend |
| `baselines/prod/eg-pz-prod-s3-onboarding-jobs-ohio.json` | 3 | Onboarding |

### BCES-IQ Environment (8 files)
| File | Statements | Pattern |
|------|------------|---------|
| `baselines/bces-iq/bces-iq-prod-s3-asset-photos-ohio.json` | 3 | Standard |
| `baselines/bces-iq/bces-iq-prod-s3-ir-photos-ohio.json` | 3 | Standard |
| `baselines/bces-iq/bces-iq-prod-s3-attachments-ohio.json` | 3 | Standard |
| `baselines/bces-iq/bces-iq-prod-s3-reporting-jobs-ohio.json` | 3 | Standard |
| `baselines/bces-iq/bces-iq-prod-s3-slds-ohio.json` | 3 | Standard |
| `baselines/bces-iq/bces-iq-prod-s3-branding-ohio.json` | 4 | Branding |
| `baselines/bces-iq/bces-iq-prod-s3-frontend-ohio.json` | 3 | Frontend |
| `baselines/bces-iq/bces-iq-prod-s3-onboarding-jobs-ohio.json` | 3 | Onboarding |

---

## Verification

1. **Structural match**: All 32 files verified against QA reference — same Sid ordering, same statement count per bucket type
2. **Bucket name accuracy**: No cross-contamination (no QA bucket names in dev files, etc.)
3. **JSON validity**: All 40 files (32 updated + 8 existing QA) parse and normalize correctly
4. **CloudFront IDs**: Each environment's frontend bucket has its own unique distribution ID preserved

---

## How to Verify After Deployment

If any S3 drift test fails after this update, it means the live AWS policy doesn't match the baseline. To debug:
```bash
# Fetch live policy for a specific bucket
aws s3api get-bucket-policy --bucket eg-pz-dev-s3-asset-photos-ohio --output json | python3 -c "import sys,json; print(json.dumps(json.loads(json.load(sys.stdin)['Policy']), indent=2))"
```
Compare the output against the corresponding baseline file. The Statement array order must match exactly.

---

## Status

- All 5 environments now have consistent baseline policies
- QA was updated in Changelog 011 (commit d610eb8)
- Dev, Staging, Prod, BCES-IQ updated in this changelog
