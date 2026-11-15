# Master Mapping Specification
## ICD-10 WHO 2019 rubric terms -> ICD-10-CM terms

Version: 0.1
Status: Design baseline

## 1. Purpose

This specification defines the canonical row-level master used to map ICD-10 WHO 2019 rubric-associated terms to ICD-10-CM disease terms.

It is intentionally term-centric, not code-centric.

WHO ICD-10 terminology contains:
- rubric title / preferred term
- inclusion terms
- exclusion terms
- Alphabetical Index terms

Many WHO inclusion, exclusion and index entries are printed in abbreviated or hierarchical form and only become clinically complete when interpreted with:
- the rubric title,
- a lead term,
- one or more modifiers,
- or the indentation path of the printed index.

ICD-10-CM target descriptions, in contrast, are typically complete disease/condition phrases attached to a category, subcategory, or code.

Therefore the mapping pipeline MUST reconstruct a clinically complete WHO source term before matching it to an ICD-10-CM target term.

## 2. WHO source principles

WHO Volume 2 states that:
- a rubric is a 3-character category or 4-character subcategory;
- inclusion terms are examples of diagnostic statements classified to a rubric;
- inclusion terms can represent different conditions or synonyms;
- they are not a subclassification;
- inclusion lists are not exhaustive;
- some inclusion terms must be read together with the rubric title.

WHO Volume 3 states that:
- the Alphabetical Index is an essential adjunct to the Tabular List;
- it contains many diagnostic terms not present in Volume 1;
- the Index and Tabular List must be used together;
- index wording may be incomplete without hierarchical context.

These principles drive the source reconstruction model below.

## 3. Canonical output required by the project

The human-facing master export MUST contain exactly these core columns, in this order:

| Column | Required | Meaning |
|---|---|---|
| STT | Yes | Stable sequential/export identifier |
| ICD10_CODE | Yes | WHO ICD-10 code assigned to the source term |
| ICD10_RUBRIC_NAME | Yes | Clinically reconstructed WHO source term used for mapping |
| ICD10_RUBRIC_TYPE | Yes | WHO source term type |
| ICD10CM_CODE | Conditional | Mapped ICD-10-CM code; blank if no accepted map |
| ICD10CM_NAME | Conditional | Official ICD-10-CM target name |
| MAPPING_LEVEL | Yes | Semantic relationship / mapping level |
| MAPPING_NOTES | Yes | Compact evidence, ambiguity, reconstruction or review note |

Canonical header:

STT;ICD10_CODE;ICD10_RUBRIC_NAME;ICD10_RUBRIC_TYPE;ICD10CM_CODE;ICD10CM_NAME;MAPPING_LEVEL;MAPPING_NOTES

## 4. Meaning of ICD10_RUBRIC_NAME

Despite the export name, this field is NOT always the literal printed rubric title.

It is the clinically complete WHO source expression presented to the mapper.

### PREFERRED
Use the WHO rubric/category/subcategory title as printed.

Example pattern:
WHO code: Axx.x
Printed title: Complete disease phrase
ICD10_RUBRIC_NAME = printed title

### INCLUDE
If the printed inclusion term is already complete:
ICD10_RUBRIC_NAME = inclusion term

If the inclusion term depends on the rubric title:
ICD10_RUBRIC_NAME = reconstructed complete phrase

The original printed fragment MUST still be retained internally.

### EXCLUDE
Reconstruct the excluded condition as a complete clinical expression.

Do NOT treat EXCLUDE as a synonym of the owning WHO code.
The owning code is context/provenance; the excluded term may semantically belong elsewhere.

### INDEX
Reconstruct the complete index expression using:
lead term + ordered modifier path.

The flattened full expression is used for candidate generation.
The raw index hierarchy MUST be preserved internally.

## 5. Internal source model

The eight-column export is not sufficient to safely generate mappings.
Internally each WHO source row MUST retain:

- source_term_id
- who_code
- rubric_title
- rubric_type
- raw_term
- reconstructed_term
- lead_term nullable
- modifier_path nullable
- parent_term_id nullable
- index_depth nullable
- source_volume
- source_page_or_location nullable
- source_order
- reconstruction_method
- reconstruction_confidence
- source_notes nullable

### reconstruction_method
Allowed:
- AS_PRINTED
- RUBRIC_PREFIX
- RUBRIC_CONTEXT
- INDEX_PATH
- MANUAL
- AI_ASSISTED_REVIEWED

AI may propose reconstruction but MUST NOT overwrite raw source text.

## 6. ICD10_RUBRIC_TYPE controlled vocabulary

Use exactly:

- PREFERRED
- INCLUDE
- EXCLUDE
- INDEX

Do not mix:
- synonym
- preferred synonym
- note
- other

into this field.

If later required, NOTE should be modeled separately because it is an instruction/context object, not a mapping term.

## 7. ICD-10-CM target model

Target dataset currently supplied:
Icd10cm_order_2026.txt

Fields:
- RowID
- Long_description
- ICD10CM_Code

Internal normalized target fields:

- cm_row_id
- cm_code_raw
- cm_code_display
- cm_name
- cm_category
- cm_parent_code
- cm_code_depth
- cm_terminal_flag
- cm_version = 2026
- source_file

Never modify the official Long_description.

A display code containing a decimal may be derived for UI only.

## 8. Mapping levels

MAPPING_LEVEL must express semantic relation, not confidence.

Recommended controlled values:

### L1_EXACT
WHO reconstructed term and ICD-10-CM term denote the same clinical concept at effectively the same granularity.

### L2_CM_NARROWER
The ICD-10-CM term is more specific than the WHO source term.

Typical example:
WHO source lacks laterality/complication subtype, while CM adds it.

One WHO term may therefore map to multiple CM child concepts.

### L3_CM_BROADER
The ICD-10-CM target is broader than the WHO source expression.

This should be uncommon and normally requires review.

### L4_RELATED
Clinically related but not safely equivalent, narrower, or broader.

Do not use this level for automatic enrichment.

### L5_MULTIPLE
The WHO source expression requires more than one ICD-10-CM target or cannot be represented by one target alone.

The master may contain multiple rows sharing the same source_term_id/STT lineage.

### L6_NO_MATCH
No defensible ICD-10-CM term found.

### L7_EXCLUDED_RELATION
Used for WHO EXCLUDE entries when a CM target is identified.
This records where the excluded condition maps, while explicitly preventing synonym/enrichment behavior under the owning WHO code.

## 9. Mapping level vs confidence

Do not encode confidence inside MAPPING_LEVEL.

Store separately internally:

- mapping_confidence
- confidence_band
- deterministic_score
- ai_score nullable
- review_status

Suggested bands:
- HIGH
- MEDIUM
- LOW

Export confidence later if needed, but it is not part of the eight-column canonical user output.

## 10. Mapping notes standard

MAPPING_NOTES must be short but structured enough for audit.

Recommended semicolon-delimited pattern:

recon=<method>; evidence=<evidence>; issue=<issue>; review=<status>

Examples:

recon=INDEX_PATH; evidence=exact normalized term + same A01 family; review=approved

recon=RUBRIC_CONTEXT; evidence=CM child under same category; issue=CM adds complication specificity; review=needs_review

recon=INDEX_PATH; evidence=lexical only; issue=cross-category candidate; review=manual

recon=AS_PRINTED; issue=no defensible CM target; review=approved_no_match

For EXCLUDE:
recon=RUBRIC_CONTEXT; relation=excluded_from_owner; target=identified; review=approved

## 11. Mapping cardinality

Master MUST support:

### 1 -> 1
One WHO term maps to one CM term.

### 1 -> N
One WHO term maps to multiple narrower CM terms.

Do not deduplicate these away.

### N -> 1
Multiple WHO source terms map to the same CM term.

These are candidates for synonym/canonical clustering, but source rows remain preserved.

### N -> N
Allowed when source terms are context-dependent or overlapping.

## 12. STT rule

STT must identify the source mapping row stably.

Recommended:
- integer export sequence for human use
- immutable source_term_id internally

When one WHO source term maps to multiple CM targets:
- keep internal source_term_id identical
- export may use STT values such as 123, 123.1, 123.2 OR separate rows with a hidden parent STT.

Preferred machine-safe implementation:
- STT = unique integer per export row
- SOURCE_TERM_ID = hidden/internal stable UUID or bigint

Do not overload STT as the database primary key.

## 13. Deduplication rules

Deduplication happens AFTER mapping.

Never delete source rows.

Classify duplicates:

1. EXACT_SOURCE_DUPLICATE
   same code + type + normalized reconstructed term

2. CONTEXTUAL_DUPLICATE
   same normalized wording but different WHO code/type/context
   Do NOT auto-merge.

3. SAME_TARGET_CONCEPT
   distinct WHO rows map to same CM code
   candidate for canonical concept clustering

4. SAME_CLINICAL_CONCEPT_DIFFERENT_CM_SPECIFICITY
   requires human review

5. INDEX_PATH_VARIANT
   different index paths reconstruct to the same clinical phrase
   preserve paths, optionally cluster

## 14. Candidate generation contract

The candidate engine receives:

WHO:
- code
- rubric title
- type
- raw term
- reconstructed term
- hierarchy/path context

CM:
- bounded candidate set

Candidate generation order:
1. same code / code-prefix family
2. descendants of the WHO-equivalent CM family
3. same 3-character category
4. exact normalized term
5. target index support when available
6. lexical/token similarity
7. semantic similarity
8. cross-category fallback

AI MUST map the reconstructed term, not the isolated raw fragment.

## 15. Special handling by rubric type

### PREFERRED
Primary mapping target.
Eligible for exact/narrower/broader mapping.

### INCLUDE
Treat as an independent clinical source expression assigned to the WHO code.
May map to:
- same CM code as preferred
- a narrower CM code
- a different CM branch if CM reorganizes the concept

Do not assume INCLUDE = synonym.

### INDEX
Treat as terminology evidence.
The assigned WHO code is retained, but the clinical phrase is reconstructed from the full index path.
Index terms may be highly useful for finding a more specific CM code.

### EXCLUDE
Never enrich the owning WHO code with the excluded CM code.
Store it as a negative semantic relation.
If target is found, use L7_EXCLUDED_RELATION.

## 16. Example conceptual rows

| STT | ICD10_CODE | ICD10_RUBRIC_NAME | ICD10_RUBRIC_TYPE | ICD10CM_CODE | ICD10CM_NAME | MAPPING_LEVEL | MAPPING_NOTES |
|---:|---|---|---|---|---|---|---|
| 1 | A01.0 | Typhoid fever | PREFERRED | A010 | Typhoid fever | L1_EXACT | recon=AS_PRINTED; evidence=same family + exact concept |
| 2 | A01.0 | Typhoid meningitis | INDEX | A0101 | Typhoid meningitis | L1_EXACT | recon=INDEX_PATH; evidence=exact CM term |
| 3 | A01.0 | Typhoid fever with heart involvement | INDEX | A0102 | Typhoid fever with heart involvement | L1_EXACT | recon=INDEX_PATH; evidence=exact CM term |
| 4 | A01.0 | Typhoid fever with other complications | INDEX | A0109 | Typhoid fever with other complications | L1_EXACT | recon=INDEX_PATH; evidence=exact CM term |

These examples illustrate the intended structure only; final rows must be generated from the WHO 2019 source and verified against the target dataset.

## 17. Review rules

Human review is mandatory when:
- mapping crosses 3-character category unexpectedly
- CM target is broader
- one WHO term has multiple plausible CM branches
- laterality/episode/severity/complication meaning is introduced
- obstetric/perinatal/injury/poisoning/external-cause logic applies
- an EXCLUDE term is involved
- reconstruction confidence is not HIGH
- AI agents disagree

## 18. Golden principle

The project is not creating new WHO codes.

It is creating a term-level enrichment layer:

WHO code
+ WHO source terminology
+ reconstructed clinical expression
+ mapped ICD-10-CM concept/code
+ semantic mapping relation
+ provenance

The WHO code remains the authoritative statistical classification anchor.
The ICD-10-CM code is an extension/mapping target, not a replacement.
