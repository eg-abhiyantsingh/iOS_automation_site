package com.egalvanic.tests;

import com.egalvanic.constants.AppConstants;
import com.egalvanic.utils.ExtentReportManager;
import com.egalvanic.utils.S3PolicyChecker;
import com.egalvanic.utils.S3PolicyChecker.PolicyCheckResult;
import org.testng.Assert;
import org.testng.annotations.*;

/**
 * S3 Bucket Policy Drift Detection - Smoke Test
 * ================================================
 * Jira Ticket: ZP-774
 * Author: Abhiyant Singh (QA Automation Engineer)
 * 
 * PURPOSE:
 * Detects unauthorized changes to S3 bucket policies across all 5 environments.
 * Runs BEFORE Authentication module as a smoke test to verify infrastructure integrity.
 * 
 * SCOPE:
 * 42 test cases: 2 pre-conditions + 40 bucket checks
 * 
 * TESTNG GROUPS (for per-environment execution):
 *   "smoke"    -> All tests
 *   "dev"      -> Pre-conditions + DEV buckets
 *   "qa"       -> Pre-conditions + QA buckets
 *   "staging"  -> Pre-conditions + STAGING buckets
 *   "prod"     -> Pre-conditions + PROD buckets
 *   "bces-iq"  -> Pre-conditions + BCES-IQ buckets
 * 
 * EXECUTION:
 *   All:    mvn test -DsuiteXmlFile=testng-smoke.xml
 *   Single: mvn test -DsuiteXmlFile=testng-smoke-prod.xml
 *   Groups: mvn test -DsuiteXmlFile=testng-smoke.xml -Dgroups=dev,qa
 * 
 * DOES NOT REQUIRE: Appium, iOS Simulator, Z Platform app
 * REQUIRES: AWS CLI, AWS profile eg-pz-readonly, jq, baselines/ directory
 */
public class S3BucketPolicyDriftTest {

    @BeforeSuite(alwaysRun = true)
    public void suiteSetup() {
        // Initialize Extent Reports (idempotent - safe if BaseTest also calls this)
        ExtentReportManager.initReports();
        System.out.println("  [SMOKE] Extent Reports initialized for smoke test suite");
    }

    @AfterSuite(alwaysRun = true)
    public void suiteTeardown() {
        // Flush reports (generates HTML files)
        ExtentReportManager.flushReports();
        System.out.println("  [SMOKE] Extent Reports flushed");
    }

    @BeforeClass(alwaysRun = true)
    public void classSetup() {
        System.out.println("\n========================================================");
        System.out.println("  S3 Bucket Policy Drift Detection - Smoke Test");
        System.out.println("  Jira: ZP-774 | Scope: 40 Buckets x 5 Environments");
        System.out.println("========================================================\n");
        System.out.println("  AWS Profile:    " + S3PolicyChecker.getAwsProfile());
        System.out.println("  AWS Region:     " + S3PolicyChecker.getAwsRegion());
        System.out.println("  AWS Account:    " + S3PolicyChecker.getAwsAccountId());
        System.out.println("  Baselines Dir:  " + S3PolicyChecker.getBaselinesDir());
        System.out.println("");
    }

    // ============================================================
    // PRE-CONDITION TESTS
    // ============================================================

    @Test(priority = 1, groups = {"smoke", "dev", "qa", "staging", "prod", "bces-iq"})
    public void TC_SMOKE_01_verifyAwsAccess() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_SMOKE_TEST,
            AppConstants.FEATURE_S3_DRIFT_DETECTION,
            "TC_SMOKE_01 - Verify AWS CLI Access"
        );
        logStep("Verifying AWS CLI with profile: " + S3PolicyChecker.getAwsProfile());

        boolean awsAccessible = S3PolicyChecker.verifyAwsAccess();

        if (awsAccessible) {
            logStep("AWS CLI access verified successfully");
            ExtentReportManager.logPass("AWS CLI access working with profile: " + S3PolicyChecker.getAwsProfile());
        } else {
            ExtentReportManager.logFail("AWS CLI access failed");
            Assert.fail("AWS access verification failed. Install AWS CLI and configure profile '"
                + S3PolicyChecker.getAwsProfile() + "'.");
        }
    }

    @Test(priority = 2, groups = {"smoke", "dev", "qa", "staging", "prod", "bces-iq"},
          dependsOnMethods = "TC_SMOKE_01_verifyAwsAccess")
    public void TC_SMOKE_02_verifyBaselineFilesExist() {
        ExtentReportManager.createTest(
            AppConstants.MODULE_SMOKE_TEST,
            AppConstants.FEATURE_S3_DRIFT_DETECTION,
            "TC_SMOKE_02 - Verify Baseline Policy Files Exist"
        );
        logStep("Checking baselines directory: " + S3PolicyChecker.getBaselinesDir());

        boolean baselinesValid = S3PolicyChecker.verifyBaselinesExist();

        if (baselinesValid) {
            logStep("All 40 baseline policy files found");
            ExtentReportManager.logPass("All 40 baseline policy files present");
        } else {
            ExtentReportManager.logFail("Baseline files missing at: " + S3PolicyChecker.getBaselinesDir());
            Assert.fail("Baseline policy files missing or incomplete. Expected 40 files in: "
                + S3PolicyChecker.getBaselinesDir());
        }
    }

    // ============================================================
    // DEV ENVIRONMENT - 8 Buckets (TC_SMOKE_03 to TC_SMOKE_10)
    // ============================================================

    @Test(priority = 3, groups = {"smoke", "dev"},
          dependsOnMethods = {"TC_SMOKE_01_verifyAwsAccess", "TC_SMOKE_02_verifyBaselineFilesExist"})
    public void TC_SMOKE_03_devAssetPhotosPolicy() {
        checkBucketPolicy("dev", "eg-pz-dev-s3-asset-photos-ohio",
            "TC_SMOKE_03 - DEV: Asset Photos Bucket Policy");
    }

    @Test(priority = 4, groups = {"smoke", "dev"},
          dependsOnMethods = {"TC_SMOKE_01_verifyAwsAccess", "TC_SMOKE_02_verifyBaselineFilesExist"})
    public void TC_SMOKE_04_devBrandingPolicy() {
        checkBucketPolicy("dev", "eg-pz-dev-s3-branding-ohio",
            "TC_SMOKE_04 - DEV: Branding Bucket Policy (Public Read)");
    }

    @Test(priority = 5, groups = {"smoke", "dev"},
          dependsOnMethods = {"TC_SMOKE_01_verifyAwsAccess", "TC_SMOKE_02_verifyBaselineFilesExist"})
    public void TC_SMOKE_05_devIrPhotosPolicy() {
        checkBucketPolicy("dev", "eg-pz-dev-s3-ir-photos-ohio",
            "TC_SMOKE_05 - DEV: IR Photos Bucket Policy");
    }

    @Test(priority = 6, groups = {"smoke", "dev"},
          dependsOnMethods = {"TC_SMOKE_01_verifyAwsAccess", "TC_SMOKE_02_verifyBaselineFilesExist"})
    public void TC_SMOKE_06_devFrontendPolicy() {
        checkBucketPolicy("dev", "eg-pz-dev-s3-frontend-ohio",
            "TC_SMOKE_06 - DEV: Frontend Bucket Policy");
    }

    @Test(priority = 7, groups = {"smoke", "dev"},
          dependsOnMethods = {"TC_SMOKE_01_verifyAwsAccess", "TC_SMOKE_02_verifyBaselineFilesExist"})
    public void TC_SMOKE_07_devOnboardingJobsPolicy() {
        checkBucketPolicy("dev", "eg-pz-dev-s3-onboarding-jobs-ohio",
            "TC_SMOKE_07 - DEV: Onboarding Jobs Bucket Policy");
    }

    @Test(priority = 8, groups = {"smoke", "dev"},
          dependsOnMethods = {"TC_SMOKE_01_verifyAwsAccess", "TC_SMOKE_02_verifyBaselineFilesExist"})
    public void TC_SMOKE_08_devAttachmentsPolicy() {
        checkBucketPolicy("dev", "eg-pz-dev-s3-attachments-ohio",
            "TC_SMOKE_08 - DEV: Attachments Bucket Policy");
    }

    @Test(priority = 9, groups = {"smoke", "dev"},
          dependsOnMethods = {"TC_SMOKE_01_verifyAwsAccess", "TC_SMOKE_02_verifyBaselineFilesExist"})
    public void TC_SMOKE_09_devReportingJobsPolicy() {
        checkBucketPolicy("dev", "eg-pz-dev-s3-reporting-jobs-ohio",
            "TC_SMOKE_09 - DEV: Reporting Jobs Bucket Policy");
    }

    @Test(priority = 10, groups = {"smoke", "dev"},
          dependsOnMethods = {"TC_SMOKE_01_verifyAwsAccess", "TC_SMOKE_02_verifyBaselineFilesExist"})
    public void TC_SMOKE_10_devSldsPolicy() {
        checkBucketPolicy("dev", "eg-pz-dev-s3-slds-ohio",
            "TC_SMOKE_10 - DEV: SLDS Bucket Policy");
    }

    // ============================================================
    // QA ENVIRONMENT - 8 Buckets (TC_SMOKE_11 to TC_SMOKE_18)
    // ============================================================

    @Test(priority = 11, groups = {"smoke", "qa"},
          dependsOnMethods = {"TC_SMOKE_01_verifyAwsAccess", "TC_SMOKE_02_verifyBaselineFilesExist"})
    public void TC_SMOKE_11_qaAssetPhotosPolicy() {
        checkBucketPolicy("qa", "eg-pz-qa-s3-asset-photos-ohio",
            "TC_SMOKE_11 - QA: Asset Photos Bucket Policy");
    }

    @Test(priority = 12, groups = {"smoke", "qa"},
          dependsOnMethods = {"TC_SMOKE_01_verifyAwsAccess", "TC_SMOKE_02_verifyBaselineFilesExist"})
    public void TC_SMOKE_12_qaBrandingPolicy() {
        checkBucketPolicy("qa", "eg-pz-qa-s3-branding-ohio",
            "TC_SMOKE_12 - QA: Branding Bucket Policy (Public Read)");
    }

    @Test(priority = 13, groups = {"smoke", "qa"},
          dependsOnMethods = {"TC_SMOKE_01_verifyAwsAccess", "TC_SMOKE_02_verifyBaselineFilesExist"})
    public void TC_SMOKE_13_qaIrPhotosPolicy() {
        checkBucketPolicy("qa", "eg-pz-qa-s3-ir-photos-ohio",
            "TC_SMOKE_13 - QA: IR Photos Bucket Policy");
    }

    @Test(priority = 14, groups = {"smoke", "qa"},
          dependsOnMethods = {"TC_SMOKE_01_verifyAwsAccess", "TC_SMOKE_02_verifyBaselineFilesExist"})
    public void TC_SMOKE_14_qaFrontendPolicy() {
        checkBucketPolicy("qa", "eg-pz-qa-s3-frontend-ohio",
            "TC_SMOKE_14 - QA: Frontend Bucket Policy");
    }

    @Test(priority = 15, groups = {"smoke", "qa"},
          dependsOnMethods = {"TC_SMOKE_01_verifyAwsAccess", "TC_SMOKE_02_verifyBaselineFilesExist"})
    public void TC_SMOKE_15_qaOnboardingJobsPolicy() {
        checkBucketPolicy("qa", "eg-pz-qa-s3-onboarding-jobs-ohio",
            "TC_SMOKE_15 - QA: Onboarding Jobs Bucket Policy");
    }

    @Test(priority = 16, groups = {"smoke", "qa"},
          dependsOnMethods = {"TC_SMOKE_01_verifyAwsAccess", "TC_SMOKE_02_verifyBaselineFilesExist"})
    public void TC_SMOKE_16_qaAttachmentsPolicy() {
        checkBucketPolicy("qa", "eg-pz-qa-s3-attachments-ohio",
            "TC_SMOKE_16 - QA: Attachments Bucket Policy");
    }

    @Test(priority = 17, groups = {"smoke", "qa"},
          dependsOnMethods = {"TC_SMOKE_01_verifyAwsAccess", "TC_SMOKE_02_verifyBaselineFilesExist"})
    public void TC_SMOKE_17_qaReportingJobsPolicy() {
        checkBucketPolicy("qa", "eg-pz-qa-s3-reporting-jobs-ohio",
            "TC_SMOKE_17 - QA: Reporting Jobs Bucket Policy");
    }

    @Test(priority = 18, groups = {"smoke", "qa"},
          dependsOnMethods = {"TC_SMOKE_01_verifyAwsAccess", "TC_SMOKE_02_verifyBaselineFilesExist"})
    public void TC_SMOKE_18_qaSldsPolicy() {
        checkBucketPolicy("qa", "eg-pz-qa-s3-slds-ohio",
            "TC_SMOKE_18 - QA: SLDS Bucket Policy");
    }

    // ============================================================
    // STAGING ENVIRONMENT - 8 Buckets (TC_SMOKE_19 to TC_SMOKE_26)
    // ============================================================

    @Test(priority = 19, groups = {"smoke", "staging"},
          dependsOnMethods = {"TC_SMOKE_01_verifyAwsAccess", "TC_SMOKE_02_verifyBaselineFilesExist"})
    public void TC_SMOKE_19_stagingAssetPhotosPolicy() {
        checkBucketPolicy("staging", "eg-pz-staging-s3-asset-photos-ohio",
            "TC_SMOKE_19 - STAGING: Asset Photos Bucket Policy");
    }

    @Test(priority = 20, groups = {"smoke", "staging"},
          dependsOnMethods = {"TC_SMOKE_01_verifyAwsAccess", "TC_SMOKE_02_verifyBaselineFilesExist"})
    public void TC_SMOKE_20_stagingBrandingPolicy() {
        checkBucketPolicy("staging", "eg-pz-staging-s3-branding-ohio",
            "TC_SMOKE_20 - STAGING: Branding Bucket Policy (Public Read)");
    }

    @Test(priority = 21, groups = {"smoke", "staging"},
          dependsOnMethods = {"TC_SMOKE_01_verifyAwsAccess", "TC_SMOKE_02_verifyBaselineFilesExist"})
    public void TC_SMOKE_21_stagingIrPhotosPolicy() {
        checkBucketPolicy("staging", "eg-pz-staging-s3-ir-photos-ohio",
            "TC_SMOKE_21 - STAGING: IR Photos Bucket Policy");
    }

    @Test(priority = 22, groups = {"smoke", "staging"},
          dependsOnMethods = {"TC_SMOKE_01_verifyAwsAccess", "TC_SMOKE_02_verifyBaselineFilesExist"})
    public void TC_SMOKE_22_stagingFrontendPolicy() {
        checkBucketPolicy("staging", "eg-pz-staging-s3-frontend-ohio",
            "TC_SMOKE_22 - STAGING: Frontend Bucket Policy");
    }

    @Test(priority = 23, groups = {"smoke", "staging"},
          dependsOnMethods = {"TC_SMOKE_01_verifyAwsAccess", "TC_SMOKE_02_verifyBaselineFilesExist"})
    public void TC_SMOKE_23_stagingOnboardingJobsPolicy() {
        checkBucketPolicy("staging", "eg-pz-staging-s3-onboarding-jobs-ohio",
            "TC_SMOKE_23 - STAGING: Onboarding Jobs Bucket Policy");
    }

    @Test(priority = 24, groups = {"smoke", "staging"},
          dependsOnMethods = {"TC_SMOKE_01_verifyAwsAccess", "TC_SMOKE_02_verifyBaselineFilesExist"})
    public void TC_SMOKE_24_stagingAttachmentsPolicy() {
        checkBucketPolicy("staging", "eg-pz-staging-s3-attachments-ohio",
            "TC_SMOKE_24 - STAGING: Attachments Bucket Policy");
    }

    @Test(priority = 25, groups = {"smoke", "staging"},
          dependsOnMethods = {"TC_SMOKE_01_verifyAwsAccess", "TC_SMOKE_02_verifyBaselineFilesExist"})
    public void TC_SMOKE_25_stagingReportingJobsPolicy() {
        checkBucketPolicy("staging", "eg-pz-staging-s3-reporting-jobs-ohio",
            "TC_SMOKE_25 - STAGING: Reporting Jobs Bucket Policy");
    }

    @Test(priority = 26, groups = {"smoke", "staging"},
          dependsOnMethods = {"TC_SMOKE_01_verifyAwsAccess", "TC_SMOKE_02_verifyBaselineFilesExist"})
    public void TC_SMOKE_26_stagingSldsPolicy() {
        checkBucketPolicy("staging", "eg-pz-staging-s3-slds-ohio",
            "TC_SMOKE_26 - STAGING: SLDS Bucket Policy");
    }

    // ============================================================
    // PROD ENVIRONMENT - 8 Buckets (TC_SMOKE_27 to TC_SMOKE_34)
    // ============================================================

    @Test(priority = 27, groups = {"smoke", "prod"},
          dependsOnMethods = {"TC_SMOKE_01_verifyAwsAccess", "TC_SMOKE_02_verifyBaselineFilesExist"})
    public void TC_SMOKE_27_prodAssetPhotosPolicy() {
        checkBucketPolicy("prod", "eg-pz-prod-s3-asset-photos-ohio",
            "TC_SMOKE_27 - PROD: Asset Photos Bucket Policy");
    }

    @Test(priority = 28, groups = {"smoke", "prod"},
          dependsOnMethods = {"TC_SMOKE_01_verifyAwsAccess", "TC_SMOKE_02_verifyBaselineFilesExist"})
    public void TC_SMOKE_28_prodBrandingPolicy() {
        checkBucketPolicy("prod", "eg-pz-prod-s3-branding-ohio",
            "TC_SMOKE_28 - PROD: Branding Bucket Policy (Public Read)");
    }

    @Test(priority = 29, groups = {"smoke", "prod"},
          dependsOnMethods = {"TC_SMOKE_01_verifyAwsAccess", "TC_SMOKE_02_verifyBaselineFilesExist"})
    public void TC_SMOKE_29_prodIrPhotosPolicy() {
        checkBucketPolicy("prod", "eg-pz-prod-s3-ir-photos-ohio",
            "TC_SMOKE_29 - PROD: IR Photos Bucket Policy");
    }

    @Test(priority = 30, groups = {"smoke", "prod"},
          dependsOnMethods = {"TC_SMOKE_01_verifyAwsAccess", "TC_SMOKE_02_verifyBaselineFilesExist"})
    public void TC_SMOKE_30_prodFrontendPolicy() {
        checkBucketPolicy("prod", "eg-pz-prod-s3-frontend-ohio",
            "TC_SMOKE_30 - PROD: Frontend Bucket Policy");
    }

    @Test(priority = 31, groups = {"smoke", "prod"},
          dependsOnMethods = {"TC_SMOKE_01_verifyAwsAccess", "TC_SMOKE_02_verifyBaselineFilesExist"})
    public void TC_SMOKE_31_prodOnboardingJobsPolicy() {
        checkBucketPolicy("prod", "eg-pz-prod-s3-onboarding-jobs-ohio",
            "TC_SMOKE_31 - PROD: Onboarding Jobs Bucket Policy");
    }

    @Test(priority = 32, groups = {"smoke", "prod"},
          dependsOnMethods = {"TC_SMOKE_01_verifyAwsAccess", "TC_SMOKE_02_verifyBaselineFilesExist"})
    public void TC_SMOKE_32_prodAttachmentsPolicy() {
        checkBucketPolicy("prod", "eg-pz-prod-s3-attachments-ohio",
            "TC_SMOKE_32 - PROD: Attachments Bucket Policy");
    }

    @Test(priority = 33, groups = {"smoke", "prod"},
          dependsOnMethods = {"TC_SMOKE_01_verifyAwsAccess", "TC_SMOKE_02_verifyBaselineFilesExist"})
    public void TC_SMOKE_33_prodReportingJobsPolicy() {
        checkBucketPolicy("prod", "eg-pz-prod-s3-reporting-jobs-ohio",
            "TC_SMOKE_33 - PROD: Reporting Jobs Bucket Policy");
    }

    @Test(priority = 34, groups = {"smoke", "prod"},
          dependsOnMethods = {"TC_SMOKE_01_verifyAwsAccess", "TC_SMOKE_02_verifyBaselineFilesExist"})
    public void TC_SMOKE_34_prodSldsPolicy() {
        checkBucketPolicy("prod", "eg-pz-prod-s3-slds-ohio",
            "TC_SMOKE_34 - PROD: SLDS Bucket Policy");
    }

    // ============================================================
    // BCES-IQ ENVIRONMENT - 8 Buckets (TC_SMOKE_35 to TC_SMOKE_42)
    // ============================================================

    @Test(priority = 35, groups = {"smoke", "bces-iq"},
          dependsOnMethods = {"TC_SMOKE_01_verifyAwsAccess", "TC_SMOKE_02_verifyBaselineFilesExist"})
    public void TC_SMOKE_35_bcesIqAssetPhotosPolicy() {
        checkBucketPolicy("bces-iq", "bces-iq-prod-s3-asset-photos-ohio",
            "TC_SMOKE_35 - BCES-IQ: Asset Photos Bucket Policy");
    }

    @Test(priority = 36, groups = {"smoke", "bces-iq"},
          dependsOnMethods = {"TC_SMOKE_01_verifyAwsAccess", "TC_SMOKE_02_verifyBaselineFilesExist"})
    public void TC_SMOKE_36_bcesIqBrandingPolicy() {
        checkBucketPolicy("bces-iq", "bces-iq-prod-s3-branding-ohio",
            "TC_SMOKE_36 - BCES-IQ: Branding Bucket Policy (Public Read)");
    }

    @Test(priority = 37, groups = {"smoke", "bces-iq"},
          dependsOnMethods = {"TC_SMOKE_01_verifyAwsAccess", "TC_SMOKE_02_verifyBaselineFilesExist"})
    public void TC_SMOKE_37_bcesIqIrPhotosPolicy() {
        checkBucketPolicy("bces-iq", "bces-iq-prod-s3-ir-photos-ohio",
            "TC_SMOKE_37 - BCES-IQ: IR Photos Bucket Policy");
    }

    @Test(priority = 38, groups = {"smoke", "bces-iq"},
          dependsOnMethods = {"TC_SMOKE_01_verifyAwsAccess", "TC_SMOKE_02_verifyBaselineFilesExist"})
    public void TC_SMOKE_38_bcesIqFrontendPolicy() {
        checkBucketPolicy("bces-iq", "bces-iq-prod-s3-frontend-ohio",
            "TC_SMOKE_38 - BCES-IQ: Frontend Bucket Policy");
    }

    @Test(priority = 39, groups = {"smoke", "bces-iq"},
          dependsOnMethods = {"TC_SMOKE_01_verifyAwsAccess", "TC_SMOKE_02_verifyBaselineFilesExist"})
    public void TC_SMOKE_39_bcesIqOnboardingJobsPolicy() {
        checkBucketPolicy("bces-iq", "bces-iq-prod-s3-onboarding-jobs-ohio",
            "TC_SMOKE_39 - BCES-IQ: Onboarding Jobs Bucket Policy");
    }

    @Test(priority = 40, groups = {"smoke", "bces-iq"},
          dependsOnMethods = {"TC_SMOKE_01_verifyAwsAccess", "TC_SMOKE_02_verifyBaselineFilesExist"})
    public void TC_SMOKE_40_bcesIqAttachmentsPolicy() {
        checkBucketPolicy("bces-iq", "bces-iq-prod-s3-attachments-ohio",
            "TC_SMOKE_40 - BCES-IQ: Attachments Bucket Policy");
    }

    @Test(priority = 41, groups = {"smoke", "bces-iq"},
          dependsOnMethods = {"TC_SMOKE_01_verifyAwsAccess", "TC_SMOKE_02_verifyBaselineFilesExist"})
    public void TC_SMOKE_41_bcesIqReportingJobsPolicy() {
        checkBucketPolicy("bces-iq", "bces-iq-prod-s3-reporting-jobs-ohio",
            "TC_SMOKE_41 - BCES-IQ: Reporting Jobs Bucket Policy");
    }

    @Test(priority = 42, groups = {"smoke", "bces-iq"},
          dependsOnMethods = {"TC_SMOKE_01_verifyAwsAccess", "TC_SMOKE_02_verifyBaselineFilesExist"})
    public void TC_SMOKE_42_bcesIqSldsPolicy() {
        checkBucketPolicy("bces-iq", "bces-iq-prod-s3-slds-ohio",
            "TC_SMOKE_42 - BCES-IQ: SLDS Bucket Policy");
    }

    // ============================================================
    // PRIVATE HELPER METHODS
    // ============================================================

    private void checkBucketPolicy(String environment, String bucketName, String testTitle) {
        ExtentReportManager.createTest(
            AppConstants.MODULE_SMOKE_TEST,
            AppConstants.FEATURE_S3_DRIFT_DETECTION,
            testTitle
        );

        logStep("Bucket: " + bucketName);
        logStep("Environment: " + environment.toUpperCase());
        logStep("Policy type: " + (S3PolicyChecker.isBrandingBucket(bucketName)
            ? "Branding (PublicReadAccess + AllowAuthenticatedUploads)"
            : "Non-Branding (AllowAuthenticatedUploads only)"));
        logStep("Baseline file: " + S3PolicyChecker.getBaselineFilePath(bucketName));

        PolicyCheckResult result = S3PolicyChecker.checkBucket(bucketName);

        if (result.isPassed()) {
            logStep("Policy matches baseline - No drift detected");
            ExtentReportManager.logPass("Policy verified: " + bucketName);
        } else {
            StringBuilder failureMessage = new StringBuilder();
            failureMessage.append("S3 Bucket Policy Drift Detected!\n");
            failureMessage.append("Bucket:      ").append(bucketName).append("\n");
            failureMessage.append("Environment: ").append(environment.toUpperCase()).append("\n");
            failureMessage.append("Baseline:    ").append(result.getBaselineFilePath()).append("\n");

            if (result.getErrorMessage() != null) {
                failureMessage.append("Error:       ").append(result.getErrorMessage()).append("\n");
                logStep("Error: " + result.getErrorMessage());
            }
            if (result.getDiffDetails() != null && !result.getDiffDetails().isEmpty()) {
                failureMessage.append("Diff:\n").append(result.getDiffDetails()).append("\n");
                logStep("Diff:\n" + result.getDiffDetails());
            }
            if (result.getExpectedPolicy() != null) {
                logStep("Expected (from baseline):\n" + result.getExpectedPolicy());
            }
            if (result.getActualPolicy() != null) {
                logStep("Actual (from AWS):\n" + result.getActualPolicy());
            }

            failureMessage.append("FIX: If intentional, update the baseline file and commit to git.");

            ExtentReportManager.logFail("DRIFT DETECTED: " + bucketName
                + " | " + (result.getErrorMessage() != null ? result.getErrorMessage() : "Policy mismatch"));

            Assert.fail(failureMessage.toString());
        }
    }

    private void logStep(String message) {
        ExtentReportManager.logInfo(message);
        System.out.println("  [SMOKE] " + message);
    }
}
