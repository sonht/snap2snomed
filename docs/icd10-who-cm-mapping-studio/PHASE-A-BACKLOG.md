# Phase A backlog - ICD-10 WHO -> ICD-10-CM Mapping Studio

## Epic A1 - Project profile and schema

### A1.1 Project type/profile
- Add MappingProjectType
- Add source/target system + version metadata
- Add mapping profile
- Preserve existing projects with safe defaults

Acceptance:
- existing project tests pass
- ICD10_WHO_TO_ICD10_CM project can be created

### A1.2 Mapping source term schema
- V22 migration
- MappingSourceTerm entity/repository
- SourceTermType enum
- preserve parent term links
- immutable source text

Acceptance:
- PREFERRED/INCLUDE/EXCLUDE/INDEX import round trip
- parent index hierarchy test

## Epic A2 - Import

### A2.1 WHO term importer
Input contract:
- who_code
- term_id
- term_type
- term_text
- parent_term_id
- source_location
- source_order

Validation:
- missing source code
- duplicate term id
- invalid type
- broken parent
- orphan index modifier

### A2.2 ICD-10-CM target adapter
Use target terminology abstraction, not WHO-specific code in controller.
Expose target lookup/search/descendant operations through provider interface.

## Epic A3 - Candidate engine

### A3.1 Candidate schema
- V23 migration
- MappingCandidate entity
- per-component scores
- evidence source

### A3.2 Deterministic candidate generators
Implement strategies:
1. ExactCodeFamilyStrategy
2. SameCategoryStrategy
3. TargetIndexStrategy
4. NormalizedTextStrategy
5. TokenSimilarityStrategy

Return bounded top-N set.

### A3.3 Candidate ranking
- stable deterministic ordering
- no random scoring
- persist scoring components
- configurable N

## Epic A4 - Proposal/review

### A4.1 Proposal schema
- V24 migration
- MappingProposal
- MappingEvidence
- ProposalStatus
- RiskLevel

### A4.2 Review endpoints
- candidate generate
- proposal list
- approve
- reject
- no-map

### A4.3 Approval bridge
On approval:
- create/update MapRowTarget
- copy approved relationship
- store provenance link

Must not bypass existing map row workflow constraints.

## Epic A5 - UI

### A5.1 Project setup
Route:
- /classification-project/:projectid

### A5.2 Import inspector
Show:
- term counts
- type counts
- invalid rows
- duplicate IDs
- broken parent links

### A5.3 Mapping workbench
Three-column design:
LEFT: WHO context
CENTER: candidate list
RIGHT: evidence + decision

Actions:
- approve
- reject
- no map
- needs expert review

### A5.4 Review queue
Filters:
- source type
- chapter
- risk
- status
- confidence band

## Epic A6 - QA and regression

### A6.1 Unit tests
- normalization
- hierarchy candidate logic
- relationship preservation
- EXCLUDE safety

### A6.2 Integration tests
- import -> candidate -> approve -> MapRowTarget
- existing mapping workflows remain unchanged

### A6.3 Gold fixtures
Add a small fixture dataset spanning:
- infectious disease
- neoplasm
- diabetes
- obstetrics
- injury
- poisoning
- laterality

No production auto-accept until gold fixture tests exist.

## Suggested implementation order

1. A1.1
2. A1.2
3. A2.1
4. A2.2
5. A3.1
6. A3.2
7. A3.3
8. A4.1
9. A4.2
10. A4.3
11. A5.1-A5.4
12. A6.1-A6.3

## Pull request strategy

PR 1 - schema and project profile
PR 2 - WHO source term import
PR 3 - target terminology adapter + deterministic candidates
PR 4 - proposal/review/approval bridge
PR 5 - Angular mapping workbench
PR 6 - regression + gold fixtures

Do not combine AI agents into Phase A PRs.
Phase A creates the stable deterministic substrate first.
