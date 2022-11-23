package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.EventPriorityRepository

@Service
class PrisonRegimeService(private val eventPriorityRepository: EventPriorityRepository) {

  fun getEventPrioritiesForPrison(code: String) =
    eventPriorityRepository.findByPrisonCode(code)
      .groupBy { it.eventType }
      .mapValues { it.value.map { ep -> Priority(ep.priority) } }
      .ifEmpty { defaultPriorities() }

  private fun defaultPriorities() =
    EventType.values().associateWith { listOf(Priority(it.defaultPriority)) }
}

// TODO this will need to contain more information e.g. subtype/subcategories.
data class Priority(val priority: Int)
