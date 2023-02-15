package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.toModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentCategoryRepository

@Service
class AppointmentCategoryService(
  private val appointmentCategoryRepository: AppointmentCategoryRepository
) {
  fun getAll(includeInactive: Boolean) =
    appointmentCategoryRepository.findAllOrdered().filter { includeInactive || it.active }.toModel()
}
