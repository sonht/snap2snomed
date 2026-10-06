package org.snomed.snap2snomed.classification.agent;

import java.util.Optional;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.snomed.snap2snomed.classification.repository.MappingAgentJobRepository;
import org.snomed.snap2snomed.model.Map;
import org.snomed.snap2snomed.model.Project;
import org.snomed.snap2snomed.repository.MapRepository;
import org.snomed.snap2snomed.repository.ProjectRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AgentJobService {

  private final MappingAgentJobRepository jobRepository;
  private final ProjectRepository projectRepository;
  private final MapRepository mapRepository;

  @Transactional
  public MappingAgentJob create(CreateJobRequest request) {
    Project project = projectRepository.findById(request.projectId)
        .orElseThrow(() -> new IllegalArgumentException("Unknown project " + request.projectId));

    Map map = null;
    if (request.mapId != null) {
      Optional<Map> mapOpt = mapRepository.findById(request.mapId);
      map = mapOpt.orElseThrow(() -> new IllegalArgumentException("Unknown map " + request.mapId));
      if (!map.getProject().getId().equals(project.getId())) {
        throw new IllegalArgumentException("Map does not belong to project");
      }
    }

    MappingAgentJob job = MappingAgentJob.builder()
        .project(project)
        .map(map)
        .jobType(request.jobType)
        .role(request.role)
        .status(AgentJobStatus.READY)
        .promptVersion(request.promptVersion)
        .inputPayload(request.inputPayload)
        .priority(request.priority == null ? 0 : request.priority)
        .build();

    return jobRepository.save(job);
  }

  public static class CreateJobRequest {
    public Long projectId;
    public Long mapId;
    public AgentJobType jobType;
    public AgentRole role;
    public String promptVersion;
    public String inputPayload;
    public Integer priority;
  }
}
