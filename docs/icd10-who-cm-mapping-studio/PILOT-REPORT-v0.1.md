# Pilot report - WHO ICD-10 rubric terms -> ICD-10-CM 2026

## Scope

Pilot size: 47 mapping rows.

Distribution:
- 20 PREFERRED
- 10 INCLUDE
- 5 EXCLUDE
- 12 INDEX

Target:
- Icd10cm_order_2026.txt
- 2026 ICD-10-CM long descriptions

WHO source basis:
- WHO 2019 preferred-code backbone from project data
- WHO Tabular List inclusion/exclusion examples
- WHO Alphabetical Index hierarchical expressions

The pilot is a design validation dataset, not yet a production release.

## Key findings

### 1. The unit of work must be a term, not a code

A02.2 demonstrates the problem clearly.

WHO preferred:
- A02.2 Localized Salmonella infections

WHO inclusion/index expressions include:
- Salmonella meningitis
- Salmonella pneumonia
- Salmonella arthritis
- Salmonella osteomyelitis

ICD-10-CM 2026 separates these:
- A0221 Salmonella meningitis
- A0222 Salmonella pneumonia
- A0223 Salmonella arthritis
- A0224 Salmonella osteomyelitis

Therefore a code-to-code crosswalk would lose useful specificity.

### 2. Index fragments must be reconstructed before mapping

Examples:

Printed hierarchical path:
- Arthritis
  - in
    - typhoid fever

Reconstructed source expression:
- Typhoid arthritis

Target:
- A0104 Typhoid arthritis

Another example:

- Fever
  - typhoid
    - with
      - gastro-intestinal perforation

Reconstructed:
- Typhoid fever with gastrointestinal perforation

Target:
- A0109 Typhoid fever with other complications

Relationship:
- L3_CM_BROADER

### 3. WHO source type must survive mapping

The phrase "Salmonella meningitis" can occur as an INCLUDE and as an INDEX expression.
Both may map to A0221, but the two source records must remain distinct.

Canonicalization may cluster them later; it must not destroy provenance.

### 4. INCLUDE is not automatically a synonym

Example:
- WHO A02.0 INCLUDE: Salmonellosis
- ICD-10-CM A020: Salmonella enteritis

Because the raw term "Salmonellosis" is broader than "Salmonella enteritis", the pilot assigns:
- L2_CM_NARROWER

WHO rubric context explains why the term is classified under A02.0, but semantic granularity must still be recorded.

### 5. EXCLUDE requires a negative mapping relation

Example:
- owning WHO rubric: A04.6 Enteritis due to Yersinia enterocolitica
- EXCLUDE term: Extraintestinal yersiniosis
- referenced WHO code: A28.2
- ICD-10-CM target: A282 Extraintestinal yersiniosis

Master representation:
- ICD10_CODE = A04.6
- ICD10_RUBRIC_NAME = Extraintestinal yersiniosis
- ICD10_RUBRIC_TYPE = EXCLUDE
- ICD10CM_CODE = A282
- MAPPING_LEVEL = L7_EXCLUDED_RELATION

The target is evidence about the excluded condition. It must never become an enrichment synonym of A04.6.

### 6. CM category/subcategory terms are valid mapping targets

The project maps terminology/concepts, not only reportable billing codes.

Examples:
- WHO Typhogastric fever -> A010 Typhoid fever
- WHO Infection due to Salmonella typhi -> A010 Typhoid fever

A010 may be a parent term in the CM hierarchy, but remains the correct semantic target when CM does not require a more specific concept.

The target importer must therefore retain all rows from Icd10cm_order_2026.txt, not only terminal codes.

### 7. One CM target can intentionally collect multiple WHO expressions

Examples:
- Abortive typhoid fever -> A010 Typhoid fever
- Hemorrhagic typhoid fever -> A010 Typhoid fever

These are L3_CM_BROADER because the source expressions carry qualifiers that the CM target does not preserve.

Similarly:
- Typhoid endocarditis -> A0102 Typhoid fever with heart involvement
- Typhoid myocarditis -> A0102 Typhoid fever with heart involvement

This is not duplicate data. It is many source expressions mapping into one broader CM concept.

## Mapping level distribution in pilot

- L1_EXACT: 36
- L2_CM_NARROWER: 1
- L3_CM_BROADER: 5
- L7_EXCLUDED_RELATION: 5

The pilot intentionally does not force examples into L4_RELATED, L5_MULTIPLE or L6_NO_MATCH. Those should be added in the next gold-set expansion.

## Required schema amendments before implementation

The internal table must preserve, at minimum:

- source_term_id
- who_code
- rubric_title
- rubric_type
- raw_term
- reconstructed_term
- reconstruction_method
- lead_term
- modifier_path
- index_depth
- referenced_who_code (especially for EXCLUDE)
- cm_code
- cm_name
- mapping_level
- mapping_confidence
- review_status
- mapping_notes

The human export remains the agreed eight columns.

## Implementation gate

Do not begin AI auto-mapping until:

1. The 8-column master contract is frozen.
2. Raw term and reconstructed term are stored separately.
3. Index path reconstruction has deterministic tests.
4. EXCLUDE negative relations have tests.
5. Non-terminal CM targets are supported.
6. Duplicate clinical expressions across rubric types preserve provenance.
7. A gold set includes examples for every mapping level L1-L7.
