package com.github.rikkola.drlgen.generation.runner;

import dev.langchain4j.model.chat.ChatModel;
import com.github.rikkola.drlgen.generation.config.ModelConfiguration;
import com.github.rikkola.drlgen.generation.model.GenerationResult;
import com.github.rikkola.drlgen.generation.model.TestScenario;
import com.github.rikkola.drlgen.generation.provider.TestScenarioProvider;
import com.github.rikkola.drlgen.generation.service.DRLGenerationService;
import com.github.rikkola.drlgen.generation.model.PlainEnglishGenerationResult;
import com.github.rikkola.drlgen.generation.model.YAMLGenerationResult;
import com.github.rikkola.drlgen.generation.service.PlainEnglishGenerationService;
import com.github.rikkola.drlgen.generation.service.YAMLRuleGenerationService;

import java.time.Duration;
import java.util.*;

/**
 * Main runner for comparing DRL, YAML, and Plain English rule generation across models.
 *
 * Usage:
 *   java ComparisonRunner [options]
 *
 * Options:
 *   --models <model1,model2,...>   Comma-separated list of models (default: all from models.yaml)
 *   --scenarios <name1,name2,...>  Filter scenarios by name substring
 *   --output <filename.csv>        CSV output file (default: comparison-results.csv)
 *   --drl-only                     Only test DRL generation
 *   --yaml-only                    Only test YAML generation
 *   --english-only                 Only test Plain English pipeline (2-stage)
 *   --formats <DRL,YAML,ENGLISH>   Comma-separated list of formats to test
 */
public class ComparisonRunner {

    private final DRLGenerationService drlService;
    private final YAMLRuleGenerationService yamlService;
    private final PlainEnglishGenerationService englishService;

    public ComparisonRunner() {
        this.drlService = new DRLGenerationService();
        this.yamlService = new YAMLRuleGenerationService();
        this.englishService = new PlainEnglishGenerationService();
    }

    public static void main(String[] args) {
        ComparisonRunner runner = new ComparisonRunner();
        runner.run(args);
    }

    public void run(String[] args) {
        // Parse arguments
        List<String> models = parseModels(args);
        List<TestScenario> scenarios = parseScenarios(args);
        String outputFile = parseOutput(args);
        Set<String> formats = parseFormats(args);

        System.out.println("Starting Drools Rule Generation Comparison");
        System.out.println("Models: " + models.size());
        System.out.println("Scenarios: " + scenarios.size());
        System.out.println("Formats: " + String.join(", ", formats));
        System.out.println();

        ComparisonReport report = new ComparisonReport();

        int totalTests = models.size() * scenarios.size() * formats.size();
        int currentTest = 0;

        for (String modelName : models) {
            System.out.println("\n=== Testing model: " + modelName + " ===");

            ChatModel chatModel;
            try {
                chatModel = ModelConfiguration.createModel(modelName);
            } catch (Exception e) {
                System.err.println("Failed to load model " + modelName + ": " + e.getMessage());
                // Add failure results for all scenarios
                for (TestScenario scenario : scenarios) {
                    for (String format : formats) {
                        report.addResult(ComparisonResult.failure(
                                modelName, scenario.name(), format,
                                Duration.ZERO, "Model load failed: " + e.getMessage()));
                    }
                }
                continue;
            }

            for (TestScenario scenario : scenarios) {
                // Test DRL generation
                if (formats.contains("DRL")) {
                    currentTest++;
                    System.out.printf("[%d/%d] %s - %s (DRL)... ",
                            currentTest, totalTests, modelName, scenario.name());

                    ComparisonResult drlResult = testDRL(chatModel, modelName, scenario);
                    report.addResult(drlResult);
                    System.out.println(drlResult.getStatusString() +
                            (drlResult.success() ? " (" + drlResult.rulesFired() + " rules)" : ""));
                }

                // Test YAML generation
                if (formats.contains("YAML")) {
                    currentTest++;
                    System.out.printf("[%d/%d] %s - %s (YAML)... ",
                            currentTest, totalTests, modelName, scenario.name());

                    ComparisonResult yamlResult = testYAML(chatModel, modelName, scenario);
                    report.addResult(yamlResult);
                    System.out.println(yamlResult.getStatusString() +
                            (yamlResult.success() ? " (" + yamlResult.rulesFired() + " rules)" : ""));
                }

                // Test Plain English pipeline
                if (formats.contains("ENGLISH")) {
                    currentTest++;
                    System.out.printf("[%d/%d] %s - %s (ENGLISH)... ",
                            currentTest, totalTests, modelName, scenario.name());

                    ComparisonResult englishResult = testEnglish(chatModel, modelName, scenario);
                    report.addResult(englishResult);
                    System.out.println(englishResult.getStatusString() +
                            (englishResult.success() ? " (" + englishResult.rulesFired() + " rules)" : ""));
                }
            }
        }

        // Generate reports
        report.printConsoleReport();
        report.writeCsvReport(outputFile);
    }

    private ComparisonResult testDRL(ChatModel model, String modelName, TestScenario scenario) {
        try {
            GenerationResult result = drlService.generateAndTest(model, scenario);
            if (result.isSuccessful()) {
                return ComparisonResult.success(modelName, scenario.name(), "DRL",
                        result.rulesFired(), result.generationTime());
            } else {
                return ComparisonResult.failure(modelName, scenario.name(), "DRL",
                        result.generationTime(), result.validationMessage());
            }
        } catch (Exception e) {
            return ComparisonResult.failure(modelName, scenario.name(), "DRL",
                    Duration.ZERO, "Exception: " + e.getMessage());
        }
    }

    private ComparisonResult testYAML(ChatModel model, String modelName, TestScenario scenario) {
        try {
            YAMLGenerationResult result = yamlService.generateAndTest(model, scenario);
            if (result.isSuccessful()) {
                return ComparisonResult.success(modelName, scenario.name(), "YAML",
                        result.rulesFired(), result.generationTime());
            } else {
                String errorMsg = !result.yamlValidationPassed() ? result.yamlValidationMessage() :
                        !result.conversionPassed() ? result.conversionMessage() :
                                !result.drlValidationPassed() ? result.drlValidationMessage() :
                                        result.executionMessage();
                return ComparisonResult.failure(modelName, scenario.name(), "YAML",
                        result.generationTime(), errorMsg);
            }
        } catch (Exception e) {
            return ComparisonResult.failure(modelName, scenario.name(), "YAML",
                    Duration.ZERO, "Exception: " + e.getMessage());
        }
    }

    private ComparisonResult testEnglish(ChatModel model, String modelName, TestScenario scenario) {
        try {
            PlainEnglishGenerationResult result = englishService.generateAndTest(model, scenario);
            if (result.isSuccessful()) {
                return ComparisonResult.success(modelName, scenario.name(), "ENGLISH",
                        result.rulesFired(), result.getGenerationTime());
            } else {
                String errorMsg = !result.stage1EnglishGenerationPassed() ? result.stage1EnglishGenerationMessage() :
                        !result.stage2DrlConversionPassed() ? result.stage2DrlConversionMessage() :
                                !result.drlValidationPassed() ? result.drlValidationMessage() :
                                        result.executionMessage();
                return ComparisonResult.failure(modelName, scenario.name(), "ENGLISH",
                        result.getGenerationTime(), errorMsg);
            }
        } catch (Exception e) {
            return ComparisonResult.failure(modelName, scenario.name(), "ENGLISH",
                    Duration.ZERO, "Exception: " + e.getMessage());
        }
    }

    private List<String> parseModels(String[] args) {
        String modelsArg = getArgValue(args, "--models");
        List<String> availableModels = ModelConfiguration.getAvailableModels();

        if (modelsArg == null) {
            // Default: all models from configuration
            return availableModels;
        }

        List<String> models = new ArrayList<>();
        for (String modelName : modelsArg.split(",")) {
            modelName = modelName.trim();
            // Check if exact match exists
            if (availableModels.contains(modelName)) {
                models.add(modelName);
            } else {
                // Try partial match
                for (String available : availableModels) {
                    if (available.contains(modelName) || modelName.contains(available)) {
                        models.add(available);
                        break;
                    }
                }
            }
        }
        return models.isEmpty() ? availableModels : models;
    }

    private List<TestScenario> parseScenarios(String[] args) {
        String scenariosArg = getArgValue(args, "--scenarios");
        List<TestScenario> allScenarios = TestScenarioProvider.getAllScenarios();

        if (scenariosArg == null) {
            return allScenarios;
        }

        List<String> filters = Arrays.asList(scenariosArg.toLowerCase().split(","));
        List<TestScenario> filtered = new ArrayList<>();
        for (TestScenario scenario : allScenarios) {
            for (String filter : filters) {
                if (scenario.name().toLowerCase().contains(filter.trim())) {
                    filtered.add(scenario);
                    break;
                }
            }
        }
        return filtered.isEmpty() ? allScenarios : filtered;
    }

    private String parseOutput(String[] args) {
        String output = getArgValue(args, "--output");
        return output != null ? output : "comparison-results.csv";
    }

    private Set<String> parseFormats(String[] args) {
        // Check for format flags
        boolean drlOnly = hasFlag(args, "--drl-only");
        boolean yamlOnly = hasFlag(args, "--yaml-only");
        boolean englishOnly = hasFlag(args, "--english-only");

        // Check for explicit formats argument
        String formatsArg = getArgValue(args, "--formats");

        if (formatsArg != null) {
            Set<String> formats = new LinkedHashSet<>();
            for (String format : formatsArg.toUpperCase().split(",")) {
                format = format.trim();
                if (format.equals("DRL") || format.equals("YAML") || format.equals("ENGLISH")) {
                    formats.add(format);
                }
            }
            return formats.isEmpty() ? new LinkedHashSet<>(Arrays.asList("DRL", "YAML")) : formats;
        }

        // Handle legacy flags
        if (drlOnly) {
            return new LinkedHashSet<>(Collections.singletonList("DRL"));
        } else if (yamlOnly) {
            return new LinkedHashSet<>(Collections.singletonList("YAML"));
        } else if (englishOnly) {
            return new LinkedHashSet<>(Collections.singletonList("ENGLISH"));
        }

        // Default: DRL and YAML (for backwards compatibility)
        return new LinkedHashSet<>(Arrays.asList("DRL", "YAML"));
    }

    private String getArgValue(String[] args, String argName) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals(argName)) {
                return args[i + 1];
            }
        }
        return null;
    }

    private boolean hasFlag(String[] args, String flag) {
        for (String arg : args) {
            if (arg.equals(flag)) {
                return true;
            }
        }
        return false;
    }
}
