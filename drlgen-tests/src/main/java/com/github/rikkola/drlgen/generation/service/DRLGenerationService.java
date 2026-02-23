package com.github.rikkola.drlgen.generation.service;

import dev.langchain4j.model.chat.ChatModel;
import com.github.rikkola.drlgen.execution.DRLPopulatorRunner;
import com.github.rikkola.drlgen.execution.DRLRunnerResult;
import com.github.rikkola.drlgen.generation.agent.DRLGenerationAgent;
import com.github.rikkola.drlgen.generation.config.ModelConfiguration;
import com.github.rikkola.drlgen.generation.model.GenerationResult;
import com.github.rikkola.drlgen.generation.model.TestScenario;
import com.github.rikkola.drlgen.service.DRLValidationService;
import com.github.rikkola.drlgen.util.StringCleanupUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.rikkola.drlgen.generation.util.FactVerificationUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Orchestrates the DRL generation, validation, and execution workflow.
 */
public class DRLGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(DRLGenerationService.class);

    private final DRLValidationService validationService;
    private final Function<ChatModel, DRLGenerationAgent> agentFactory;

    /**
     * Default constructor using real LLM agent.
     */
    public DRLGenerationService() {
        this(new DRLValidationService(), DRLGenerationAgent::create);
    }

    /**
     * Constructor with custom validation service, using real LLM agent.
     */
    public DRLGenerationService(DRLValidationService validationService) {
        this(validationService, DRLGenerationAgent::create);
    }

    /**
     * Constructor with custom agent factory for testing.
     * Allows injection of mock agents.
     */
    public DRLGenerationService(DRLValidationService validationService,
                                 Function<ChatModel, DRLGenerationAgent> agentFactory) {
        this.validationService = validationService;
        this.agentFactory = agentFactory;
    }

    /**
     * Generates DRL from a requirement and validates it (no test execution).
     * Suitable for web UI where only generation and validation is needed.
     */
    public GenerationResult generateAndValidate(ChatModel model, String requirement, String factTypesDescription) {
        String modelName = ModelConfiguration.extractModelName(model);
        Instant start = Instant.now();

        logger.info("Starting DRL generation for requirement using model '{}'", modelName);

        // Generate DRL
        DRLGenerationAgent agent = agentFactory.apply(model);
        Instant genStart = Instant.now();
        String generatedDrl;
        try {
            generatedDrl = agent.generateDRL(requirement, factTypesDescription, "");
            generatedDrl = StringCleanupUtils.cleanupDrl(generatedDrl);
            logger.debug("Generated DRL:\n{}", generatedDrl);
        } catch (Exception e) {
            logger.error("DRL generation failed: {}", e.getMessage());
            return GenerationResult.failed(modelName, "Generation failed: " + e.getMessage(),
                    Duration.between(start, Instant.now()));
        }
        Duration genTime = Duration.between(genStart, Instant.now());
        logger.info("DRL generation completed in {}ms", genTime.toMillis());

        // Validate DRL
        String validationResult;
        boolean validationPassed;
        try {
            validationResult = validationService.validateDRLStructure(generatedDrl);
            validationPassed = !validationResult.contains("ERROR:");
            logger.info("Validation result: {} (passed: {})", validationResult, validationPassed);
        } catch (Exception e) {
            logger.error("Validation failed with exception: {}", e.getMessage());
            return GenerationResult.partial(modelName, generatedDrl, false,
                    "Validation exception: " + e.getMessage(), genTime, Duration.between(start, Instant.now()));
        }

        if (!validationPassed) {
            logger.warn("DRL validation failed: {}", validationResult);
            return GenerationResult.partial(modelName, generatedDrl, false,
                    validationResult, genTime, Duration.between(start, Instant.now()));
        }

        // Return success (validation only, no execution)
        return new GenerationResult(
                modelName,
                generatedDrl,
                true,
                validationResult,
                true,
                "Validation passed (no test execution)",
                0,
                java.util.List.of(),
                genTime,
                Duration.between(start, Instant.now())
        );
    }

    /**
     * Generates DRL from a scenario using the specified model and validates execution.
     */
    public GenerationResult generateAndTest(ChatModel model, TestScenario scenario) {
        String modelName = ModelConfiguration.extractModelName(model);
        Instant start = Instant.now();

        logger.info("Starting DRL generation for scenario '{}' using model '{}'", scenario.name(), modelName);
        logger.debug("--- Scenario Details ---");
        logger.debug("Description: {}", scenario.description());
        logger.debug("Requirement:\n{}", scenario.requirement());
        logger.debug("Fact Types:\n{}", scenario.getFactTypesDescription());
        logger.debug("Test Cases: {}", scenario.testCases().size());
        for (TestScenario.TestCase tc : scenario.testCases()) {
            logger.debug("  - {}: input={}", tc.name(), tc.inputJson());
        }

        // Generate DRL
        DRLGenerationAgent agent = agentFactory.apply(model);
        Instant genStart = Instant.now();
        String generatedDrl;
        try {
            generatedDrl = agent.generateDRL(
                    scenario.requirement(),
                    scenario.getFactTypesDescription(),
                    scenario.getTestScenarioDescription()
            );
            // Clean up DRL if it contains markdown code blocks
            generatedDrl = StringCleanupUtils.cleanupDrl(generatedDrl);
            logger.debug("Generated DRL:\n{}", generatedDrl);
        } catch (Exception e) {
            logger.error("DRL generation failed: {}", e.getMessage());
            return GenerationResult.failed(modelName, "Generation failed: " + e.getMessage(),
                    Duration.between(start, Instant.now()));
        }
        Duration genTime = Duration.between(genStart, Instant.now());
        logger.info("DRL generation completed in {}ms", genTime.toMillis());

        // Validate DRL
        String validationResult;
        boolean validationPassed;
        try {
            validationResult = validationService.validateDRLStructure(generatedDrl);
            validationPassed = !validationResult.contains("ERROR:");
            logger.info("Validation result: {} (passed: {})", validationResult, validationPassed);
        } catch (Exception e) {
            logger.error("Validation failed with exception: {}", e.getMessage());
            return GenerationResult.partial(modelName, generatedDrl, false,
                    "Validation exception: " + e.getMessage(), genTime, Duration.between(start, Instant.now()));
        }

        if (!validationPassed) {
            logger.warn("DRL validation failed: {}", validationResult);
            return GenerationResult.partial(modelName, generatedDrl, false,
                    validationResult, genTime, Duration.between(start, Instant.now()));
        }

        // Execute DRL with all test cases
        int totalRulesFired = 0;
        List<Object> allResultingFacts = new ArrayList<>();
        int testCasesPassed = 0;

        for (TestScenario.TestCase testCase : scenario.testCases()) {
            logger.info("Executing test case '{}' with input: {}", testCase.name(), testCase.inputJson());
            try {
                DRLRunnerResult result = DRLPopulatorRunner.runDRLWithJsonFacts(
                        generatedDrl, testCase.inputJson(), 100);

                logger.info("Test case '{}': {} rules fired, {} facts in working memory",
                        testCase.name(), result.firedRules(), result.objects().size());

                // Verify rules fired based on expectation
                if (testCase.expectRulesToFire() && result.firedRules() == 0) {
                    String error = "No rules fired - the generated rules did not match the input facts";
                    logger.error("Test case '{}' failed: {}", testCase.name(), error);
                    return GenerationResult.partial(modelName, generatedDrl, true,
                            "Test case '" + testCase.name() + "' failed: " + error,
                            genTime, Duration.between(start, Instant.now()));
                }
                if (!testCase.expectRulesToFire() && result.firedRules() > 0) {
                    String error = "Expected no rules to fire, but " + result.firedRules() + " fired";
                    logger.error("Test case '{}' failed: {}", testCase.name(), error);
                    return GenerationResult.partial(modelName, generatedDrl, true,
                            "Test case '" + testCase.name() + "' failed: " + error,
                            genTime, Duration.between(start, Instant.now()));
                }

                // Verify expected values (support both legacy and new formats)
                if (testCase.hasTypedExpectations()) {
                    // New: Type-aware verification
                    String factError = FactVerificationUtils.verifyExpectedFacts(result.objects(), testCase.expectedFacts());
                    if (factError != null) {
                        logger.error("Test case '{}' failed: {}", testCase.name(), factError);
                        return GenerationResult.partial(modelName, generatedDrl, true,
                                "Test case '" + testCase.name() + "' failed: " + factError,
                                genTime, Duration.between(start, Instant.now()));
                    }
                } else if (testCase.expectedFieldValues() != null && !testCase.expectedFieldValues().isEmpty()) {
                    // Legacy: Field-based verification (backward compatibility)
                    String fieldError = FactVerificationUtils.verifyExpectedFields(result.objects(), testCase.expectedFieldValues());
                    if (fieldError != null) {
                        logger.error("Test case '{}' failed: {}", testCase.name(), fieldError);
                        return GenerationResult.partial(modelName, generatedDrl, true,
                                "Test case '" + testCase.name() + "' failed: " + fieldError,
                                genTime, Duration.between(start, Instant.now()));
                    }
                }

                totalRulesFired += result.firedRules();
                allResultingFacts.addAll(result.objects());
                testCasesPassed++;
            } catch (Throwable e) {
                logger.error("Test case '{}' failed: {}", testCase.name(), e.getMessage());
                return GenerationResult.partial(modelName, generatedDrl, true,
                        "Test case '" + testCase.name() + "' failed: " + e.getMessage(),
                        genTime, Duration.between(start, Instant.now()));
            }
        }

        if (testCasesPassed == 0) {
            return GenerationResult.failed(modelName, "No test cases to execute", Duration.between(start, Instant.now()));
        }

        logger.info("All {} test cases passed. Total rules fired: {}", testCasesPassed, totalRulesFired);

        return new GenerationResult(
                modelName,
                generatedDrl,
                true,
                validationResult,
                true,
                "All " + testCasesPassed + " test cases passed",
                totalRulesFired,
                allResultingFacts,
                genTime,
                Duration.between(start, Instant.now())
        );
    }
}
