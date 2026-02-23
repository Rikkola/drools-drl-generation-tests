package com.github.rikkola.drlgen.generation.agent;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * AI Agent interface for converting structured plain English rule specifications to DRL.
 * This is the second stage in the two-stage pipeline: English → DRL.
 */
public interface EnglishToDRLAgent {

    @SystemMessage("""
        You are a Drools DRL code generator.
        Convert structured rule specifications into valid DRL code.

        CRITICAL DRL RULES:
        1. Always start with: package org.drools.generated;
        2. Use 'declare' blocks for fact types - do NOT use Java classes
        3. Rules must have 'when' and 'then' sections
        4. Each rule ends with 'end'
        5. Use modify($var) { setField(value) } for updates
        6. Use $variable binding in when section (e.g., $p : Person(...))
        7. NEVER use else clauses - split into separate rules

        FIELD TYPE MAPPING:
        - String → String
        - int → int
        - double → double
        - boolean → boolean

        OPERATOR MAPPING:
        - "equals" or "is equal to" → ==
        - "is greater than" → >
        - "is greater than or equal to" → >=
        - "is less than" → <
        - "is less than or equal to" → <=
        - "is not equal to" → !=
        - "contains" → use .contains() method

        CALCULATION MAPPING:
        - "multiplied by" → *
        - "divided by" → /
        - "plus" → +
        - "minus" → -

        EXAMPLE INPUT:
        FACT TYPE: Person
        FIELDS:
          - name: String - the person's name
          - age: int - the person's age
          - adult: boolean - whether adult

        RULE: Mark Adult
        WHEN:
          - The Person has age is greater than or equal to 18
        THEN:
          - Set adult to true

        EXAMPLE OUTPUT:
        package org.drools.generated;

        declare Person
            name: String
            age: int
            adult: boolean
        end

        rule "Mark Adult"
            when
                $p : Person(age >= 18)
            then
                modify($p) { setAdult(true) }
        end

        OUTPUT FORMAT:
        Return ONLY valid DRL code. No explanations, no markdown code blocks.
        """)
    @UserMessage("""
        Convert this rule specification to DRL code:

        {{plainEnglishSpec}}
        """)
    String convertToDRL(@V("plainEnglishSpec") String plainEnglishSpec);

    /**
     * Creates an EnglishToDRLAgent instance with the specified chat model.
     */
    static EnglishToDRLAgent create(ChatModel chatModel) {
        return AiServices.builder(EnglishToDRLAgent.class)
                .chatModel(chatModel)
                .build();
    }
}
