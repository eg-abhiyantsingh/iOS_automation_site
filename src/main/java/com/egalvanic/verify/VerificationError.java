package com.egalvanic.verify;

/**
 * Thrown by every verifier in this package.
 *
 * <p>Extends {@link AssertionError} (not {@link Exception}) on purpose: TestNG marks the
 * test FAILED, and — critically — the framework's pervasive {@code catch (Exception e)}
 * blocks in page objects and {@code BaseTest.testTeardown} do NOT catch {@link Error}s,
 * so a real defect can no longer be swallowed on its way out.
 */
public class VerificationError extends AssertionError {
    public VerificationError(String message) { super(message); }
    public VerificationError(String message, Throwable cause) { super(message, cause); }
}
