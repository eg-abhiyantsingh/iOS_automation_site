package com.egalvanic.listeners;

import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;
import org.testng.internal.annotations.DisabledRetryAnalyzer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Applies {@link EnvironmentRetryAnalyzer} to EVERY @Test method suite-wide
 * (service-loader registered — no per-class annotation churn across the
 * ~1,7xx tests). A test that already declares its own retryAnalyzer keeps it.
 */
public class EnvironmentRetryTransformer implements IAnnotationTransformer {

    @Override
    public void transform(ITestAnnotation annotation, Class testClass,
                          Constructor testConstructor, Method testMethod) {
        Class<?> existing = annotation.getRetryAnalyzerClass();
        if (existing == null || existing == DisabledRetryAnalyzer.class) {
            annotation.setRetryAnalyzer(EnvironmentRetryAnalyzer.class);
        }
    }
}
