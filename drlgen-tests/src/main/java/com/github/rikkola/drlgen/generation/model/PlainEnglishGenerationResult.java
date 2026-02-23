package com.github.rikkola.drlgen.generation.model;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * Result of the two-stage Plain English rule generation pipeline.
 * Stage 1: Requirement -> Structured Plain English (English Generation)
 * Stage 2: Plain English -> DRL (DRL Conversion)
 */
public record PlainEnglishGenerationResult(
        String modelName,
        String generatedEnglish,
        String convertedDrl,
        boolean stage1EnglishGenerationPassed,
        String stage1EnglishGenerationMessage,
        boolean stage2DrlConversionPassed,
        String stage2DrlConversionMessage,
        boolean drlValidationPassed,
        String drlValidationMessage,
        boolean executionPassed,
        String executionMessage,
        int rulesFired,
        List<Object> resultingFacts,
        Duration stage1EnglishGenerationTime,
        Duration stage2DrlConversionTime,
        Duration totalTime
) {
    public boolean isSuccessful() {
        return stage1EnglishGenerationPassed && stage2DrlConversionPassed && drlValidationPassed && executionPassed && rulesFired > 0;
    }

    public String getSummary() {
        return String.format(
                "Model: %s | Stage1-EnglishGen: %s | Stage2-DrlConv: %s | DRL Valid: %s | Executed: %s | Rules Fired: %d | EnglishGen: %dms | DrlConv: %dms",
                modelName, stage1EnglishGenerationPassed, stage2DrlConversionPassed, drlValidationPassed, executionPassed,
                rulesFired, stage1EnglishGenerationTime.toMillis(), stage2DrlConversionTime.toMillis()
        );
    }

    public Duration getGenerationTime() {
        return stage1EnglishGenerationTime.plus(stage2DrlConversionTime);
    }

    public static PlainEnglishGenerationResult stage1EnglishGenerationFailed(String modelName, String message, Duration totalTime) {
        return new PlainEnglishGenerationResult(
                modelName, "", "", false, message, false, "N/A", false, "N/A", false, "N/A",
                0, Collections.emptyList(), Duration.ZERO, Duration.ZERO, totalTime
        );
    }

    public static PlainEnglishGenerationResult stage2DrlConversionFailed(String modelName, String english,
                                                              String message, Duration stage1EnglishGenerationTime, Duration totalTime) {
        return new PlainEnglishGenerationResult(
                modelName, english, "", true, "English generation OK", false, message, false, "N/A", false, "N/A",
                0, Collections.emptyList(), stage1EnglishGenerationTime, Duration.ZERO, totalTime
        );
    }

    public static PlainEnglishGenerationResult validationFailed(String modelName, String english, String drl,
                                                                  String message, Duration stage1EnglishGenerationTime,
                                                                  Duration stage2DrlConversionTime, Duration totalTime) {
        return new PlainEnglishGenerationResult(
                modelName, english, drl, true, "English generation OK", true, "DRL conversion OK", false, message, false, "N/A",
                0, Collections.emptyList(), stage1EnglishGenerationTime, stage2DrlConversionTime, totalTime
        );
    }

    public static PlainEnglishGenerationResult executionFailed(String modelName, String english, String drl,
                                                                 String message, Duration stage1EnglishGenerationTime,
                                                                 Duration stage2DrlConversionTime, Duration totalTime) {
        return new PlainEnglishGenerationResult(
                modelName, english, drl, true, "English generation OK", true, "DRL conversion OK", true, "DRL valid", false, message,
                0, Collections.emptyList(), stage1EnglishGenerationTime, stage2DrlConversionTime, totalTime
        );
    }
}
