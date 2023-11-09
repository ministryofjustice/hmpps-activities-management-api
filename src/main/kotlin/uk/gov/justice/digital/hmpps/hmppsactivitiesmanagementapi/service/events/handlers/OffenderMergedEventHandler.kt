package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentAttendeeRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AuditRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.EventReviewRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.WaitingListRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.TransactionHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OffenderMergedEvent

@Component
@Transactional(readOnly = true)
class OffenderMergedEventHandler(
  private val rolloutPrisonRepository: RolloutPrisonRepository,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
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
        if (rolloutPrisonRepository.prisonIsRolledOut(prisonId)) {
          transactionHandler.newSpringTransaction {
            processMergeEvent(prisonId, newNumber, oldNumber, prisoner.bookingId?.let { it.toInt() } ?: 0)
          }
        } else {
          log.info("MERGE: $prisonId is not rolled out on activities and appointments - ignoring merge")
        }
      }
    }
    return Outcome.success()
  }

  private fun RolloutPrisonRepository.prisonIsRolledOut(prisonCode: String) =
    this.findByCode(prisonCode)?.isActivitiesRolledOut() == true

  fun processMergeEvent(prisonCode: String, newNumber: String, oldNumber: String, newBookingId: Int) {
    log.info("MERGE: Processing merge event from $oldNumber to $newNumber with new booking Id $newBookingId")

    // Update all allocation rows - any status
    val allocations = allocationRepository.findByPrisonCodeAndPrisonerNumber(prisonCode, oldNumber)
    log.info("MERGE: Would alter ${allocations.size} allocation rows (including new booking id $newBookingId")

    // Update all attendance rows - past and present
    val attendances = attendanceRepository.findByPrisonerNumber(oldNumber)
    log.info("MERGE: Would alter ${attendances.size} attendance rows")

    // Update all waiting list rows
    val waitingListItems = waitingListRepository.findByPrisonCodeAndPrisonerNumber(prisonCode, oldNumber)
    log.info("MERGE: Would alter ${waitingListItems.size} waiting list rows")

    // Update all local audit rows
    val localAuditItems = auditRepository.findByPrisonCodeAndPrisonerNumber(prisonCode, oldNumber)
    log.info("MERGE: Would alter ${localAuditItems.size} local audit rows")

    // Update all event review rows
    val eventReviewItems = eventReviewRepository.findByPrisonCodeAndPrisonerNumber(prisonCode, oldNumber)
    log.info("MERGE: Would alter ${eventReviewItems.size} events of interest rows (including new booking Id)")

    // Update appointment attendees
    val appointmentAttendees = appointmentAttendeeRepository.findByPrisonerNumber(oldNumber)
    log.info("MERGE: Would alter ${appointmentAttendees.size} appointment attendee rows (including new booking Id)")

    // TODO: Update prisoner exclusions from old to new number

    /*
    val auditRow = LocalAuditRecord(
      username = "MERGE-EVENT",
      auditType = AuditType.PRISONER,
      detailType = AuditEventType.PRISONER_MERGE,
      recordedTime = LocalDateTime.now(),
      prisonCode = prisonCode,
      message = "Prisoner number $oldNumber was merged to a new prisoner number $newNumber",
    )
    auditRepository.save(auditRow)
    */
  }
}
