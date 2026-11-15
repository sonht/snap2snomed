package org.snomed.snap2snomed.classification.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.snomed.snap2snomed.classification.model.MappingSourceItem;
import org.snomed.snap2snomed.classification.repository.MappingAgentJobRepository;
import org.snomed.snap2snomed.classification.repository.MappingAgentRunRepository;
import org.snomed.snap2snomed.classification.repository.MappingProposalRepository;
import org.snomed.snap2snomed.classification.repository.MappingSourceItemRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AgentWorkerService {

  private final MappingAgentJobRepository jobRepository;
  private final MappingAgentRunRepository runRepository;
  private final MappingProposalRepository proposalRepository;
  private final MappingSourceItemRepository sourceItemRepository;
  private final ObjectMapper objectMapper;

  @Transactional
  public MappingAgentJob claimNext(String workerId) {
    List<MappingAgentJob> queued =
        jobRepository.findQueueForUpdate(AgentJobStatus.READY, PageRequest.of(0, 1));
    if (queued.isEmpty()) {
      return null;
    }

    MappingAgentJob job = queued.get(0);
    job.setStatus(AgentJobStatus.CLAIMED);
    job.setClaimedBy(workerId);
    job.setClaimToken(UUID.randomUUID().toString());
    job.setStarted(Instant.now());
    job.setHeartbeat(Instant.now());
    return jobRepository.save(job);
  }

  @Transactional
  public MappingAgentJob heartbeat(Long jobId, String claimToken) {
    MappingAgentJob job = requireClaim(jobId, claimToken);
    if (job.getStatus() == AgentJobStatus.CLAIMED) {
      job.setStatus(AgentJobStatus.RUNNING);
    }
    job.setHeartbeat(Instant.now());
    return jobRepository.save(job);
  }

  @Transactional
  public MappingAgentJob submit(Long jobId, String claimToken, SubmitResult result) throws Exception {
    MappingAgentJob job = requireClaim(jobId, claimToken);
    Instant now = Instant.now();

    MappingAgentRun run = MappingAgentRun.builder()
        .job(job)
        .role(job.getRole())
        .runtimeType(result.runtimeType == null ? "CLI" : result.runtimeType)
        .runtimeName(result.runtimeName)
        .runtimeVersion(result.runtimeVersion)
        .promptVersion(job.getPromptVersion())
        .inputHash(sha256(job.getInputPayload()))
        .outputJson(result.outputJson)
        .status(AgentRunStatus.COMPLETED)
        .started(job.getStarted() == null ? now : job.getStarted())
        .finished(now)
        .build();
    run = runRepository.save(run);

    int proposalCount = importProposals(run, result.outputJson);

    job.setResultPayload(result.outputJson);
    job.setFinished(now);
    job.setHeartbeat(now);
    job.setStatus(proposalCount > 0 ? AgentJobStatus.NEEDS_REVIEW : AgentJobStatus.COMPLETED);
    job.setClaimToken(null);
    return jobRepository.save(job);
  }

  @Transactional
  public MappingAgentJob fail(Long jobId, String claimToken, String errorMessage) {
    MappingAgentJob job = requireClaim(jobId, claimToken);
    job.setStatus(AgentJobStatus.FAILED);
    job.setErrorMessage(errorMessage == null ? "Worker failed" :
        errorMessage.substring(0, Math.min(errorMessage.length(), 4096)));
    job.setFinished(Instant.now());
    job.setClaimToken(null);
    return jobRepository.save(job);
  }

  private MappingAgentJob requireClaim(Long jobId, String claimToken) {
    return jobRepository.findByIdAndClaimToken(jobId, claimToken)
        .orElseThrow(() -> new IllegalArgumentException("Invalid or expired job claim"));
  }

  private int importProposals(MappingAgentRun run, String outputJson) throws Exception {
    if (outputJson == null || outputJson.isBlank()) {
      return 0;
    }

    JsonNode root = objectMapper.readTree(outputJson);
    JsonNode proposals = root.path("proposals");
    if (!proposals.isArray()) {
      return 0;
    }

    List<MappingProposal> entities = new ArrayList<>();
    for (JsonNode item : proposals) {
      long sourceItemId = item.path("source_item_id").asLong(0);
      if (sourceItemId == 0) {
        continue;
      }

      MappingSourceItem source = sourceItemRepository.findById(sourceItemId)
          .orElseThrow(() -> new IllegalArgumentException("Unknown source_item_id " + sourceItemId));

      MappingRelation relation = MappingRelation.valueOf(item.path("mapping_relation").asText());
      MappingProposal proposal = MappingProposal.builder()
          .sourceItem(source)
          .agentRun(run)
          .cmCode(textOrNull(item, "cm_code"))
          .cmName(textOrNull(item, "cm_name"))
          .mappingRelation(relation)
          .confidence(textOrNull(item, "confidence"))
          .mappingNotes(textOrNull(item, "mapping_notes"))
          .status(ProposalStatus.PROPOSED)
          .build();
      entities.add(proposal);
    }

    proposalRepository.saveAll(entities);
    return entities.size();
  }

  private String textOrNull(JsonNode node, String name) {
    JsonNode value = node.get(name);
    return value == null || value.isNull() ? null : value.asText();
  }

  private String sha256(String value) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
    StringBuilder out = new StringBuilder();
    for (byte b : hash) out.append(String.format("%02x", b));
    return out.toString();
  }

  public static class SubmitResult {
    public String runtimeType;
    public String runtimeName;
    public String runtimeVersion;
    public String outputJson;
  }
}
