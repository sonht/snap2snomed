# Terminology Agent Runtime

## Design goal

Run AI-assisted terminology mapping automatically without embedding an OpenAI, Anthropic, Gemini, or other cloud model API into Snap2SNOMED.

The application owns workflow and truth. The external agent owns reasoning.

## Components

### 1. Snap2SNOMED backend

Persists:
- mapping_source_item
- mapping_agent_job
- mapping_agent_run
- mapping_proposal

Creates jobs with:
- job type
- role
- prompt version
- immutable input payload
- priority

### 2. Worker boundary

The standalone worker:
- authenticates with a machine token
- claims one READY job
- sends heartbeat
- invokes a configured local CLI command
- validates JSON syntax
- submits result or failure

The worker has no DB credentials.

### 3. CLI runtime

Examples:
- Codex CLI
- Claude Code CLI
- another future CLI agent
- local model wrapper

No runtime-specific logic is stored in Snap2SNOMED business services.

## Job state machine

READY -> CLAIMED -> RUNNING -> NEEDS_REVIEW

Failure:
RUNNING -> FAILED

Future phases may insert:
VALIDATING -> COMPLETED

## Roles

- TERMINOLOGY_RECONSTRUCTOR
- CANDIDATE_RANKER
- MAPPING_AGENT
- CODING_RULES_REVIEWER
- ADVERSARIAL_REVIEWER
- RELEASE_QA

## Security

User endpoints keep JWT authentication.

Only /agent-worker/** bypasses JWT, and these endpoints require X-Agent-Worker-Token.

Worker permissions are intentionally narrow:
- claim
- heartbeat
- submit result
- submit failure

The worker cannot:
- edit project membership
- approve mappings
- publish terminology
- write MapRowTarget directly

## Proposal contract

Agent results are stored as MappingProposal.

A proposal is not an approved mapping.

Required proposal keys:
- source_item_id
- mapping_relation

Optional when applicable:
- cm_code
- cm_name
- confidence
- mapping_notes

Allowed relations:
- EXACT
- CM_NARROWER
- CM_BROADER
- RELATED
- MULTIPLE
- NO_MATCH
- EXCLUDED_RELATION

## Source terminology boundary

Source items are separated into:

TERM:
- PREFERRED
- INCLUDE
- EXCLUDE
- INDEX

INSTRUCTION:
- NOTE
- USE_NOTE
- IF_DESIRED_NOTE
- SEE_NOTE
- DAGGER_ASTERISK_NOTE
- OTHER

Only TERM items are mapping eligible by default.

INSTRUCTION items are supplied as reasoning context and evidence.

Rule:
**Map terms; interpret notes; never map notes blindly.**
