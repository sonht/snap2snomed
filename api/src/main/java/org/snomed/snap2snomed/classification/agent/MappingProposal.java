package org.snomed.snap2snomed.classification.agent;

import java.time.Instant;
import javax.persistence.*;
import lombok.*;
import org.snomed.snap2snomed.classification.model.MappingSourceItem;
import org.snomed.snap2snomed.model.Snap2SnomedEntity;

@Entity
@Table(name = "mapping_proposal")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MappingProposal implements Snap2SnomedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "source_item_id", nullable = false)
  private MappingSourceItem sourceItem;

  @ManyToOne
  @JoinColumn(name = "agent_run_id")
  private MappingAgentRun agentRun;

  @Column(name = "cm_code", length = 50)
  private String cmCode;

  @Column(name = "cm_name", length = 2048)
  private String cmName;

  @Enumerated(EnumType.STRING)
  @Column(name = "mapping_relation", nullable = false, length = 40)
  private MappingRelation mappingRelation;

  @Column(name = "confidence", length = 16)
  private String confidence;

  @Column(name = "mapping_notes", length = 4096)
  private String mappingNotes;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private ProposalStatus status;

  @Column(name = "created", nullable = false, updatable = false)
  private Instant created;

  @Column(name = "modified")
  private Instant modified;

  @PrePersist
  void prePersist() {
    if (status == null) status = ProposalStatus.PROPOSED;
    if (created == null) created = Instant.now();
    modified = Instant.now();
  }

  @PreUpdate
  void preUpdate() {
    modified = Instant.now();
  }
}
