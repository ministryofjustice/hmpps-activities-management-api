package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentCancelledOnTransferEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentAttendeeRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentInstanceRepository
import java.time.LocalDateTime

@Service
@Transactional
class AppointmentAttendeeService(
  private val prisonApiClient: PrisonApiApplicationClient,
  private val appointmentInstanceRepository: AppointmentInstanceRepository,
  private val appointmentAttendeeRepository: AppointmentAttendeeRepository,
  private val auditService: AuditService,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun cancelFutureOffenderAppointments(prisonCode: String, prisonerNumber: String) {
    prisonApiClient.getPrisonerDetails(
      prisonerNumber = prisonerNumber,
      fullInfo = true,
      extraInfo = true,
    ).block()?.let {
      appointmentInstanceRepository.findByPrisonCodeAndPrisonerNumberFromNow(prisonCode, prisonerNumber)
        .forEach {
          appointmentAttendeeRepository.findById(it.appointmentAttendeeId)
            .ifPresent { attendee ->
              if (attendee.isIndividualAppointment()) {
                attendee.removeAppointment(attendee.appointment)

                log.info(
                  "Removed appointment '${attendee.appointment.appointmentId}' " +
                    "as it is part of an individual appointment series. This will also remove attendee '${attendee.appointmentAttendeeId}' " +
                    "for prisoner '$prisonerNumber'.",
                )
              } else {
                attendee.removeFromAppointment()
                log.info("Removed the appointment attendee '${it.appointmentAttendeeId}' for prisoner $prisonerNumber at prison $prisonCode on ${it.appointmentDate}.")
              }

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
}
