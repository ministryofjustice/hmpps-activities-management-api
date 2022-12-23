package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.toModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityForPrisonerProjection
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ScheduledInstanceRepository
import java.time.LocalDate

@Service
class ScheduledInstanceService(private val repository: ScheduledInstanceRepository) {

  fun getActivityScheduleInstanceById(id: Long): ActivityScheduleInstance = repository.findOrThrowNotFound(id).toModel()

  fun getActivityScheduleInstancesByDateRange(
    prisonCode: String,
    prisonerNumber: String?,
    dateRange: LocalDateRange,
    slot: TimeSlot?,
  ): List<ActivityScheduleInstance> {
    val activities =
      prisonerNumber?.let {
        repository.getActivityScheduleInstancesByPrisonerNumberAndDateRange(
          prisonCode,
          prisonerNumber,
          dateRange.start,
          dateRange.endInclusive
        ).toModel()
      } ?: repository.getActivityScheduleInstancesByPrisonCodeAndDateRange(
        prisonCode,
        dateRange.start,
        dateRange.endInclusive
      ).toModel()

    return if (slot != null) {
      activities.filter { TimeSlot.slot(it.startTime) == slot }
    } else {
      activities
    }
  }

  fun getScheduledInstancesByPrisonerNumbers(
    prisonCode: String,
    prisonerNumbers: Set<String>,
    date: LocalDate?,
    slot: TimeSlot?,
  ): List<ActivityForPrisonerProjection> =
    repository.getActivitiesForPrisonerList(prisonCode, prisonerNumbers, date).filter {
      TimeSlot.slot(it.startTime) == slot
    }
}
