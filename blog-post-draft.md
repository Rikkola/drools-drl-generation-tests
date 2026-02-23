# Can AI Write Business Rules? Testing LLMs on Drools Rule Generation

*February 2026*

Business rules engines like Drools have been the backbone of enterprise decision automation for decades. But can modern AI models generate valid, executable business rules from natural language requirements? We set out to answer this question with a comprehensive testing framework.

## The Challenge

Drools Rule Language (DRL) has a specific syntax that combines Java-like expressions with declarative rule constructs. A simple rule looks like this:

```drl
rule "Check Adult"
when
    $person : Person(age >= 18)
then
    modify($person) { setAdult(true) };
end
```

The question is: can an LLM generate syntactically correct *and* semantically accurate rules from plain English requirements like *"Mark a person as adult if they are 18 years or older"*?

## Our Testing Approach

We built a comprehensive testing framework that evaluates AI models through a three-stage pipeline:

1. **Generation** - The AI model generates rule code from natural language requirements
2. **Validation** - The generated code is compiled using the Drools compiler
3. **Execution** - Rules are executed against test cases to verify correct behavior

A test only passes if all three stages succeed. This ensures we're not just measuring syntax correctness, but actual rule functionality.

## Test Scenarios

We created 18 test scenarios covering common business rule patterns:

| Category | Scenarios |
|----------|-----------|
| **Age & Classification** | Adult validation, Senior citizen discounts, Age categorization |
| **Order & Pricing** | Tiered discounts, Shipping calculations, Loyalty points |
| **Validation** | Email validation, Password strength, Status transitions |
| **Financial** | Tax calculations, Loan eligibility, Insurance tiers, Tax brackets |
| **Other** | Grade calculations, Inventory alerts, Priority assignment |

Each scenario includes complete type definitions, natural language requirements, and test cases with expected outcomes.

## Three Generation Formats

One key insight from our research: the *format* in which you ask the AI to generate rules significantly impacts success rates.

### 1. Direct DRL Generation

The model generates Drools Rule Language directly. This is the fastest approach but demands the highest accuracy from the model—one syntax error means complete failure.

### 2. YAML Intermediate Format

The model generates a structured YAML representation:

```yaml
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

This YAML is then converted to DRL automatically. The structured format provides better guardrails for the model.

### 3. Plain English Pipeline (Two-Stage)

The model first generates a plain English description of the rules, then a second pass converts that description to executable code. This two-stage approach shows remarkable improvements for weaker models.

## The Models We Tested

We evaluated six current Ollama models, ranging from 3.4B to 108.6B parameters:

| Model | Parameters | Context | Best Format |
|-------|------------|---------|-------------|
| QWEN3-CODER-NEXT | 79.7B | 262K | DRL (100%) |
| GRANITE4-SMALL-H | 32.2B | 1M | DRL (100%) |
| QWEN2.5-CODER-14B | 14.8B | 32K | DRL (100%) |
| QWEN3 | 8.2B | 40K | DRL (88%) |
| LLAMA4 | 108.6B | 10.5M | English (77%) |
| GRANITE4 | 3.4B | 131K | English (67%) |

## Key Findings

### Model Size Doesn't Guarantee Success

Perhaps our most surprising finding: **LLAMA4 (108.6B parameters) was outperformed by QWEN2.5-CODER (14.8B)** on direct DRL generation. The smaller model achieved 100% success while the larger model only managed 41%.

The difference? Code specialization. Models specifically trained on code (the "CODER" variants) dramatically outperform general-purpose models on structured code generation tasks.

### Format Choice Matters—A Lot

For models struggling with direct DRL generation, alternative formats provide a lifeline:

| Model | DRL Success | YAML Success | English Success |
|-------|-------------|--------------|-----------------|
| LLAMA4 | 41% | 72% | **77%** |
| GRANITE4 | 18% | 61% | **67%** |

Using the English pipeline improved LLAMA4's success rate by **36 percentage points**. For production systems using weaker models, this is the difference between a system that works and one that doesn't.

### YAML as a Universal Improver

Across all models, YAML generation showed more consistent results than direct DRL. The structured format:

- Provides clearer boundaries for the model
- Reduces syntax error opportunities
- Enables better error recovery

For scenarios where direct DRL fails, YAML often succeeds.

### Speed vs. Accuracy Trade-offs

| Format | Median Generation Time | Reliability |
|--------|----------------------|-------------|
| DRL | 30-75ms | Varies by model |
| YAML | 40-100ms | More consistent |
| English | 80-160ms | Best for weak models |

The two-stage English pipeline takes roughly twice as long but provides the highest reliability for challenging models.

## Common Failure Patterns

Understanding why models fail helps in prompt engineering. We observed several recurring issues:

**DRL Failures:**
- Mismatched syntax (common with LLAMA4)
- Confusion between `modify()` blocks and setter methods
- Mixing query syntax with rule syntax
- Type declaration errors

**YAML Failures:**
- Schema inconsistencies
- Type mismatches (String vs int)
- Malformed YAML structure

## Recommendations

Based on our testing, here's our guidance for production deployments:

### For High-Stakes Applications
Use **QWEN3-CODER-NEXT** or **GRANITE4-SMALL-H** with direct DRL generation. These achieved 100% success across all scenarios.

### For Cost-Sensitive Deployments
**QWEN2.5-CODER-14B** offers the best balance—a 14B parameter model that matches the accuracy of 80B+ models.

### For General-Purpose Models
If you must use a general-purpose model like LLAMA4, use the **Plain English pipeline**. It nearly doubles your success rate.

### Format Selection Strategy
1. Try DRL generation first (fastest)
2. Fall back to YAML if DRL fails
3. Use English pipeline for complex scenarios or weaker models

## What This Means

AI-generated business rules are no longer theoretical—they work today with the right model and format choices. Key takeaways:

1. **Code-specialized models excel** at structured code generation
2. **Format flexibility** can compensate for model limitations
3. **Testing infrastructure** is essential—compilation success doesn't guarantee correct behavior
4. **Parameter count is misleading**—a well-trained 14B model beats a general 108B model

The era of natural language business rule authoring is here. The question isn't whether AI can write business rules, but which AI and how to prompt it.

---

## Try It Yourself

The testing framework is open source. Run your own evaluations:

```bash
# Test with default model
mvn test -pl drools-drl-generation-tests

# Test specific model
mvn test -pl drools-drl-generation-tests -Dtest.ollama.model=qwen3-coder-next

# Run full comparison
mvn test -pl drools-drl-generation-tests -Pmodel-comparison
```

---

*This post is part of our research into AI-assisted business rule development. The full test framework and results are available in the drools-drl-generation-tests module.*
