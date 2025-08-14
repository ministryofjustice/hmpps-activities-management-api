package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment

import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool

class AppointmentManagementTest {
  private val appointmentInstance: AppointmentInstance = mock()

  @Test
  fun `should not manage video link court appointment`() {
    whenever(appointmentInstance.categoryCode) doReturn "VLB"
    AppointmentManagement.isManagedByTheService(appointmentInstance) isBool false
  }

  @Test
  fun `should not manage video link probation appointment`() {
    whenever(appointmentInstance.categoryCode) doReturn "VLPM"
    AppointmentManagement.isManagedByTheService(appointmentInstance) isBool false
  }

  @Test
  fun `should manage chaplaincy appointment`() {
    whenever(appointmentInstance.categoryCode) doReturn "CHAP"
    AppointmentManagement.isManagedByTheService(appointmentInstance) isBool true
  }
}
