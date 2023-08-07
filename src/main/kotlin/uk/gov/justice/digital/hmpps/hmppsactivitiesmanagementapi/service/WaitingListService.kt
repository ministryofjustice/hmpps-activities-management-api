package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.extensions.isActiveInPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.extensions.isActiveOutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.WaitingListApplicationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.WaitingListRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.checkCaseloadAccess
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModel
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
@PreAuthorize("hasAnyRole('ACTIVITY_HUB', 'ACTIVITY_HUB_LEAD', 'ACTIVITY_ADMIN')")
class WaitingListService(
  private val scheduleRepository: ActivityScheduleRepository,
  private val waitingListRepository: WaitingListRepository,
  private val prisonApiClient: PrisonApiClient,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getWaitingListBy(id: Long) =
    waitingListRepository.findOrThrowNotFound(id).also { checkCaseloadAccess(it.prisonCode) }.toModel()

  fun getWaitingListsBySchedule(id: Long) =
    scheduleRepository.findOrThrowNotFound(id)
      .also { checkCaseloadAccess(it.activity.prisonCode) }
      .let { schedule -> waitingListRepository.findByActivitySchedule(schedule).map { it.toModel() } }

  @Transactional
  fun addPrisoner(prisonCode: String, request: WaitingListApplicationRequest, createdBy: String) {
    checkCaseloadAccess(prisonCode)

    val schedule = scheduleRepository.findBy(request.activityScheduleId!!, prisonCode)
      ?: throw EntityNotFoundException("Activity schedule ${request.activityScheduleId} not found")

    schedule.failIfIsNotInWorkCategory()
    schedule.failIfNotFutureSchedule()
    schedule.failIfActivelyAllocated(request.prisonerNumber!!)
    request.failIfApplicationDateInFuture()
    failIfAlreadyPendingOrApproved(prisonCode, request.prisonerNumber, schedule)

    waitingListRepository.saveAndFlush(
      WaitingList(
        prisonCode = prisonCode,
        prisonerNumber = request.prisonerNumber,
        bookingId = getActivePrisonerBookingId(prisonCode, request),
        activitySchedule = schedule,
        applicationDate = request.applicationDate!!,
        requestedBy = request.requestedBy!!,
        comments = request.comments,
        createdBy = createdBy,
        status = request.status!!,
      ),
    )
      .also { log.info("Added ${request.status} waiting list application for prisoner ${request.prisonerNumber} to activity ${schedule.description}") }
  }

  private fun WaitingListApplicationRequest.failIfApplicationDateInFuture() {
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
        "Cannot add prisoner to the waiting list because a pending application already exists"
      }

      require(entries.none { it.status == WaitingListStatus.APPROVED }) {
        "Cannot add prisoner to the waiting list because an approved application already exists"
      }
    }
  }

  private fun getActivePrisonerBookingId(prisonCode: String, request: WaitingListApplicationRequest): Long {
    val prisonerDetails = prisonApiClient.getPrisonerDetails(request.prisonerNumber!!).block()
      ?: throw IllegalArgumentException("Prisoner with ${request.prisonerNumber} not found")

    require(prisonerDetails.isActiveInPrison(prisonCode) || prisonerDetails.isActiveOutPrison(prisonCode)) {
      "${prisonerDetails.firstName} ${prisonerDetails.lastName} is not resident at this prison"
    }

    requireNotNull(prisonerDetails.bookingId) {
      "Prisoner ${request.prisonerNumber} has no booking id at prison $prisonCode"
    }

    return prisonerDetails.bookingId
  }

  private fun ActivitySchedule.failIfIsNotInWorkCategory() {
    require(!activity.activityCategory.isNotInWork()) {
      "Cannot add prisoner to the waiting list because the activity category is 'not in work'"
    }
  }

  private fun ActivitySchedule.failIfNotFutureSchedule() {
    require(activity.endDate == null || activity.endDate?.isAfter(LocalDate.now()) == true) {
      "Cannot add prisoner to the waiting list for an activity ending on or before today"
    }
  }

  private fun ActivitySchedule.failIfActivelyAllocated(prisonerNumber: String) {
    require(allocations().none { it.prisonerNumber == prisonerNumber && (it.endDate == null || it.endDate!! > LocalDate.now()) }) {
      "Cannot add prisoner to the waiting list because they are already allocated"
    }
  }
}
