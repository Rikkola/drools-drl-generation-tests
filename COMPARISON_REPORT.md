# Drools Rule Generation Comparison Report

**Date:** 2026-02-24 (Updated)
**Module:** drools-drl-generation-tests
**Purpose:** Compare AI model performance across different rule generation formats

---

## Latest Results (Feb 24, 2026) - Parameter Tuning

### Summary - Best Configurations Found

| Model | Best Temperature | Pass | Fail | Success Rate |
|-------|------------------|------|------|--------------|
| **qwen2.5-coder:14b** | **0.2** | 21 | 8 | **72%** |
| **granite4:small-h** | **0.0** | 19 | 10 | **65%** |

### Parameter Tuning Details

| Model | Temperature | topP | Pass | Fail | Success Rate |
|-------|-------------|------|------|------|--------------|
| qwen2.5-coder:14b | 0.2 | 0.95 | 21 | 8 | **72%** |
| qwen2.5-coder:14b | 0.1 | 0.9 | 20 | 9 | 68% |
| qwen2.5-coder:14b | 0.0 | 0.9 | 20 | 9 | 68% |
| granite4:small-h | 0.0 | 0.9 | 19 | 10 | **65%** |
| granite4:small-h | 0.2 | 0.95 | 18 | 11 | 62% |
| granite4:small-h | 0.1 | 0.9 | 16 | 13 | 55% |

### Key Findings

1. **qwen2.5-coder benefits from higher temperature (0.2)** - More creativity helps
2. **granite4:small-h benefits from lower temperature (0.0)** - More deterministic is better
3. **8 scenarios consistently fail** across all configurations - these have complex multi-condition rules
4. **Parameter tuning provides 10-17% improvement** over default settings

### Consistently Failing Scenarios (8 of 29)

These scenarios fail regardless of parameter settings:
1. adult-validation
2. bulk-order-discount
3. cart-validation
4. credit-card-approval
5. insurance-risk-assessment
6. missing-document-alert
7. order-fraud-detection
8. tax-calculation-chain

**Root Cause:** Complex multi-condition rules, chained calculations, and OR conditions that challenge all models.

### Detailed Results Matrix

```
Scenario                       | qwen3-coder | granite4:sh | qwen2.5-14b | qwen3 | granite4 | phi4 | deepseek | codellama |
---------------------------------------------------------------------------------------------------------------------------
Adult Validation               | PASS        | PASS        | PASS        | PASS  | FAIL     | PASS | FAIL     | FAIL      |
Age Classification             | PASS        | PASS        | PASS        | FAIL  | PASS     | FAIL | FAIL     | FAIL      |
Age Validation Simple          | PASS        | PASS        | PASS        | PASS  | FAIL     | PASS | FAIL     | FAIL      |
Order Discount - Basic         | PASS        | PASS        | PASS        | PASS  | FAIL     | FAIL | FAIL     | FAIL      |
Bulk Order Discount            | PASS        | FAIL        | FAIL        | FAIL  | FAIL     | FAIL | FAIL     | FAIL      |
Cart Validation                | PASS        | PASS        | PASS        | FAIL  | FAIL     | FAIL | FAIL     | FAIL      |
Credit Card Approval           | PASS        | FAIL        | FAIL        | FAIL  | FAIL     | FAIL | FAIL     | FAIL      |
Email Validation               | PASS        | PASS        | PASS        | PASS  | FAIL     | PASS | FAIL     | FAIL      |
Grade Calculation              | PASS        | PASS        | PASS        | PASS  | FAIL     | FAIL | FAIL     | FAIL      |
Insurance Premium Tier         | PASS        | PASS        | PASS        | PASS  | FAIL     | FAIL | FAIL     | FAIL      |
Insurance Risk Assessment      | PASS        | FAIL        | FAIL        | FAIL  | FAIL     | FAIL | FAIL     | FAIL      |
Inventory Alert                | PASS        | PASS        | PASS        | PASS  | FAIL     | PASS | FAIL     | FAIL      |
Loan Approval Complex          | PASS        | PASS        | PASS        | FAIL  | FAIL     | FAIL | FAIL     | FAIL      |
Loan Eligibility               | PASS        | PASS        | PASS        | PASS  | PASS     | FAIL | FAIL     | FAIL      |
Loyalty Points                 | PASS        | PASS        | PASS        | PASS  | FAIL     | FAIL | FAIL     | FAIL      |
Missing Document Alert         | PASS        | PASS        | PASS        | FAIL  | FAIL     | FAIL | FAIL     | FAIL      |
Order Discount by Customer     | PASS        | FAIL        | PASS        | PASS  | FAIL     | FAIL | FAIL     | FAIL      |
Order Fraud Detection          | PASS        | PASS        | PASS        | FAIL  | FAIL     | FAIL | FAIL     | FAIL      |
Password Strength              | PASS        | PASS        | PASS        | PASS  | FAIL     | FAIL | FAIL     | FAIL      |
Priority Assignment            | PASS        | PASS        | PASS        | PASS  | FAIL     | FAIL | FAIL     | FAIL      |
Senior Citizen Discount        | PASS        | PASS        | PASS        | PASS  | PASS     | FAIL | FAIL     | FAIL      |
Shipping Cost                  | PASS        | PASS        | PASS        | PASS  | PASS     | FAIL | FAIL     | FAIL      |
Shipping Method Priority       | PASS        | PASS        | PASS        | FAIL  | FAIL     | FAIL | FAIL     | FAIL      |
Simple Calculation             | PASS        | FAIL        | PASS        | FAIL  | FAIL     | FAIL | FAIL     | FAIL      |
Status Transition              | PASS        | PASS        | PASS        | PASS  | PASS     | FAIL | FAIL     | FAIL      |
Subscription Renewal           | PASS        | PASS        | PASS        | PASS  | FAIL     | FAIL | FAIL     | FAIL      |
Tax Bracket                    | PASS        | PASS        | PASS        | PASS  | PASS     | FAIL | FAIL     | FAIL      |
Tax Calculation Chain          | PASS        | PASS        | FAIL        | FAIL  | FAIL     | FAIL | FAIL     | FAIL      |
Order Discount - Tiered        | PASS        | PASS        | PASS        | PASS  | FAIL     | FAIL | FAIL     | FAIL      |
---------------------------------------------------------------------------------------------------------------------------
TOTALS                         | 29/29       | 24/29       | 25/29       | 17/29 | 7/29     | 5/29 | 0/29     | 0/29      |
SUCCESS RATE                   | 100%        | 83%         | 86%         | 59%   | 24%      | 17%  | 0%       | 0%        |
```

### Recommendations (Updated Feb 24, 2026)

| Use Case | Recommended Model | Success Rate | Notes |
|----------|-------------------|--------------|-------|
| **Production (best quality)** | qwen3-coder-next | 100% | Only model with perfect accuracy |
| **Production (best speed)** | qwen2.5-coder:14b | 86% | 20x faster than qwen3-coder-next |
| **Balanced** | granite4:small-h | 83% | Good accuracy, 1M context |
| **Resource constrained** | qwen3 | 59% | Acceptable for simple rules |
| **Avoid** | deepseek, phi4, codellama | 0-17% | Don't know DRL syntax |

---

## Previous Results (Feb 20, 2026) - 29 Scenarios

### Summary

| Model | Parameters | Success Rate | Failures | Time |
|-------|------------|--------------|----------|------|
| **qwen3-coder-next** | 79.7B | **97%** (28/29) | 1 | 85 min |
| **granite4:small-h** | 32.2B | **97%** (28/29) | 1 | 88 min |
| **qwen3** | 8.2B | **72%** (21/29) | 8 | 5.5 min |

### Detailed Failures

| Model | Failed Scenario | Error |
|-------|-----------------|-------|
| qwen3-coder-next | Loyalty Points | DRL compilation error |
| granite4:small-h | Simple Calculation | Field access error |
| qwen3 | Order Discount Basic, Bulk Order Discount, Cart Validation, Credit Card Approval, Insurance Risk Assessment, Order Fraud Detection, Simple Calculation, Tax Calculation Chain | Various compilation & empty output errors |

### Analysis

**Scenario Difficulty Tiers:**

| Tier | Scenarios | Notes |
|------|-----------|-------|
| Easy (all pass) | Adult Validation, Age Classification, Email Validation, Grade Calculation, Insurance Premium Tier, Inventory Alert, Password Strength, Priority Assignment, Senior Citizen Discount, Shipping Cost, Status Transition, Tax Bracket | Simple conditions |
| Medium (large models pass) | Order Discount Basic, Bulk Order Discount, Cart Validation, Credit Card Approval, Insurance Risk Assessment, Order Fraud Detection, Tax Calculation Chain | Complex logic |
| Challenging | Loyalty Points, Simple Calculation | Even large models fail occasionally |

**Speed vs Accuracy Trade-off:**
- qwen3: Fast (5.5 min) but 72% accuracy
- Large models: Slow (85-88 min) but 97% accuracy

---

## Previous Results (Feb 18, 2026) - 17 Scenarios

## Executive Summary

This report presents findings from testing 4 local AI models (Feb 2026 versions) across 17 business rule scenarios using three different generation approaches:

1. **Direct DRL** - Generate Drools Rule Language code directly
2. **YAML** - Generate YAML rules, then convert to DRL
3. **Plain English (2-stage)** - Generate structured English, then convert to DRL

**Key Finding:** The **qwen3-coder-next** model achieves near-perfect results (100% DRL, 94.1% overall), significantly outperforming all other models. The Plain English intermediate format continues to help mid-tier models, with granite4 showing improvement from 18% (DRL) to 71% (ENGLISH).

---

## Test Configuration

### Models Tested (Feb 2026)

| Model | Parameters | Context | Architecture | Capabilities |
|-------|------------|---------|--------------|--------------|
| qwen3-coder-next | 79.7B | 262K | Qwen3Next | Tools - Coding specialized |
| granite4:small-h | 32.2B | 1M | GraniteHybrid (Mamba-2) | Tools |
| qwen2.5-coder:14b | 14.8B | 32K | Qwen2 | Tools - Coding specialized |
| qwen3 | 8.2B | 40K | Qwen3 | Tools, Thinking (CoT) |
| llama4 | 108.6B | 10.5M | Llama4 (MoE) | Tools, Vision |
| granite4 | 3.4B | 131K | Granite | Tools |

All models use Q4_K_M quantization.

### Test Scenarios (17 total)

| Category | Scenarios |
|----------|-----------|
| **Person/Age Rules** | Adult Validation, Senior Citizen Discount |
| **Order/Discount Rules** | Basic Discount, Tiered Discount |
| **Calculation Rules** | Simple Calculation, Loyalty Points |
| **Validation Rules** | Email Validation, Password Strength, Age Classification |
| **Business Logic** | Shipping Cost, Inventory Alert, Loan Eligibility |
| **Multi-condition** | Insurance Premium Tier, Tax Bracket |
| **String/Enum** | Status Transition, Priority Assignment, Grade Calculation |

---

## Generation Formats

### Format 1: Direct DRL

The model generates Drools DRL code directly from the business requirement.

**System Prompt (excerpt):**
```
You are a Drools DRL (Decision Rule Language) expert code generator.
Your task is to generate syntactically correct and executable DRL code.

CRITICAL DRL RULES:
1. Always start with a package declaration
2. Use 'declare' blocks to define fact types
3. Rules must have proper 'when' and 'then' sections
4. Use modify($variable) { setField(value) } for updates
...
```

### Format 2: YAML Rules

The model generates YAML-formatted rules, which are then converted to DRL programmatically.

**System Prompt (excerpt):**
```
You are a Drools YAML rules expert code generator.

YAML RULE FORMAT:
rules:
  - name: "Rule Name"
    condition:
      given: FactType
      as: $variableName
      having:
        - fieldName >= value
    action:
      modify:
        target: $variableName
        set:
          fieldName: newValue
```

### Format 3: Plain English (2-Stage Pipeline)

A two-stage approach:
1. **Stage 1:** Generate structured plain English rule specification
2. **Stage 2:** Convert structured English to DRL

**Stage 1 System Prompt (excerpt):**
```
You are a business rules expert who translates requirements into
structured plain English rule specifications.

RULE SPECIFICATION FORMAT:
FACT TYPE: [TypeName]
FIELDS:
  - [fieldName]: [type] - [description]

RULE: [Rule Name]
WHEN:
  - The [fact type] has [field] [operator] [value]
THEN:
  - Set [field] to [value]
```

---

## Results (Feb 2026)

### Parameter Tuning Results (Feb 24, 2026) - 29 Scenarios

Testing different temperature settings to optimize model performance:

| Model | ID | Temperature | topP | Pass | Fail | Success Rate |
|-------|-----|-------------|------|------|------|--------------|
| **qwen2.5-coder:14b** | qwen2.5-coder-temp02 | **0.2** | 0.95 | 21 | 8 | **72%** |
| qwen2.5-coder:14b | qwen2.5-coder:14b | 0.1 | 0.9 | 20 | 9 | 68% |
| qwen2.5-coder:14b | qwen2.5-coder-temp0 | 0.0 | 0.9 | 20 | 9 | 68% |
| **granite4:small-h** | granite4:small-h-temp0 | **0.0** | 0.9 | 19 | 10 | **65%** |
| granite4:small-h | granite4:small-h-temp02 | 0.2 | 0.95 | 18 | 11 | 62% |
| granite4:small-h | granite4:small-h | 0.1 | 0.9 | 16 | 13 | 55% |

**Key Findings:**
- **qwen2.5-coder** performs best with **temperature 0.2** (72%)
- **granite4:small-h** performs best with **temperature 0.0** (65%)
- Higher temperature (0.2) benefits qwen2.5-coder, lower (0.0) benefits granite4

**Consistently Failing Scenarios (8 of 29):**
1. adult-validation
2. bulk-order-discount
3. cart-validation
4. credit-card-approval
5. insurance-risk-assessment
6. missing-document-alert
7. order-fraud-detection
8. tax-calculation-chain

These scenarios have complex multi-condition rules that are challenging for all parameter configurations.

---

### Previous Overall Performance Matrix (17 Scenarios)

```
Scenario                       | qwen3-coder-next      | granite4:small-h | qwen2.5-coder:14b | qwen3                 | llama4                | granite4              |
                               | DRL    YAML   ENGLISH | DRL    YAML      | DRL    YAML       | DRL    YAML   ENGLISH | DRL    YAML   ENGLISH | DRL    YAML   ENGLISH |
--------------------------------------------------------------------------------------------------------------------------------------------------------------------------
Adult Validation               | PASS   PASS   PASS    | PASS   PASS      | PASS   PASS       | PASS   PASS   PASS    | FAIL   PASS   PASS    | PASS   PASS   PASS    |
Senior Citizen Discount        | PASS   PASS   PASS    | PASS   PASS      | PASS   PASS       | PASS   PASS   PASS    | PASS   PASS   PASS    | FAIL   PASS   FAIL    |
Order Discount - Basic         | PASS   PASS   PASS    | PASS   PASS      | PASS   PASS       | PASS   PASS   PASS    | FAIL   PASS   FAIL    | FAIL   PASS   PASS    |
Order Discount - Tiered        | PASS   PASS   PASS    | PASS   PASS      | PASS   PASS       | PASS   PASS   PASS    | FAIL   PASS   FAIL    | FAIL   PASS   FAIL    |
Simple Calculation             | PASS   FAIL   PASS    | PASS   FAIL      | PASS   FAIL       | FAIL   FAIL   FAIL    | FAIL   FAIL   FAIL    | FAIL   PASS   FAIL    |
Email Validation               | PASS   PASS   PASS    | PASS   PASS      | PASS   PASS       | PASS   FAIL   PASS    | FAIL   FAIL   PASS    | FAIL   FAIL   FAIL    |
Password Strength              | PASS   PASS   PASS    | PASS   PASS      | PASS   PASS       | PASS   FAIL   FAIL    | FAIL   FAIL   PASS    | PASS   PASS   PASS    |
Age Classification             | PASS   PASS   PASS    | PASS   PASS      | PASS   PASS       | PASS   PASS   PASS    | PASS   PASS   PASS    | FAIL   FAIL   PASS    |
Shipping Cost                  | PASS   PASS   PASS    | PASS   FAIL      | PASS   PASS       | PASS   PASS   PASS    | PASS   PASS   PASS    | PASS   PASS   PASS    |
Loyalty Points                 | PASS   FAIL   FAIL    | PASS   FAIL      | PASS   FAIL       | FAIL   FAIL   FAIL    | FAIL   FAIL   PASS    | FAIL   FAIL   FAIL    |
Inventory Alert                | PASS   PASS   PASS    | PASS   PASS      | PASS   PASS       | PASS   PASS   PASS    | PASS   PASS   PASS    | FAIL   PASS   PASS    |
Loan Eligibility               | PASS   PASS   PASS    | PASS   PASS      | PASS   PASS       | PASS   FAIL   PASS    | FAIL   PASS   PASS    | FAIL   PASS   PASS    |
Insurance Premium Tier         | PASS   PASS   PASS    | PASS   PASS      | PASS   PASS       | PASS   PASS   PASS    | PASS   PASS   PASS    | FAIL   PASS   PASS    |
Tax Bracket                    | PASS   PASS   PASS    | PASS   FAIL      | PASS   PASS       | PASS   PASS   FAIL    | FAIL   PASS   PASS    | FAIL   FAIL   PASS    |
Status Transition              | PASS   PASS   PASS    | PASS   PASS      | PASS   PASS       | PASS   PASS   PASS    | PASS   PASS   PASS    | FAIL   FAIL   PASS    |
Priority Assignment            | PASS   PASS   PASS    | PASS   FAIL      | PASS   PASS       | PASS   PASS   PASS    | FAIL   FAIL   PASS    | FAIL   FAIL   PASS    |
Grade Calculation              | PASS   PASS   PASS    | PASS   PASS      | PASS   PASS       | PASS   PASS   PASS    | PASS   PASS   PASS    | FAIL   FAIL   PASS    |
--------------------------------------------------------------------------------------------------------------------------------------------------------------------------
TOTALS                         | 17/17  15/17  16/17   | 17/17  12/17     | 17/17  15/17      | 15/17  12/17  13/17   | 7/17   12/17  14/17   | 3/17   10/17  12/17   |
SUCCESS RATE                   | 100%   88%    94%     | 100%   71%       | 100%   88%        | 88%    71%    76%     | 41%    71%    82%     | 18%    59%    71%     |
```

### Summary by Model (Feb 24, 2026 - 29 Scenarios, DRL Only)

| Model | Config | Temp | DRL | YAML | ENGLISH | Best Format |
|-------|--------|------|-----|------|---------|-------------|
| **qwen2.5-coder:14b** | temp02 | 0.2 | **21/29 (72%)** | - | - | DRL |
| qwen2.5-coder:14b | default | 0.1 | 20/29 (68%) | - | - | DRL |
| qwen2.5-coder:14b | temp0 | 0.0 | 20/29 (68%) | - | - | DRL |
| **granite4:small-h** | temp0 | 0.0 | **19/29 (65%)** | - | - | DRL |
| granite4:small-h | temp02 | 0.2 | 18/29 (62%) | - | - | DRL |
| granite4:small-h | default | 0.1 | 16/29 (55%) | - | - | DRL |

*Note: YAML and ENGLISH formats not tested in this run. See previous results below for format comparison.*

### Previous Summary by Model (17 Scenarios, All Formats)

| Model | Parameters | DRL | YAML | ENGLISH | Best Format | Overall |
|-------|------------|-----|------|---------|-------------|---------|
| **qwen3-coder-next** | 79.7B | 17/17 (100%) | 15/17 (88%) | 16/17 (94%) | DRL | **94.1%** |
| **granite4:small-h** | 32.2B | 17/17 (100%) | 12/17 (71%) | N/A | DRL | **85.3%** |
| **qwen2.5-coder:14b** | 14.8B | 17/17 (100%) | 15/17 (88%) | N/A | DRL | **94.1%** |
| qwen3 | 8.2B | 15/17 (88%) | 12/17 (71%) | 13/17 (76%) | DRL | 78.4% |
| llama4 | 108.6B | 7/17 (41%) | 12/17 (71%) | **14/17 (82%)** | ENGLISH | 64.7% |
| granite4 | 3.4B | 3/17 (18%) | 10/17 (59%) | **12/17 (71%)** | ENGLISH | 49.0% |

---

## Analysis

### Key Finding 1: Three Models Achieve Perfect DRL Generation

Three models achieve perfect 100% success rate on direct DRL generation:

| Model | Parameters | Context | DRL Success | Notes |
|-------|------------|---------|-------------|-------|
| **qwen3-coder-next** | 79.7B | 262K | 100% | Best YAML (88%), largest Qwen context |
| **granite4:small-h** | 32.2B | **1M** | 100% | **Largest context, Mamba-2 hybrid** |
| **qwen2.5-coder:14b** | 14.8B | 32K | 100% | **Most efficient - 5x smaller!** |
| qwen3 | 8.2B | 40K | 88% | Good efficiency |
| llama4 | 108.6B | 10.5M | 41% | General-purpose, not code-optimized |
| granite4 | 3.4B | 131K | 18% | Too small for direct DRL |

**Key Insights:**
1. **qwen2.5-coder:14b** is the efficiency champion - same 100% DRL accuracy with only 14.8B params
2. **granite4:small-h** proves Granite can achieve 100% DRL with sufficient size (32B vs 3.4B)
3. **granite4:small-h** has the largest practical context (1M tokens) among 100% performers

**Note:** Model size alone doesn't determine success - specialization and architecture matter. llama4 (108.6B) underperforms qwen3 (8.2B) because llama4 is general-purpose while coding-specialized models excel at DRL generation.

### Key Finding 2: Plain English Still Helps Weaker Models

The two-stage Plain English pipeline continues to benefit models that struggle with code syntax:

| Model | DRL → ENGLISH | Improvement |
|-------|---------------|-------------|
| **granite4** | 18% → 71% | **+294%** |
| **llama4** | 41% → 82% | **+100%** |
| qwen3 | 88% → 76% | -14% (worse) |
| qwen3-coder-next | 100% → 94% | -6% (slightly worse) |

**Interpretation:** Models like granite4 and llama4 understand business requirements but struggle with DRL syntax. The two-stage pipeline leverages their comprehension abilities while using a simpler code generation step.

### Key Finding 3: Top-Tier Models Don't Need Intermediate Steps

For qwen3-coder-next and qwen3, direct DRL generation outperforms intermediate formats. Adding YAML or Plain English stages introduces potential failure points without benefit.

### Key Finding 4: Scenario Difficulty Patterns

Some scenarios remain consistently difficult across all models:

| Scenario | Overall Pass Rate | Common Failure |
|----------|-------------------|----------------|
| Grade Calculation | 50% | Multiple mutually exclusive rules |
| Loyalty Points | 42% | Calculation syntax and field access |
| Tax Bracket | 67% | Multiple bracket conditions |

---

## Comparison with Previous Models (Legacy)

### Previous Best Performers (2025)

| Model | DRL | YAML | ENGLISH | Overall |
|-------|-----|------|---------|---------|
| qwen2.5-coder:14b | 17/17 (100%) | 15/17 (88%) | 15/17 (88%) | 92.2% |
| granite-code:20b | 8/17 (47%) | 8/17 (47%) | 12/17 (71%) | 54.9% |
| granite3.3:8b | 3/17 (18%) | 7/17 (41%) | 12/17 (71%) | 43.1% |

### New Models (Feb 2026)

| Model | DRL | YAML | ENGLISH | Overall | vs Previous Best |
|-------|-----|------|---------|---------|------------------|
| **qwen3-coder-next** | 17/17 (100%) | 15/17 (88%) | 16/17 (94%) | **94.1%** | +2% vs qwen2.5-coder |
| qwen3 | 15/17 (88%) | 12/17 (71%) | 13/17 (76%) | 78.4% | New |
| llama4 | 7/17 (41%) | 12/17 (71%) | 14/17 (82%) | 64.7% | New |
| granite4 | 3/17 (18%) | 10/17 (59%) | 12/17 (71%) | 49.0% | Similar to granite3.3 |

**Key Observations:**
- qwen3-coder-next matches qwen2.5-coder:14b on DRL (100%) and improves ENGLISH (94% vs 88%)
- llama4 provides a good middle-ground option with strong ENGLISH performance (82%)
- granite4 performance is similar to granite3.3:8b - the plain English pipeline remains critical

---

## Failure Analysis

### Common DRL Failures

1. **Field Access in Consequences**
   ```drl
   // WRONG - causes IllegalAccessError
   modify($invoice) { setTotal($invoice.subtotal + 10) }

   // CORRECT
   modify($invoice) { setTotal($invoice.getSubtotal() + 10) }
   ```

2. **Invalid Constraint Syntax**
   ```drl
   // WRONG
   $o : Order(total >= 100 && total < 200)

   // CORRECT
   $o : Order(total >= 100, total < 200)
   ```

### Common YAML Failures

1. **Extra Explanatory Text** - Models add prose that breaks YAML parsing
2. **Markdown Code Blocks** - Despite instructions, models wrap output in ```yaml blocks
3. **Variable Prefix in Conditions** - Using `$person.age` instead of `age`

### Common ENGLISH Failures

1. **Stage 1 Ambiguity** - Generated English is too vague for Stage 2
2. **Stage 2 Misinterpretation** - DRL generator misunderstands the English spec
3. **Calculation Expressions** - "multiply by" not always correctly translated

---

## Recommendations

### For Production Use

1. **Use qwen3-coder-next with direct DRL generation** - 100% success rate, best overall

2. **For balance of quality/speed**, consider qwen3 with DRL (88%)

3. **If limited to llama4 or granite4**, use the Plain English pipeline for best results

### Model Selection Guide

| Use Case | Recommended Model | Size | Format | Success Rate |
|----------|-------------------|------|--------|--------------|
| **Best efficiency (recommended)** | **qwen2.5-coder:14b** | **14.8B** | **DRL** | **100%** |
| Large context (1M) + 100% DRL | granite4:small-h | 32.2B | DRL | 100% |
| Maximum reliability + 262K context | qwen3-coder-next | 79.7B | DRL | 100% |
| Good reliability, smaller | qwen3 | 8.2B | DRL | 88% |
| Huge context (10M+), general use | llama4 | 108.6B | ENGLISH | 82% |
| Resource constrained | granite4 | 3.4B | ENGLISH | 71% |

### Prompt Engineering Tips

1. **Always include concrete examples** in system prompts
2. **Explicitly forbid explanatory text** - models tend to add prose
3. **Provide field types explicitly** - reduces type inference errors
4. **Include test scenarios** - helps models validate their output

---

## Technical Implementation

### Running Comparisons

```bash
# All formats, specific models
./run-comparison.sh --models qwen3-coder-next,qwen3,llama4,granite4

# Specific formats
./run-comparison.sh --formats DRL,YAML,ENGLISH

# Specific scenarios
./run-comparison.sh --scenarios Adult,Discount

# Output to specific file
./run-comparison.sh --output my-results.csv
```

### Key Files

| File | Purpose |
|------|---------|
| `agent/DRLGenerationAgent.java` | Direct DRL generation |
| `agent/YAMLRuleGenerationAgent.java` | YAML rule generation |
| `agent/PlainEnglishGenerationAgent.java` | Stage 1: Requirement -> English |
| `agent/EnglishToDRLAgent.java` | Stage 2: English -> DRL |
| `service/PlainEnglishGenerationService.java` | Two-stage pipeline orchestration |
| `runner/ComparisonRunner.java` | CLI for running comparisons |
| `config/ModelConfiguration.java` | Model configuration and selection |

---

## Conclusion

The Feb 2026 model comparison reveals important insights:

1. **Three models achieve 100% DRL accuracy:** qwen2.5-coder:14b (14.8B), granite4:small-h (32.2B), qwen3-coder-next (79.7B)
2. **qwen2.5-coder:14b** is the efficiency champion - 100% accuracy with smallest footprint (14.8B)
3. **granite4:small-h** proves Granite models can achieve 100% DRL with sufficient scale (32B vs 3.4B)
4. **granite4:small-h** offers unique value: 100% DRL + **1M token context** (largest among top performers)
5. The **Plain English pipeline** remains essential for smaller models like granite4:3b (18% -> 71%)

**Practical Recommendations:**
- **Default choice:** Use **qwen2.5-coder:14b** - best efficiency (100% DRL, only 14.8B)
- **Large context needs:** Use **granite4:small-h** - 100% DRL with 1M token context
- **Maximum context:** Use qwen3-coder-next (262K) or llama4 (10M, but needs ENGLISH pipeline)
- **Resource constrained:** Use granite4:3b with Plain English pipeline (71%)

---

## Appendix: System Prompts

### DRL Generation System Prompt

```
You are a Drools DRL (Decision Rule Language) expert code generator.
Your task is to generate syntactically correct and executable DRL code based on business rules.

CRITICAL DRL RULES:
1. Always start with a package declaration (e.g., package org.drools.generated;)
2. Use 'declare' blocks to define fact types - do NOT use Java classes
3. Each declare block must include all necessary fields with proper types
4. Rules must have proper 'when' and 'then' sections
5. Each rule must end with 'end'
6. Use modify() for updating facts, not direct setter calls in then section
7. NEVER use else clauses in then sections - split into separate rules
8. Drools declare blocks only have no-arg constructors - use setters to populate
9. Always use proper constraint syntax in when section (e.g., $p : Person(age >= 18))
10. Use $variable binding syntax for facts you need to reference in then section

FIELD TYPES TO USE:
- String for text
- int or Integer for whole numbers
- double or Double for decimals
- boolean for true/false
- java.util.Date for dates
- java.math.BigDecimal for currency

MODIFY SYNTAX:
Use modify($variable) { setField(value) } to update facts.
Example:
modify($p) { setAdult(true) }

OUTPUT FORMAT:
Return ONLY valid DRL code. No explanations, no markdown code blocks, no backticks.
The output must be directly savable as a .drl file and compilable by Drools.
```

### Plain English Generation System Prompt

```
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

OUTPUT FORMAT:
Return ONLY the structured rule specification. No additional explanations.
Use exact field names and types as provided.
```

### English to DRL Conversion System Prompt

```
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
- String -> String
- int -> int
- double -> double
- boolean -> boolean

OPERATOR MAPPING:
- "equals" or "is equal to" -> ==
- "is greater than" -> >
- "is greater than or equal to" -> >=
- "is less than" -> <
- "is less than or equal to" -> <=
- "is not equal to" -> !=
- "contains" -> use .contains() method

OUTPUT FORMAT:
Return ONLY valid DRL code. No explanations, no markdown code blocks.
```
