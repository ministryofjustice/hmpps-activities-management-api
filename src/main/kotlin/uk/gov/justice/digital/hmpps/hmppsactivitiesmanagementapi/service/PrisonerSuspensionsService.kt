package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api.CaseNoteSubType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api.CaseNoteType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api.CaseNotesApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.onOrAfter
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.onOrBefore
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toIsoDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toMediumFormatStyle
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PlannedSuspension
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AddCaseNoteRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.SuspendPrisonerRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.UnsuspendPrisonerRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.checkCaseloadAccess
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional
class PrisonerSuspensionsService(
  private val allocationRepository: AllocationRepository,
  private val caseNotesApiClient: CaseNotesApiClient,
  private val attendanceSuspensionDomainService: AttendanceSuspensionDomainService,
  private val outboundEventsService: OutboundEventsService,
  private val transactionHandler: TransactionHandler,
) {

  fun suspend(prisonCode: String, request: SuspendPrisonerRequest, byWhom: String) {
    checkCaseloadAccess(prisonCode)

    val allocationIds = request.allocationIds.toSet()
    val prisonerNumber = request.prisonerNumber!!
    val suspendFrom = request.suspendFrom!!.also {
      require(it.onOrAfter(LocalDate.now())) { "Suspension start date must be on or after today's date" }
    }

    require(listOf(PrisonerStatus.SUSPENDED, PrisonerStatus.SUSPENDED_WITH_PAY).contains(request.status)) {
      "Only 'SUSPENDED' or 'SUSPENDED_WITH_PAY' are allowed for status"
    }

    val impactedAttendanceIds = transactionHandler.newSpringTransaction {
      val allocations = allocationRepository.findAllById(allocationIds)
        .also { allocations ->
          allocations.checkAllBelongToSame(prisonCode, request.prisonerNumber)
          allocations.checkAllSuspensionStartDates(request.suspendFrom)
          allocations.checkNotEnded()
          allocations.checkNotAlreadySuspended()
        }

      val mayBeCaseNoteId =
        request.suspensionCaseNote?.let { createCaseNote(prisonCode, prisonerNumber, suspendFrom, it, allocations) }
      val now = LocalDateTime.now()
      val attendanceIds = allocations.flatMap { allocation ->
        allocation.plannedSuspension().let { plannedSuspension ->
          plannedSuspension?.plan(suspendFrom, now, byWhom, mayBeCaseNoteId)
            ?: allocation.addPlannedSuspension(
              PlannedSuspension(
                allocation = allocation,
                plannedStartDate = maxOf(suspendFrom, allocation.startDate),
                plannedBy = byWhom,
                plannedAt = now,
                caseNoteId = mayBeCaseNoteId,
                paid = PrisonerStatus.SUSPENDED_WITH_PAY == request.status,
              ),
            )
        }

        allocation.takeIf { it.status(PrisonerStatus.ACTIVE) && it.isCurrentlySuspended() }?.let { alloc ->
          alloc.activatePlannedSuspension(request.status)
          attendanceSuspensionDomainService.suspendFutureAttendancesForAllocation(LocalDateTime.now(), alloc).map(Attendance::attendanceId)
        } ?: emptyList()
      }

      allocationRepository.saveAllAndFlush(allocations)
      attendanceIds
    }

    publishChangesFor(allocationIds, impactedAttendanceIds)
  }

  private fun Collection<Allocation>.checkAllSuspensionStartDates(suspendStartDate: LocalDate) {
    forEach { allocation ->
      require(allocation.endDate == null || suspendStartDate.onOrBefore(allocation.endDate!!)) {
        "Allocation ${allocation.allocationId}: Suspension start date must be on or before the allocation end date ${allocation.endDate!!.toIsoDate()}"
      }
    }
  }

  private fun Collection<Allocation>.checkNotEnded() {
    forEach { allocation ->
      require(allocation.isEnded().not()) {
        "Allocation ${allocation.allocationId}: Cannot be suspended because it is ended."
      }
    }
  }

  private fun Collection<Allocation>.checkNotAlreadySuspended() {
    forEach { allocation ->
      require(allocation.isCurrentlySuspended().not()) {
        "Allocation ${allocation.allocationId}: Is already suspended."
      }
    }
  }

  private fun createCaseNote(
    prisonCode: String,
    prisonerNumber: String,
    suspendFrom: LocalDate,
    request: AddCaseNoteRequest,
    allocations: Collection<Allocation>,
  ): Long {
    val prefix = if (allocations.size > 1) {
      "Suspended from all activities from ${suspendFrom.toMediumFormatStyle()}"
    } else {
      "Suspended from activity from ${suspendFrom.toMediumFormatStyle()} - ${allocations.single().activitySchedule.description}"
    }

    return caseNotesApiClient.postCaseNote(
      prisonCode,
      prisonerNumber,
      request.text!!,
      request.type!!,
      if (request.type == CaseNoteType.GEN) CaseNoteSubType.HIS else CaseNoteSubType.NEG_GEN,
      prefix,
    ).caseNoteId.toLong()
  }

  fun unsuspend(prisonCode: String, request: UnsuspendPrisonerRequest, byWhom: String) {
    checkCaseloadAccess(prisonCode)

    val allocationIds = request.allocationIds.toSet()
    val suspendUntil = request.suspendUntil!!
    val impactedAttendanceIds = transactionHandler.newSpringTransaction {
      val allocations = allocationRepository.findAllById(allocationIds)
        .also { allocations ->
          allocations.checkAllBelongToSame(prisonCode, request.prisonerNumber!!)
          allocations.checkAllSuspended()
          allocations.checkSuspensionEndDates(suspendUntil)
        }

      val attendanceIds = allocations.flatMap { allocation ->
        val plannedSuspensionStartDate = allocation.plannedSuspension()!!.startDate()

        allocation.plannedSuspension()!!.endOn(maxOf(suspendUntil, plannedSuspensionStartDate), byWhom)

        allocation.takeIf { (it.status(PrisonerStatus.SUSPENDED) || it.status(PrisonerStatus.SUSPENDED_WITH_PAY)) && !it.isCurrentlySuspended() }?.let { alloc ->
          alloc.reactivateSuspension()
          attendanceSuspensionDomainService.resetSuspendedFutureAttendancesForAllocation(LocalDateTime.now(), alloc).map(Attendance::attendanceId)
        } ?: emptyList()
      }

      allocationRepository.saveAllAndFlush(allocations)
      attendanceIds
    }

    publishChangesFor(allocationIds, impactedAttendanceIds)
  }

  private fun Collection<Allocation>.checkAllBelongToSame(prisonCode: String, prisonerNumber: String) {
    forEach { allocation ->
      require(allocation.prisonCode() == prisonCode && allocation.prisonerNumber == prisonerNumber) {
        "Allocation ${allocation.allocationId}: Must be in prison code $prisonCode and belong to prisoner number $prisonerNumber"
      }
    }
  }

  private fun Collection<Allocation>.checkAllSuspended() {
    forEach { allocation ->
      requireNotNull(allocation.plannedSuspension()) {
        "Allocation ${allocation.allocationId}: Must be suspended to unsuspend it."
      }
    }
  }

  private fun Collection<Allocation>.checkSuspensionEndDates(suspendUntil: LocalDate) {
    forEach { allocation ->
      require(allocation.plannedEndDate() == null || suspendUntil.onOrBefore(allocation.plannedEndDate()!!)) {
        "Allocation ${allocation.allocationId}: Suspension end date must be on or before the allocation end date: ${allocation.plannedEndDate()!!.toIsoDate()}"
      }
    }
  }

  private fun publishChangesFor(allocationIds: Collection<Long>, attendanceIds: Collection<Long>) {
    allocationIds.forEach { allocationId ->
      outboundEventsService.send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, allocationId)
    }

    attendanceIds.forEach { attendanceId ->
      outboundEventsService.send(OutboundEvent.PRISONER_ATTENDANCE_AMENDED, attendanceId)
    }
  }
}
