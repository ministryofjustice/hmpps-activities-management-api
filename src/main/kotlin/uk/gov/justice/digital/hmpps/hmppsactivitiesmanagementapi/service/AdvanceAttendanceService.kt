package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AdvanceAttendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AdvanceAttendanceCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AdvanceAttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PrisonerScheduledActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ScheduledInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.checkCaseloadAccess
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transform
import java.time.LocalDate
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AdvanceAttendance as ModelAdvanceAttendance

@Service
@Transactional
class AdvanceAttendanceService(
  private val scheduledInstanceRepository: ScheduledInstanceRepository,
  private val advanceAttendanceRepository: AdvanceAttendanceRepository,
  private val prisonerScheduledActivityRepository: PrisonerScheduledActivityRepository,
  private val prisonerSearchApiClient: PrisonerSearchApiApplicationClient,
  private val allocationRepository: AllocationRepository,
) {

  @Transactional(readOnly = true)
  fun getAttendanceById(id: Long) = advanceAttendanceRepository.findOrThrowNotFound(id)
    .also { advanceAttendance -> checkCaseloadAccess(advanceAttendance.scheduledInstance.activitySchedule.activity.prisonCode) }
    .let { advanceAttendance -> transform(advanceAttendance, true, determinePay(advanceAttendance)) }

  fun create(request: AdvanceAttendanceCreateRequest, recordedBy: String): ModelAdvanceAttendance {
    val today = LocalDate.now()

    val scheduledInstance = scheduledInstanceRepository.findOrThrowNotFound(request.scheduleInstanceId!!)
      .also { scheduledInstance ->
        require(scheduledInstance.advanceAttendances.none { it.prisonerNumber == request.prisonerNumber }) { "Prisoner ${request.prisonerNumber} already has an advance attendance record for this session" }

        require(scheduledInstance.sessionDate > today) { "Can only create an advance attendance for future dates" }

        require(scheduledInstance.sessionDate <= today.plusDays(14)) { "Can only create an advance attendance for up to 14 days in advance" }

        scheduledInstance.activitySchedule.activity.let { activity ->
          checkCaseloadAccess(activity.prisonCode)

          require(!request.issuePayment!! || activity.isPaid()) { "Cannot issue payment for an unpaid activity" }
        }
      }

    require(prisonerScheduledActivityRepository.getByScheduledInstanceIdAndPrisonerNumber(request.scheduleInstanceId, request.prisonerNumber!!).isPresent) { "Prisoner ${request.prisonerNumber} is not scheduled for this session" }

    return advanceAttendanceRepository.save(
      AdvanceAttendance(
        scheduledInstance = scheduledInstance,
        prisonerNumber = request.prisonerNumber,
        issuePayment = request.issuePayment!!,
        recordedBy = recordedBy,
        recordedTime = LocalDateTime.now(),
      ),
    )
      .let { attendance -> transform(attendance, true, determinePay(attendance)) }
  }

  fun update(
    advanceAttendanceId: Long,
    issuePayment: Boolean,
    recordedBy: String,
  ): ModelAdvanceAttendance = advanceAttendanceRepository.findOrThrowNotFound(advanceAttendanceId)
    .also { advanceAttendance -> checkCaseloadAccess(advanceAttendance.scheduledInstance.activitySchedule.activity.prisonCode) }
    .let { advanceAttendance ->
      val activity = advanceAttendance.scheduledInstance.activitySchedule.activity

      require(advanceAttendance.scheduledInstance.sessionDate > LocalDate.now()) { "Advance attendance can only be updated for future sessions" }

      require(!issuePayment || activity.isPaid()) { "Cannot issue payment for an unpaid activity" }

      advanceAttendance.updatePayment(
        issuePayment = issuePayment,
        recordedBy = recordedBy,
      )
        .let { advanceAttendance -> advanceAttendanceRepository.save(advanceAttendance) }
        .let { advanceAttendance -> transform(advanceAttendance, true, determinePay(advanceAttendance)) }
    }

  fun delete(advanceAttendanceId: Long) = advanceAttendanceRepository.findOrThrowNotFound(advanceAttendanceId)
    .also { advanceAttendance -> checkCaseloadAccess(advanceAttendance.scheduledInstance.activitySchedule.activity.prisonCode) }
    .let { advanceAttendance -> advanceAttendanceRepository.delete(advanceAttendance) }

  fun determinePay(attendance: AdvanceAttendance): Int? {
    if (!attendance.issuePayment) return null

    val activity = attendance.scheduledInstance.activitySchedule.activity

    if (activity.isPaid().not()) return 0

    val prisoner = prisonerSearchApiClient.findByPrisonerNumber(attendance.prisonerNumber)

    val incentiveLevelCode = prisoner?.currentIncentive?.level?.code

    val prisonerScheduledActivity = prisonerScheduledActivityRepository.getByScheduledInstanceIdAndPrisonerNumber(attendance.scheduledInstance.scheduledInstanceId, attendance.prisonerNumber)
      .orElseThrow { EntityNotFoundException("Prisoner ${attendance.prisonerNumber} is not scheduled for this session") }

    val payBand = allocationRepository.findById(prisonerScheduledActivity.allocationId).get().payBand

    return payBand?.let { incentiveLevelCode?.let { activity.activityPayFor(payBand, incentiveLevelCode) }?.rate } ?: 0
  }
}
