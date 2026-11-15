package org.snomed.snap2snomed.classification.agent;

import java.time.Instant;
import javax.persistence.*;
import lombok.*;
import org.snomed.snap2snomed.model.Snap2SnomedEntity;

@Entity
@Table(name = "mapping_agent_run")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MappingAgentRun implements Snap2SnomedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "job_id", nullable = false)
  private MappingAgentJob job;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 40)
  private AgentRole role;

  @Column(name = "runtime_type", nullable = false, length = 40)
  private String runtimeType;

  @Column(name = "runtime_name", length = 128)
  private String runtimeName;

  @Column(name = "runtime_version", length = 128)
  private String runtimeVersion;

  @Column(name = "prompt_version", nullable = false, length = 64)
  private String promptVersion;

  @Column(name = "input_hash", length = 128)
  private String inputHash;

  @Lob
  @Column(name = "output_json", columnDefinition = "LONGTEXT")
  private String outputJson;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private AgentRunStatus status;

  @Column(name = "started", nullable = false)
  private Instant started;

  @Column(name = "finished")
  private Instant finished;
}
