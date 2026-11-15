package org.snomed.snap2snomed.classification.repository;

import java.util.List;
import org.snomed.snap2snomed.classification.agent.MappingProposal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MappingProposalRepository extends JpaRepository<MappingProposal, Long> {
  List<MappingProposal> findBySourceItemIdOrderByCreatedDesc(Long sourceItemId);
}
