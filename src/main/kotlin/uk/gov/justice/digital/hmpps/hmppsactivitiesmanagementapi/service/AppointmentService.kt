package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
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
  private val appointmentInstanceRepository: AppointmentInstanceRepository
) {
  fun getAppointmentById(appointmentId: Long) =
    appointmentRepository.findOrThrowNotFound(appointmentId).toModel()

  fun createAppointment(request: AppointmentCreateRequest): Appointment {
    val category = appointmentCategoryRepository.findOrThrowIllegalArgument(request.categoryId)

    TODO("Not yet implemented")
  }
}
