package org.snomed.snap2snomed.classification.agent;

public enum AgentJobStatus {
  READY,
  CLAIMED,
  RUNNING,
  VALIDATING,
  COMPLETED,
  FAILED,
  NEEDS_REVIEW
}
