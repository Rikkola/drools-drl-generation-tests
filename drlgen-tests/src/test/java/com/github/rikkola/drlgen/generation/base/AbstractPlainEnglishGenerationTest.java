package com.github.rikkola.drlgen.generation.base;

import dev.langchain4j.model.chat.ChatModel;
import com.github.rikkola.drlgen.generation.config.ModelConfiguration;
import com.github.rikkola.drlgen.generation.model.TestScenario;
import com.github.rikkola.drlgen.generation.model.PlainEnglishGenerationResult;
import com.github.rikkola.drlgen.generation.service.PlainEnglishGenerationService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for Plain English rule generation tests providing common setup and utilities.
 * Tests the two-stage pipeline:
 * Stage 1 (English Generation): Requirement -> Plain English
 * Stage 2 (DRL Conversion): Plain English -> DRL
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractPlainEnglishGenerationTest {

    protected PlainEnglishGenerationService generationService;
    protected ChatModel defaultModel;

    @BeforeAll
    void setUp() {
        generationService = new PlainEnglishGenerationService();
        defaultModel = ModelConfiguration.createFromEnvironment();
    }

    /**
     * Generates rules via Plain English pipeline and asserts successful generation and execution.
     */
    protected PlainEnglishGenerationResult generateAndAssertSuccess(TestScenario scenario) {
        return generateAndAssertSuccess(defaultModel, scenario);
    }

    /**
     * Generates rules with a specific model and asserts success.
     */
    protected PlainEnglishGenerationResult generateAndAssertSuccess(ChatModel model, TestScenario scenario) {
        PlainEnglishGenerationResult result = generationService.generateAndTest(model, scenario);

        printResult(scenario, result);

        assertThat(result.stage1EnglishGenerationPassed())
                .as("Stage 1 (English generation) should pass")
                .isTrue();
        assertThat(result.stage2DrlConversionPassed())
                .as("Stage 2 (DRL conversion) should pass")
                .isTrue();
        assertThat(result.drlValidationPassed())
                .as("DRL validation should pass")
                .isTrue();
        assertThat(result.executionPassed())
                .as("DRL execution should pass")
                .isTrue();
        assertThat(result.rulesFired())
                .as("At least one rule should fire")
                .isGreaterThan(0);

        return result;
    }

    /**
     * Generates rules without assertions - for comparison tests.
     */
    protected PlainEnglishGenerationResult generateWithoutAssertions(TestScenario scenario) {
        return generateWithoutAssertions(defaultModel, scenario);
    }

    /**
     * Generates rules with a specific model without assertions.
     */
    protected PlainEnglishGenerationResult generateWithoutAssertions(ChatModel model, TestScenario scenario) {
        PlainEnglishGenerationResult result = generationService.generateAndTest(model, scenario);
        printResult(scenario, result);
        return result;
    }

    /**
     * Generates rules with retry on failure.
     */
    protected PlainEnglishGenerationResult generateWithRetry(TestScenario scenario, int maxAttempts) {
        return generateWithRetry(defaultModel, scenario, maxAttempts);
    }

    /**
     * Generates rules with a specific model with retry on failure.
     */
    protected PlainEnglishGenerationResult generateWithRetry(ChatModel model, TestScenario scenario, int maxAttempts) {
        PlainEnglishGenerationResult lastResult = null;
        for (int i = 0; i < maxAttempts; i++) {
            lastResult = generationService.generateAndTest(model, scenario);
            if (lastResult.isSuccessful()) {
                System.out.println("Succeeded on attempt " + (i + 1));
                return lastResult;
            }
            String failureReason = !lastResult.stage1EnglishGenerationPassed() ? lastResult.stage1EnglishGenerationMessage() :
                    !lastResult.stage2DrlConversionPassed() ? lastResult.stage2DrlConversionMessage() :
                    !lastResult.drlValidationPassed() ? lastResult.drlValidationMessage() :
                    lastResult.executionMessage();
            System.out.println("Attempt " + (i + 1) + " failed: " + failureReason);
        }
        return lastResult;
    }

    /**
     * Prints the generation result to console.
     */
    protected void printResult(TestScenario scenario, PlainEnglishGenerationResult result) {
        System.out.println("=== Plain English Generation Result ===");
        System.out.println("Scenario: " + scenario.name());
        System.out.println(result.getSummary());

        if (!result.generatedEnglish().isEmpty()) {
            System.out.println("\n--- Generated Plain English ---");
            System.out.println(result.generatedEnglish());
        }
        if (result.stage2DrlConversionPassed() && !result.convertedDrl().isEmpty()) {
            System.out.println("\n--- Converted DRL ---");
            System.out.println(result.convertedDrl());
        }
        if (!result.isSuccessful()) {
            System.out.println("\n--- Failure Details ---");
            System.out.println("Stage 1 (English Generation): " + result.stage1EnglishGenerationMessage());
            System.out.println("Stage 2 (DRL Conversion): " + result.stage2DrlConversionMessage());
            System.out.println("DRL Validation: " + result.drlValidationMessage());
            System.out.println("Execution: " + result.executionMessage());
        }
        System.out.println("=".repeat(50));
    }
}
