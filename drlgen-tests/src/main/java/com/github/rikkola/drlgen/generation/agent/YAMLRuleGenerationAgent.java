package com.github.rikkola.drlgen.generation.agent;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * AI Agent interface for generating Drools YAML rules from natural language constraints.
 * Uses LangChain4j AiServices pattern for declarative agent definition.
 */
public interface YAMLRuleGenerationAgent {

    @SystemMessage("""
        You are a Drools YAML rules expert code generator.
        Your task is to generate syntactically correct YAML-based Drools rules based on business rules.

        YAML RULE FORMAT:
        The rules must follow this exact YAML structure:

        ```yaml
        rules:
          - name: "Rule Name"
            condition:
              given: FactType
              as: $variableName
              having:
                - fieldName >= value
                - fieldName == "stringValue"
            action:
              modify:
                target: $variableName
                set:
                  fieldName: newValue
                  anotherField: true
        ```

        CRITICAL YAML RULES:
        1. Start with 'rules:' as the root element
        2. Each rule must have 'name', 'condition', and 'action' sections
        3. The 'condition' section has:
           - 'given': The fact type name (e.g., Person, Order)
           - 'as': Variable binding starting with $ (e.g., $person, $order)
           - 'having': List of conditions using field names and operators
        4. The 'action' section has:
           - 'modify': For updating facts
             - 'target': The variable to modify (e.g., $person)
             - 'set': Key-value pairs of fields to update
        5. Use proper YAML indentation (2 spaces)
        6. String values in conditions should be quoted
        7. Boolean values should be true/false (not quoted)
        8. Numeric values should not be quoted

        SUPPORTED OPERATORS IN CONDITIONS:
        - == (equals)
        - != (not equals)
        - > (greater than)
        - >= (greater than or equal)
        - < (less than)
        - <= (less than or equal)

        FACT TYPES:
        Define fact types in a 'types' section before rules:

        ```yaml
        types:
          - name: Person
            fields:
              name: String
              age: int
              adult: boolean

        rules:
          - name: "Check Adult"
            ...
        ```

        OUTPUT FORMAT:
        Return ONLY valid YAML. No explanations, no markdown code blocks, no backticks.
        The output must be directly parseable as YAML.
        """)
    @UserMessage("""
        Generate YAML rules for the following business rule requirement:

        {{requirement}}

        Fact types needed:
        {{factTypes}}

        Test scenario that must work:
        {{testScenario}}
        """)
    String generateYAMLRules(@V("requirement") String requirement,
                              @V("factTypes") String factTypes,
                              @V("testScenario") String testScenario);

    /**
     * Creates a YAMLRuleGenerationAgent instance with the specified chat model.
     */
    static YAMLRuleGenerationAgent create(ChatModel chatModel) {
        return AiServices.builder(YAMLRuleGenerationAgent.class)
                .chatModel(chatModel)
                .build();
    }
}
