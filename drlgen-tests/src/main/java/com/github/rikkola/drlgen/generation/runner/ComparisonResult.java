package com.github.rikkola.drlgen.generation.runner;

import java.time.Duration;

/**
 * Represents the result of a single generation test.
 */
public record ComparisonResult(
        String modelName,
        String scenarioName,
        String format,  // "DRL" or "YAML"
        boolean success,
        int rulesFired,
        Duration generationTime,
        String errorMessage
) {
    public static ComparisonResult success(String modelName, String scenarioName, String format,
                                           int rulesFired, Duration generationTime) {
        return new ComparisonResult(modelName, scenarioName, format, true, rulesFired, generationTime, null);
    }

    public static ComparisonResult failure(String modelName, String scenarioName, String format,
                                           Duration generationTime, String errorMessage) {
        return new ComparisonResult(modelName, scenarioName, format, false, 0, generationTime, errorMessage);
    }

    public String getStatusString() {
        return success ? "PASS" : "FAIL";
    }
}
