package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrenceAllocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import java.security.Principal
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment as AppointmentModel

@Service
class AppointmentOccurrenceService(
  private val appointmentRepository: AppointmentRepository,
  private val appointmentOccurrenceRepository: AppointmentOccurrenceRepository,
  private val referenceCodeService: ReferenceCodeService,
  private val locationService: LocationService,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
) {
  fun updateAppointmentOccurrence(appointmentOccurrenceId: Long, request: AppointmentOccurrenceUpdateRequest, principal: Principal): AppointmentModel {
    val appointmentOccurrence = appointmentOccurrenceRepository.findOrThrowNotFound(appointmentOccurrenceId)
    val appointment = appointmentOccurrence.appointment

    val updated = LocalDateTime.now()
    val updatedBy = principal.name

    request.categoryCode?.apply {
      failIfCategoryNotFound(this)
      appointment.categoryCode = this
      appointment.updated = updated
      appointment.updatedBy = updatedBy
    }

    if (request.inCell == true) {
      appointmentOccurrence.internalLocationId = null
      appointmentOccurrence.inCell = true
      appointmentOccurrence.updated = updated
      appointmentOccurrence.updatedBy = updatedBy
    } else {
      request.internalLocationId?.apply {
        failIfLocationNotFound(this, appointment.prisonCode)
        appointmentOccurrence.internalLocationId = this
        appointmentOccurrence.inCell = false
        appointmentOccurrence.updated = updated
        appointmentOccurrence.updatedBy = updatedBy
      }
    }

    request.startDate?.apply {
      appointmentOccurrence.startDate = this
      appointmentOccurrence.updated = updated
      appointmentOccurrence.updatedBy = updatedBy
    }

    request.startTime?.apply {
      appointmentOccurrence.startTime = this
      appointmentOccurrence.updated = updated
      appointmentOccurrence.updatedBy = updatedBy
    }

    request.endTime?.apply {
      appointmentOccurrence.endTime = this
      appointmentOccurrence.updated = updated
      appointmentOccurrence.updatedBy = updatedBy
    }

    request.comment?.apply {
      appointmentOccurrence.comment = this
      appointmentOccurrence.updated = updated
      appointmentOccurrence.updatedBy = updatedBy
    }

    request.prisonerNumbers?.apply {
      val prisonerMap = prisonerSearchApiClient.findByPrisonerNumbers(this).block()!!
        .filter { prisoner -> prisoner.prisonId == appointment.prisonCode }
        .associateBy { it.prisonerNumber }

      failIfMissingPrisoners(this, prisonerMap)

      appointmentOccurrence.allocations()
        .filter { !prisonerMap.containsKey(it.prisonerNumber) }
        .forEach { appointmentOccurrence.removeAllocation(it) }

      val prisonerAllocationMap = appointmentOccurrence.allocations().associateBy { it.prisonerNumber }
      val newPrisoners = prisonerMap.filter { !prisonerAllocationMap.containsKey(it.key) }.values

      newPrisoners.forEach {
        appointmentOccurrence.addAllocation(
          AppointmentOccurrenceAllocation(
            appointmentOccurrence = appointmentOccurrence,
            prisonerNumber = it.prisonerNumber,
            bookingId = it.bookingId!!.toLong(),
          ),
        )
      }

      appointmentOccurrence.updated = updated
      appointmentOccurrence.updatedBy = updatedBy
    }

    return appointmentRepository.saveAndFlush(appointment).toModel()
  }

  private fun failIfCategoryNotFound(categoryCode: String) {
    referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT)[categoryCode]
      ?: throw IllegalArgumentException("Appointment Category with code $categoryCode not found or is not active")
  }

  private fun failIfLocationNotFound(internalLocationId: Long, prisonCode: String) {
    locationService.getLocationsForAppointmentsMap(prisonCode)[internalLocationId]
      ?: throw IllegalArgumentException("Appointment location with id $internalLocationId not found in prison '$prisonCode'")
  }

  private fun failIfMissingPrisoners(prisonerNumbers: List<String>, prisonerMap: Map<String, Prisoner>) {
    prisonerNumbers.filter { number -> !prisonerMap.containsKey(number) }.let {
      if (it.any()) throw IllegalArgumentException("Prisoner(s) with prisoner number(s) '${it.joinToString("', '")}' not found, were inactive or are residents of a different prison.")
    }
  }
}
