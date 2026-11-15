# Terminology Agent Worker

This worker lets Snap2SNOMED use an external AI agent runtime without embedding a cloud AI API in the application.

## Boundary

Snap2SNOMED owns:
- authoritative terminology data
- job orchestration
- source/candidate payloads
- review state
- approved mappings

The worker owns:
- invoking a locally configured CLI agent
- collecting structured reasoning output
- returning proposals

The worker cannot approve a mapping. Submitted results become `mapping_proposal` rows and jobs move to `NEEDS_REVIEW`.

## Authentication

Configure the backend with:

```bash
export SNAP2SNOMED_AGENT_WORKER_TOKEN='use-a-long-random-secret'
```

Spring relaxed binding maps this to:

`snap2snomed.agent-worker.token`

Configure the worker with the same secret:

```bash
export SNAP2SNOMED_AGENT_TOKEN='use-a-long-random-secret'
```

This machine token is separate from end-user JWT authentication.

## Codex CLI example

Example only; use the CLI invocation supported by the installed Codex version:

```bash
export SNAP2SNOMED_API_URL='http://127.0.0.1:8080'
export SNAP2SNOMED_AGENT_TOKEN='...'
export TERMINOLOGY_AGENT_COMMAND='codex exec -'
export TERMINOLOGY_AGENT_RUNTIME='CODEX_CLI'
python3 agent-worker/worker.py
```

## Claude Code example

Example only; use the CLI invocation supported by the installed Claude version:

```bash
export TERMINOLOGY_AGENT_COMMAND='claude -p'
export TERMINOLOGY_AGENT_RUNTIME='CLAUDE_CODE'
python3 agent-worker/worker.py
```

The application does not depend on either CLI. Replacing the runtime does not change the job contract.

## Job flow

```
READY
  -> CLAIMED
  -> RUNNING
  -> agent CLI
  -> structured JSON validation
  -> MappingProposal
  -> NEEDS_REVIEW
  -> human/rules approval later
```

On runtime failure:

```
RUNNING -> FAILED
```

## Output contract

The CLI must return JSON only:

```json
{
  "proposals": [
    {
      "source_item_id": 123,
      "cm_code": "A0101",
      "cm_name": "Typhoid meningitis",
      "mapping_relation": "EXACT",
      "confidence": "HIGH",
      "mapping_notes": "recon=INDEX_PATH; evidence=exact target term; review=needs_review"
    }
  ]
}
```

Supported mapping relations:
- EXACT
- CM_NARROWER
- CM_BROADER
- RELATED
- MULTIPLE
- NO_MATCH
- EXCLUDED_RELATION

## Safety properties

- The worker never receives database credentials.
- It cannot write directly to `MapRowTarget`.
- It cannot approve proposals.
- Raw source terminology remains immutable.
- Every run stores runtime identity, prompt version, input hash and output.
- A failed or malformed CLI response does not create approved mappings.
