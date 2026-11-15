package org.snomed.snap2snomed.classification.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MappingSourceItemTest {

  @Test
  void instructionIsContextAndNotMappingEligible() {
    MappingSourceItem item = MappingSourceItem.builder()
        .sourceItemType(SourceItemType.INSTRUCTION)
        .sourceTermType(SourceTermType.INCLUDE)
        .instructionType(InstructionType.USE_NOTE)
        .whoCode("A40")
        .rawText("Use additional code if desired to identify septic shock")
        .sourceOrder(1L)
        .mappingEligible(true)
        .build();

    item.prePersist();

    assertThat(item.getMappingEligible()).isFalse();
    assertThat(item.getSourceTermType()).isNull();
    assertThat(item.getInstructionType()).isEqualTo(InstructionType.USE_NOTE);
  }

  @Test
  void termCannotCarryInstructionType() {
    MappingSourceItem item = MappingSourceItem.builder()
        .sourceItemType(SourceItemType.TERM)
        .sourceTermType(SourceTermType.INDEX)
        .instructionType(InstructionType.NOTE)
        .whoCode("A01.0")
        .rawText("meningitis")
        .reconstructedText("Typhoid meningitis")
        .sourceOrder(2L)
        .mappingEligible(true)
        .build();

    item.prePersist();

    assertThat(item.getMappingEligible()).isTrue();
    assertThat(item.getSourceTermType()).isEqualTo(SourceTermType.INDEX);
    assertThat(item.getInstructionType()).isNull();
  }
}
