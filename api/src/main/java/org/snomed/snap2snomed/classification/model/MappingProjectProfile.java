package org.snomed.snap2snomed.classification.model;

import java.time.Instant;
import javax.persistence.*;
import lombok.*;
import org.snomed.snap2snomed.model.Project;
import org.snomed.snap2snomed.model.Snap2SnomedEntity;

@Entity
@Table(name = "mapping_project_profile")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MappingProjectProfile implements Snap2SnomedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(optional = false)
  @JoinColumn(name = "project_id", nullable = false, unique = true)
  private Project project;

  @Enumerated(EnumType.STRING)
  @Column(name = "project_type", nullable = false, length = 50)
  @Builder.Default
  private MappingProjectType projectType = MappingProjectType.SIMPLE_SNOMED;

  @Column(name = "source_system_uri")
  private String sourceSystemUri;

  @Column(name = "source_version", length = 64)
  private String sourceVersion;

  @Column(name = "target_system_uri")
  private String targetSystemUri;

  @Column(name = "target_version", length = 64)
  private String targetVersion;

  @Column(name = "mapping_profile", length = 100)
  private String mappingProfile;

  @Column(name = "ai_enabled", nullable = false)
  @Builder.Default
  private Boolean aiEnabled = false;

  @Column(name = "auto_accept_policy_version", length = 64)
  private String autoAcceptPolicyVersion;

  @Column(name = "created", nullable = false, updatable = false)
  private Instant created;

  @Column(name = "modified")
  private Instant modified;

  @PrePersist
  void prePersist() {
    created = created == null ? Instant.now() : created;
    modified = Instant.now();
  }

  @PreUpdate
  void preUpdate() {
    modified = Instant.now();
  }
}
