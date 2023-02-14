package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowIllegalArgument
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import java.security.Principal
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Appointment as AppointmentEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrence as AppointmentOccurrenceEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrenceAllocation as AppointmentOccurrenceAllocationEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment as AppointmentModel

@Service
class AppointmentService(
  private val appointmentCategoryRepository: AppointmentCategoryRepository,
  private val appointmentRepository: AppointmentRepository,
  private val locationService: LocationService,
  private val prisonerSearchApiClient: PrisonerSearchApiClient
) {
  fun getAppointmentById(appointmentId: Long) =
    appointmentRepository.findOrThrowNotFound(appointmentId).toModel()

  fun createAppointment(request: AppointmentCreateRequest, principal: Principal): AppointmentModel {
    // TODO: Check that the supplied prison code is in the principle's case load

    val category = appointmentCategoryRepository.findOrThrowIllegalArgument(request.categoryId!!)

    failIfLocationNotFound(request)

    val prisonerMap = prisonerSearchApiClient.findByPrisonerNumbers(request.prisonerNumbers).block()!!
      .filter { prisoner -> prisoner.prisonId == request.prisonCode }
      .associateBy { it.prisonerNumber}

    failIfMissingPrisoners(request.prisonerNumbers, prisonerMap)

    return AppointmentEntity(
      category = category,
      prisonCode = request.prisonCode!!,
      internalLocationId = if (request.inCell) null else request.internalLocationId,
      inCell = request.inCell,
      startDate = request.startDate!!,
      startTime = request.startTime!!,
      endTime = request.endTime,
      comment = request.comment,
      createdBy = principal.name
    ).apply {
      this.addOccurrence(
        AppointmentOccurrenceEntity(
          appointment = this,
          internalLocationId = this.internalLocationId,
          inCell = this.inCell,
          startDate = this.startDate,
          startTime = this.startTime,
          endTime = this.endTime
        ).apply {
          prisonerMap.map { (_, prisoner) ->
            AppointmentOccurrenceAllocationEntity(
              appointmentOccurrence = this,
              prisonerNumber = prisoner.prisonerNumber,
              bookingId = prisoner.bookingId!!.toLong()
            )
          }.forEach { allocation -> this.addAllocation(allocation) }
        }
      )
    }.let { (appointmentRepository.saveAndFlush(it)).toModel() }
  }

  private fun failIfLocationNotFound(request: AppointmentCreateRequest) {
    if (!request.inCell) {
      locationService.getLocationsForAppointments(request.prisonCode!!)
        ?.firstOrNull { location -> location.locationId == request.internalLocationId }
        ?: throw IllegalArgumentException("Appointment location with id ${request.internalLocationId} not found in prison '${request.prisonCode}'")
    }
  }

  private fun failIfMissingPrisoners(prisonerNumbers: List<String>, prisonerMap: Map<String, Prisoner>) {
    prisonerNumbers.filter { number -> !prisonerMap.containsKey(number) }.let {
      if (it.any()) throw IllegalArgumentException("Prisoner(s) with prisoner number(s) '${it.joinToString("', '")}' not found, were inactive or are residents of a different prison.")
    }
  }
}
