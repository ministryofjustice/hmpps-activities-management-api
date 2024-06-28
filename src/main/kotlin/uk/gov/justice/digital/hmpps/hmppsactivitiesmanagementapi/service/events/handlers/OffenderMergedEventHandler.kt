package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.Feature
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.LocalAuditRecord
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AuditRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.EventReviewRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.WaitingListRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentAttendeeRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.isActivitiesRolledOutAt
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.TransactionHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OffenderMergedEvent
import java.time.LocalDateTime

@Component
@Transactional(readOnly = true)
class OffenderMergedEventHandler(
  private val rolloutPrisonRepository: RolloutPrisonRepository,
  private val prisonApi: PrisonApiApplicationClient,
  private val allocationRepository: AllocationRepository,
  private val attendanceRepository: AttendanceRepository,
  private val waitingListRepository: WaitingListRepository,
  private val auditRepository: AuditRepository,
  private val eventReviewRepository: EventReviewRepository,
  private val appointmentAttendeeRepository: AppointmentAttendeeRepository,
  private val transactionHandler: TransactionHandler,
  featureSwitches: FeatureSwitches,
) : EventHandler<OffenderMergedEvent> {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  private val mergeIsEnabled = featureSwitches.isEnabled(Feature.OFFENDER_MERGED_ENABLED)

  init {
    log.info("MERGE: enabled='$mergeIsEnabled'")
  }

  override fun handle(event: OffenderMergedEvent): Outcome {
    val newNumber = event.prisonerNumber()
    val oldNumber = event.removedPrisonerNumber()

    prisonApi.getPrisonerDetailsLite(newNumber).let { prisoner ->
      prisoner.agencyId?.let { prisonCode ->
        if (rolloutPrisonRepository.isActivitiesRolledOutAt(prisonCode)) {
          transactionHandler.newSpringTransaction {
            processMergeEvent(
              OffenderMergeDetails(
                prisonCode = prisonCode,
                oldNumber = oldNumber,
                newNumber = newNumber,
                newBookingId = prisoner.bookingId,
              ),
            )
          }
        } else {
          log.info("MERGE: $prisonCode is not rolled out on activities and appointments - ignoring merge for new prisoner number $newNumber")
        }
      }
    }
    return Outcome.success()
  }

  private fun processMergeEvent(offenderMergeDetails: OffenderMergeDetails) {
    log.info("MERGE: Processing merge event from ${offenderMergeDetails.oldNumber} to ${offenderMergeDetails.newNumber} with new booking ID ${offenderMergeDetails.newBookingId}")

    mergeAllocations(offenderMergeDetails)
    mergeAttendances(offenderMergeDetails)
    mergeWaitingLists(offenderMergeDetails)
    mergeLocalAuditItems(offenderMergeDetails)
    mergeEventReviewItems(offenderMergeDetails)
    mergeAppointmentAttendees(offenderMergeDetails)

    auditRepository.save(
      LocalAuditRecord(
        username = "MERGE-EVENT",
        auditType = AuditType.PRISONER,
        detailType = AuditEventType.PRISONER_MERGE,
        recordedTime = LocalDateTime.now(),
        prisonCode = offenderMergeDetails.prisonCode,
        prisonerNumber = offenderMergeDetails.newNumber,
        message = "Prisoner number ${offenderMergeDetails.oldNumber} was merged to a new prisoner number ${offenderMergeDetails.newNumber}",
      ),
    )
  }

  private fun mergeAllocations(mergeDetails: OffenderMergeDetails) {
    allocationRepository.findByPrisonCodeAndPrisonerNumber(mergeDetails.prisonCode, mergeDetails.newNumber).filter { it.bookingId != mergeDetails.newBookingId }
      .also { log.info("MERGE: Would alter ${it.size} allocation rows to new booking id ${mergeDetails.newBookingId}") }

    allocationRepository.findByPrisonCodeAndPrisonerNumber(mergeDetails.prisonCode, mergeDetails.oldNumber)
      .also { log.info("MERGE: Would alter ${it.size} allocation rows to new prisoner number ${mergeDetails.newNumber}") }

    if (mergeIsEnabled) {
      log.info("MERGE: old allocation prisoner number ${mergeDetails.oldNumber} to new prisoner number ${mergeDetails.newNumber}")

      // Note: the order of the updates here is intentional here, whilst it won't break anything it avoids an
      // unnecessary update. If we were to update the old prisoner number to new prisoner number first then the
      // following update of booking id would also update the records we have just updated.
      allocationRepository.mergePrisonerToNewBookingId(mergeDetails.newNumber, mergeDetails.newBookingId)
      allocationRepository.mergeOldPrisonerNumberToNew(oldNumber = mergeDetails.oldNumber, newNumber = mergeDetails.newNumber, mergeDetails.newBookingId)
    }
  }

  private fun mergeAttendances(mergeDetails: OffenderMergeDetails) {
    attendanceRepository.findByPrisonerNumber(mergeDetails.oldNumber)
      .also { log.info("MERGE: Would alter ${it.size} attendance rows") }

    if (mergeIsEnabled) {
      log.info("MERGE: old attendance prisoner number ${mergeDetails.oldNumber} to new prisoner number ${mergeDetails.newNumber}")
      attendanceRepository.mergeOldPrisonerNumberToNew(oldNumber = mergeDetails.oldNumber, newNumber = mergeDetails.newNumber)
    }
  }

  private fun mergeWaitingLists(mergeDetails: OffenderMergeDetails) {
    waitingListRepository.findByPrisonCodeAndPrisonerNumber(mergeDetails.prisonCode, mergeDetails.newNumber).filter { it.bookingId != mergeDetails.newBookingId }
      .also { log.info("MERGE: Would alter ${it.size} waiting list rows to new booking id ${mergeDetails.newBookingId}") }

    waitingListRepository.findByPrisonCodeAndPrisonerNumber(mergeDetails.prisonCode, mergeDetails.oldNumber)
      .also { log.info("MERGE: Would alter ${it.size} waiting list rows to new prisoner number ${mergeDetails.newNumber}") }

    if (mergeIsEnabled) {
      log.info("MERGE: old waiting list prisoner number ${mergeDetails.oldNumber} to new prisoner number ${mergeDetails.newNumber}")
      waitingListRepository.mergePrisonerToNewBookingId(mergeDetails.newNumber, mergeDetails.newBookingId)
      waitingListRepository.mergeOldPrisonerNumberToNew(mergeDetails.oldNumber, mergeDetails.newNumber, mergeDetails.newBookingId)
    }
  }

  private fun mergeLocalAuditItems(mergeDetails: OffenderMergeDetails) {
    auditRepository.findByPrisonCodeAndPrisonerNumber(mergeDetails.prisonCode, mergeDetails.oldNumber)
      .also { log.info("MERGE: Would alter ${it.size} local audit rows to new prisoner number ${mergeDetails.newNumber}") }

    if (mergeIsEnabled) {
      log.info("MERGE: old local audit record prisoner number ${mergeDetails.oldNumber} to new prisoner number ${mergeDetails.newNumber}")
      auditRepository.mergeOldPrisonerNumberToNew(oldNumber = mergeDetails.oldNumber, newNumber = mergeDetails.newNumber)
    }
  }

  private fun mergeEventReviewItems(mergeDetails: OffenderMergeDetails) {
    eventReviewRepository.findByPrisonCodeAndPrisonerNumber(mergeDetails.prisonCode, mergeDetails.newNumber).filter { it.bookingId?.toLong() != mergeDetails.newBookingId }
      .also { log.info("MERGE: Would alter ${it.size} event review rows to new booking id ${mergeDetails.newBookingId}") }

    eventReviewRepository.findByPrisonCodeAndPrisonerNumber(mergeDetails.prisonCode, mergeDetails.oldNumber)
      .also { log.info("MERGE: Would alter ${it.size} events of interest rows to new prisoner number ${mergeDetails.newNumber}") }

    if (mergeIsEnabled) {
      log.info("MERGE: old event review prisoner number ${mergeDetails.oldNumber} to new prisoner number ${mergeDetails.newNumber}")
      eventReviewRepository.mergePrisonerToNewBookingId(mergeDetails.newNumber, mergeDetails.newBookingId)
      eventReviewRepository.mergeOldPrisonerNumberToNew(oldNumber = mergeDetails.oldNumber, mergeDetails.newNumber, mergeDetails.newBookingId)
    }
  }

  private fun mergeAppointmentAttendees(mergeDetails: OffenderMergeDetails) {
    appointmentAttendeeRepository.findByPrisonerNumber(mergeDetails.newNumber).filter { it.bookingId != mergeDetails.newBookingId }
      .also { log.info("MERGE: Would alter ${it.size} appointment attendee rows to new booking id ${mergeDetails.newBookingId}") }

    appointmentAttendeeRepository.findByPrisonerNumber(mergeDetails.oldNumber)
      .also { log.info("MERGE: Would alter ${it.size} appointment attendee rows to new prisoner number ${mergeDetails.newNumber}") }

    if (mergeIsEnabled) {
      log.info("MERGE: old appointment attendee prisoner number ${mergeDetails.oldNumber} to new prisoner number ${mergeDetails.newNumber}")
      appointmentAttendeeRepository.mergePrisonerToNewBookingId(mergeDetails.newNumber, mergeDetails.newBookingId)
      appointmentAttendeeRepository.mergeOldPrisonerNumberToNew(oldNumber = mergeDetails.oldNumber, newNumber = mergeDetails.newNumber, mergeDetails.newBookingId)
    }
  }
}

private data class OffenderMergeDetails(val prisonCode: String, val newNumber: String, val oldNumber: String, val newBookingId: Long? = null)
