package org.snomed.snap2snomed.classification.repository;

import java.util.Optional;
import javax.persistence.LockModeType;
import org.snomed.snap2snomed.classification.agent.AgentJobStatus;
import org.snomed.snap2snomed.classification.agent.MappingAgentJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MappingAgentJobRepository extends JpaRepository<MappingAgentJob, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(value = "select j from MappingAgentJob j where j.status = :status order by j.priority desc, j.created asc")
  java.util.List<MappingAgentJob> findQueueForUpdate(@Param("status") AgentJobStatus status,
      org.springframework.data.domain.Pageable pageable);

  Optional<MappingAgentJob> findByIdAndClaimToken(Long id, String claimToken);
}
