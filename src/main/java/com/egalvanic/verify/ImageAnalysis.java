package com.egalvanic.verify;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

/**
 * Driver-free pixel analysis used by {@link AssetLoadVerifier}.
 *
 * <p>Deliberately separated from any Appium dependency so its logic can be unit-tested
 * on a plain JVM (see {@code VerifierSelfTest}). The heuristic: a genuinely rendered
 * PDF / image / single-line-diagram has BOTH colour variety and luminance spread;
 * a blank, grey, or failed-to-load tile collapses to a near-uniform region.
 *
 * <p>Pure JDK — no OpenCV/AShot dependency added to the build.
 */
public final class ImageAnalysis {

    private ImageAnalysis() {}

    /** Default thresholds below which a region is considered "not really rendered". */
    public static final int MIN_UNIQUE_COLORS = 24;
    public static final double MIN_STDDEV = 8.0;

    public record Result(int uniqueColors, double stdDev) {
        public boolean looksBlank() {
            return uniqueColors < MIN_UNIQUE_COLORS || stdDev < MIN_STDDEV;
        }
        @Override public String toString() {
            return "uniqueColors=" + uniqueColors + ", stdDev=" + String.format("%.1f", stdDev);
        }
    }

    public static Result analyzeFull(BufferedImage img) {
        return analyze(img, 0, 0, img.getWidth(), img.getHeight());
    }

    /** Sample a grid over the region and return colour-variety + luminance std-dev. */
    public static Result analyze(BufferedImage img, int x, int y, int w, int h) {
        if (img == null) throw new VerificationError("null image");
        x = Math.max(0, x); y = Math.max(0, y);
        w = Math.min(w, img.getWidth() - x);
        h = Math.min(h, img.getHeight() - y);
        if (w <= 0 || h <= 0) throw new VerificationError("region off-screen / zero-size");

        Set<Integer> colors = new HashSet<>();
        double sum = 0, sumSq = 0;
        int n = 0;
        int stepX = Math.max(1, w / 64);
        int stepY = Math.max(1, h / 64);
        for (int i = x; i < x + w; i += stepX) {
            for (int j = y; j < y + h; j += stepY) {
                int rgb = img.getRGB(i, j);
                colors.add(rgb & 0xF8F8F8); // quantize: ignore sub-perceptual noise
                int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
                double lum = 0.299 * r + 0.587 * g + 0.114 * b;
                sum += lum;
                sumSq += lum * lum;
                n++;
            }
        }
        double mean = sum / n;
        double variance = Math.max(0, sumSq / n - mean * mean);
        return new Result(colors.size(), Math.sqrt(variance));
    }
}
