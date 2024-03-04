package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.persistence.EntityNotFoundException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import toPrisonerDeclinedFromWaitingListEvent
import toPrisonerRemovedFromWaitingListEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.isActiveAtPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.WaitingListApplication
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.WaitingListApplicationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.WaitingListApplicationUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.WaitingListSearchRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.WaitingListRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.WaitingListSearchSpecification
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISONER_NUMBER_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.activityMetricsMap
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.checkCaseloadAccess
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.determineEarliestReleaseDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModel
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class WaitingListService(
  private val scheduleRepository: ActivityScheduleRepository,
  private val waitingListRepository: WaitingListRepository,
  private val waitingListSearchSpecification: WaitingListSearchSpecification,
  private val activityRepository: ActivityRepository,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val telemetryClient: TelemetryClient,
  private val auditService: AuditService,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getWaitingListBy(id: Long): WaitingListApplication {
    val waitingList = waitingListRepository
      .findOrThrowNotFound(id)
      .checkCaseloadAccess()

    val prisoner =
      prisonerSearchApiClient.findByPrisonerNumber(waitingList.prisonerNumber) ?: throw NullPointerException("Prisoner ${waitingList.prisonerNumber} not found for waiting list id $id")
    return waitingList.toModel(determineEarliestReleaseDate(prisoner))
  }

  fun getWaitingListsBySchedule(id: Long) =
    scheduleRepository.findOrThrowNotFound(id).checkCaseloadAccess().let(waitingListRepository::findByActivitySchedule).map {
      val prisoner = prisonerSearchApiClient.findByPrisonerNumber(it.prisonerNumber) ?: throw NullPointerException("Prisoner ${it.prisonerNumber} not found for waiting list id $id")

      it.toModel(determineEarliestReleaseDate(prisoner))
    }

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
        initialStatus = request.status!!,
      ),
    )
      .also { log.info("Added ${request.status} waiting list application for prisoner ${request.prisonerNumber} to activity ${schedule.description}") }
      .also { logPrisonerAddedMetric(request.prisonerNumber) }
  }

  fun searchWaitingLists(
    prisonCode: String,
    request: WaitingListSearchRequest,
    pageNumber: Int,
    pageSize: Int,
  ): Page<WaitingListApplication> {
    checkCaseloadAccess(prisonCode)

    var spec = waitingListSearchSpecification.prisonCodeEquals(prisonCode)
    request.applicationDateFrom?.let { spec = spec.and(waitingListSearchSpecification.applicationDateFrom(it)) }
    request.applicationDateTo?.let { spec = spec.and(waitingListSearchSpecification.applicationDateTo(it)) }

    request.status?.let { spec = spec.and(waitingListSearchSpecification.statusIn(it)) }

    request.prisonerNumbers?.let { spec = spec.and(waitingListSearchSpecification.prisonerNumberIn(it)) }

    request.activityId?.let { spec = spec.and(waitingListSearchSpecification.activityIdEqual(it)) }

    val pageable: Pageable = PageRequest.of(pageNumber, pageSize, Sort.by("applicationDate"))
    return waitingListRepository.findAll(spec, pageable).map {
      val prisoner =
        prisonerSearchApiClient.findByPrisonerNumber(it.prisonerNumber) ?: throw NullPointerException("Prisoner ${it.prisonerNumber} not found")

      it.toModel(determineEarliestReleaseDate(prisoner))
    }
  }

  fun fetchOpenApplicationsForPrison(prisonCode: String) = waitingListRepository.findByPrisonCodeAndStatusIn(prisonCode, setOf(WaitingListStatus.PENDING, WaitingListStatus.APPROVED, WaitingListStatus.DECLINED))

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
    val prisonerDetails = prisonerSearchApiClient.findByPrisonerNumber(request.prisonerNumber!!)
      ?: throw IllegalArgumentException("Prisoner with ${request.prisonerNumber} not found")

    require(prisonerDetails.isActiveAtPrison(prisonCode)) {
      "${prisonerDetails.firstName} ${prisonerDetails.lastName} is not resident at this prison"
    }

    requireNotNull(prisonerDetails.bookingId) {
      "Prisoner ${request.prisonerNumber} has no booking id at prison $prisonCode"
    }

    return prisonerDetails.bookingId.toLong()
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
  fun updateWaitingList(id: Long, request: WaitingListApplicationUpdateRequest, updatedBy: String): WaitingListApplication {
    val waitingList = waitingListRepository
      .findOrThrowNotFound(id)
      .checkCaseloadAccess()
      .failIfNotUpdatable()
      .apply {
        updateApplicationDate(request, updatedBy)
        updateRequestedBy(request, updatedBy)
        updateComments(request, updatedBy)
        updateStatus(request, updatedBy)
      }
    val prisoner =
      prisonerSearchApiClient.findByPrisonerNumber(waitingList.prisonerNumber) ?: throw NullPointerException("Prisoner ${waitingList.prisonerNumber} not found for waiting list id $id")
    return waitingList.toModel(determineEarliestReleaseDate(prisoner))
  }

  private fun WaitingList.failIfNotUpdatable() = also {
    require(isStatus(WaitingListStatus.APPROVED, WaitingListStatus.PENDING, WaitingListStatus.DECLINED)) {
      "The waiting list $waitingListId can no longer be updated"
    }

    require(activitySchedule.allocations(true).none { it.prisonerNumber == prisonerNumber }) {
      "The waiting list $waitingListId can no longer be updated because the prisoner has already been allocated to the activity"
    }

    val otherApplications =
      waitingListRepository.findByPrisonCodeAndPrisonerNumberAndActivitySchedule(
        prisonCode,
        prisonerNumber,
        activitySchedule,
      ).filter { it != this }

    require(otherApplications.none { (it.updatedTime ?: it.creationTime) > (updatedTime ?: creationTime) }) {
      "The waiting list $waitingListId can no longer be updated because there is a more recent application for this prisoner"
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
      logMetric(TelemetryEvent.PRISONER_DECLINED_FROM_WAITLIST, prisonerNumber)
    }
  }

  private fun logMetric(event: TelemetryEvent, prisonerNumber: String) {
    val propertiesMap = mapOf(
      PRISONER_NUMBER_PROPERTY_KEY to prisonerNumber,
    )

    telemetryClient.trackEvent(event.value, propertiesMap, activityMetricsMap())
  }

  @Transactional
  fun removeOpenApplications(prisonCode: String, prisonerNumber: String, removedBy: String) {
    waitingListRepository.findByPrisonCodeAndPrisonerNumberAndStatusIn(
      prisonCode,
      prisonerNumber,
      setOf(WaitingListStatus.PENDING, WaitingListStatus.APPROVED, WaitingListStatus.DECLINED),
    ).forEach { application ->
      waitingListRepository.saveAndFlush(application.remove(removedBy))
      auditService.logEvent(application.toPrisonerRemovedFromWaitingListEvent())
    }
  }

  @Transactional
  fun declinePendingOrApprovedApplications(
    activityId: Long,
    reason: String,
    declinedBy: String,
  ) {
    waitingListRepository.findByActivityAndStatusIn(
      activityRepository.findOrThrowNotFound(activityId),
      setOf(WaitingListStatus.PENDING, WaitingListStatus.APPROVED),
    ).forEach { application ->
      waitingListRepository.saveAndFlush(application.decline(reason, declinedBy))
      auditService.logEvent(application.toPrisonerDeclinedFromWaitingListEvent())
    }
  }

  private fun WaitingList.decline(reason: String, declinedBy: String): WaitingList {
    val statusBefore = this.status

    apply {
      status = WaitingListStatus.DECLINED
      declinedReason = reason
      updatedTime = LocalDateTime.now()
      updatedBy = declinedBy
    }

    log.info("Declined $statusBefore waiting list application ${this.waitingListId} for prisoner number ${this.prisonerNumber}")

    return this
  }

  private fun WaitingList.remove(removedBy: String): WaitingList {
    val statusBefore = this.status

    apply {
      status = WaitingListStatus.REMOVED
      updatedTime = LocalDateTime.now()
      updatedBy = removedBy
    }

    log.info("Removed $statusBefore waiting list application ${this.waitingListId} for prisoner number ${this.prisonerNumber}")

    return this
  }
}
