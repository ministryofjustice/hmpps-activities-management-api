package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ScheduledInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transformActivityScheduleInstances

@Service
class ScheduledInstanceService(private val repository: ScheduledInstanceRepository) {

  fun getActivityScheduleInstancesByDateRange(
    prisonCode: String,
    prisonerNumber: String?,
    dateRange: LocalDateRange,
    slot: TimeSlot?,
  ): List<ActivityScheduleInstance> {
    val activities = transformActivityScheduleInstances(
      prisonerNumber?.let {
        repository.getActivityScheduleInstancesByPrisonerNumberAndDateRange(
          prisonCode,
          prisonerNumber,
          dateRange.start,
          dateRange.endInclusive
        )
      } ?: repository.getActivityScheduleInstancesByPrisonCodeAndDateRange(
        prisonCode,
        dateRange.start,
        dateRange.endInclusive
      )
    )

    return if (slot != null) {
      activities.filter { TimeSlot.slot(it.startTime) == slot }
    } else {
      activities
    }
  }
}
