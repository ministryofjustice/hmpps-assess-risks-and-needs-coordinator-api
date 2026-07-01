package uk.gov.justice.digital.hmpps.arnscoordinatorapi.events

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence.OasysEvent as PersistenceOasysEvent

/**
 * Maps the persistence layer [PersistenceOasysEvent] to the event contract [OasysEvent].
 * OasysEventEnumParityTest ensures parity.
 */
fun PersistenceOasysEvent.toEventsOasysEvent(): OasysEvent = OasysEvent.valueOf(name)
