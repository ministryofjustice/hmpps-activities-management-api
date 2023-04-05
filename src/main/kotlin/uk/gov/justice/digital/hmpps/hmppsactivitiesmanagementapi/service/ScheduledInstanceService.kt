package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.toModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ScheduleInstanceCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ScheduledInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound

@Service
class ScheduledInstanceService(
  private val repository: ScheduledInstanceRepository,
  private val attendanceReasonRepository: AttendanceReasonRepository,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
) {
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

  fun uncancelScheduledInstance(id: Long) {
    val scheduledInstance = repository.findOrThrowNotFound(id)
    scheduledInstance.uncancel()
    repository.save(scheduledInstance)
  }

  fun cancelScheduledInstance(instanceId: Long, scheduleInstanceCancelRequest: ScheduleInstanceCancelRequest) {
    val scheduledInstance = repository.findOrThrowNotFound(instanceId)

    scheduledInstance.cancelSession(
      reason = scheduleInstanceCancelRequest.reason,
      by = scheduleInstanceCancelRequest.username,
      cancelComment = scheduleInstanceCancelRequest.comment,
    ) { attendanceList ->
      val attendanceReason = attendanceReasonRepository.findByCode("CANCELLED")
      val prisonerNumbers = scheduledInstance.attendances.map { it.prisonerNumber }
      val prisoners = prisonerSearchApiClient.findByPrisonerNumbers(prisonerNumbers).block()?.associateBy { it.prisonerNumber }

      attendanceList.forEach {
        val payIncentiveCode = prisoners?.get(it.prisonerNumber)?.currentIncentive?.level?.code
        it.cancel(attendanceReason, payIncentiveCode)
      }
    }

    repository.saveAndFlush(scheduledInstance)
  }
}
