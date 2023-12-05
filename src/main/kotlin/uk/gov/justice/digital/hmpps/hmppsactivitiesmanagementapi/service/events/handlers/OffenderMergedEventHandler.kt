package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.OffenderMergeDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.LocalAuditRecord
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentAttendeeRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AuditRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.EventReviewRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.WaitingListRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.isActivitiesRolledOutAt
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.TransactionHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OffenderMergedEvent
import java.time.LocalDateTime

@Component
@Transactional(readOnly = true)
class OffenderMergedEventHandler(
  private val rolloutPrisonRepository: RolloutPrisonRepository,
  private val prisonerSearchApiClient: PrisonerSearchApiApplicationClient,
  private val allocationRepository: AllocationRepository,
  private val attendanceRepository: AttendanceRepository,
  private val waitingListRepository: WaitingListRepository,
  private val auditRepository: AuditRepository,
  private val eventReviewRepository: EventReviewRepository,
  private val appointmentAttendeeRepository: AppointmentAttendeeRepository,
  private val transactionHandler: TransactionHandler,
) : EventHandler<OffenderMergedEvent> {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(event: OffenderMergedEvent): Outcome {
    log.info("MERGE: Handling offender merged event {}", event)
    val newNumber = event.prisonerNumber()
    val oldNumber = event.removedPrisonerNumber()

    prisonerSearchApiClient.findByPrisonerNumber(newNumber)?.let { prisoner ->
      log.info("MERGE: Search $newNumber = ${prisoner.firstName} ${prisoner.lastName} ${prisoner.prisonId} ${prisoner.status}")
      prisoner.prisonId?.let { prisonId ->
        if (rolloutPrisonRepository.isActivitiesRolledOutAt(prisonId)) {
          transactionHandler.newSpringTransaction {
            processMergeEvent(
              OffenderMergeDetails(
                prisonCode = prisonId,
                oldNumber = oldNumber,
                newNumber = newNumber,
                newBookingId = prisoner.bookingId?.toLong(),
              ),
            )
          }
        } else {
          log.info("MERGE: $prisonId is not rolled out on activities and appointments - ignoring merge")
        }
      }
    }
    return Outcome.success()
  }

  private fun processMergeEvent(offenderMergeDetails: OffenderMergeDetails) {
    log.info("MERGE: Processing merge event from ${offenderMergeDetails.oldNumber} to ${offenderMergeDetails.newNumber} with new booking ID ${offenderMergeDetails.newBookingId}")

    // TODO: Prisoner number is not mutable at present this will need changing on any impacted entities!

    // Ideally we may be better off looking at modeling an Offender/Prisoner entity. This is quite a restructure though.

    mergeAllocations(offenderMergeDetails)
    mergeAttendances(offenderMergeDetails)
    mergeWaitingLists(offenderMergeDetails)
    mergeLocalAuditItems(offenderMergeDetails)
    mergeEventReviewItems(offenderMergeDetails)
    mergeAppointmentAttendees(offenderMergeDetails)

    log.info("MERGE: Recording the merge event into the local audit table")

    auditRepository.save(
      LocalAuditRecord(
        username = "MERGE-EVENT",
        auditType = AuditType.PRISONER,
        detailType = AuditEventType.PRISONER_MERGE,
        recordedTime = LocalDateTime.now(),
        prisonCode = offenderMergeDetails.prisonCode,
        message = "Prisoner number ${offenderMergeDetails.oldNumber} was merged to a new prisoner number ${offenderMergeDetails.newNumber}",
      ),
    )
  }

  private fun mergeAllocations(mergeDetails: OffenderMergeDetails) {
    allocationRepository.findByPrisonCodeAndPrisonerNumber(mergeDetails.prisonCode, mergeDetails.oldNumber)
      .onEach { allocation -> allocation.merge(mergeDetails) }
      .also { log.info("MERGE: Would alter ${it.size} allocation rows (including new booking id ${mergeDetails.newBookingId}") }
  }

  private fun mergeAttendances(offenderMergeDetails: OffenderMergeDetails) {
    attendanceRepository.findByPrisonerNumber(offenderMergeDetails.oldNumber)
      .onEach { attendance -> attendance.merge(offenderMergeDetails) }
      .also { log.info("MERGE: Would alter ${it.size} attendance rows") }
  }

  private fun mergeWaitingLists(mergeDetails: OffenderMergeDetails) {
    waitingListRepository.findByPrisonCodeAndPrisonerNumber(mergeDetails.prisonCode, mergeDetails.oldNumber)
      .onEach { waitingList -> waitingList.merge(mergeDetails) }
      .also { log.info("MERGE: Would alter ${it.size} waiting list rows") }
  }

  private fun mergeLocalAuditItems(offenderMergeDetails: OffenderMergeDetails) {
    auditRepository.findByPrisonCodeAndPrisonerNumber(offenderMergeDetails.prisonCode, offenderMergeDetails.oldNumber)
      .onEach { localAuditRecord -> localAuditRecord.merge(offenderMergeDetails) }
      .also { log.info("MERGE: Would alter ${it.size} local audit rows") }
  }

  private fun mergeEventReviewItems(mergeDetails: OffenderMergeDetails) {
    eventReviewRepository.findByPrisonCodeAndPrisonerNumber(mergeDetails.prisonCode, mergeDetails.oldNumber)
      .onEach { eventReview -> eventReview.merge(mergeDetails) }
      .also { log.info("MERGE: Would alter ${it.size} events of interest rows (including new booking Id)") }
  }

  private fun mergeAppointmentAttendees(offenderMergeDetails: OffenderMergeDetails) {
    appointmentAttendeeRepository.findByPrisonerNumber(offenderMergeDetails.oldNumber)
      .onEach { appointmentAttendee -> appointmentAttendee.merge(offenderMergeDetails) }
      .also {
        log.info("MERGE: Would alter ${it.size} appointment attendee rows (including new booking Id)")
      }
  }
}
