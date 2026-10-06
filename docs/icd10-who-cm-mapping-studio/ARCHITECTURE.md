# ICD-10 WHO -> ICD-10-CM Mapping Studio

## 1. Decision

This capability belongs primarily in `sonht/snap2snomed` as an AI-assisted mapping workbench.
`vnts-terminology-service` remains the runtime terminology registry and publication target.

The existing Snap2SNOMED domain model is retained:
- Project
- Task
- Map
- MapRow
- MapRowTarget
- ImportedCodeSet / ImportedCode
- author / review / reconcile workflow
- MappingRelationship
- FHIR terminology integration

AI proposals MUST NOT write directly to `MapRowTarget`.
AI output is stored in separate proposal/evidence tables and becomes a normal `MapRowTarget` only after deterministic validation and/or human approval.

## 2. Current codebase findings

Backend:
- Spring Boot 2.7.x / Maven
- JPA + Envers auditing
- Flyway migrations
- Existing controllers for projects, tasks, mappings and imported code sets
- Existing `CodeSetImportService`, `MappingService`, `MapViewService`, `FhirService`
- Existing `TerminologyClient` / `TerminologyProvider`

Frontend:
- Angular client under `ui/snapclient`
- Existing routes for map list, map view and map work
- Existing automap implementation using FHIR autosuggest
- Existing review-oriented UI model

Existing `MappingRelationship` already supports:
- TARGET_EQUIVALENT
- TARGET_NARROWER
- TARGET_BROADER
- TARGET_INEXACT

This is compatible with the WHO -> CM use case and should be preserved for approved mappings.

## 3. New project type

Introduce a project-level discriminator:

`MappingProjectType`
- SIMPLE_SNOMED
- ICD10_WHO_TO_ICD10_CM
- GENERIC_CODE_SYSTEM_TO_CODE_SYSTEM

Phase A may initially implement only `ICD10_WHO_TO_ICD10_CM` while keeping the enum extensible.

Project metadata for this type:
- source system URI
- source version
- target system URI
- target version
- mapping profile
- enabled source term types
- AI enabled flag
- auto-accept policy version

Example:
- source: ICD-10 WHO
- source version: 2019
- target: ICD-10-CM
- target version: FY2026
- profile: ICD10_WHO_CM_ENRICHMENT_V1

## 4. Source term model

Do not collapse all WHO content into one display field.

Add `MappingSourceTerm`:
- id
- map_id
- imported_code_id
- term_type
- term_text
- normalized_text
- parent_term_id nullable
- source_location
- source_order
- active
- metadata_json

`SourceTermType`:
- PREFERRED
- INCLUDE
- EXCLUDE
- INDEX
- NOTE

Important:
- EXCLUDE is never treated as a synonym.
- Parent/child links must preserve WHO Index indentation.
- Source text is immutable after import; normalized text is derived.

## 5. AI-assisted candidate model

### MappingCandidate
- id
- source_term_id
- target_code
- target_display
- candidate_rank
- candidate_source
- hierarchy_score
- lexical_score
- index_score
- semantic_score nullable
- attribute_score nullable
- contradiction_score nullable
- deterministic_score
- created_at

Candidate sources:
- EXACT_CODE_FAMILY
- SAME_CATEGORY
- TARGET_INDEX
- NORMALIZED_TEXT
- TOKEN_MATCH
- EMBEDDING
- MANUAL

### MappingProposal
- id
- source_term_id
- candidate_id nullable
- target_code
- target_display
- proposed_relationship
- final_score
- proposal_status
- risk_level
- generated_by
- agent_run_id nullable
- rationale_summary
- created_at
- updated_at

Proposal status:
- PROPOSED
- RULE_REJECTED
- NEEDS_REVIEW
- ACCEPTED
- REJECTED
- SUPERSEDED

Risk:
- LOW
- MEDIUM
- HIGH

## 6. Evidence and agent audit

### MappingEvidence
- id
- proposal_id
- evidence_type
- evidence_key
- evidence_value
- pass_flag nullable
- weight nullable

Evidence types:
- SAME_WHO_FAMILY
- EXACT_NORMALIZED_TERM
- WHO_INDEX_SUPPORT
- CM_INDEX_SUPPORT
- ATTRIBUTE_MATCH
- EXCLUSION_CONFLICT
- ACUITY_CONFLICT
- LATERALITY_CONFLICT
- CONGENITAL_ACQUIRED_CONFLICT
- OBSTETRIC_CONTEXT_CONFLICT
- PERINATAL_CONTEXT_CONFLICT
- COMBINATION_CODE_WARNING
- SEVENTH_CHARACTER_WARNING

### AgentRun
- id
- agent_type
- model_provider
- model_name
- model_version
- prompt_version
- input_hash
- output_json
- status
- tokens_in
- tokens_out
- latency_ms
- created_at

Agents:
1. TERMINOLOGY_PARSER
2. CANDIDATE_RANKER
3. CODING_RULES_REVIEWER
4. ADVERSARIAL_REVIEWER
5. DEDUPLICATION_REVIEWER
6. RELEASE_QA

Do not store hidden chain-of-thought. Store only structured rationale, evidence and decisions.

## 7. Deterministic pipeline

State flow:

INGESTED
-> NORMALIZED
-> CANDIDATES_READY
-> AI_PROPOSED
-> RULE_CHECKED
-> ADVERSARIAL_REVIEWED
-> HUMAN_REVIEW
-> APPROVED
-> CANONICALIZED
-> QA_PASSED
-> RELEASED

Candidate generation order:
1. exact target code prefix/family
2. descendant target codes
3. same 3-character category
4. target index hits
5. normalized lexical match
6. token match
7. embedding candidates
8. cross-category fallback

LLM is not allowed to search the entire target classification directly in normal mode.
It receives the source context plus a bounded candidate set.

## 8. ICD-10 specific safeguards

High-risk rules force human review:
- obstetric codes
- perinatal codes
- injury codes
- poisoning/adverse effect/underdosing
- external causes
- neoplasms with site/behavior distinctions
- laterality
- 7th character
- combination codes
- code-first/use-additional-code conventions
- etiology/manifestation pairs
- congenital vs acquired distinctions

WHO EXCLUDE terms:
- may be mapped to a target concept/code
- are stored as exclusion relationships
- must never be merged into the WHO code synonym cluster

## 9. Approved mapping behavior

Only approved mappings become normal Snap2SNOMED mapping targets.

Conversion:
`MappingProposal ACCEPTED`
-> create/update `MapRowTarget`
-> retain proposal id in provenance link

Add optional provenance extension table:
`MapRowTargetProvenance`
- map_row_target_id
- source_term_id
- proposal_id
- approval_type
- reviewer
- approved_at

Approval type:
- HUMAN
- AUTO_POLICY
- IMPORTED_GOLD

## 10. Backend packages

Add under:
`api/src/main/java/org/snomed/snap2snomed/`

`classification/`
- model/
- repository/
- service/
- controller/
- dto/
- rules/
- candidate/
- ai/
- release/

Recommended services:
- SourceTermImportService
- SourceTermNormalizationService
- CandidateGenerationService
- MappingProposalService
- MappingEvidenceService
- RiskAssessmentService
- MappingApprovalService
- CanonicalizationService
- MappingReleaseService
- AgentOrchestrationService

Keep existing `MappingService` responsible for approved mapping workflow.

## 11. Phase A REST API

Base:
`/api/classification-mapping`

Projects:
- POST /projects/{projectId}/initialize
- GET /projects/{projectId}/summary

Source terms:
- POST /maps/{mapId}/source-terms/import
- GET /maps/{mapId}/source-terms
- GET /source-terms/{termId}

Candidates:
- POST /source-terms/{termId}/candidates/generate
- POST /maps/{mapId}/candidates/generate
- GET /source-terms/{termId}/candidates

Proposals:
- POST /source-terms/{termId}/proposals/generate
- GET /source-terms/{termId}/proposals
- POST /proposals/{proposalId}/approve
- POST /proposals/{proposalId}/reject

Review queue:
- GET /maps/{mapId}/review-queue
  filters: risk, confidence, sourceTermType, status, chapter, disagreement

Phase A does not need release publishing yet.

## 12. Angular routes

Add:
- /classification-project/:projectid
- /classification-project/:projectid/source
- /classification-project/:projectid/mapping
- /classification-project/:projectid/review
- /classification-project/:projectid/quality

Phase A screens:

1. Project Setup
   - source / target terminology
   - versions
   - profile
   - term type selection

2. Import Inspector
   - counts
   - malformed rows
   - duplicate IDs
   - broken parent links

3. Mapping Workbench
   - source code context
   - source term
   - candidate list
   - deterministic evidence
   - AI proposal
   - approve / reject / no-map

4. Review Queue
   - filters
   - batch selection
   - disagreement indicator
   - risk badge

Reuse existing author/review/task concepts where possible.

## 13. Frontend structure

Under `ui/snapclient/src/app/`:

`classification-mapping/`
- project-setup/
- source-import/
- mapping-workbench/
- review-queue/
- quality/
- shared/

Services:
- classification-mapping.service.ts
- mapping-candidate.service.ts
- mapping-proposal.service.ts

Models:
- source-term.ts
- mapping-candidate.ts
- mapping-proposal.ts
- mapping-evidence.ts

Do not modify the existing SNOMED map-work component for Phase A.
Keep the new workflow isolated until behavior is stable.

## 14. Flyway

Current MySQL migrations are at V20.
Use new MySQL migrations beginning at V21.

Phase A:
- V21__classification_mapping_project.sql
- V22__mapping_source_term.sql
- V23__mapping_candidate.sql
- V24__mapping_proposal_and_evidence.sql
- V25__map_row_target_provenance.sql

All foreign keys must use explicit indexes.
Avoid JSON-only modeling for query-critical fields.

## 15. Candidate scoring

Keep component scores separately.

Initial deterministic formula:

final_deterministic_score =
- hierarchy: 0.30
- normalized lexical: 0.25
- target index support: 0.20
- clinical attribute match: 0.15
- exact code-family bonus: 0.10
- contradiction penalties: applied after positive score

AI score does not replace deterministic score.
AI agreement is an additional signal.

Auto-accept policy must be configurable and versioned.
Default Phase A behavior: no auto-accept.

## 16. Phase A acceptance criteria

Phase A is complete when:

1. A project can be configured as ICD-10 WHO 2019 -> ICD-10-CM.
2. WHO source terms can be imported with PREFERRED/INCLUDE/EXCLUDE/INDEX types.
3. Parent-child Index structure is preserved.
4. Candidate generation returns bounded ranked ICD-10-CM candidates.
5. Candidate scores/evidence are persisted.
6. A reviewer can approve/reject a proposal.
7. Approved proposal creates a normal MapRowTarget.
8. EXCLUDE terms cannot accidentally become synonyms.
9. Every approval has provenance.
10. Existing SNOMED mapping behavior remains regression-green.

## 17. Non-goals for Phase A

Not yet:
- autonomous bulk approval
- canonical dedup clusters
- multi-agent disagreement orchestration
- FHIR ConceptMap publication
- VNTS production publishing
- model benchmarking
- prompt self-improvement

Those belong to Phases B-D.

## 18. VNTS integration boundary

Snap2SNOMED owns:
- import
- candidate generation
- AI proposals
- human review
- QA
- release preparation

VNTS owns:
- released terminology versions
- CodeSystem
- ConceptMap
- ValueSet
- search
- lookup
- translate
- expand
- provenance of published releases

No AI agent writes directly to VNTS production.
