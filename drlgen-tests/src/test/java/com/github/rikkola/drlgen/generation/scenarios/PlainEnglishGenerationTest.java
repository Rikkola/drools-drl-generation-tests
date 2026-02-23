package com.github.rikkola.drlgen.generation.scenarios;

import com.github.rikkola.drlgen.generation.base.AbstractPlainEnglishGenerationTest;
import com.github.rikkola.drlgen.generation.model.TestScenario;
import com.github.rikkola.drlgen.generation.provider.TestScenarioProvider;
import com.github.rikkola.drlgen.generation.model.PlainEnglishGenerationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the two-stage Plain English rule generation pipeline.
 * Stage 1 (English Generation): Requirement -> Structured Plain English
 * Stage 2 (DRL Conversion): Plain English -> DRL
 */
@DisplayName("Plain English Rule Generation Tests")
class PlainEnglishGenerationTest extends AbstractPlainEnglishGenerationTest {

    @Test
    @DisplayName("Generate DRL via Plain English for adult validation")
    void testAdultValidationPlainEnglish() {
        TestScenario scenario = TestScenarioProvider.getScenarioByName("Adult Validation");
        PlainEnglishGenerationResult result = generateAndAssertSuccess(scenario);

        // Verify Plain English contains expected content
        assertThat(result.generatedEnglish())
                .containsIgnoringCase("18")
                .containsIgnoringCase("adult");

        // Verify converted DRL has proper structure
        assertThat(result.convertedDrl())
                .contains("rule")
                .contains("when")
                .contains("then")
                .contains("end");
    }

    @Test
    @DisplayName("Generate DRL via Plain English for senior citizen validation")
    void testSeniorCitizenPlainEnglish() {
        TestScenario scenario = TestScenarioProvider.getScenarioByName("Senior Citizen Discount");
        PlainEnglishGenerationResult result = generateAndAssertSuccess(scenario);

        assertThat(result.generatedEnglish())
                .containsIgnoringCase("65");
    }

    @Test
    @DisplayName("Generate DRL via Plain English for order discount")
    void testOrderDiscountPlainEnglish() {
        TestScenario scenario = TestScenarioProvider.getScenarioByName("Order Discount - Basic");
        PlainEnglishGenerationResult result = generateAndAssertSuccess(scenario);

        assertThat(result.generatedEnglish())
                .containsIgnoringCase("discount");
    }

    @Test
    @DisplayName("Generate rules with retry on failure")
    void testPlainEnglishGenerationWithRetry() {
        TestScenario scenario = TestScenarioProvider.getScenarioByName("Adult Validation");
        PlainEnglishGenerationResult result = generateWithRetry(scenario, 3);

        assertThat(result.isSuccessful())
                .as("Plain English generation should succeed within 3 attempts")
                .isTrue();
    }

    @Test
    @DisplayName("Verify two-stage pipeline produces valid DRL")
    void testTwoStagePipelineProducesValidDRL() {
        TestScenario scenario = TestScenarioProvider.getScenarioByName("Adult Validation");
        PlainEnglishGenerationResult result = generateAndAssertSuccess(scenario);

        // Verify both stages completed
        assertThat(result.stage1EnglishGenerationPassed()).isTrue();
        assertThat(result.stage2DrlConversionPassed()).isTrue();

        // Verify the converted DRL has proper structure
        assertThat(result.convertedDrl())
                .contains("package")
                .contains("rule")
                .contains("when")
                .contains("then")
                .contains("end");
    }

    @Test
    @DisplayName("Verify stage timing is recorded")
    void testStageTiming() {
        TestScenario scenario = TestScenarioProvider.getScenarioByName("Adult Validation");
        PlainEnglishGenerationResult result = generateAndAssertSuccess(scenario);

        // Both stages should have non-zero timing
        assertThat(result.stage1EnglishGenerationTime().toMillis())
                .as("Stage 1 (English Generation) should have recorded time")
                .isGreaterThan(0);
        assertThat(result.stage2DrlConversionTime().toMillis())
                .as("Stage 2 (DRL Conversion) should have recorded time")
                .isGreaterThan(0);

        // Total generation time should be sum of stages
        assertThat(result.getGenerationTime())
                .isEqualTo(result.stage1EnglishGenerationTime().plus(result.stage2DrlConversionTime()));
    }

    @Test
    @DisplayName("Generate DRL via Plain English for email validation")
    void testEmailValidationPlainEnglish() {
        TestScenario scenario = TestScenarioProvider.getScenarioByName("Email Validation");
        PlainEnglishGenerationResult result = generateAndAssertSuccess(scenario);

        assertThat(result.generatedEnglish())
                .containsIgnoringCase("email");
    }

    @Test
    @DisplayName("Generate DRL via Plain English for loan eligibility")
    void testLoanEligibilityPlainEnglish() {
        TestScenario scenario = TestScenarioProvider.getScenarioByName("Loan Eligibility");
        PlainEnglishGenerationResult result = generateAndAssertSuccess(scenario);

        // This is a multi-condition rule
        assertThat(result.generatedEnglish())
                .containsIgnoringCase("income")
                .containsIgnoringCase("credit");
    }
}
