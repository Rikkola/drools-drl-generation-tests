package com.github.rikkola.drlgen.generation.service;

import org.yaml.snakeyaml.Yaml;

import java.util.List;
import java.util.Map;

/**
 * Converts Drools YAML rules to DRL format for execution.
 */
public class YAMLToDRLConverter {

    /**
     * Converts YAML rules to DRL format.
     *
     * @param yamlContent The YAML rules content
     * @return The equivalent DRL code
     * @throws YAMLConversionException if conversion fails
     */
    public String convertToDRL(String yamlContent) throws YAMLConversionException {
        try {
            Yaml yaml = new Yaml();
            Map<String, Object> root = yaml.load(yamlContent);

            if (root == null) {
                throw new YAMLConversionException("Empty or invalid YAML content");
            }

            StringBuilder drl = new StringBuilder();
            drl.append("package org.drools.generated;\n\n");

            // Process types section
            if (root.containsKey("types")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> types = (List<Map<String, Object>>) root.get("types");
                for (Map<String, Object> type : types) {
                    drl.append(convertType(type));
                    drl.append("\n");
                }
            }

            // Process rules section
            if (root.containsKey("rules")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> rules = (List<Map<String, Object>>) root.get("rules");
                for (Map<String, Object> rule : rules) {
                    drl.append(convertRule(rule));
                    drl.append("\n");
                }
            } else {
                throw new YAMLConversionException("No 'rules' section found in YAML");
            }

            return drl.toString();
        } catch (YAMLConversionException e) {
            throw e;
        } catch (Exception e) {
            throw new YAMLConversionException("Failed to parse YAML: " + e.getMessage(), e);
        }
    }

    /**
     * Validates YAML structure without converting.
     *
     * @param yamlContent The YAML content to validate
     * @return Validation result message
     */
    public ValidationResult validateYAML(String yamlContent) {
        try {
            Yaml yaml = new Yaml();
            Map<String, Object> root = yaml.load(yamlContent);

            if (root == null) {
                return new ValidationResult(false, "Empty or invalid YAML content");
            }

            // Check for rules section
            if (!root.containsKey("rules")) {
                return new ValidationResult(false, "Missing 'rules' section");
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rules = (List<Map<String, Object>>) root.get("rules");

            if (rules == null || rules.isEmpty()) {
                return new ValidationResult(false, "No rules defined in 'rules' section");
            }

            // Validate each rule
            for (int i = 0; i < rules.size(); i++) {
                Map<String, Object> rule = rules.get(i);
                String ruleValidation = validateRule(rule, i);
                if (ruleValidation != null) {
                    return new ValidationResult(false, ruleValidation);
                }
            }

            return new ValidationResult(true, "YAML structure is valid");
        } catch (Exception e) {
            return new ValidationResult(false, "YAML parsing error: " + e.getMessage());
        }
    }

    private String validateRule(Map<String, Object> rule, int index) {
        if (!rule.containsKey("name")) {
            return "Rule " + index + " is missing 'name'";
        }
        if (!rule.containsKey("condition")) {
            return "Rule '" + rule.get("name") + "' is missing 'condition'";
        }
        if (!rule.containsKey("action")) {
            return "Rule '" + rule.get("name") + "' is missing 'action'";
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> condition = (Map<String, Object>) rule.get("condition");
        if (!condition.containsKey("given")) {
            return "Rule '" + rule.get("name") + "' condition is missing 'given' (fact type)";
        }

        return null; // Valid
    }

    private String convertType(Map<String, Object> type) {
        StringBuilder sb = new StringBuilder();
        String typeName = (String) type.get("name");
        sb.append("declare ").append(typeName).append("\n");

        @SuppressWarnings("unchecked")
        Map<String, String> fields = (Map<String, String>) type.get("fields");
        if (fields != null) {
            for (Map.Entry<String, String> field : fields.entrySet()) {
                sb.append("    ").append(field.getKey()).append(" : ").append(field.getValue()).append("\n");
            }
        }

        sb.append("end\n");
        return sb.toString();
    }

    private String convertRule(Map<String, Object> rule) {
        StringBuilder sb = new StringBuilder();
        String ruleName = (String) rule.get("name");
        sb.append("rule \"").append(ruleName).append("\"\n");

        // Convert condition (when clause)
        sb.append("when\n");
        @SuppressWarnings("unchecked")
        Map<String, Object> condition = (Map<String, Object>) rule.get("condition");
        sb.append(convertCondition(condition));

        // Convert action (then clause)
        sb.append("then\n");
        @SuppressWarnings("unchecked")
        Map<String, Object> action = (Map<String, Object>) rule.get("action");
        sb.append(convertAction(action));

        sb.append("end\n");
        return sb.toString();
    }

    private String convertCondition(Map<String, Object> condition) {
        StringBuilder sb = new StringBuilder();

        String factType = (String) condition.get("given");
        String variable = (String) condition.getOrDefault("as", "$fact");

        // Build constraints from 'having' list
        StringBuilder constraints = new StringBuilder();
        if (condition.containsKey("having")) {
            @SuppressWarnings("unchecked")
            List<String> havingList = (List<String>) condition.get("having");
            if (havingList != null && !havingList.isEmpty()) {
                // Clean up constraints - remove variable prefix if present
                List<String> cleanedConstraints = havingList.stream()
                        .map(c -> stripVariablePrefix(c, variable))
                        .toList();
                constraints.append(String.join(", ", cleanedConstraints));
            }
        }

        sb.append("    ").append(variable).append(" : ").append(factType);
        if (constraints.length() > 0) {
            sb.append("(").append(constraints).append(")");
        } else {
            sb.append("()");
        }
        sb.append("\n");

        return sb.toString();
    }

    /**
     * Removes variable prefix from constraint if present.
     * E.g., "$person.age >= 18" becomes "age >= 18"
     */
    private String stripVariablePrefix(String constraint, String variable) {
        // Pattern: $variableName.fieldName -> fieldName
        String prefix = variable + ".";
        if (constraint.startsWith(prefix)) {
            return constraint.substring(prefix.length());
        }
        // Also handle cases where the constraint contains the variable reference in the middle
        return constraint.replace(prefix, "");
    }

    private String convertAction(Map<String, Object> action) {
        StringBuilder sb = new StringBuilder();

        if (action.containsKey("modify")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> modify = (Map<String, Object>) action.get("modify");
            String target = (String) modify.get("target");

            @SuppressWarnings("unchecked")
            Map<String, Object> setFields = (Map<String, Object>) modify.get("set");

            if (setFields != null && !setFields.isEmpty()) {
                // Use separate modify calls for each field (safer approach)
                for (Map.Entry<String, Object> field : setFields.entrySet()) {
                    String setterName = "set" + capitalize(field.getKey());
                    Object value = field.getValue();
                    String valueStr = formatValue(value);
                    sb.append("    modify(").append(target).append(") { ")
                      .append(setterName).append("(").append(valueStr).append(") }\n");
                }
            }
        }

        return sb.toString();
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private String formatValue(Object value) {
        if (value instanceof String) {
            return "\"" + value + "\"";
        } else if (value instanceof Boolean) {
            return value.toString();
        } else {
            return String.valueOf(value);
        }
    }

    /**
     * Result of YAML validation.
     */
    public record ValidationResult(boolean valid, String message) {}

    /**
     * Exception thrown when YAML conversion fails.
     */
    public static class YAMLConversionException extends Exception {
        public YAMLConversionException(String message) {
            super(message);
        }

        public YAMLConversionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
