package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceAllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceRepository

@Service
class AppointmentOccurrenceAllocationService(
  private val prisonApiClient: PrisonApiApplicationClient,
  private val appointmentInstanceRepository: AppointmentInstanceRepository,
  private val appointmentOccurrenceRepository: AppointmentOccurrenceRepository,
  private val appointmentOccurrenceAllocationRepository: AppointmentOccurrenceAllocationRepository,
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
      appointmentInstanceRepository.findByPrisonCodeAndPrisonerNumberFromToday(prisonCode, prisonerNumber)
        .forEach {
          appointmentOccurrenceAllocationRepository.findById(it.appointmentOccurrenceAllocationId).ifPresent { allocation ->
            if (allocation.appointmentOccurrence.allocations()
              .none { currentAllocation -> currentAllocation.appointmentOccurrenceAllocationId != it.appointmentOccurrenceAllocationId }
            ) {
              appointmentOccurrenceRepository.delete(allocation.appointmentOccurrence)
              log.info("Removed appointment occurrence'${allocation.appointmentOccurrence.appointmentOccurrenceId}' as it is now orphaned")
            }
          }
          appointmentOccurrenceAllocationRepository.deleteById(it.appointmentOccurrenceAllocationId)
          log.info("Removed appointment '${it.appointmentDescription}' for prisoner $prisonerNumber at prison $prisonCode on ${it.appointmentDate}.")
        }
    }
  }
}
