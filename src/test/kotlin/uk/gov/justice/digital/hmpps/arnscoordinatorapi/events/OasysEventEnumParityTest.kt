package uk.gov.justice.digital.hmpps.arnscoordinatorapi.events

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence.OasysEvent as PersistenceOasysEvent

class OasysEventEnumParityTest {

  @Test
  fun `persistence and event OasysEvent enums have identical value names`() {
    val persistenceNames = PersistenceOasysEvent.entries.map { it.name }.toSet()
    val eventNames = OasysEvent.entries.map { it.name }.toSet()
    assertThat(eventNames).isEqualTo(persistenceNames)
  }
}
