package org.snomed.snap2snomed.classification.agent;

import org.snomed.snap2snomed.security.WebSecurity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/classification-mapping/agent-jobs")
public class AgentJobRestController {

  private final AgentJobService agentJobService;
  private final WebSecurity webSecurity;

  public AgentJobRestController(AgentJobService agentJobService, WebSecurity webSecurity) {
    this.agentJobService = agentJobService;
    this.webSecurity = webSecurity;
  }

  @PostMapping
  public ResponseEntity<?> create(@RequestBody AgentJobService.CreateJobRequest request) {
    if (request.projectId == null || request.jobType == null || request.role == null ||
        request.promptVersion == null || request.promptVersion.isBlank() ||
        request.inputPayload == null || request.inputPayload.isBlank()) {
      return ResponseEntity.badRequest().body("Missing required agent job fields");
    }

    if (!webSecurity.isAdminUser() && !webSecurity.isProjectOwnerForId(request.projectId)) {
      return ResponseEntity.status(403).body("Only project owners or admins can create agent jobs");
    }

    MappingAgentJob job = agentJobService.create(request);
    return ResponseEntity.ok(java.util.Map.of(
        "job_id", job.getId(),
        "status", job.getStatus().name()
    ));
  }
}
