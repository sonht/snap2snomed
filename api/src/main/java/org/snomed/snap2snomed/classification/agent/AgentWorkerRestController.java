package org.snomed.snap2snomed.classification.agent;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/agent-worker")
public class AgentWorkerRestController {

  private final AgentWorkerService workerService;

  @Value("${snap2snomed.agent-worker.token:}")
  private String configuredToken;

  public AgentWorkerRestController(AgentWorkerService workerService) {
    this.workerService = workerService;
  }

  @PostMapping("/claim")
  public ResponseEntity<?> claim(
      @RequestHeader("X-Agent-Worker-Token") String token,
      @RequestHeader(value = "X-Agent-Worker-Id", defaultValue = "worker") String workerId) {
    requireToken(token);
    MappingAgentJob job = workerService.claimNext(workerId);
    if (job == null) {
      return ResponseEntity.noContent().build();
    }

    java.util.Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("job_id", job.getId());
    payload.put("claim_token", job.getClaimToken());
    payload.put("job_type", job.getJobType());
    payload.put("role", job.getRole());
    payload.put("prompt_version", job.getPromptVersion());
    payload.put("input_payload", job.getInputPayload());
    return ResponseEntity.ok(payload);
  }

  @PostMapping("/{jobId}/heartbeat")
  public ResponseEntity<Void> heartbeat(
      @PathVariable Long jobId,
      @RequestHeader("X-Agent-Worker-Token") String token,
      @RequestHeader("X-Agent-Claim-Token") String claimToken) {
    requireToken(token);
    workerService.heartbeat(jobId, claimToken);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{jobId}/result")
  public ResponseEntity<Void> result(
      @PathVariable Long jobId,
      @RequestHeader("X-Agent-Worker-Token") String token,
      @RequestHeader("X-Agent-Claim-Token") String claimToken,
      @RequestBody AgentWorkerService.SubmitResult result) throws Exception {
    requireToken(token);
    workerService.submit(jobId, claimToken, result);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{jobId}/fail")
  public ResponseEntity<Void> fail(
      @PathVariable Long jobId,
      @RequestHeader("X-Agent-Worker-Token") String token,
      @RequestHeader("X-Agent-Claim-Token") String claimToken,
      @RequestBody(required = false) java.util.Map<String, String> body) {
    requireToken(token);
    workerService.fail(jobId, claimToken, body == null ? null : body.get("error"));
    return ResponseEntity.noContent().build();
  }

  private void requireToken(String token) {
    if (configuredToken == null || configuredToken.isBlank()) {
      throw new IllegalStateException("Agent worker token is not configured");
    }
    boolean valid = MessageDigest.isEqual(
        configuredToken.getBytes(StandardCharsets.UTF_8),
        token.getBytes(StandardCharsets.UTF_8));
    if (!valid) {
      throw new SecurityException("Invalid agent worker token");
    }
  }
}
