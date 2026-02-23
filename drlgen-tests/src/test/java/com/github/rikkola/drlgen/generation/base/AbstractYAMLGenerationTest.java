package com.github.rikkola.drlgen.generation.base;

import dev.langchain4j.model.chat.ChatModel;
import com.github.rikkola.drlgen.generation.config.ModelConfiguration;
import com.github.rikkola.drlgen.generation.model.TestScenario;
import com.github.rikkola.drlgen.generation.model.YAMLGenerationResult;
import com.github.rikkola.drlgen.generation.service.YAMLRuleGenerationService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for YAML rule generation tests providing common setup and utilities.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractYAMLGenerationTest {

    protected YAMLRuleGenerationService generationService;
    protected ChatModel defaultModel;

    @BeforeAll
    void setUp() {
        generationService = new YAMLRuleGenerationService();
        defaultModel = ModelConfiguration.createFromEnvironment();
    }

    /**
     * Generates YAML rules and asserts successful generation, conversion, and execution.
     */
    protected YAMLGenerationResult generateAndAssertSuccess(TestScenario scenario) {
        return generateAndAssertSuccess(defaultModel, scenario);
    }

    /**
     * Generates YAML rules with a specific model and asserts success.
     */
    protected YAMLGenerationResult generateAndAssertSuccess(ChatModel model, TestScenario scenario) {
        YAMLGenerationResult result = generationService.generateAndTest(model, scenario);

        printResult(scenario, result);

        assertThat(result.yamlValidationPassed())
                .as("YAML validation should pass")
                .isTrue();
        assertThat(result.conversionPassed())
                .as("YAML to DRL conversion should pass")
                .isTrue();
        assertThat(result.drlValidationPassed())
                .as("Converted DRL validation should pass")
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
     * Generates YAML rules without assertions - for comparison tests.
     */
    protected YAMLGenerationResult generateWithoutAssertions(TestScenario scenario) {
        return generateWithoutAssertions(defaultModel, scenario);
    }

    /**
     * Generates YAML rules with a specific model without assertions.
     */
    protected YAMLGenerationResult generateWithoutAssertions(ChatModel model, TestScenario scenario) {
        YAMLGenerationResult result = generationService.generateAndTest(model, scenario);
        printResult(scenario, result);
        return result;
    }

    /**
     * Generates YAML with retry on failure.
     */
    protected YAMLGenerationResult generateWithRetry(TestScenario scenario, int maxAttempts) {
        return generateWithRetry(defaultModel, scenario, maxAttempts);
    }

    /**
     * Generates YAML with a specific model with retry on failure.
     */
    protected YAMLGenerationResult generateWithRetry(ChatModel model, TestScenario scenario, int maxAttempts) {
        YAMLGenerationResult lastResult = null;
        for (int i = 0; i < maxAttempts; i++) {
            lastResult = generationService.generateAndTest(model, scenario);
            if (lastResult.isSuccessful()) {
                System.out.println("Succeeded on attempt " + (i + 1));
                return lastResult;
            }
            System.out.println("Attempt " + (i + 1) + " failed: " + lastResult.yamlValidationMessage());
        }
        return lastResult;
    }

    /**
     * Prints the generation result to console.
     */
    protected void printResult(TestScenario scenario, YAMLGenerationResult result) {
        System.out.println("=== YAML Generation Result ===");
        System.out.println("Scenario: " + scenario.name());
        System.out.println(result.getSummary());
        System.out.println("\nGenerated YAML:\n" + result.generatedYaml());
        if (result.conversionPassed() && !result.convertedDrl().isEmpty()) {
            System.out.println("\nConverted DRL:\n" + result.convertedDrl());
        }
        if (!result.isSuccessful()) {
            System.out.println("\nFailure Details:");
            System.out.println("YAML Validation: " + result.yamlValidationMessage());
            System.out.println("Conversion: " + result.conversionMessage());
            System.out.println("DRL Validation: " + result.drlValidationMessage());
            System.out.println("Execution: " + result.executionMessage());
        }
        System.out.println("=".repeat(50));
    }
}
