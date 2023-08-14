package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.microsoft.applicationinsights.TelemetryClient
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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.WaitingListApplicationUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.WaitingListRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.NUMBER_OF_RESULTS_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISONER_NUMBER_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.checkCaseloadAccess
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModel
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
@PreAuthorize("hasAnyRole('ACTIVITY_HUB', 'ACTIVITY_HUB_LEAD', 'ACTIVITY_ADMIN')")
class WaitingListService(
  private val scheduleRepository: ActivityScheduleRepository,
  private val waitingListRepository: WaitingListRepository,
  private val prisonApiClient: PrisonApiClient,
  private val telemetryClient: TelemetryClient,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getWaitingListBy(id: Long) =
    waitingListRepository.findOrThrowNotFound(id).checkCaseloadAccess().toModel()

  fun getWaitingListsBySchedule(id: Long) =
    scheduleRepository
      .findOrThrowNotFound(id)
      .checkCaseloadAccess()
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
    request.failIfNotAllowableStatus()
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
      .also { logPrisonerAddedMetric(request.prisonerNumber) }
  }

  private fun WaitingListApplicationRequest.failIfNotAllowableStatus() {
    require(
      status != null &&
        listOf(WaitingListStatus.ALLOCATED, WaitingListStatus.REMOVED).none { it == status },
    ) {
      "Only statuses of PENDING, APPROVED and DECLINED are allowed when adding a prisoner to a waiting list"
    }
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

  @Transactional
  fun updateWaitingList(id: Long, request: WaitingListApplicationUpdateRequest, updatedBy: String) =
    waitingListRepository
      .findOrThrowNotFound(id)
      .checkCaseloadAccess()
      .failIfNotUpdatable()
      .apply {
        updateApplicationDate(request, updatedBy)
        updateRequestedBy(request, updatedBy)
        updateComments(request, updatedBy)
        updateStatus(request, updatedBy)
      }.toModel()

  private fun WaitingList.failIfNotUpdatable() = also {
    require(isStatus(WaitingListStatus.APPROVED, WaitingListStatus.PENDING, WaitingListStatus.DECLINED)) {
      "The waiting list $waitingListId can no longer be updated"
    }
  }

  private fun WaitingList.updateApplicationDate(request: WaitingListApplicationUpdateRequest, changedBy: String) {
    request.applicationDate?.takeUnless { it == applicationDate }?.let { updatedApplicationDate ->
      require(updatedApplicationDate <= creationTime.toLocalDate()) {
        "The application date cannot be after the date the application was initially created ${creationTime.toLocalDate()}"
      }

      applicationDate = updatedApplicationDate
      updatedTime = LocalDateTime.now()
      updatedBy = changedBy
    }
  }

  private fun WaitingList.updateRequestedBy(request: WaitingListApplicationUpdateRequest, changedBy: String) {
    request.requestedBy?.takeUnless { it == requestedBy }?.let { updatedRequestedBy ->
      require(updatedRequestedBy.isNotBlank()) {
        "Requested by cannot be blank or empty"
      }

      requestedBy = updatedRequestedBy
      updatedTime = LocalDateTime.now()
      updatedBy = changedBy
    }
  }

  private fun WaitingList.updateComments(request: WaitingListApplicationUpdateRequest, changedBy: String) {
    request.comments?.takeUnless { it == comments }?.let { updatedComments ->
      comments = updatedComments
      updatedTime = LocalDateTime.now()
      updatedBy = changedBy
    }
  }

  private fun WaitingList.updateStatus(request: WaitingListApplicationUpdateRequest, changedBy: String) {
    request.status?.takeUnless { it == status }?.let { updatedStatus ->
      require(
        listOf(
          WaitingListStatus.APPROVED,
          WaitingListStatus.PENDING,
          WaitingListStatus.DECLINED,
        ).contains(updatedStatus),
      ) {
        "Only PENDING, APPROVED or DECLINED are allowed for the status change"
      }

      status = updatedStatus
      updatedTime = LocalDateTime.now()
      updatedBy = changedBy
      logStatusChangeMetric(this.prisonerNumber, status)
    }
  }

  private fun ActivitySchedule.checkCaseloadAccess() = also { checkCaseloadAccess(activity.prisonCode) }

  private fun WaitingList.checkCaseloadAccess() = also { checkCaseloadAccess(prisonCode) }

  private fun logPrisonerAddedMetric(prisonerNumber: String) {
    logMetric(TelemetryEvent.PRISONER_ADDED_TO_WAITLIST, prisonerNumber)
  }

  private fun logStatusChangeMetric(prisonerNumber: String, status: WaitingListStatus) {
    if (status == WaitingListStatus.APPROVED) {
      logMetric(TelemetryEvent.PRISONER_APPROVED_ON_WAITLIST, prisonerNumber)
    }

    if (status == WaitingListStatus.DECLINED) {
      logMetric(TelemetryEvent.PRISONER_REJECTED_FROM_WAITLIST, prisonerNumber)
    }
  }

  private fun logMetric(event: TelemetryEvent, prisonerNumber: String) {
    val propertiesMap = mapOf(
      PRISONER_NUMBER_KEY to prisonerNumber,
    )
    val metricsMap = mapOf(
      NUMBER_OF_RESULTS_KEY to 1.0,
    )

    telemetryClient.trackEvent(event.value, propertiesMap, metricsMap)
  }
}
