package com.github.rikkola.drlgen.generation.service;

import dev.langchain4j.model.chat.ChatModel;
import com.github.rikkola.drlgen.execution.DRLPopulatorRunner;
import com.github.rikkola.drlgen.execution.DRLRunnerResult;
import com.github.rikkola.drlgen.generation.agent.EnglishToDRLAgent;
import com.github.rikkola.drlgen.generation.agent.PlainEnglishGenerationAgent;
import com.github.rikkola.drlgen.generation.config.ModelConfiguration;
import com.github.rikkola.drlgen.generation.model.PlainEnglishGenerationResult;
import com.github.rikkola.drlgen.generation.model.TestScenario;
import com.github.rikkola.drlgen.generation.util.FactVerificationUtils;
import com.github.rikkola.drlgen.service.DRLValidationService;
import com.github.rikkola.drlgen.util.StringCleanupUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Orchestrates the two-stage Plain English rule generation pipeline:
 * Stage 1 (English Generation): Requirement → Structured Plain English
 * Stage 2 (DRL Conversion): Plain English → DRL
 * Then validates and executes the DRL.
 */
public class PlainEnglishGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(PlainEnglishGenerationService.class);

    private final DRLValidationService drlValidationService;
    private final Function<ChatModel, PlainEnglishGenerationAgent> englishAgentFactory;
    private final Function<ChatModel, EnglishToDRLAgent> drlAgentFactory;

    /**
     * Default constructor using real LLM agents.
     */
    public PlainEnglishGenerationService() {
        this(new DRLValidationService(), PlainEnglishGenerationAgent::create, EnglishToDRLAgent::create);
    }

    /**
     * Constructor with custom agent factories for testing.
     * Allows injection of mock agents for both pipeline stages.
     */
    public PlainEnglishGenerationService(DRLValidationService drlValidationService,
                                          Function<ChatModel, PlainEnglishGenerationAgent> englishAgentFactory,
                                          Function<ChatModel, EnglishToDRLAgent> drlAgentFactory) {
        this.drlValidationService = drlValidationService;
        this.englishAgentFactory = englishAgentFactory;
        this.drlAgentFactory = drlAgentFactory;
    }

    /**
     * Generates rules using the two-stage pipeline and tests execution.
     */
    public PlainEnglishGenerationResult generateAndTest(ChatModel model, TestScenario scenario) {
        String modelName = ModelConfiguration.extractModelName(model);
        Instant start = Instant.now();

        logger.info("Starting Plain English rule generation for scenario '{}' using model '{}'",
                scenario.name(), modelName);
        logger.debug("--- Scenario Details ---");
        logger.debug("Description: {}", scenario.description());
        logger.debug("Requirement:\n{}", scenario.requirement());
        logger.debug("Fact Types:\n{}", scenario.getFactTypesDescription());
        logger.debug("Test Cases: {}", scenario.testCases().size());
        for (TestScenario.TestCase tc : scenario.testCases()) {
            logger.debug("  - {}: input={}", tc.name(), tc.inputJson());
        }

        // Stage 1: Generate Plain English from requirement
        PlainEnglishGenerationAgent englishAgent = englishAgentFactory.apply(model);
        Instant stage1Start = Instant.now();
        String generatedEnglish;
        try {
            generatedEnglish = englishAgent.generatePlainEnglish(
                    scenario.requirement(),
                    scenario.getFactTypesDescription(),
                    scenario.getTestScenarioDescription()
            );
            generatedEnglish = StringCleanupUtils.cleanupText(generatedEnglish);
            logger.debug("Generated Plain English:\n{}", generatedEnglish);
        } catch (Exception e) {
            logger.error("Plain English generation failed: {}", e.getMessage());
            return PlainEnglishGenerationResult.stage1EnglishGenerationFailed(modelName,
                    "English generation failed: " + e.getMessage(), Duration.between(start, Instant.now()));
        }
        Duration stage1EnglishGenerationTime = Duration.between(stage1Start, Instant.now());
        logger.info("Stage 1 (English generation) completed in {}ms", stage1EnglishGenerationTime.toMillis());

        // Stage 2: Convert Plain English to DRL
        EnglishToDRLAgent drlAgent = drlAgentFactory.apply(model);
        Instant stage2Start = Instant.now();
        String convertedDrl;
        try {
            convertedDrl = drlAgent.convertToDRL(generatedEnglish);
            convertedDrl = StringCleanupUtils.cleanupDrl(convertedDrl);
            logger.debug("Converted DRL:\n{}", convertedDrl);
        } catch (Exception e) {
            logger.error("English to DRL conversion failed: {}", e.getMessage());
            return PlainEnglishGenerationResult.stage2DrlConversionFailed(modelName, generatedEnglish,
                    "DRL conversion failed: " + e.getMessage(), stage1EnglishGenerationTime, Duration.between(start, Instant.now()));
        }
        Duration stage2DrlConversionTime = Duration.between(stage2Start, Instant.now());
        logger.info("Stage 2 (DRL conversion) completed in {}ms", stage2DrlConversionTime.toMillis());

        // Validate DRL
        String drlValidationResult;
        boolean drlValidationPassed;
        try {
            drlValidationResult = drlValidationService.validateDRLStructure(convertedDrl);
            drlValidationPassed = !drlValidationResult.contains("ERROR:");
            logger.info("DRL validation result: {} (passed: {})", drlValidationResult, drlValidationPassed);
        } catch (Exception e) {
            logger.error("DRL validation failed with exception: {}", e.getMessage());
            return PlainEnglishGenerationResult.validationFailed(modelName, generatedEnglish, convertedDrl,
                    "DRL validation exception: " + e.getMessage(), stage1EnglishGenerationTime, stage2DrlConversionTime,
                    Duration.between(start, Instant.now()));
        }

        if (!drlValidationPassed) {
            logger.warn("Converted DRL validation failed: {}", drlValidationResult);
            return PlainEnglishGenerationResult.validationFailed(modelName, generatedEnglish, convertedDrl,
                    drlValidationResult, stage1EnglishGenerationTime, stage2DrlConversionTime, Duration.between(start, Instant.now()));
        }

        // Execute DRL with all test cases
        int totalRulesFired = 0;
        List<Object> allResultingFacts = new ArrayList<>();
        int testCasesPassed = 0;

        for (TestScenario.TestCase testCase : scenario.testCases()) {
            logger.info("Executing test case '{}' with input: {}", testCase.name(), testCase.inputJson());
            try {
                DRLRunnerResult result = DRLPopulatorRunner.runDRLWithJsonFacts(
                        convertedDrl, testCase.inputJson(), 100);

                logger.info("Test case '{}': {} rules fired, {} facts in working memory",
                        testCase.name(), result.firedRules(), result.objects().size());

                // Verify rules fired count if specified
                String rulesFiredError = testCase.validateRulesFired(result.firedRules());
                if (rulesFiredError != null) {
                    logger.error("Test case '{}' failed: {}", testCase.name(), rulesFiredError);
                    return PlainEnglishGenerationResult.executionFailed(modelName, generatedEnglish, convertedDrl,
                            "Test case '" + testCase.name() + "' failed: " + rulesFiredError,
                            stage1EnglishGenerationTime, stage2DrlConversionTime,
                            Duration.between(start, Instant.now()));
                }

                // Verify expected values (support both legacy and new formats)
                if (testCase.hasTypedExpectations()) {
                    // New: Type-aware verification
                    String factError = FactVerificationUtils.verifyExpectedFacts(result.objects(), testCase.expectedFacts());
                    if (factError != null) {
                        logger.error("Test case '{}' failed: {}", testCase.name(), factError);
                        return PlainEnglishGenerationResult.executionFailed(modelName, generatedEnglish, convertedDrl,
                                "Test case '" + testCase.name() + "' failed: " + factError,
                                stage1EnglishGenerationTime, stage2DrlConversionTime,
                                Duration.between(start, Instant.now()));
                    }
                } else if (testCase.expectedFieldValues() != null && !testCase.expectedFieldValues().isEmpty()) {
                    // Legacy: Field-based verification (backward compatibility)
                    String fieldError = FactVerificationUtils.verifyExpectedFields(result.objects(), testCase.expectedFieldValues());
                    if (fieldError != null) {
                        logger.error("Test case '{}' failed: {}", testCase.name(), fieldError);
                        return PlainEnglishGenerationResult.executionFailed(modelName, generatedEnglish, convertedDrl,
                                "Test case '" + testCase.name() + "' failed: " + fieldError,
                                stage1EnglishGenerationTime, stage2DrlConversionTime,
                                Duration.between(start, Instant.now()));
                    }
                }

                totalRulesFired += result.firedRules();
                allResultingFacts.addAll(result.objects());
                testCasesPassed++;
            } catch (Throwable e) {
                logger.error("Test case '{}' failed: {}", testCase.name(), e.getMessage());
                return PlainEnglishGenerationResult.executionFailed(modelName, generatedEnglish, convertedDrl,
                        "Test case '" + testCase.name() + "' failed: " + e.getMessage(),
                        stage1EnglishGenerationTime, stage2DrlConversionTime,
                        Duration.between(start, Instant.now()));
            }
        }

        if (testCasesPassed == 0) {
            return PlainEnglishGenerationResult.stage1EnglishGenerationFailed(modelName, "No test cases to execute",
                    Duration.between(start, Instant.now()));
        }

        logger.info("All {} test cases passed. Total rules fired: {}", testCasesPassed, totalRulesFired);

        return new PlainEnglishGenerationResult(
                modelName,
                generatedEnglish,
                convertedDrl,
                true,
                "English generation successful",
                true,
                "DRL conversion successful",
                true,
                drlValidationResult,
                true,
                "All " + testCasesPassed + " test cases passed",
                totalRulesFired,
                allResultingFacts,
                stage1EnglishGenerationTime,
                stage2DrlConversionTime,
                Duration.between(start, Instant.now())
        );
    }
}
