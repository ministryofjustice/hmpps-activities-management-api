package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment as AppointmentModel

@Service
class AppointmentOccurrenceService(
  private val appointmentRepository: AppointmentRepository,
  private val appointmentOccurrenceRepository: AppointmentOccurrenceRepository,
  private val referenceCodeService: ReferenceCodeService,
  private val locationService: LocationService,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
) {
  fun updateAppointmentOccurrence(appointmentOccurrenceId: Long, request: AppointmentOccurrenceUpdateRequest): AppointmentModel {
    val appointment = appointmentRepository.findByAppointmentOccurrenceId(appointmentOccurrenceId) ?: throw IllegalArgumentException("Parent appointment for appointment occurrence with id $appointmentOccurrenceId not found")

    val appointmentOccurrence = appointmentOccurrenceRepository.findOrThrowNotFound(appointmentOccurrenceId)

    failIfCategoryNotFound(request.categoryCode!!)

    failIfLocationNotFound(request, appointment.prisonCode)

    val prisonerMap = prisonerSearchApiClient.findByPrisonerNumbers(request.prisonerNumbers ?: emptyList()).block()!!
      .filter { prisoner -> prisoner.prisonId == appointment.prisonCode }
      .associateBy { it.prisonerNumber }

    failIfMissingPrisoners(request.prisonerNumbers ?: emptyList(), prisonerMap)

    return appointment.toModel()
  }

  private fun failIfCategoryNotFound(categoryCode: String) {
    referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT)[categoryCode]
      ?: throw IllegalArgumentException("Appointment Category with code $categoryCode not found or is not active")
  }

  private fun failIfLocationNotFound(request: AppointmentOccurrenceUpdateRequest, prisonCode: String) {
    if (request.inCell != true && request.internalLocationId != null) {
      locationService.getLocationsForAppointmentsMap(prisonCode)[request.internalLocationId]
        ?: throw IllegalArgumentException("Appointment location with id ${request.internalLocationId} not found in prison '$prisonCode'")
    }
  }

  private fun failIfMissingPrisoners(prisonerNumbers: List<String>, prisonerMap: Map<String, Prisoner>) {
    prisonerNumbers.filter { number -> !prisonerMap.containsKey(number) }.let {
      if (it.any()) throw IllegalArgumentException("Prisoner(s) with prisoner number(s) '${it.joinToString("', '")}' not found, were inactive or are residents of a different prison.")
    }
  }
}
