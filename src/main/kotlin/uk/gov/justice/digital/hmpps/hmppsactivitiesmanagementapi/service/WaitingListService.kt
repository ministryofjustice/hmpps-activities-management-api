package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.extensions.isActiveInPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.extensions.isActiveOutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.WaitingListCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.WaitingListRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.checkCaseloadAccess
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class WaitingListService(
  private val scheduleRepository: ActivityScheduleRepository,
  private val waitingListRepository: WaitingListRepository,
  private val prisonApiClient: PrisonApiClient,
) {

  @Transactional
  @PreAuthorize("hasAnyRole('ACTIVITY_HUB', 'ACTIVITY_HUB_LEAD', 'ACTIVITY_ADMIN')")
  fun addPrisoner(prisonCode: String, request: WaitingListCreateRequest, createdBy: String) {
    checkCaseloadAccess(prisonCode)

    val schedule = scheduleRepository.findBy(request.activityScheduleId!!, prisonCode)
      ?: throw EntityNotFoundException("Activity schedule ${request.activityScheduleId} not found")

    schedule.failIfIsNotInWorkCategory(request.prisonerNumber!!)
    schedule.failIfNotFutureSchedule()
    schedule.failIfActivelyAllocated(request.prisonerNumber)
    request.failIfApplicationDateInFuture()
    failIfAlreadyPendingOrApproved(prisonCode, request.prisonerNumber, schedule)

    WaitingList(
      prisonCode = prisonCode,
      prisonerNumber = request.prisonerNumber,
      bookingId = getActivePrisonerBookingId(prisonCode, request),
      activitySchedule = schedule,
      applicationDate = request.applicationDate!!,
      requestedBy = request.requestedBy!!,
      comments = request.comments,
      createdBy = createdBy,
      status = request.status!!.waitingListStatus,
    )
  }

  private fun WaitingListCreateRequest.failIfApplicationDateInFuture() {
    require(applicationDate!! <= LocalDate.now()) { "Application date cannot be not be in the future" }
  }

  private fun failIfAlreadyPendingOrApproved(
    prisonCode: String,
    prisonerNumber: String,
    schedule: ActivitySchedule,
  ) {
    waitingListRepository.findByPrisonCodeAndPrisonerNumberAndActivitySchedule(
      prisonCode,
      prisonerNumber,
      schedule,
    ).let { entries ->
      require(entries.none { it.status == WaitingListStatus.PENDING }) {
        "A pending waiting list application already exists for $prisonerNumber"
      }

      require(entries.none { it.status == WaitingListStatus.APPROVED }) {
        "An approved waiting list application already exists for $prisonerNumber"
      }
    }
  }

  private fun getActivePrisonerBookingId(prisonCode: String, request: WaitingListCreateRequest): Long {
    val prisonerDetails = prisonApiClient.getPrisonerDetails(request.prisonerNumber!!).block()
      ?: throw IllegalArgumentException("Prisoner with ${request.prisonerNumber} not found")

    require(prisonerDetails.isActiveInPrison(prisonCode) || prisonerDetails.isActiveOutPrison(prisonCode)) {
      "Prisoner ${request.prisonerNumber} is not active in/out at prison $prisonCode"
    }

    requireNotNull(prisonerDetails.bookingId) {
      "Prisoner ${request.prisonerNumber} has no booking id at prison $prisonCode"
    }

    return prisonerDetails.bookingId
  }

  private fun ActivitySchedule.failIfIsNotInWorkCategory(prisonerNumber: String) {
    require(!activity.activityCategory.isNotInWork()) {
      "Cannot add prisoner $prisonerNumber to the waiting list for a 'not in work' activity"
    }
  }

  private fun ActivitySchedule.failIfNotFutureSchedule() {
    require(activity.endDate == null || activity.endDate?.isAfter(LocalDate.now()) == true) {
      "Activity must end in the future to add a prisoner to a waiting list"
    }
  }

  private fun ActivitySchedule.failIfActivelyAllocated(prisonerNumber: String) {
    require(allocations().none { it.prisonerNumber == prisonerNumber && (it.endDate == null || it.endDate!! > LocalDate.now()) }) {
      "Prisoner $prisonerNumber cannot be added to the waiting list because they are already allocated"
    }
  }
}
