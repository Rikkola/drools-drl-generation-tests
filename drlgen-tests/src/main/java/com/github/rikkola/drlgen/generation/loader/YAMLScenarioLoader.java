package com.github.rikkola.drlgen.generation.loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rikkola.drlgen.generation.model.TestScenario;
import com.github.rikkola.drlgen.generation.model.TestScenario.ExpectedFact;
import com.github.rikkola.drlgen.generation.model.TestScenario.FactTypeDefinition;
import com.github.rikkola.drlgen.generation.model.TestScenario.TestCase;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Loads TestScenario objects from YAML files.
 */
public class YAMLScenarioLoader {

    private static final String DEFAULT_SCENARIOS_DIR = "scenarios";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Yaml yaml = new Yaml();

    /**
     * Load a single scenario from a YAML file.
     *
     * @param filename the filename (e.g., "adult-validation.yaml")
     * @return the parsed TestScenario
     */
    public TestScenario loadScenario(String filename) {
        String path = DEFAULT_SCENARIOS_DIR + "/" + filename;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalArgumentException("Scenario file not found: " + path);
            }
            Map<String, Object> data = yaml.load(is);
            return parseScenario(data);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load scenario: " + filename, e);
        }
    }

    /**
     * Load all scenarios from the default scenarios directory.
     *
     * @return list of all scenarios
     */
    public List<TestScenario> loadAllScenarios() {
        return loadAllScenarios(DEFAULT_SCENARIOS_DIR);
    }

    /**
     * Load all scenarios from a specified directory.
     *
     * @param directory the directory path relative to classpath
     * @return list of all scenarios
     */
    public List<TestScenario> loadAllScenarios(String directory) {
        List<TestScenario> scenarios = new ArrayList<>();
        try {
            URL dirUrl = getClass().getClassLoader().getResource(directory);
            if (dirUrl == null) {
                return scenarios;
            }

            URI uri = dirUrl.toURI();

            if (uri.getScheme().equals("jar")) {
                try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
                    Path dirPath = fs.getPath(directory);
                    loadScenariosFromPath(dirPath, scenarios);
                }
            } else {
                Path dirPath = Paths.get(uri);
                loadScenariosFromPath(dirPath, scenarios);
            }
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("Failed to load scenarios from directory: " + directory, e);
        }
        return scenarios;
    }

    private void loadScenariosFromPath(Path dirPath, List<TestScenario> scenarios) throws IOException {
        try (Stream<Path> paths = Files.list(dirPath)) {
            paths.filter(p -> p.toString().endsWith(".yaml") || p.toString().endsWith(".yml"))
                    .sorted()
                    .forEach(path -> {
                        String filename = path.getFileName().toString();
                        scenarios.add(loadScenario(filename));
                    });
        }
    }

    @SuppressWarnings("unchecked")
    private TestScenario parseScenario(Map<String, Object> data) {
        String name = (String) data.get("name");
        String description = (String) data.get("description");
        String requirement = (String) data.get("requirement");

        List<FactTypeDefinition> factTypes = parseFactTypes((List<Map<String, Object>>) data.get("factTypes"));
        List<TestCase> testCases = parseTestCases((List<Map<String, Object>>) data.get("testCases"));

        return new TestScenario(name, description, requirement, factTypes, testCases);
    }

    @SuppressWarnings("unchecked")
    private List<FactTypeDefinition> parseFactTypes(List<Map<String, Object>> factTypesData) {
        if (factTypesData == null) {
            return List.of();
        }
        List<FactTypeDefinition> result = new ArrayList<>();
        for (Map<String, Object> ft : factTypesData) {
            String typeName = (String) ft.get("name");
            Map<String, String> fields = new LinkedHashMap<>();
            Map<String, Object> fieldsData = (Map<String, Object>) ft.get("fields");
            if (fieldsData != null) {
                fieldsData.forEach((k, v) -> fields.put(k, String.valueOf(v)));
            }
            result.add(new FactTypeDefinition(typeName, fields));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<TestCase> parseTestCases(List<Map<String, Object>> testCasesData) {
        if (testCasesData == null) {
            return List.of();
        }
        List<TestCase> result = new ArrayList<>();
        for (Map<String, Object> tc : testCasesData) {
            String name = (String) tc.get("name");
            Object input = tc.get("input");
            String inputJson = convertToJson(input);

            // Parse legacy expectedFields (backward compatibility)
            Map<String, Object> expectedFields = (Map<String, Object>) tc.get("expectedFields");
            if (expectedFields == null) {
                expectedFields = Map.of();
            }

            // Parse new typed expectedFacts
            List<ExpectedFact> expectedFacts = parseExpectedFacts(
                    (List<Map<String, Object>>) tc.get("expectedFacts"));

            // Parse expectedRulesFired (null = don't check, 0 = no rules, N = exactly N rules)
            Integer expectedRulesFired = null;
            Object expectedRulesFiredObj = tc.get("expectedRulesFired");
            if (expectedRulesFiredObj != null) {
                expectedRulesFired = ((Number) expectedRulesFiredObj).intValue();
            }

            result.add(new TestCase(name, inputJson, expectedFields, expectedFacts, expectedRulesFired));
        }
        return result;
    }

    /**
     * Parses the expectedFacts list from YAML.
     */
    @SuppressWarnings("unchecked")
    private List<ExpectedFact> parseExpectedFacts(List<Map<String, Object>> expectedFactsData) {
        if (expectedFactsData == null) {
            return List.of();
        }

        List<ExpectedFact> result = new ArrayList<>();
        for (Map<String, Object> ef : expectedFactsData) {
            String type = (String) ef.get("type");
            Map<String, Object> fields = (Map<String, Object>) ef.get("fields");
            if (fields == null) {
                fields = Map.of();
            }
            result.add(new ExpectedFact(type, fields));
        }
        return result;
    }

    private String convertToJson(Object input) {
        try {
            return objectMapper.writeValueAsString(input);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert input to JSON", e);
        }
    }
}
