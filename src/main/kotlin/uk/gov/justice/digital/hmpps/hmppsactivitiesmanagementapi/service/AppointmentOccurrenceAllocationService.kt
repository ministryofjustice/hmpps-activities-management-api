package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentCancelledOnTransferEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentAttendeeRepository
import java.time.LocalDateTime

@Service
@Transactional
class AppointmentOccurrenceAllocationService(
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
            .ifPresent { allocation ->
              if (allocation.isIndividualAppointment()) {
                allocation.removeAppointment(allocation.appointment)

                log.info(
                  "Removed appointment occurrence '${allocation.appointment.appointmentId}' " +
                    "as it is part of an individual appointment. This will also remove allocation '${allocation.appointmentAttendeeId}' " +
                    "for prisoner '$prisonerNumber'.",
                )
              } else {
                allocation.removeFromAppointment()
                log.info("Removed the appointment occurrence allocation '${it.appointmentAttendeeId}' for prisoner $prisonerNumber at prison $prisonCode on ${it.appointmentDate}.")
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
