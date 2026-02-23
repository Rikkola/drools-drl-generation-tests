package com.github.rikkola.drlgen.generation.comparison;

import dev.langchain4j.model.chat.ChatModel;
import com.github.rikkola.drlgen.generation.config.ModelConfiguration;
import com.github.rikkola.drlgen.generation.model.GenerationResult;
import com.github.rikkola.drlgen.generation.model.TestScenario;
import com.github.rikkola.drlgen.generation.provider.TestScenarioProvider;
import com.github.rikkola.drlgen.generation.service.DRLGenerationService;
import com.github.rikkola.drlgen.generation.model.YAMLGenerationResult;
import com.github.rikkola.drlgen.generation.service.YAMLRuleGenerationService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Compares DRL generation vs YAML generation across different models and scenarios.
 * This helps identify which format works better for each model.
 * Models are loaded from models.yaml configuration.
 */
@DisplayName("DRL vs YAML Format Comparison Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("comparison")
@Tag("slow")
class FormatComparisonTest {

    private DRLGenerationService drlService;
    private YAMLRuleGenerationService yamlService;
    private ChatModel defaultModel;

    @BeforeAll
    void setUp() {
        drlService = new DRLGenerationService();
        yamlService = new YAMLRuleGenerationService();
        defaultModel = ModelConfiguration.createFromEnvironment();
    }

    static Stream<String> availableModels() {
        return ModelConfiguration.getAvailableModels().stream();
    }

    @Test
    @DisplayName("Compare DRL vs YAML for adult validation - default model")
    void compareFormatsAdultValidation() {
        TestScenario scenario = TestScenarioProvider.getScenarioByName("Adult Validation");

        FormatComparisonResult comparison = compareFormats(defaultModel, scenario);
        printComparisonResult(comparison);

        // At least one format should work
        assertThat(comparison.drlSuccess || comparison.yamlSuccess)
                .as("At least one format should work for adult validation")
                .isTrue();
    }

    @Test
    @DisplayName("Compare DRL vs YAML for order discount - default model")
    void compareFormatsOrderDiscount() {
        TestScenario scenario = TestScenarioProvider.getScenarioByName("Order Discount - Basic");

        FormatComparisonResult comparison = compareFormats(defaultModel, scenario);
        printComparisonResult(comparison);

        // At least one format should work
        assertThat(comparison.drlSuccess || comparison.yamlSuccess)
                .as("At least one format should work for order discount")
                .isTrue();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("availableModels")
    @DisplayName("Compare DRL vs YAML across all models - adult validation")
    void compareFormatsAcrossModels(String modelName) {
        TestScenario scenario = TestScenarioProvider.getScenarioByName("Adult Validation");
        ChatModel model = ModelConfiguration.createModel(modelName);

        FormatComparisonResult comparison = compareFormats(model, scenario);
        printComparisonResult(comparison);

        // Log results for analysis
        System.out.printf("[%s] DRL: %s, YAML: %s%n",
                modelName,
                comparison.drlSuccess ? "PASS" : "FAIL",
                comparison.yamlSuccess ? "PASS" : "FAIL");
    }

    @Test
    @DisplayName("Full format comparison report - all scenarios, default model")
    void fullFormatComparisonReport() {
        List<FormatComparisonResult> results = new ArrayList<>();

        List<TestScenario> scenarios = List.of(
                TestScenarioProvider.getScenarioByName("Adult Validation"),
                TestScenarioProvider.getScenarioByName("Senior Citizen Discount"),
                TestScenarioProvider.getScenarioByName("Order Discount - Basic")
        );

        for (TestScenario scenario : scenarios) {
            results.add(compareFormats(defaultModel, scenario));
        }

        // Print summary report
        printComparisonReport(results);

        // At least 50% of scenarios should work with at least one format
        long successCount = results.stream()
                .filter(r -> r.drlSuccess || r.yamlSuccess)
                .count();
        assertThat(successCount)
                .as("At least 50%% of scenarios should work")
                .isGreaterThanOrEqualTo(scenarios.size() / 2);
    }

    private FormatComparisonResult compareFormats(ChatModel model, TestScenario scenario) {
        String modelName = ModelConfiguration.extractModelName(model);

        // Test DRL generation
        GenerationResult drlResult;
        try {
            drlResult = drlService.generateAndTest(model, scenario);
        } catch (Exception e) {
            drlResult = null;
        }

        // Test YAML generation
        YAMLGenerationResult yamlResult;
        try {
            yamlResult = yamlService.generateAndTest(model, scenario);
        } catch (Exception e) {
            yamlResult = null;
        }

        return new FormatComparisonResult(
                modelName,
                scenario.name(),
                drlResult != null && drlResult.isSuccessful(),
                yamlResult != null && yamlResult.isSuccessful(),
                drlResult != null ? drlResult.generationTime().toMillis() : -1,
                yamlResult != null ? yamlResult.generationTime().toMillis() : -1,
                drlResult != null ? drlResult.rulesFired() : 0,
                yamlResult != null ? yamlResult.rulesFired() : 0,
                drlResult,
                yamlResult
        );
    }

    private void printComparisonResult(FormatComparisonResult result) {
        System.out.println("=".repeat(60));
        System.out.printf("Format Comparison: %s | %s%n", result.modelName, result.scenarioName);
        System.out.println("=".repeat(60));
        System.out.printf("DRL:  %s (time: %dms, rules: %d)%n",
                result.drlSuccess ? "PASS" : "FAIL",
                result.drlTimeMs,
                result.drlRulesFired);
        System.out.printf("YAML: %s (time: %dms, rules: %d)%n",
                result.yamlSuccess ? "PASS" : "FAIL",
                result.yamlTimeMs,
                result.yamlRulesFired);

        if (!result.drlSuccess && result.drlResult != null) {
            System.out.println("\nDRL Failure: " + result.drlResult.validationMessage());
        }
        if (!result.yamlSuccess && result.yamlResult != null) {
            System.out.println("\nYAML Failure: " + result.yamlResult.yamlValidationMessage());
            if (result.yamlResult.conversionPassed()) {
                System.out.println("Conversion: " + result.yamlResult.conversionMessage());
            }
            if (!result.yamlResult.drlValidationPassed()) {
                System.out.println("DRL Validation: " + result.yamlResult.drlValidationMessage());
            }
        }
        System.out.println();
    }

    private void printComparisonReport(List<FormatComparisonResult> results) {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("FORMAT COMPARISON SUMMARY REPORT");
        System.out.println("=".repeat(70));
        System.out.printf("%-30s | %-10s | %-10s%n", "Scenario", "DRL", "YAML");
        System.out.println("-".repeat(70));

        int drlPasses = 0;
        int yamlPasses = 0;

        for (FormatComparisonResult result : results) {
            System.out.printf("%-30s | %-10s | %-10s%n",
                    result.scenarioName,
                    result.drlSuccess ? "PASS" : "FAIL",
                    result.yamlSuccess ? "PASS" : "FAIL");
            if (result.drlSuccess) drlPasses++;
            if (result.yamlSuccess) yamlPasses++;
        }

        System.out.println("-".repeat(70));
        System.out.printf("%-30s | %d/%d      | %d/%d%n",
                "TOTAL",
                drlPasses, results.size(),
                yamlPasses, results.size());
        System.out.println("=".repeat(70));

        // Determine winner
        if (drlPasses > yamlPasses) {
            System.out.println("Winner: DRL format performs better with this model");
        } else if (yamlPasses > drlPasses) {
            System.out.println("Winner: YAML format performs better with this model");
        } else {
            System.out.println("Result: Both formats perform equally");
        }
        System.out.println();
    }

    /**
     * Result of comparing DRL vs YAML generation for a single scenario.
     */
    record FormatComparisonResult(
            String modelName,
            String scenarioName,
            boolean drlSuccess,
            boolean yamlSuccess,
            long drlTimeMs,
            long yamlTimeMs,
            int drlRulesFired,
            int yamlRulesFired,
            GenerationResult drlResult,
            YAMLGenerationResult yamlResult
    ) {}
}
