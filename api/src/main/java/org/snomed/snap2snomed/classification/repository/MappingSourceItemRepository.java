package org.snomed.snap2snomed.classification.repository;

import java.util.List;
import org.snomed.snap2snomed.classification.model.MappingSourceItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MappingSourceItemRepository extends JpaRepository<MappingSourceItem, Long> {
  List<MappingSourceItem> findByMapIdOrderBySourceOrderAsc(Long mapId);
}
