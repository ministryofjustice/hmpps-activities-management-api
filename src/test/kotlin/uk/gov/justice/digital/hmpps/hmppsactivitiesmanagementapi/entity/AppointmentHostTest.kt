package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentHostPrisonStaff
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentHost
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PRISON_STAFF_APPOINTMENT_HOST_ID

class AppointmentHostTest {
  @Test
  fun `entity to model mapping`() {
    val entity = appointmentHostPrisonStaff()
    entity.toModel() isEqualTo AppointmentHost(
      PRISON_STAFF_APPOINTMENT_HOST_ID,
      "Prison staff",
    )
  }
}
