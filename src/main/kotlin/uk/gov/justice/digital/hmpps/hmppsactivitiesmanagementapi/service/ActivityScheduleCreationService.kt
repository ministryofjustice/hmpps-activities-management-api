package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityScheduleCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.Slot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import java.time.DayOfWeek
import java.time.LocalTime

@Service
class ActivityScheduleCreationService(
  private val activityRepository: ActivityRepository,
  private val prisonApiClient: PrisonApiClient,
  private val prisonRegimeService: PrisonRegimeService
) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @PreAuthorize("hasAnyRole('ACTIVITY_HUB', 'ACTIVITY_HUB_LEAD', 'ACTIVITY_ADMIN')")
  fun createSchedule(
    activityId: Long,
    request: ActivityScheduleCreateRequest
  ): ActivityScheduleLite {
    activityRepository.findOrThrowNotFound(activityId).let { activity ->
      val scheduleLocation = getLocationForSchedule(activity, request)

      val prisonRegime = prisonRegimeService.getPrisonRegimeByPrisonCode(activity.prisonCode)
      val timeSlots =
        mapOf(
          TimeSlot.AM to Pair(prisonRegime.amStart, prisonRegime.amFinish),
          TimeSlot.PM to Pair(prisonRegime.pmStart, prisonRegime.pmFinish),
          TimeSlot.ED to Pair(prisonRegime.edStart, prisonRegime.edFinish)
        )

      activity.addSchedule(
        description = request.description!!,
        internalLocationId = scheduleLocation.locationId.toInt(),
        internalLocationCode = scheduleLocation.internalLocationCode ?: "",
        internalLocationDescription = scheduleLocation.description,
        capacity = request.capacity!!,
        startDate = request.startDate!!,
        endDate = request.endDate,
        runsOnBankHoliday = request.runsOnBankHoliday
      ).let { schedule ->
        schedule.addSlots(request.slots!!, timeSlots)

        val persisted = activityRepository.saveAndFlush(activity)

        log.info("Schedule '${schedule.description}' added to activity '${activity.summary}' at prison '${activity.prisonCode}'")

        return persisted.schedules().last().toModelLite()
      }
    }
  }

  private fun ActivitySchedule.addSlots(slots: List<Slot>, timeSlots: Map<TimeSlot, Pair<LocalTime, LocalTime>>) {

    slots.forEach { slot ->
      val (start, end) = timeSlots[TimeSlot.valueOf(slot.timeSlot!!)]!!

      val daysOfWeek = setOfNotNull(
        DayOfWeek.MONDAY.takeIf { slot.monday },
        DayOfWeek.TUESDAY.takeIf { slot.tuesday },
        DayOfWeek.WEDNESDAY.takeIf { slot.wednesday },
        DayOfWeek.THURSDAY.takeIf { slot.thursday },
        DayOfWeek.FRIDAY.takeIf { slot.friday },
        DayOfWeek.SATURDAY.takeIf { slot.saturday },
        DayOfWeek.SUNDAY.takeIf { slot.sunday }
      )

      this.addSlot(start, end, daysOfWeek)
    }
  }

  private fun getLocationForSchedule(activity: Activity, request: ActivityScheduleCreateRequest): Location {
    val location = prisonApiClient.getLocation(request.locationId!!).block()!!
    failIfPrisonsDiffer(activity, location)
    return location
  }

  private fun failIfPrisonsDiffer(activity: Activity, location: Location) {
    if (activity.prisonCode != location.agencyId) {
      throw IllegalArgumentException("The activities prison '${activity.prisonCode}' does not match that of the locations '${location.agencyId}'")
    }
  }
}
