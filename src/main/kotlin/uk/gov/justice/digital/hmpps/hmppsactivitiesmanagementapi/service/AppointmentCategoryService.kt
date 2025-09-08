package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentParentCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.CategoryStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentCategoryRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentParentCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import java.util.Optional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentCategorySummary as ModelAppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AppointmentCategory as ModelAppointmentCategory

@Service
@Transactional
class AppointmentCategoryService(
  private val appointmentCategoryRepository: AppointmentCategoryRepository,
  private val appointmentParentCategoryRepository: AppointmentParentCategoryRepository,
) {

  fun get(): List<AppointmentCategorySummary> = appointmentCategoryRepository.findAll()
    .filter { it.status == CategoryStatus.ACTIVE }
    .map {
      ModelAppointmentCategorySummary(it.code, it.description)
    }

  fun create(request: AppointmentCategoryRequest): ModelAppointmentCategory {
    require(!appointmentCategoryRepository.findByCode(request.code).isPresent) { "Appointment Category ${request.code} is found" }
    val appointmentParentCategory = validateAppointmentParentCategory(request.appointmentParentCategoryId)

    return appointmentCategoryRepository.save(
      AppointmentCategory(
        code = request.code,
        description = request.description,
        appointmentParentCategory = appointmentParentCategory.get(),
        status = request.status,
      ),
    ).toModel()
  }

  fun update(appointmentCategoryId: Long, request: AppointmentCategoryRequest): ModelAppointmentCategory = appointmentCategoryRepository.findOrThrowNotFound(appointmentCategoryId)
    .let { appointmentCategory ->
      val appointmentParentCategory = validateAppointmentParentCategory(request.appointmentParentCategoryId)
      appointmentCategory.updateCategory(request, appointmentParentCategory.get())
      return appointmentCategoryRepository.save(appointmentCategory).toModel()
    }

  fun delete(appointmentCategoryId: Long) = appointmentCategoryRepository.findOrThrowNotFound(appointmentCategoryId)
    .let { appointmentCategory -> appointmentCategoryRepository.delete(appointmentCategory) }

  private fun validateAppointmentParentCategory(appointmentParentCategoryId: Long?): Optional<AppointmentParentCategory> = appointmentParentCategoryRepository.findById(appointmentParentCategoryId!!)
    .also { appointmentParentCategory ->
      require(appointmentParentCategory == null || appointmentParentCategory.isPresent) { "Appointment Parent Category $appointmentParentCategoryId not found" }
    }
}
