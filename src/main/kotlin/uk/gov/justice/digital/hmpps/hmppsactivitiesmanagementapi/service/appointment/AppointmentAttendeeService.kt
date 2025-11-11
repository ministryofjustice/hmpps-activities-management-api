package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentManagement
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentCancelledOnTransferEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentAttendeeRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.AppointmentAttendeeRemovalReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AuditService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.TransactionHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import java.time.LocalDateTime

@Service
@Transactional
class AppointmentAttendeeService(
  private val appointmentAttendeeRepository: AppointmentAttendeeRepository,
  private val appointmentInstanceRepository: AppointmentInstanceRepository,
  private val appointmentAttendeeRemovalReasonRepository: AppointmentAttendeeRemovalReasonRepository,
  private val transactionHandler: TransactionHandler,
  private val outboundEventsService: OutboundEventsService,
  private val auditService: AuditService,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  /**
   * Prisoners will only be removed from appointments which are mastered by the Activities and Appointments service. For
   * example, some appointment types, such as video link court and video link probation, are managed by a different
   * service (BVLS).
   */
  fun removePrisonerFromFutureAppointments(prisonCode: String, prisonerNumber: String, removedTime: LocalDateTime, removalReasonId: Long, removedBy: String) {
    val removalReason = appointmentAttendeeRemovalReasonRepository.findOrThrowNotFound(removalReasonId)
    appointmentInstanceRepository.findByPrisonCodeAndPrisonerNumberFromNow(prisonCode, prisonerNumber).filter(AppointmentManagement::isManagedByTheService)
      .forEach {
        appointmentAttendeeRepository.findById(it.appointmentAttendeeId)
          .ifPresent { attendee ->
            transactionHandler.newSpringTransaction {
              attendee.remove(removedTime, removalReason, removedBy)
            }.also { updatedAttendee ->
              outboundEventsService.send(OutboundEvent.APPOINTMENT_INSTANCE_DELETED, updatedAttendee.appointmentAttendeeId, categoryCode = updatedAttendee.appointment.categoryCode)
            }

            log.info("Removed appointment attendee with id '${it.appointmentAttendeeId}' for prisoner '$prisonerNumber' from appointment with id '${it.appointmentId}'")

            // TODO: Rename to more generic PrisonerRemovedFromFutureAppointments and pass in reason
            auditService.logEvent(
              AppointmentCancelledOnTransferEvent(
                appointmentSeriesId = it.appointmentSeriesId,
                appointmentId = it.appointmentId,
                prisonCode = it.prisonCode,
                prisonerNumber = it.prisonerNumber,
                createdAt = removedTime,
              ),
            )
          }
      }
  }
}
