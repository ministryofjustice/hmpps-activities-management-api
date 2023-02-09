package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceAllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceRepository
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
  private val appointmentOccurrenceRepository: AppointmentOccurrenceRepository,
  private val appointmentOccurrenceAllocationRepository: AppointmentOccurrenceAllocationRepository,
  private val appointmentInstanceRepository: AppointmentInstanceRepository,
  private val prisonerSearchApiClient: PrisonerSearchApiClient
) {
  fun getAppointmentById(appointmentId: Long) =
    appointmentRepository.findOrThrowNotFound(appointmentId).toModel()

  // @Transactional
  fun createAppointment(request: AppointmentCreateRequest, principal: Principal): AppointmentModel {
    appointmentCategoryRepository.findOrThrowIllegalArgument(request.categoryId!!)
    val category = appointmentCategoryRepository.findOrThrowIllegalArgument(request.categoryId)

    // val prisoners = prisonerSearchApiClient.findByPrisonerNumbers(request.prisonerNumbers).block()!!

    // prisoners.filter { prisoner -> prisoner.prisonId !== request.prisonCode }
    // val inactivePrisoners = prisoners.filter { prisoner -> prisoner.prisonId !== request.prisonCode }

    // val missingPrisoners = request.prisonerNumbers.filter { number -> prisoners.all { prisoner -> prisoner.prisonerNumber === number } }

//    val prisoners = request.prisonerNumbers.map{ prisonerNumber ->
//      prisonApiClient.getPrisonerDetails(prisonerNumber, false)
//        .let { prisoner -> prisoner ?: throw IllegalArgumentException("Prisoner with prisoner number $prisonerNumber not found.") }
//        .let { prisoner -> prisoner.activeFlag ?: throw IllegalStateException("Prisoner $prisonerNumber is not active.") }
//        .let { prisoner -> prisoner.agencyId != request.prisonCode ?: throw IllegalStateException("Prisoner $prisonerNumber is not active.") }
//    }

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
          request.prisonerNumbers.map { number ->
            AppointmentOccurrenceAllocationEntity(
              appointmentOccurrence = this,
              prisonerNumber = number,
              bookingId = -1
            )
          }.forEach { allocation -> this.addAllocation(allocation) }
        }
      )
    }.let { (appointmentRepository.saveAndFlush(it)).toModel() }
  }
}
