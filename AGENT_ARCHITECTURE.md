# Agent Architecture - drools-drl-generation-tests

This document explains the agent structure and generation pipelines in the drools-drl-generation-tests module.

## Overview

The module uses **4 LangChain4j agents** with **3 service orchestrators** providing different generation pipelines for AI-generated Drools rules.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              TEST SCENARIO                                   │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ • requirement: "Adults are 18 or older..."                          │    │
│  │ • factTypes: [{Person: {name: String, age: int, adult: boolean}}]   │    │
│  │ • testCases: [{input: JSON, expectedRulesFired: 1, expected: {...}}]│    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
            ┌─────────────────────────┼─────────────────────────┐
            │                         │                         │
            ▼                         ▼                         ▼
┌───────────────────┐    ┌───────────────────┐    ┌───────────────────────┐
│  PIPELINE 1: DRL  │    │ PIPELINE 2: YAML  │    │ PIPELINE 3: ENGLISH   │
│   (Single-stage)  │    │   (Multi-stage)   │    │    (Two-stage)        │
└───────────────────┘    └───────────────────┘    └───────────────────────┘
         │                        │                         │
         ▼                        ▼                         ▼
┌───────────────────┐    ┌───────────────────┐    ┌───────────────────────┐
│ DRLGeneration     │    │ YAMLRuleGeneration│    │ PlainEnglishGeneration│
│ Service           │    │ Service           │    │ Service               │
└───────────────────┘    └───────────────────┘    └───────────────────────┘
         │                        │                    │           │
         ▼                        ▼                    ▼           │
┌───────────────────┐    ┌───────────────────┐    ┌─────────────┐ │
│ DRLGeneration     │    │ YAMLRuleGeneration│    │ PlainEnglish│ │
│ Agent (LLM)       │    │ Agent (LLM)       │    │ Agent (LLM) │ │
│                   │    │                   │    │ [Stage 1]   │ │
│ @SystemMessage:   │    │ @SystemMessage:   │    └─────────────┘ │
│ "Generate valid   │    │ "Generate YAML    │          │         │
│  DRL with declare │    │  rules with types │          ▼         ▼
│  blocks..."       │    │  and rules..."    │    ┌─────────────────────┐
└───────────────────┘    └───────────────────┘    │ EnglishToDRL Agent  │
         │                        │               │ (LLM) [Stage 2]     │
         │                        ▼               │                     │
         │               ┌───────────────────┐    │ Converts structured │
         │               │ YAMLToDRL         │    │ English → DRL       │
         │               │ Converter         │    └─────────────────────┘
         │               │ (Programmatic)    │              │
         │               └───────────────────┘              │
         │                        │                         │
         └────────────────────────┼─────────────────────────┘
                                  │
                                  ▼
                    ┌───────────────────────────┐
                    │   DRL VALIDATION          │
                    │   (DRLValidationService)  │
                    │   • Syntax check          │
                    │   • Structure validation  │
                    └───────────────────────────┘
                                  │
                                  ▼
                    ┌───────────────────────────┐
                    │   DRL EXECUTION           │
                    │   (DRLPopulatorRunner)    │
                    │   • Compile rules         │
                    │   • Insert test facts     │
                    │   • Fire rules            │
                    │   • Check results         │
                    └───────────────────────────┘
                                  │
                                  ▼
                    ┌───────────────────────────┐
                    │   GENERATION RESULT       │
                    │   • generatedDRL          │
                    │   • validationPassed      │
                    │   • executionPassed       │
                    │   • rulesFired            │
                    │   • timing metrics        │
                    └───────────────────────────┘
```

## The 4 Agents

| Agent | Input | Output | Purpose |
|-------|-------|--------|---------|
| **DRLGenerationAgent** | Requirement + Facts | Raw DRL code | Direct code generation |
| **YAMLRuleGenerationAgent** | Requirement + Facts | YAML structure | Structured rule definition |
| **PlainEnglishGenerationAgent** | Requirement + Facts | Structured English | Intermediate representation |
| **EnglishToDRLAgent** | Structured English | DRL code | Translate English → DRL |

### DRLGenerationAgent

- **Purpose**: Direct generation of executable DRL code from natural language requirements
- **Location**: `src/main/java/org/drools/generation/agent/DRLGenerationAgent.java`
- **System Prompt Enforces**:
  - Must use `declare` blocks for fact types (not Java classes)
  - Proper `when`/`then` syntax with `end` keywords
  - Use `modify()` for updates instead of direct setters
  - No else clauses in then sections

### YAMLRuleGenerationAgent

- **Purpose**: Generate Drools YAML-based rules from natural language
- **Location**: `src/main/java/org/drools/generation/agent/YAMLRuleGenerationAgent.java`
- **Output Format**:
```yaml
types:
  - name: Person
    fields:
      name: String
      age: int
      adult: boolean

rules:
  - name: "Check Adult"
    condition:
      given: Person
      as: $person
      having:
        - age >= 18
    action:
      modify:
        target: $person
        set:
          adult: true
```

### PlainEnglishGenerationAgent

- **Purpose**: Convert requirements into intermediate structured English rule specifications
- **Location**: `src/main/java/org/drools/generation/agent/PlainEnglishGenerationAgent.java`
- **Used in**: First stage of the two-stage pipeline

### EnglishToDRLAgent

- **Purpose**: Convert structured English specifications to DRL
- **Location**: `src/main/java/org/drools/generation/agent/EnglishToDRLAgent.java`
- **Operator Mapping**:
  - "is greater than" → `>`
  - "is greater than or equal to" → `>=`
  - "contains" → `.contains()` method
  - "multiplied by" → `*`

## Service Layer

### DRLGenerationService

**Workflow**:
1. Creates `DRLGenerationAgent` with the ChatModel
2. Calls `agent.generateDRL()` with requirement, fact types, and test scenario
3. Cleans up markdown artifacts (```drl``` blocks)
4. Validates DRL structure using `DRLValidationService`
5. Executes with test cases using `DRLPopulatorRunner`
6. Returns `GenerationResult`

### YAMLRuleGenerationService

**Multi-Stage Workflow**:
1. Generate YAML using `YAMLRuleGenerationAgent`
2. Validate YAML structure using `YAMLToDRLConverter.validateYAML()`
3. Convert YAML to DRL using `YAMLToDRLConverter.convertToDRL()`
4. Validate converted DRL
5. Execute with test cases
6. Returns `YAMLGenerationResult`

### PlainEnglishGenerationService

**Two-Stage Pipeline**:
1. **Stage 1**: `PlainEnglishGenerationAgent.generatePlainEnglish()` → Structured English
2. **Stage 2**: `EnglishToDRLAgent.convertToDRL()` → DRL from English
3. Validate and execute DRL
4. Returns `PlainEnglishGenerationResult`

## Model Configuration

All agents connect to **Ollama** via LangChain4j:

```
┌─────────────────────────────────────────────┐
│           ModelConfiguration                │
│  ┌───────────────────────────────────────┐  │
│  │ Current Models (Feb 2026):            │  │
│  │ • granite4           (temp: 0.1)      │  │
│  │ • qwen3              (temp: 0.0)      │  │
│  │ • qwen3-coder-next   (temp: 0.1)      │  │
│  │ • llama4             (temp: 0.1)      │  │
│  └───────────────────────────────────────┘  │
│           ↓ OllamaChatModel.builder()       │
│  ┌───────────────────────────────────────┐  │
│  │ http://localhost:11434                │  │
│  │ timeout: 5 minutes                    │  │
│  │ context: 16384 tokens                 │  │
│  └───────────────────────────────────────┘  │
└─────────────────────────────────────────────┘
```

### Supported Models (Current)

| Model Type | Model Name | Parameters | Context | Temperature | DRL Success |
|------------|------------|------------|---------|-------------|-------------|
| QWEN3_CODER_NEXT | qwen3-coder-next | 79.7B | 262K | 0.1 | 100% |
| GRANITE4_SMALL_H | granite4:small-h | 32.2B | 1M | 0.1 | 100% (Mamba-2 hybrid) |
| QWEN25_CODER_14B | qwen2.5-coder:14b-instruct-q4_K_M | 14.8B | 32K | 0.1 | 100% (most efficient) |
| QWEN3 | qwen3 | 8.2B | 40K | 0.0 | 88% |
| LLAMA4 | llama4 | 108.6B | 10.5M | 0.1 | 41% (use ENGLISH pipeline) |
| GRANITE4 | granite4 | 3.4B | 131K | 0.1 | 18% (use ENGLISH pipeline) |

### Legacy Models (Deprecated)

| Model Type | Model Name | Default Temperature |
|------------|------------|---------------------|
| GRANITE_CODE_8B | granite-code:8b | 0.1 |
| GRANITE_CODE_20B | granite-code:20b | 0.05 |
| GRANITE3_MOE | granite3-moe:3b | 0.1 |
| GRANITE_33_8B | granite3.3:8b | 0.1 |
| QWEN_CODER_14B | qwen2.5-coder:14b-instruct-q4_K_M | 0.1 |
| QWEN3_14B | qwen3:14b | 0.0 |
| LLAMA3_8B | llama3.2:8b | 0.1 |
| CODELLAMA_13B | codellama:13b | 0.1 |

## Data Models

### TestScenario

```java
record TestScenario(
    String name,
    String description,
    String requirement,
    List<FactTypeDefinition> expectedFactTypes,
    List<TestCase> testCases
)
```

**Nested Classes**:
- `FactTypeDefinition`: Type name + field mapping (field name → type)
- `TestCase`: Input JSON, expected rules fired, expected field values

### GenerationResult

Complete result of DRL generation with:
- Generated DRL code
- Validation status and messages
- Execution status and rules fired count
- Timing metrics (generation time, total time)

## Key Files

| File | Purpose |
|------|---------|
| `agent/DRLGenerationAgent.java` | Direct DRL generation from requirements |
| `agent/YAMLRuleGenerationAgent.java` | YAML rule generation |
| `agent/PlainEnglishGenerationAgent.java` | Requirement → structured English |
| `agent/EnglishToDRLAgent.java` | English specifications → DRL |
| `service/DRLGenerationService.java` | Orchestrates DRL pipeline |
| `service/YAMLRuleGenerationService.java` | Orchestrates YAML pipeline |
| `service/PlainEnglishGenerationService.java` | Orchestrates 2-stage English pipeline |
| `service/YAMLToDRLConverter.java` | YAML structure → valid DRL syntax |
| `config/ModelConfiguration.java` | Ollama model setup and configuration |

## Test Framework

### Test Scenarios (20+ in TestScenarioProvider)

Examples:
- **Adult Validation**: Age >= 18 → set adult=true
- **Senior Citizen Discount**: Age >= 65 → set senior=true, then discountPercent=15
- **Tiered Discounts**: Mutually exclusive discount tiers
- **Email Validation**: Email contains @ → valid=true
- **Password Strength**: Length-based strength classification
- **Loan Eligibility**: Multiple conditions (income, credit score, employment)
- **Tax Bracket**: Income-based tax rate assignment
- **Grade Calculation**: Score-to-letter-grade conversion

### Comparison Testing

- **ModelComparisonTest**: Tests all models against all scenarios
- **FormatComparisonTest**: Compares DRL vs YAML generation
- **ComparisonRunner**: CLI tool for comprehensive comparison reports