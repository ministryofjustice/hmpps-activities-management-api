package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceAllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowIllegalArgument
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound

@Service
class AppointmentService(
  private val appointmentCategoryRepository: AppointmentCategoryRepository,
  private val appointmentRepository: AppointmentRepository,
  private val appointmentOccurrenceRepository: AppointmentOccurrenceRepository,
  private val appointmentOccurrenceAllocationRepository: AppointmentOccurrenceAllocationRepository,
  private val appointmentInstanceRepository: AppointmentInstanceRepository,
  private val prisonApiClient: PrisonApiClient
) {
  fun getAppointmentById(appointmentId: Long) =
    appointmentRepository.findOrThrowNotFound(appointmentId).toModel()

  // @Transactional
  fun createAppointment(request: AppointmentCreateRequest): Appointment {
    appointmentCategoryRepository.findOrThrowIllegalArgument(request.categoryId)
    // val category = appointmentCategoryRepository.findOrThrowIllegalArgument(request.categoryId)

//    val prisoners = request.prisonerNumbers.map{ prisonerNumber ->
//      prisonApiClient.getPrisonerDetails(prisonerNumber, false)
//        .let { prisoner -> prisoner ?: throw IllegalArgumentException("Prisoner with prisoner number $prisonerNumber not found.") }
//        .let { prisoner -> prisoner.activeFlag ?: throw IllegalStateException("Prisoner $prisonerNumber is not active.") }
//        .let { prisoner -> prisoner.agencyId != request.prisonCode ?: throw IllegalStateException("Prisoner $prisonerNumber is not active.") }
//    }

    TODO("Not yet implemented")
  }
}
