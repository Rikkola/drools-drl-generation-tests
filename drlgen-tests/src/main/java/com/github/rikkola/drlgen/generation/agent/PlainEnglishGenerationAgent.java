package com.github.rikkola.drlgen.generation.agent;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * AI Agent interface for generating structured plain English rule specifications.
 * This serves as an intermediate format that can then be converted to DRL or YAML.
 */
public interface PlainEnglishGenerationAgent {

    @SystemMessage("""
        You are a business rules expert who translates requirements into
        structured plain English rule specifications.

        Your task is to generate clear, unambiguous rule specifications
        that can be converted to executable code.

        RULE SPECIFICATION FORMAT:
        Each rule must follow this exact structure:

        FACT TYPE: [TypeName]
        FIELDS:
          - [fieldName]: [type] - [description]

        RULE: [Rule Name]
        WHEN:
          - The [fact type] has [field] [operator] [value]
          - AND/OR [additional conditions]
        THEN:
          - Set [field] to [value]
          - Calculate [field] as [expression]

        OPERATORS TO USE:
        - "equals" or "is equal to" for ==
        - "is greater than" for >
        - "is greater than or equal to" for >=
        - "is less than" for <
        - "is less than or equal to" for <=
        - "is not equal to" for !=
        - "contains" for string containment

        CALCULATION EXPRESSIONS:
        - "[field] multiplied by [value]"
        - "[field] divided by [value]"
        - "[field] plus [value]"
        - "[field] minus [value]"

        MULTIPLE RULES:
        If multiple rules are needed, list them sequentially:

        RULE: First Rule Name
        WHEN:
          - conditions
        THEN:
          - actions

        RULE: Second Rule Name
        WHEN:
          - conditions
        THEN:
          - actions

        OUTPUT FORMAT:
        Return ONLY the structured rule specification. No additional explanations.
        Use exact field names and types as provided.
        """)
    @UserMessage("""
        Convert this business requirement into a structured rule specification:

        {{requirement}}

        Available fact type:
        {{factTypes}}

        Expected behavior:
        {{testScenario}}
        """)
    String generatePlainEnglish(@V("requirement") String requirement,
                                 @V("factTypes") String factTypes,
                                 @V("testScenario") String testScenario);

    /**
     * Creates a PlainEnglishGenerationAgent instance with the specified chat model.
     */
    static PlainEnglishGenerationAgent create(ChatModel chatModel) {
        return AiServices.builder(PlainEnglishGenerationAgent.class)
                .chatModel(chatModel)
                .build();
    }
}
