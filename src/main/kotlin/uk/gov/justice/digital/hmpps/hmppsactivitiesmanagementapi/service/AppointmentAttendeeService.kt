package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentCancelledOnTransferEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentAttendeeRemovalReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentAttendeeRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import java.time.LocalDateTime

@Service
@Transactional
class AppointmentAttendeeService(
  private val appointmentAttendeeRemovalReasonRepository: AppointmentAttendeeRemovalReasonRepository,
  private val appointmentInstanceRepository: AppointmentInstanceRepository,
  private val appointmentAttendeeRepository: AppointmentAttendeeRepository,
  private val transactionHandler: TransactionHandler,
  private val auditService: AuditService,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun removePrisonerFromFutureAppointments(prisonCode: String, prisonerNumber: String, removalReasonId: Long, removedBy: String) {
    val removedTime = LocalDateTime.now()
    val removalReason = appointmentAttendeeRemovalReasonRepository.findOrThrowNotFound(removalReasonId)
    appointmentInstanceRepository.findByPrisonCodeAndPrisonerNumberFromNow(prisonCode, prisonerNumber)
      .forEach {
        appointmentAttendeeRepository.findById(it.appointmentAttendeeId)
          .ifPresent { attendee ->
            transactionHandler.newSpringTransaction {
              attendee.remove(removedTime, removalReason, removedBy)
            }

            log.info("Removed appointment attendee with id '${it.appointmentAttendeeId}' for prisoner '$prisonerNumber' from appointment with id '${it.appointmentId}'")

            auditService.logEvent(
              AppointmentCancelledOnTransferEvent(
                appointmentSeriesId = it.appointmentSeriesId,
                appointmentId = it.appointmentId,
                prisonCode = it.prisonCode,
                prisonerNumber = it.prisonerNumber,
                createdAt = LocalDateTime.now(),
              ),
            )
          }
      }
  }
}
