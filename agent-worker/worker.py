#!/usr/bin/env python3
"""
Terminology Agent Worker.

No cloud AI API is used by this worker. It claims jobs from Snap2SNOMED,
invokes a locally configured CLI agent process, and submits structured JSON
results back to the application.

Environment:
  SNAP2SNOMED_API_URL          e.g. http://127.0.0.1:8080
  SNAP2SNOMED_AGENT_TOKEN      shared machine token
  SNAP2SNOMED_WORKER_ID        optional, defaults to hostname
  TERMINOLOGY_AGENT_COMMAND    command invoked for reasoning
                               e.g. "codex exec -"
                               or "claude -p"
  TERMINOLOGY_AGENT_PROMPT     path to master prompt markdown
  TERMINOLOGY_AGENT_RUNTIME    optional label, e.g. CODEX_CLI
  TERMINOLOGY_AGENT_VERSION    optional version label
  TERMINOLOGY_AGENT_POLL_SEC   optional, defaults to 15
  TERMINOLOGY_AGENT_WORKDIR    optional, defaults to ./work
"""

import json
import os
import shlex
import socket
import subprocess
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path


API_URL = os.environ.get("SNAP2SNOMED_API_URL", "http://127.0.0.1:8080").rstrip("/")
TOKEN = os.environ.get("SNAP2SNOMED_AGENT_TOKEN", "")
WORKER_ID = os.environ.get("SNAP2SNOMED_WORKER_ID", socket.gethostname())
AGENT_COMMAND = os.environ.get("TERMINOLOGY_AGENT_COMMAND", "")
PROMPT_PATH = os.environ.get(
    "TERMINOLOGY_AGENT_PROMPT",
    "docs/icd10-who-cm-mapping-studio/MASTER-MAPPING-PROMPT-v0.2.md",
)
RUNTIME_NAME = os.environ.get("TERMINOLOGY_AGENT_RUNTIME", "CLI_AGENT")
RUNTIME_VERSION = os.environ.get("TERMINOLOGY_AGENT_VERSION", "")
POLL_SEC = int(os.environ.get("TERMINOLOGY_AGENT_POLL_SEC", "15"))
WORKDIR = Path(os.environ.get("TERMINOLOGY_AGENT_WORKDIR", "./work"))


def request(method, path, body=None, claim_token=None):
    headers = {
        "X-Agent-Worker-Token": TOKEN,
        "X-Agent-Worker-Id": WORKER_ID,
        "Content-Type": "application/json",
    }
    if claim_token:
        headers["X-Agent-Claim-Token"] = claim_token

    data = None if body is None else json.dumps(body).encode("utf-8")
    req = urllib.request.Request(
        API_URL + path,
        data=data,
        headers=headers,
        method=method,
    )
    try:
        with urllib.request.urlopen(req, timeout=60) as response:
            if response.status == 204:
                return None
            raw = response.read().decode("utf-8")
            return json.loads(raw) if raw else None
    except urllib.error.HTTPError as exc:
        detail = exc.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"HTTP {exc.code} {path}: {detail}") from exc


def load_prompt():
    path = Path(PROMPT_PATH)
    if not path.exists():
        raise RuntimeError(f"Master prompt not found: {path}")
    return path.read_text(encoding="utf-8")


def build_agent_input(job, prompt):
    contract = """
OUTPUT CONTRACT
Return valid JSON only. Do not wrap JSON in markdown.

Schema:
{
  "proposals": [
    {
      "source_item_id": 123,
      "cm_code": "A0101",
      "cm_name": "Typhoid meningitis",
      "mapping_relation": "EXACT",
      "confidence": "HIGH",
      "mapping_notes": "recon=INDEX_PATH; semantic_kind=DISEASE_OR_CONDITION; ..."
    }
  ]
}

mapping_relation MUST be one of:
EXACT, CM_NARROWER, CM_BROADER, RELATED, MULTIPLE, NO_MATCH, EXCLUDED_RELATION.

For NO_MATCH, cm_code and cm_name may be null.
Never invent source_item_id values.
"""
    return (
        prompt
        + "\n\n"
        + contract
        + "\n\nJOB METADATA\n"
        + json.dumps(
            {
                "job_id": job["job_id"],
                "job_type": job["job_type"],
                "role": job["role"],
                "prompt_version": job["prompt_version"],
            },
            ensure_ascii=False,
            indent=2,
        )
        + "\n\nINPUT PAYLOAD\n"
        + job["input_payload"]
    )


def invoke_agent(agent_input, job_dir):
    if not AGENT_COMMAND.strip():
        raise RuntimeError("TERMINOLOGY_AGENT_COMMAND is not configured")

    input_file = job_dir / "agent-input.txt"
    output_file = job_dir / "agent-output.json"
    input_file.write_text(agent_input, encoding="utf-8")

    command = shlex.split(AGENT_COMMAND)
    result = subprocess.run(
        command,
        input=agent_input,
        text=True,
        capture_output=True,
        cwd=str(job_dir),
        timeout=3600,
        check=False,
    )

    (job_dir / "agent-stderr.log").write_text(result.stderr or "", encoding="utf-8")
    if result.returncode != 0:
        raise RuntimeError(
            f"Agent command exited {result.returncode}: {(result.stderr or '')[-2000:]}"
        )

    output = (result.stdout or "").strip()
    try:
        parsed = json.loads(output)
    except json.JSONDecodeError as exc:
        (job_dir / "agent-raw-output.txt").write_text(output, encoding="utf-8")
        raise RuntimeError(f"Agent did not return valid JSON: {exc}") from exc

    if not isinstance(parsed, dict) or not isinstance(parsed.get("proposals", []), list):
        raise RuntimeError("Agent JSON does not satisfy output contract")

    output_file.write_text(
        json.dumps(parsed, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    return json.dumps(parsed, ensure_ascii=False)


def process(job, prompt):
    job_id = job["job_id"]
    claim_token = job["claim_token"]
    job_dir = WORKDIR / f"job-{job_id}"
    job_dir.mkdir(parents=True, exist_ok=True)

    (job_dir / "job.json").write_text(
        json.dumps(job, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )

    request("POST", f"/agent-worker/{job_id}/heartbeat", claim_token=claim_token)

    try:
        agent_input = build_agent_input(job, prompt)
        output_json = invoke_agent(agent_input, job_dir)
        request(
            "POST",
            f"/agent-worker/{job_id}/result",
            body={
                "runtimeType": "CLI",
                "runtimeName": RUNTIME_NAME,
                "runtimeVersion": RUNTIME_VERSION,
                "outputJson": output_json,
            },
            claim_token=claim_token,
        )
        print(f"completed job {job_id}", flush=True)
    except Exception as exc:
        try:
            request(
                "POST",
                f"/agent-worker/{job_id}/fail",
                body={"error": str(exc)},
                claim_token=claim_token,
            )
        finally:
            print(f"failed job {job_id}: {exc}", file=sys.stderr, flush=True)


def main():
    if not TOKEN:
        raise SystemExit("SNAP2SNOMED_AGENT_TOKEN is required")
    prompt = load_prompt()
    WORKDIR.mkdir(parents=True, exist_ok=True)

    print(
        f"terminology-agent-worker id={WORKER_ID} api={API_URL} poll={POLL_SEC}s",
        flush=True,
    )

    while True:
        try:
            job = request("POST", "/agent-worker/claim")
            if job:
                process(job, prompt)
            else:
                time.sleep(POLL_SEC)
        except KeyboardInterrupt:
            return
        except Exception as exc:
            print(f"worker loop error: {exc}", file=sys.stderr, flush=True)
            time.sleep(POLL_SEC)


if __name__ == "__main__":
    main()
