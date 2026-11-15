package org.snomed.snap2snomed.classification.agent;

import java.time.Instant;
import javax.persistence.*;
import lombok.*;
import org.snomed.snap2snomed.model.Map;
import org.snomed.snap2snomed.model.Project;
import org.snomed.snap2snomed.model.Snap2SnomedEntity;

@Entity
@Table(name = "mapping_agent_job")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MappingAgentJob implements Snap2SnomedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "project_id", nullable = false)
  private Project project;

  @ManyToOne
  @JoinColumn(name = "map_id")
  private Map map;

  @Enumerated(EnumType.STRING)
  @Column(name = "job_type", nullable = false, length = 40)
  private AgentJobType jobType;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 40)
  private AgentRole role;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private AgentJobStatus status;

  @Column(name = "prompt_version", nullable = false, length = 64)
  private String promptVersion;

  @Lob
  @Column(name = "input_payload", nullable = false, columnDefinition = "LONGTEXT")
  private String inputPayload;

  @Lob
  @Column(name = "result_payload", columnDefinition = "LONGTEXT")
  private String resultPayload;

  @Column(name = "priority", nullable = false)
  private Integer priority;

  @Column(name = "claimed_by", length = 128)
  private String claimedBy;

  @Column(name = "claim_token", length = 64)
  private String claimToken;

  @Column(name = "created", nullable = false, updatable = false)
  private Instant created;

  @Column(name = "started")
  private Instant started;

  @Column(name = "heartbeat")
  private Instant heartbeat;

  @Column(name = "finished")
  private Instant finished;

  @Column(name = "error_message", length = 4096)
  private String errorMessage;

  @PrePersist
  void prePersist() {
    if (status == null) status = AgentJobStatus.READY;
    if (priority == null) priority = 0;
    if (created == null) created = Instant.now();
  }
}
