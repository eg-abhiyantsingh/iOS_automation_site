package com.egalvanic.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * S3 Bucket Policy Checker Utility
 * ================================
 * Jira Ticket: ZP-774
 * Author: Abhiyant Singh (QA Automation Engineer)
 * 
 * Validates S3 bucket policies against version-controlled JSON baseline files.
 * Uses AWS CLI (must be installed and configured) to fetch live policies,
 * then compares them against expected baselines stored in the baselines/ directory.
 * 
 * BASELINE FILE STRUCTURE:
 *   baselines/
 *   ├── dev/
 *   │   ├── eg-pz-dev-s3-asset-photos-ohio.json
 *   │   ├── eg-pz-dev-s3-branding-ohio.json
 *   │   └── ... (8 files per environment)
 *   ├── qa/
 *   ├── staging/
 *   ├── prod/
 *   └── bces-iq/
 * 
 * HOW TO UPDATE BASELINES:
 *   When Pradip/Terraform makes an intentional policy change:
 *   1. Update the corresponding JSON file in baselines/{env}/
 *   2. Commit the change to git
 *   3. Tests will now pass against the new expected policy
 * 
 * AWS Profile: Configurable via AWS_PROFILE env variable
 * Default: eg-pz-readonly
 * 
 * Scope: 40 S3 buckets across 5 environments (8 per environment)
 */
public class S3PolicyChecker {

    // ============================================
    // AWS CONFIGURATION
    // ============================================
    private static final String AWS_ACCOUNT_ID = "165183897698";
    private static final String AWS_REGION = "us-east-2";
    private static final String CICD_USER_ARN = "arn:aws:iam::" + AWS_ACCOUNT_ID + ":user/cicd-devops";

    // AWS Profile - overridable via environment variable
    private static final String AWS_PROFILE = System.getenv("AWS_PROFILE") != null
            ? System.getenv("AWS_PROFILE")
            : "eg-pz-readonly";

    // ============================================
    // BASELINE DIRECTORY CONFIGURATION
    // ============================================
    
    /**
     * Baselines directory path.
     * Resolved from:
     *   1. S3_BASELINES_DIR environment variable (for CI/CD)
     *   2. Project root + /baselines/ (default for Maven mvn test)
     */
    private static final String BASELINES_DIR;
    static {
        String envDir = System.getenv("S3_BASELINES_DIR");
        if (envDir != null && !envDir.trim().isEmpty()) {
            BASELINES_DIR = envDir;
        } else {
            // Maven runs from project root, so user.dir = project root
            BASELINES_DIR = System.getProperty("user.dir") + File.separator + "baselines";
        }
    }

    // ============================================
    // BUCKET CONFIGURATION
    // ============================================
    
    /** Environment prefix mapping */
    private static final Map<String, String> ENV_PREFIXES = new LinkedHashMap<>();
    static {
        ENV_PREFIXES.put("dev", "eg-pz-dev");
        ENV_PREFIXES.put("qa", "eg-pz-qa");
        ENV_PREFIXES.put("staging", "eg-pz-staging");
        ENV_PREFIXES.put("prod", "eg-pz-prod");
        ENV_PREFIXES.put("bces-iq", "bces-iq-prod");
    }

    /** Service suffixes (same for all environments) */
    private static final String[] SERVICES = {
        "s3-asset-photos-ohio",
        "s3-branding-ohio",
        "s3-ir-photos-ohio",
        "s3-frontend-ohio",
        "s3-onboarding-jobs-ohio",
        "s3-attachments-ohio",
        "s3-reporting-jobs-ohio",
        "s3-slds-ohio"
    };

    /** Branding service identifier */
    private static final String BRANDING_SERVICE = "s3-branding-ohio";

    // ============================================
    // RESULT CLASS
    // ============================================

    /**
     * Result of a single bucket policy check.
     * Contains all information needed for reporting.
     */
    public static class PolicyCheckResult {
        private final String bucketName;
        private final String environment;
        private final String service;
        private final boolean passed;
        private final String expectedPolicy;
        private final String actualPolicy;
        private final String errorMessage;
        private final String diffDetails;
        private final String baselineFilePath;

        public PolicyCheckResult(String bucketName, String environment, String service,
                                 boolean passed, String expectedPolicy, String actualPolicy,
                                 String errorMessage, String diffDetails, String baselineFilePath) {
            this.bucketName = bucketName;
            this.environment = environment;
            this.service = service;
            this.passed = passed;
            this.expectedPolicy = expectedPolicy;
            this.actualPolicy = actualPolicy;
            this.errorMessage = errorMessage;
            this.diffDetails = diffDetails;
            this.baselineFilePath = baselineFilePath;
        }

        public String getBucketName() { return bucketName; }
        public String getEnvironment() { return environment; }
        public String getService() { return service; }
        public boolean isPassed() { return passed; }
        public String getExpectedPolicy() { return expectedPolicy; }
        public String getActualPolicy() { return actualPolicy; }
        public String getErrorMessage() { return errorMessage; }
        public String getDiffDetails() { return diffDetails; }
        public String getBaselineFilePath() { return baselineFilePath; }

        @Override
        public String toString() {
            return String.format("[%s] %s | %s", passed ? "PASS" : "FAIL", environment, bucketName);
        }
    }

    // ============================================
    // PUBLIC METHODS - BUCKET INFORMATION
    // ============================================

    public static List<String> getBucketsForEnvironment(String environment) {
        String prefix = ENV_PREFIXES.get(environment);
        if (prefix == null) {
            throw new IllegalArgumentException("Invalid environment: " + environment 
                + ". Valid environments: " + ENV_PREFIXES.keySet());
        }
        List<String> buckets = new ArrayList<>();
        for (String service : SERVICES) {
            buckets.add(prefix + "-" + service);
        }
        return buckets;
    }

    public static List<String> getAllBuckets() {
        List<String> allBuckets = new ArrayList<>();
        for (String env : ENV_PREFIXES.keySet()) {
            allBuckets.addAll(getBucketsForEnvironment(env));
        }
        return allBuckets;
    }

    public static Set<String> getEnvironments() {
        return ENV_PREFIXES.keySet();
    }

    public static String getEnvironmentForBucket(String bucketName) {
        for (Map.Entry<String, String> entry : ENV_PREFIXES.entrySet()) {
            if (bucketName.startsWith(entry.getValue())) {
                return entry.getKey();
            }
        }
        return "unknown";
    }

    public static String getServiceForBucket(String bucketName) {
        for (String service : SERVICES) {
            if (bucketName.endsWith(service)) {
                return service;
            }
        }
        return "unknown";
    }

    public static boolean isBrandingBucket(String bucketName) {
        return bucketName.contains(BRANDING_SERVICE);
    }

    // ============================================
    // PUBLIC METHODS - BASELINE OPERATIONS
    // ============================================

    public static String getBaselineFilePath(String bucketName) {
        String environment = getEnvironmentForBucket(bucketName);
        return BASELINES_DIR + File.separator + environment + File.separator + bucketName + ".json";
    }

    public static String getBaselinesDir() {
        return BASELINES_DIR;
    }

    /**
     * Verify that the baselines directory exists and contains all 40 baseline files
     */
    public static boolean verifyBaselinesExist() {
        File dir = new File(BASELINES_DIR);
        if (!dir.exists() || !dir.isDirectory()) {
            System.err.println("Baselines directory not found: " + BASELINES_DIR);
            return false;
        }

        int totalFiles = 0;
        for (String env : ENV_PREFIXES.keySet()) {
            File envDir = new File(BASELINES_DIR + File.separator + env);
            if (!envDir.exists()) {
                System.err.println("Missing environment baseline directory: " + envDir.getAbsolutePath());
                return false;
            }
            File[] jsonFiles = envDir.listFiles((d, name) -> name.endsWith(".json"));
            int count = jsonFiles != null ? jsonFiles.length : 0;
            totalFiles += count;
            if (count != 8) {
                System.err.println("Environment '" + env + "' has " + count + " baseline files (expected 8)");
            }
        }

        System.out.println("Baselines directory: " + BASELINES_DIR);
        System.out.println("Total baseline files: " + totalFiles + " (expected 40)");
        return totalFiles == 40;
    }

    /**
     * Load expected policy from the version-controlled baseline JSON file
     */
    public static String loadExpectedPolicy(String bucketName) {
        String filePath = getBaselineFilePath(bucketName);
        File baselineFile = new File(filePath);

        if (!baselineFile.exists()) {
            throw new RuntimeException(
                "Baseline file NOT FOUND: " + filePath + "\n"
                + "To create it, add the expected policy JSON to:\n"
                + "  " + filePath + "\n"
                + "Then commit to git."
            );
        }

        try {
            String content = new String(Files.readAllBytes(Paths.get(filePath)));
            if (content.trim().isEmpty()) {
                throw new RuntimeException("Baseline file is EMPTY: " + filePath);
            }
            return content.trim();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to read baseline file: " + filePath + " - " + e.getMessage(), e);
        }
    }

    // ============================================
    // PUBLIC METHODS - AWS OPERATIONS
    // ============================================

    /**
     * Fetch the current bucket policy from AWS using aws CLI
     */
    public static String fetchCurrentPolicy(String bucketName) {
        try {
            String[] command;
            // If AWS_ACCESS_KEY_ID is set (GitHub Actions), don't use --profile
            // configure-aws-credentials action sets env vars directly
            if (System.getenv("AWS_ACCESS_KEY_ID") != null) {
                command = new String[]{
                    "aws", "s3api", "get-bucket-policy",
                    "--bucket", bucketName,
                    "--query", "Policy",
                    "--output", "text",
                    "--region", AWS_REGION
                };
            } else {
                // Local execution: use --profile
                command = new String[]{
                    "aws", "s3api", "get-bucket-policy",
                    "--bucket", bucketName,
                    "--query", "Policy",
                    "--output", "text",
                    "--profile", AWS_PROFILE,
                    "--region", AWS_REGION
                };
            }

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(false);
            Process process = pb.start();

            String stdout = new BufferedReader(new InputStreamReader(process.getInputStream()))
                    .lines().collect(Collectors.joining("\n")).trim();

            String stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()))
                    .lines().collect(Collectors.joining("\n")).trim();

            int exitCode = process.waitFor();

            if (exitCode == 0 && !stdout.isEmpty()) {
                return normalizeJson(stdout);
            } else if (stderr.contains("NoSuchBucketPolicy")) {
                return "NO_POLICY";
            } else if (stderr.contains("NoSuchBucket")) {
                throw new RuntimeException("Bucket does not exist: " + bucketName);
            } else if (stderr.contains("AccessDenied")) {
                throw new RuntimeException("Access denied for bucket: " + bucketName 
                    + ". Check AWS profile '" + AWS_PROFILE + "' permissions.");
            } else {
                throw new RuntimeException("AWS CLI error (exit " + exitCode + "): " + stderr);
            }

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch bucket policy for " + bucketName + ": " + e.getMessage(), e);
        }
    }

    /**
     * Verify AWS CLI is installed and credentials work
     */
    public static boolean verifyAwsAccess() {
        try {
            String[] command;
            if (System.getenv("AWS_ACCESS_KEY_ID") != null) {
                command = new String[]{
                    "aws", "s3", "ls",
                    "--region", AWS_REGION
                };
            } else {
                command = new String[]{
                    "aws", "s3", "ls",
                    "--profile", AWS_PROFILE,
                    "--region", AWS_REGION
                };
            }

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            new BufferedReader(new InputStreamReader(process.getInputStream()))
                    .lines().collect(Collectors.joining("\n"));

            int exitCode = process.waitFor();
            return exitCode == 0;

        } catch (Exception e) {
            System.err.println("AWS access verification failed: " + e.getMessage());
            return false;
        }
    }

    // ============================================
    // PUBLIC METHODS - DRIFT DETECTION
    // ============================================

    /**
     * Check a single bucket for policy drift.
     * Loads expected from baseline file, fetches current from AWS, compares.
     */
    public static PolicyCheckResult checkBucket(String bucketName) {
        String environment = getEnvironmentForBucket(bucketName);
        String service = getServiceForBucket(bucketName);
        String baselineFile = getBaselineFilePath(bucketName);

        // Step 1: Load expected policy from baseline file
        String expectedPolicy;
        try {
            expectedPolicy = loadExpectedPolicy(bucketName);
        } catch (Exception e) {
            return new PolicyCheckResult(
                bucketName, environment, service, false,
                null, null,
                "BASELINE ERROR: " + e.getMessage(), null, baselineFile
            );
        }

        // Step 2: Fetch current policy from AWS
        String currentPolicy;
        try {
            currentPolicy = fetchCurrentPolicy(bucketName);
        } catch (Exception e) {
            return new PolicyCheckResult(
                bucketName, environment, service, false,
                expectedPolicy, null,
                "AWS ERROR: " + e.getMessage(), null, baselineFile
            );
        }

        // Step 3: Handle missing policy
        if ("NO_POLICY".equals(currentPolicy)) {
            return new PolicyCheckResult(
                bucketName, environment, service, false,
                expectedPolicy, "NO POLICY ATTACHED",
                "Expected a bucket policy but none was found on the bucket",
                "MISSING: Entire bucket policy is absent", baselineFile
            );
        }

        // Step 4: Normalize and compare
        String normalizedExpected = normalizeJson(expectedPolicy);
        String normalizedActual = normalizeJson(currentPolicy);

        if (normalizedExpected.equals(normalizedActual)) {
            return new PolicyCheckResult(
                bucketName, environment, service, true,
                normalizedExpected, normalizedActual,
                null, null, baselineFile
            );
        } else {
            String diff = generateStructuredDiff(normalizedExpected, normalizedActual);
            return new PolicyCheckResult(
                bucketName, environment, service, false,
                normalizedExpected, normalizedActual,
                "Bucket policy does not match the expected baseline",
                diff, baselineFile
            );
        }
    }

    public static List<PolicyCheckResult> checkEnvironment(String environment) {
        List<PolicyCheckResult> results = new ArrayList<>();
        for (String bucket : getBucketsForEnvironment(environment)) {
            results.add(checkBucket(bucket));
        }
        return results;
    }

    public static List<PolicyCheckResult> checkAllBuckets() {
        List<PolicyCheckResult> results = new ArrayList<>();
        for (String env : ENV_PREFIXES.keySet()) {
            results.addAll(checkEnvironment(env));
        }
        return results;
    }

    // ============================================
    // PUBLIC GETTERS
    // ============================================

    public static String getAwsProfile() { return AWS_PROFILE; }
    public static String getAwsRegion() { return AWS_REGION; }
    public static String getAwsAccountId() { return AWS_ACCOUNT_ID; }
    public static String getCicdUserArn() { return CICD_USER_ARN; }

    // ============================================
    // PRIVATE HELPER METHODS
    // ============================================

    private static String normalizeJson(String json) {
        try {
            ProcessBuilder pb = new ProcessBuilder("jq", "-S", ".");
            pb.redirectErrorStream(false);
            Process process = pb.start();

            process.getOutputStream().write(json.getBytes());
            process.getOutputStream().close();

            String normalized = new BufferedReader(new InputStreamReader(process.getInputStream()))
                    .lines().collect(Collectors.joining("\n")).trim();

            int exitCode = process.waitFor();
            if (exitCode == 0 && !normalized.isEmpty()) {
                return normalized;
            }
        } catch (Exception e) {
            // jq not available, fallback below
        }

        return json.replaceAll("\\s+", " ").trim();
    }

    private static String generateStructuredDiff(String expected, String actual) {
        StringBuilder diff = new StringBuilder();
        String[] expectedLines = expected.split("\n");
        String[] actualLines = actual.split("\n");

        int maxLines = Math.max(expectedLines.length, actualLines.length);

        for (int i = 0; i < maxLines; i++) {
            String expLine = i < expectedLines.length ? expectedLines[i] : "";
            String actLine = i < actualLines.length ? actualLines[i] : "";

            if (!expLine.equals(actLine)) {
                if (!expLine.isEmpty()) {
                    diff.append("  EXPECTED: ").append(expLine.trim()).append("\n");
                }
                if (!actLine.isEmpty()) {
                    diff.append("  ACTUAL:   ").append(actLine.trim()).append("\n");
                }
                diff.append("\n");
            }
        }

        String result = diff.toString().trim();
        return result.isEmpty() ? "Policies differ but line-level diff could not be determined" : result;
    }
}
