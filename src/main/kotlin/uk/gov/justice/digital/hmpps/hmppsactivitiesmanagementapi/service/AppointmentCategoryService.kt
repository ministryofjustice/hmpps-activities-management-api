package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toAppointmentCategorySummary

@Service
class AppointmentCategoryService(
  private val referenceCodeService: ReferenceCodeService,
) {
  fun getAll(includeInactive: Boolean) =
    if (includeInactive) {
      referenceCodeService.getAppointmentCategoryReferenceCodes().toAppointmentCategorySummary()
    } else {
      referenceCodeService.getAppointmentScheduleReasons().toAppointmentCategorySummary()
    }
}
