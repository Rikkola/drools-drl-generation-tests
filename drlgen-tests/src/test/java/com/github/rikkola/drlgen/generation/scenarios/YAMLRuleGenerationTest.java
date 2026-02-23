package com.github.rikkola.drlgen.generation.scenarios;

import com.github.rikkola.drlgen.generation.base.AbstractYAMLGenerationTest;
import com.github.rikkola.drlgen.generation.model.TestScenario;
import com.github.rikkola.drlgen.generation.provider.TestScenarioProvider;
import com.github.rikkola.drlgen.generation.model.YAMLGenerationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for YAML rule generation using AI models.
 * Uses the same scenarios as DRL tests but generates YAML instead.
 */
@DisplayName("YAML Rule Generation Tests")
class YAMLRuleGenerationTest extends AbstractYAMLGenerationTest {

    @Test
    @DisplayName("Generate YAML rule for adult validation")
    void testAdultValidationYAML() {
        TestScenario scenario = TestScenarioProvider.getScenarioByName("Adult Validation");
        YAMLGenerationResult result = generateAndAssertSuccess(scenario);

        // Verify YAML contains expected structure
        assertThat(result.generatedYaml())
                .contains("rules:")
                .contains("condition:")
                .contains("action:");
    }

    @Test
    @DisplayName("Generate YAML rule for senior citizen validation")
    void testSeniorCitizenYAML() {
        TestScenario scenario = TestScenarioProvider.getScenarioByName("Senior Citizen Discount");
        YAMLGenerationResult result = generateAndAssertSuccess(scenario);

        assertThat(result.generatedYaml())
                .contains("rules:")
                .contains("65");
    }

    @Test
    @DisplayName("Generate YAML rule for order discount")
    void testOrderDiscountYAML() {
        TestScenario scenario = TestScenarioProvider.getScenarioByName("Order Discount - Basic");
        YAMLGenerationResult result = generateAndAssertSuccess(scenario);

        assertThat(result.generatedYaml())
                .contains("rules:")
                .contains("discount");
    }

    @Test
    @DisplayName("Generate YAML rules with retry on failure")
    void testYAMLGenerationWithRetry() {
        TestScenario scenario = TestScenarioProvider.getScenarioByName("Adult Validation");
        YAMLGenerationResult result = generateWithRetry(scenario, 3);

        assertThat(result.isSuccessful())
                .as("YAML generation should succeed within 3 attempts")
                .isTrue();
    }

    @Test
    @DisplayName("Verify YAML to DRL conversion produces valid DRL")
    void testYAMLToDRLConversion() {
        TestScenario scenario = TestScenarioProvider.getScenarioByName("Adult Validation");
        YAMLGenerationResult result = generateAndAssertSuccess(scenario);

        // Verify the converted DRL has proper structure
        assertThat(result.convertedDrl())
                .contains("package org.drools.generated")
                .contains("rule")
                .contains("when")
                .contains("then")
                .contains("end");
    }
}
