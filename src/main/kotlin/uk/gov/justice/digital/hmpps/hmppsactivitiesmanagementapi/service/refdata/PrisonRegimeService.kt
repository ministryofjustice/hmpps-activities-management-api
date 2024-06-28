package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalTimeRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.SlotTimes
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.EventPriorityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.PrisonPayBandRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.PrisonRegimeRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelPrisonPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transform
import java.time.LocalTime

@Service
@Transactional(readOnly = true)
class PrisonRegimeService(
  private val eventPriorityRepository: EventPriorityRepository,
  private val prisonPayBandRepository: PrisonPayBandRepository,
  private val prisonRegimeRepository: PrisonRegimeRepository,
) {

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
      .let(::EventPriorities)

  private fun defaultPriorities() =
    EventType.values().associateWith { listOf(Priority(it.defaultPriority)) }

  /**
   * Returns the pay bands configured for a prison (if any), otherwise a default set of prison pay bands.
   */
  fun getPayBandsForPrison(code: String) =
    prisonPayBandRepository.findByPrisonCode(code)
      .ifEmpty { prisonPayBandRepository.findByPrisonCode("DEFAULT") }
      .map { it.toModelPrisonPayBand() }

  /**
   * Returns the prison regime configured for a prison (if any).
   */
  fun getPrisonRegimeByPrisonCode(code: String) = transform(
    prisonRegimeRepository.findByPrisonCode(code) ?: throw EntityNotFoundException(code),
  )

  /**
   *  Converts a TimeSlot for a given Prison into a time range.
   *
   *  Note that there can be gaps between time ranges (e.g. PM finishes at 16:30 but PM doesn't start until 18:00)
   *  and that's why :
   *
   *   - The start of a period is either midnight or the end of the previous period
   *   - The end of a period is either the start of the next period or one minute to midnight.
   *
   *   @param prisonCode The code of the prison to look up the times for
   *   @param timeSlot The timeslot to convert
   */
  fun getTimeRangeForPrisonAndTimeSlot(prisonCode: String, timeSlot: TimeSlot): LocalTimeRange =
    getPrisonRegimeByPrisonCode(prisonCode)
      .let { pr ->
        val (start, end) = when (timeSlot) {
          TimeSlot.AM -> LocalTime.MIDNIGHT to pr.pmStart
          TimeSlot.PM -> pr.pmStart to pr.edStart
          TimeSlot.ED -> pr.edStart to LocalTime.of(23, 59)
        }

        LocalTimeRange(start, end)
      }

  fun getPrisonTimeSlots(prisonCode: String): Map<TimeSlot, SlotTimes> =
    getPrisonRegimeByPrisonCode(prisonCode).let { pr ->
      mapOf(
        TimeSlot.AM to Pair(pr.amStart, pr.amFinish),
        TimeSlot.PM to Pair(pr.pmStart, pr.pmFinish),
        TimeSlot.ED to Pair(pr.edStart, pr.edFinish),
      )
    }
}

data class Priority(val priority: Int, val eventCategory: EventCategory? = null)

data class EventPriorities(val priorities: Map<EventType, List<Priority>>) {

  fun getOrDefault(eventType: EventType, category: EventCategory): Int = getOrDefault(eventType, category.name)

  fun getOrDefault(eventType: EventType, category: String? = null): Int =
    priorities[eventType]?.fold(listOf<Priority>()) { acc, next ->
      if (next.eventCategory == null && acc.isEmpty()) {
        listOf(next)
      } else {
        when (next.eventCategory) {
          EventCategory.EDUCATION -> if (category?.startsWith("EDU") == true) listOf(next) else acc
          EventCategory.FAITH_SPIRITUALITY -> if (category?.startsWith("FAI") == true) listOf(next) else acc
          EventCategory.GYM_SPORTS_FITNESS -> if (category?.startsWith("GYM") == true) listOf(next) else acc
          EventCategory.INDUCTION -> if (category?.startsWith("INDUC") == true) listOf(next) else acc
          EventCategory.INDUSTRIES -> if (category?.startsWith("INDUS") == true) listOf(next) else acc
          EventCategory.INTERVENTIONS -> if (category?.startsWith("INTERV") == true) listOf(next) else acc
          EventCategory.NOT_IN_WORK -> if (category?.startsWith("NOT") == true) listOf(next) else acc
          EventCategory.OTHER -> if (category?.startsWith("OTH") == true) listOf(next) else acc
          EventCategory.PRISON_JOBS -> if (category?.startsWith("PRISON") == true) listOf(next) else acc
          else -> {
            acc
          }
        }
      }
    }?.firstOrNull()?.priority ?: eventType.defaultPriority
}
