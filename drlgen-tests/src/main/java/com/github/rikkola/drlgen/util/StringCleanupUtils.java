package com.github.rikkola.drlgen.util;

/**
 * Utility class for cleaning up generated content by removing markdown artifacts.
 */
public final class StringCleanupUtils {

    private StringCleanupUtils() {
        // Utility class
    }

    /**
     * Cleans up generated DRL by removing markdown code blocks.
     */
    public static String cleanupDrl(String drl) {
        if (drl == null) return null;
        return drl.replaceAll("```(?:drl|drools)?\\n?", "")
                  .replaceAll("```\\n?", "")
                  .trim();
    }

    /**
     * Cleans up generated YAML by removing markdown code blocks.
     */
    public static String cleanupYaml(String yaml) {
        if (yaml == null) return null;
        return yaml.replaceAll("```(?:yaml|yml)?\\n?", "")
                   .replaceAll("```\\n?", "")
                   .trim();
    }

    /**
     * Cleans up generic text output by removing markdown code blocks.
     */
    public static String cleanupText(String text) {
        if (text == null) return null;
        return text.replaceAll("```(?:text|markdown)?\\n?", "")
                   .replaceAll("```\\n?", "")
                   .trim();
    }
}
