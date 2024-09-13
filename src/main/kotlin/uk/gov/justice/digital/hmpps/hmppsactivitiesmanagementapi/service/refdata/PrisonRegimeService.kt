package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalTimeRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.SlotTimes
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.PrisonRegimeDaysOfWeek
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonRegime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.EventPriorityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.PrisonPayBandRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.PrisonRegimeRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.PrisonRegimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelPrisonPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transform
import java.time.DayOfWeek
import java.time.LocalTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.PrisonRegime as PrisonRegimeEntity

@Service
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
  @Transactional(readOnly = true)
  fun getEventPrioritiesForPrison(code: String) =
    eventPriorityRepository.findByPrisonCode(code)
      .groupBy { it.eventType }
      .mapValues { it.value.map { ep -> Priority(ep.priority, ep.eventCategory) } }
      .ifEmpty { defaultPriorities() }
      .let(::EventPriorities)

  private fun defaultPriorities() =
    EventType.entries.associateWith { listOf(Priority(it.defaultPriority)) }

  /**
   * Returns the pay bands configured for a prison (if any), otherwise a default set of prison pay bands.
   */
  @Transactional(readOnly = true)
  fun getPayBandsForPrison(code: String) =
    prisonPayBandRepository.findByPrisonCode(code)
      .ifEmpty { prisonPayBandRepository.findByPrisonCode("DEFAULT") }
      .map { it.toModelPrisonPayBand() }

  @Transactional(readOnly = true)
  fun getPrisonRegimeByPrisonCode(code: String): List<PrisonRegime> {
    val regime = prisonRegimeRepository.findByPrisonCode(code = code)
    if (regime.isEmpty()) throw EntityNotFoundException("no regime set for prison")
    return regime.transformRegime()
  }

  @Transactional(readOnly = true)
  fun getPrisonRegimesByDaysOfWeek(agencyId: String): Map<Set<DayOfWeek>, PrisonRegimeEntity> =
    getPrisonRegimeForDaysOfWeek(prisonCode = agencyId, daysOfWeek = DayOfWeek.entries.toSet()).associateBy { it.prisonRegimeDaysOfWeek.map { m -> m.dayOfWeek }.toSet() }

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

  @Transactional(readOnly = true)
  fun getTimeRangeForPrisonAndTimeSlot(prisonCode: String, timeSlot: TimeSlot, dayOfWeek: DayOfWeek): LocalTimeRange? =
    getPrisonRegimeForDayOfWeek(prisonCode = prisonCode, dayOfWeek = dayOfWeek)?.let { pr ->
      val (start, end) = when (timeSlot) {
        TimeSlot.AM -> LocalTime.MIDNIGHT to pr.pmStart
        TimeSlot.PM -> pr.pmStart to pr.edStart
        TimeSlot.ED -> pr.edStart to LocalTime.of(23, 59)
      }

      LocalTimeRange(start, end)
    }

  @Transactional(readOnly = true)
  fun getSlotTimesForTimeSlot(
    prisonCode: String,
    daysOfWeek: Set<DayOfWeek>,
    timeSlot: TimeSlot,
  ): SlotTimes? {
    val regimeTimes = getSlotTimesForDaysOfWeek(
      prisonCode = prisonCode, daysOfWeek = daysOfWeek,
    ) ?: return null

    val key = regimeTimes.keys.firstOrNull { it.containsAll(daysOfWeek) } ?: return null
    return regimeTimes[key]?.get(timeSlot)
  }

  @Transactional(readOnly = true)
  fun getSlotTimesForDaysOfWeek(
    prisonCode: String,
    daysOfWeek: Set<DayOfWeek>,
  ): Map<Set<DayOfWeek>, Map<TimeSlot, SlotTimes>>? {
    val prisonRegimes = getPrisonRegimeForDaysOfWeek(
      prisonCode = prisonCode,
      daysOfWeek = daysOfWeek,
    )
    if (prisonRegimes.isEmpty()) return null

    return prisonRegimes.associate {
      it.prisonRegimeDaysOfWeek.map { m -> m.dayOfWeek }.toSet() to
        mapOf(
          TimeSlot.AM to Pair(it.amStart, it.amFinish),
          TimeSlot.PM to Pair(it.pmStart, it.pmFinish),
          TimeSlot.ED to Pair(it.edStart, it.edFinish),
        )
    }
  }

  @Transactional
  fun setPrisonRegime(agencyId: String, slots: List<PrisonRegimeSlot>): List<PrisonRegime> {
    if (slots.map { it.dayOfWeek }.containsAll(DayOfWeek.entries).not()) {
      throw ValidationException("requires all days of week")
    }
    prisonRegimeRepository.deleteByPrisonCode(code = agencyId)

    return slots.map {
      prisonRegimeRepository.save(
        PrisonRegimeEntity(
          prisonCode = agencyId,
          amStart = it.amStart,
          amFinish = it.amFinish,
          pmStart = it.pmStart,
          pmFinish = it.pmFinish,
          edStart = it.edStart,
          edFinish = it.edFinish,
          prisonRegimeDaysOfWeek = listOf(
            PrisonRegimeDaysOfWeek(dayOfWeek = it.dayOfWeek),
          ),
        ),
      )
    }.transformRegime()
  }

  private fun getPrisonRegimeForDaysOfWeek(
    prisonCode: String,
    daysOfWeek: Set<DayOfWeek>,
  ): List<PrisonRegimeEntity> {
    val prisonRegimes = prisonRegimeRepository.findByPrisonCode(code = prisonCode)

    val regimeMatchesDays = prisonRegimes.matchesDays(daysOfWeek = daysOfWeek)
    if (regimeMatchesDays.isNotEmpty()) return regimeMatchesDays

    val regimesAcrossDays = daysOfWeek.mapNotNull { day ->
      prisonRegimes.firstOrNull {
        it.prisonRegimeDaysOfWeek.map { m -> m.dayOfWeek }.contains(day)
      }
    }

    if (regimesAcrossDays.matchesDaysAcrossRegimes(daysOfWeek = daysOfWeek)) return regimesAcrossDays.toList()

    return emptyList()
  }

  private fun getPrisonRegimeForDayOfWeek(
    prisonCode: String,
    dayOfWeek: DayOfWeek,
  ): PrisonRegimeEntity? =
    prisonRegimeRepository.findByPrisonCode(code = prisonCode)
      .firstOrNull { it.prisonRegimeDaysOfWeek.map { m -> m.dayOfWeek }.contains(dayOfWeek) }

  companion object {
    fun List<PrisonRegimeEntity>.matchesDays(daysOfWeek: Set<DayOfWeek>) =
      this.filter { it.prisonRegimeDaysOfWeek.map { m -> m.dayOfWeek }.containsAll(daysOfWeek) }

    fun List<PrisonRegimeEntity>.matchesDaysAcrossRegimes(daysOfWeek: Set<DayOfWeek>) =
      this.flatMap { it.prisonRegimeDaysOfWeek }.map { it.dayOfWeek }.containsAll(daysOfWeek)

    fun Map<Set<DayOfWeek>, PrisonRegimeEntity>.getSlotForDayAndTime(
      day: DayOfWeek,
      time: LocalTime,
    ): TimeSlot {
      val key = this.keys.first { it.contains(day) }

      return this[key]!!.getTimeSlot(time = time)
    }

    fun List<PrisonRegimeEntity>.transformRegime(): List<PrisonRegime> =
      DayOfWeek.entries.map { dayOfWeek ->
        val regime = this.first { it.prisonRegimeDaysOfWeek.map { m -> m.dayOfWeek }.contains(dayOfWeek) }
        transform(
          prisonRegime = regime,
          dayOfWeek = dayOfWeek,
        )
      }.sortedBy {
        it.dayOfWeek.value
      }

    private fun PrisonRegimeEntity.getTimeSlot(time: LocalTime): TimeSlot {
      if (time.isBefore(this.pmStart)) return TimeSlot.AM
      if (time.isBefore(this.edStart)) return TimeSlot.PM

      return TimeSlot.ED
    }
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
