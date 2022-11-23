package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventType

@Service
class PrisonRegimeService {

  // TODO this is stubbed for now until we have the DB, Entity and repository in place
  fun getEventPrioritiesForPrison(code: String) =
    EventType.values().associateWith { setOf(Priority(it.defaultPriority)) }
}

// TODO this will need to contain more information e.g. subtype/subcategories.
data class Priority(val priority: Int)
