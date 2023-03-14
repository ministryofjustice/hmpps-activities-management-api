package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.toModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ScheduledInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import java.time.LocalDate

@Service
class ScheduledInstanceService(private val repository: ScheduledInstanceRepository) {
  fun getActivityScheduleInstanceById(id: Long): ActivityScheduleInstance =
    repository.findOrThrowNotFound(id).toModel()

  fun getActivityScheduleInstancesByDateRange(
    prisonCode: String,
    dateRange: LocalDateRange,
    slot: TimeSlot?,
  ): List<ActivityScheduleInstance> {
    val activities = repository.getActivityScheduleInstancesByPrisonCodeAndDateRange(
      prisonCode,
      dateRange.start,
      dateRange.endInclusive,
    ).toModel()

    return if (slot != null) {
      activities.filter { TimeSlot.slot(it.startTime) == slot }
    } else {
      activities
    }
  }

  fun uncancelScheduledInstance(id: Long, username: String) {
    val scheduledInstance = repository.findById(id)
      .orElseThrow { EntityNotFoundException("No scheduled instance with ID [$id] exists") }
    if (scheduledInstance.sessionDate.isBefore(LocalDate.now())) {
      throw IllegalArgumentException("Cannot uncancel scheduled instance [$id] because it is in the past")
    }
    if (!scheduledInstance.cancelled) {
      throw IllegalArgumentException("Cannot uncancel scheduled instance [$id] because it is not cancelled")
    }

    val uncancelledAttendences = scheduledInstance.attendances.map {
      it.copy(
        attendanceReason = null,
        status = AttendanceStatus.WAIT,
        comment = null,
        recordedBy = null,
        recordedTime = null,
        payAmount = null,
      )
    }.toMutableList()

    val cancelledInstance = scheduledInstance.copy(
      cancelled = false,
      cancelledBy = null,
      cancelledReason = null,
      attendances = uncancelledAttendences,
    )

    repository.save(cancelledInstance)
  }
}
