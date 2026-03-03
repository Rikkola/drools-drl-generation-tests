package com.github.rikkola.drlgen.generation.service;

import dev.langchain4j.model.chat.ChatModel;
import com.github.rikkola.drlgen.execution.DRLPopulatorRunner;
import com.github.rikkola.drlgen.execution.DRLRunnerResult;
import com.github.rikkola.drlgen.generation.agent.YAMLRuleGenerationAgent;
import com.github.rikkola.drlgen.generation.config.ModelConfiguration;
import com.github.rikkola.drlgen.generation.model.TestScenario;
import com.github.rikkola.drlgen.generation.model.YAMLGenerationResult;
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
 * Orchestrates YAML rule generation, validation, conversion to DRL, and execution.
 */
public class YAMLRuleGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(YAMLRuleGenerationService.class);

    private final YAMLToDRLConverter yamlConverter;
    private final DRLValidationService drlValidationService;
    private final Function<ChatModel, YAMLRuleGenerationAgent> agentFactory;

    /**
     * Default constructor using real LLM agent.
     */
    public YAMLRuleGenerationService() {
        this(new YAMLToDRLConverter(), new DRLValidationService(), YAMLRuleGenerationAgent::create);
    }

    /**
     * Constructor with custom agent factory for testing.
     * Allows injection of mock agents.
     */
    public YAMLRuleGenerationService(YAMLToDRLConverter yamlConverter,
                                      DRLValidationService drlValidationService,
                                      Function<ChatModel, YAMLRuleGenerationAgent> agentFactory) {
        this.yamlConverter = yamlConverter;
        this.drlValidationService = drlValidationService;
        this.agentFactory = agentFactory;
    }

    /**
     * Generates YAML rules from a scenario, converts to DRL, and tests execution.
     */
    public YAMLGenerationResult generateAndTest(ChatModel model, TestScenario scenario) {
        String modelName = ModelConfiguration.extractModelName(model);
        Instant start = Instant.now();

        logger.info("Starting YAML rule generation for scenario '{}' using model '{}'", scenario.name(), modelName);
        logger.debug("--- Scenario Details ---");
        logger.debug("Description: {}", scenario.description());
        logger.debug("Requirement:\n{}", scenario.requirement());
        logger.debug("Fact Types:\n{}", scenario.getFactTypesDescription());
        logger.debug("Test Cases: {}", scenario.testCases().size());
        for (TestScenario.TestCase tc : scenario.testCases()) {
            logger.debug("  - {}: input={}", tc.name(), tc.inputJson());
        }

        // Generate YAML
        YAMLRuleGenerationAgent agent = agentFactory.apply(model);
        Instant genStart = Instant.now();
        String generatedYaml;
        try {
            generatedYaml = agent.generateYAMLRules(
                    scenario.requirement(),
                    scenario.getFactTypesDescription(),
                    scenario.getTestScenarioDescription()
            );
            generatedYaml = StringCleanupUtils.cleanupYaml(generatedYaml);
            logger.debug("Generated YAML:\n{}", generatedYaml);
        } catch (Exception e) {
            logger.error("YAML generation failed: {}", e.getMessage());
            return YAMLGenerationResult.failed(modelName, "Generation failed: " + e.getMessage(),
                    Duration.between(start, Instant.now()));
        }
        Duration genTime = Duration.between(genStart, Instant.now());
        logger.info("YAML generation completed in {}ms", genTime.toMillis());

        // Validate YAML structure
        YAMLToDRLConverter.ValidationResult yamlValidation = yamlConverter.validateYAML(generatedYaml);
        if (!yamlValidation.valid()) {
            logger.warn("YAML validation failed: {}", yamlValidation.message());
            return YAMLGenerationResult.yamlInvalid(modelName, generatedYaml, yamlValidation.message(),
                    genTime, Duration.between(start, Instant.now()));
        }
        logger.info("YAML validation passed");

        // Convert YAML to DRL
        String convertedDrl;
        try {
            convertedDrl = yamlConverter.convertToDRL(generatedYaml);
            logger.debug("Converted DRL:\n{}", convertedDrl);
        } catch (YAMLToDRLConverter.YAMLConversionException e) {
            logger.error("YAML to DRL conversion failed: {}", e.getMessage());
            return YAMLGenerationResult.conversionFailed(modelName, generatedYaml, e.getMessage(),
                    genTime, Duration.between(start, Instant.now()));
        }

        // Validate converted DRL
        String drlValidationResult;
        boolean drlValidationPassed;
        try {
            drlValidationResult = drlValidationService.validateDRLStructure(convertedDrl);
            drlValidationPassed = !drlValidationResult.contains("ERROR:");
            logger.info("DRL validation result: {} (passed: {})", drlValidationResult, drlValidationPassed);
        } catch (Exception e) {
            logger.error("DRL validation failed with exception: {}", e.getMessage());
            return YAMLGenerationResult.drlValidationFailed(modelName, generatedYaml, convertedDrl,
                    "DRL validation exception: " + e.getMessage(), genTime, Duration.between(start, Instant.now()));
        }

        if (!drlValidationPassed) {
            logger.warn("Converted DRL validation failed: {}", drlValidationResult);
            return YAMLGenerationResult.drlValidationFailed(modelName, generatedYaml, convertedDrl,
                    drlValidationResult, genTime, Duration.between(start, Instant.now()));
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
                    return YAMLGenerationResult.executionFailed(modelName, generatedYaml, convertedDrl,
                            "Test case '" + testCase.name() + "' failed: " + rulesFiredError,
                            genTime, Duration.between(start, Instant.now()));
                }

                // Verify expected values (support both legacy and new formats)
                if (testCase.hasTypedExpectations()) {
                    // New: Type-aware verification
                    String factError = FactVerificationUtils.verifyExpectedFacts(result.objects(), testCase.expectedFacts());
                    if (factError != null) {
                        logger.error("Test case '{}' failed: {}", testCase.name(), factError);
                        return YAMLGenerationResult.executionFailed(modelName, generatedYaml, convertedDrl,
                                "Test case '" + testCase.name() + "' failed: " + factError,
                                genTime, Duration.between(start, Instant.now()));
                    }
                } else if (testCase.expectedFieldValues() != null && !testCase.expectedFieldValues().isEmpty()) {
                    // Legacy: Field-based verification (backward compatibility)
                    String fieldError = FactVerificationUtils.verifyExpectedFields(result.objects(), testCase.expectedFieldValues());
                    if (fieldError != null) {
                        logger.error("Test case '{}' failed: {}", testCase.name(), fieldError);
                        return YAMLGenerationResult.executionFailed(modelName, generatedYaml, convertedDrl,
                                "Test case '" + testCase.name() + "' failed: " + fieldError,
                                genTime, Duration.between(start, Instant.now()));
                    }
                }

                totalRulesFired += result.firedRules();
                allResultingFacts.addAll(result.objects());
                testCasesPassed++;
            } catch (Throwable e) {
                logger.error("Test case '{}' failed: {}", testCase.name(), e.getMessage());
                return YAMLGenerationResult.executionFailed(modelName, generatedYaml, convertedDrl,
                        "Test case '" + testCase.name() + "' failed: " + e.getMessage(),
                        genTime, Duration.between(start, Instant.now()));
            }
        }

        if (testCasesPassed == 0) {
            return YAMLGenerationResult.failed(modelName, "No test cases to execute", Duration.between(start, Instant.now()));
        }

        logger.info("All {} test cases passed. Total rules fired: {}", testCasesPassed, totalRulesFired);

        return new YAMLGenerationResult(
                modelName,
                generatedYaml,
                convertedDrl,
                true,
                "YAML structure valid",
                true,
                "Conversion successful",
                true,
                drlValidationResult,
                true,
                "All " + testCasesPassed + " test cases passed",
                totalRulesFired,
                allResultingFacts,
                genTime,
                Duration.between(start, Instant.now())
        );
    }
}
