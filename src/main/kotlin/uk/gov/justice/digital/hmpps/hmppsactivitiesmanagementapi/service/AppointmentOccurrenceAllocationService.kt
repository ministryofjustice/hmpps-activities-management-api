package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentCancelledOnTransferEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceAllocationRepository

@Service
@Transactional
class AppointmentOccurrenceAllocationService(
  private val prisonApiClient: PrisonApiApplicationClient,
  private val appointmentInstanceRepository: AppointmentInstanceRepository,
  private val appointmentOccurrenceAllocationRepository: AppointmentOccurrenceAllocationRepository,
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
          appointmentOccurrenceAllocationRepository.findById(it.appointmentOccurrenceAllocationId)
            .ifPresent { allocation ->
              if (allocation.isIndividualAppointment()) {
                allocation.removeOccurrence(allocation.appointmentOccurrence)

                log.info(
                  "Removed appointment occurrence '${allocation.appointmentOccurrence.appointmentOccurrenceId}' " +
                    "as it is part of an individual appointment. This will also remove allocation '${allocation.appointmentOccurrenceAllocationId}' " +
                    "for prisoner '$prisonerNumber'.",
                )
              } else {
                allocation.removeFromAppointmentOccurrence()
                log.info("Removed the appointment occurrence allocation '${it.appointmentOccurrenceAllocationId}' for prisoner $prisonerNumber at prison $prisonCode on ${it.appointmentDate}.")
              }

              auditService.logEvent(
                AppointmentCancelledOnTransferEvent(
                  appointmentId = it.appointmentId,
                  appointmentOccurrenceId = it.appointmentOccurrenceId,
                  prisonCode = it.prisonCode,
                  prisonerNumber = it.prisonerNumber,
                ),
              )
            }
        }
    }
  }
}
