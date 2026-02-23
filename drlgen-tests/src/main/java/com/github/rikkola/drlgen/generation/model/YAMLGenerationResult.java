package com.github.rikkola.drlgen.generation.model;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * Result of YAML rule generation including intermediate steps.
 */
public record YAMLGenerationResult(
        String modelName,
        String generatedYaml,
        String convertedDrl,
        boolean yamlValidationPassed,
        String yamlValidationMessage,
        boolean conversionPassed,
        String conversionMessage,
        boolean drlValidationPassed,
        String drlValidationMessage,
        boolean executionPassed,
        String executionMessage,
        int rulesFired,
        List<Object> resultingFacts,
        Duration generationTime,
        Duration totalTime
) {
    public boolean isSuccessful() {
        return yamlValidationPassed && conversionPassed && drlValidationPassed && executionPassed && rulesFired > 0;
    }

    public String getSummary() {
        return String.format(
                "Model: %s | YAML Valid: %s | Converted: %s | DRL Valid: %s | Executed: %s | Rules Fired: %d | Gen Time: %dms",
                modelName, yamlValidationPassed, conversionPassed, drlValidationPassed, executionPassed, rulesFired, generationTime.toMillis()
        );
    }

    public static YAMLGenerationResult failed(String modelName, String message, Duration totalTime) {
        return new YAMLGenerationResult(
                modelName, "", "", false, message, false, message, false, message, false, message,
                0, Collections.emptyList(), Duration.ZERO, totalTime
        );
    }

    public static YAMLGenerationResult yamlInvalid(String modelName, String yaml, String message,
                                                    Duration genTime, Duration totalTime) {
        return new YAMLGenerationResult(
                modelName, yaml, "", false, message, false, "N/A", false, "N/A", false, "N/A",
                0, Collections.emptyList(), genTime, totalTime
        );
    }

    public static YAMLGenerationResult conversionFailed(String modelName, String yaml, String message,
                                                         Duration genTime, Duration totalTime) {
        return new YAMLGenerationResult(
                modelName, yaml, "", true, "YAML valid", false, message, false, "N/A", false, "N/A",
                0, Collections.emptyList(), genTime, totalTime
        );
    }

    public static YAMLGenerationResult drlValidationFailed(String modelName, String yaml, String drl,
                                                            String message, Duration genTime, Duration totalTime) {
        return new YAMLGenerationResult(
                modelName, yaml, drl, true, "YAML valid", true, "Conversion OK", false, message, false, "N/A",
                0, Collections.emptyList(), genTime, totalTime
        );
    }

    public static YAMLGenerationResult executionFailed(String modelName, String yaml, String drl,
                                                        String message, Duration genTime, Duration totalTime) {
        return new YAMLGenerationResult(
                modelName, yaml, drl, true, "YAML valid", true, "Conversion OK", true, "DRL valid", false, message,
                0, Collections.emptyList(), genTime, totalTime
        );
    }
}
