package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment

private const val VIDEO_LINK_COURT_APPOINTMENT_CATEGORY_CODE = "VLB"
private const val VIDEO_LINK_PROBATION_APPOINTMENT_CATEGORY_CODE = "VLPM"

object AppointmentManagement {
  fun isManagedByTheService(appointmentInstance: AppointmentInstance) = isManagedByTheService(appointmentInstance.categoryCode)

  /*
   * Court and probation video link appointments are not managed by the Activities and Appointments service. These types of
   * appointment are managed and owned by the book a video link service.
   */
  private fun isManagedByTheService(value: String) = value !in listOf(
    VIDEO_LINK_COURT_APPOINTMENT_CATEGORY_CODE,
    VIDEO_LINK_PROBATION_APPOINTMENT_CATEGORY_CODE,
  )
}
