package org.snomed.snap2snomed.classification.model;

import java.time.Instant;
import javax.persistence.*;
import lombok.*;
import org.snomed.snap2snomed.model.ImportedCode;
import org.snomed.snap2snomed.model.Map;
import org.snomed.snap2snomed.model.Snap2SnomedEntity;

@Entity
@Table(name = "mapping_source_item")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MappingSourceItem implements Snap2SnomedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "map_id", nullable = false)
  private Map map;

  @ManyToOne
  @JoinColumn(name = "imported_code_id")
  private ImportedCode importedCode;

  @ManyToOne
  @JoinColumn(name = "parent_item_id")
  private MappingSourceItem parentItem;

  @Enumerated(EnumType.STRING)
  @Column(name = "source_item_type", nullable = false, length = 32)
  private SourceItemType sourceItemType;

  @Enumerated(EnumType.STRING)
  @Column(name = "source_term_type", length = 32)
  private SourceTermType sourceTermType;

  @Enumerated(EnumType.STRING)
  @Column(name = "instruction_type", length = 40)
  private InstructionType instructionType;

  @Column(name = "who_code", nullable = false, length = 50)
  private String whoCode;

  @Column(name = "rubric_title", length = 2048)
  private String rubricTitle;

  @Column(name = "raw_text", nullable = false, length = 4096)
  private String rawText;

  @Column(name = "reconstructed_text", length = 4096)
  private String reconstructedText;

  @Column(name = "lead_term", length = 1024)
  private String leadTerm;

  @Column(name = "modifier_path", length = 4096)
  private String modifierPath;

  @Column(name = "index_depth")
  private Integer indexDepth;

  @Column(name = "source_volume", length = 32)
  private String sourceVolume;

  @Column(name = "source_location", length = 255)
  private String sourceLocation;

  @Column(name = "source_order", nullable = false)
  private Long sourceOrder;

  @Enumerated(EnumType.STRING)
  @Column(name = "reconstruction_method", length = 40)
  private ReconstructionMethod reconstructionMethod;

  @Column(name = "reconstruction_confidence", length = 16)
  private String reconstructionConfidence;

  @Column(name = "mapping_eligible", nullable = false)
  @Builder.Default
  private Boolean mappingEligible = true;

  @Column(name = "created", nullable = false, updatable = false)
  private Instant created;

  @Column(name = "modified")
  private Instant modified;

  @PrePersist
  void prePersist() {
    if (sourceItemType == SourceItemType.INSTRUCTION) {
      mappingEligible = false;
      sourceTermType = null;
    } else {
      instructionType = null;
    }
    created = created == null ? Instant.now() : created;
    modified = Instant.now();
  }

  @PreUpdate
  void preUpdate() {
    modified = Instant.now();
  }
}
