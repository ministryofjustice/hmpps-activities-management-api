package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.EventPriorityRepository

@Service
class PrisonRegimeService(private val eventPriorityRepository: EventPriorityRepository) {

  /**
   * Return the event priorities for a given prison.
   *
   * If no priorities are found then defaults are provided, see [EventType] for the default order.
   */
  fun getEventPrioritiesForPrison(code: String) =
    eventPriorityRepository.findByPrisonCode(code)
      .groupBy { it.eventType }
      .mapValues { it.value.map { ep -> Priority(ep.priority, ep.eventCategory) } }
      .ifEmpty { defaultPriorities() }

  private fun defaultPriorities() =
    EventType.values().associateWith { listOf(Priority(it.defaultPriority)) }
}

data class Priority(val priority: Int, val eventCategory: EventCategory? = null)
