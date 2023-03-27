package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode

class ReferenceCodeServiceTest {
  private val prisonApiClient: PrisonApiClient = mock()

  private val referenceCodeService = ReferenceCodeService(prisonApiClient)

  @Test
  fun `getReferenceCodes for a domain returns reference codes`() {
    whenever(prisonApiClient.getReferenceCodes("INT_SCH_RSN"))
      .thenReturn(
        listOf(
          appointmentCategoryReferenceCode("AC1", "Appointment Category 1"),
          appointmentCategoryReferenceCode("AC2", "Appointment Category 2"),
          appointmentCategoryReferenceCode("AC3", "Appointment Category 3"),
        ),
      )

    val locations = referenceCodeService.getReferenceCodes(ReferenceCodeDomain.APPOINTMENT_CATEGORY)

    assertThat(locations).containsExactly(
      appointmentCategoryReferenceCode("AC1", "Appointment Category 1"),
      appointmentCategoryReferenceCode("AC2", "Appointment Category 2"),
      appointmentCategoryReferenceCode("AC3", "Appointment Category 3"),
    )
  }

  @Test
  fun `getReferenceCodesMap for a domain returns mapped reference codes`() {
    whenever(prisonApiClient.getReferenceCodes("INT_SCH_RSN"))
      .thenReturn(
        listOf(
          appointmentCategoryReferenceCode("AC1", "Appointment Category 1"),
          appointmentCategoryReferenceCode("AC2", "Appointment Category 2"),
          appointmentCategoryReferenceCode("AC3", "Appointment Category 3"),
        ),
      )

    val locations = referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY)

    assertThat(locations).isEqualTo(
      mapOf(
        "AC1" to appointmentCategoryReferenceCode("AC1", "Appointment Category 1"),
        "AC2" to appointmentCategoryReferenceCode("AC2", "Appointment Category 2"),
        "AC3" to appointmentCategoryReferenceCode("AC3", "Appointment Category 3"),
      ),
    )
  }

  @Test
  fun `getScheduleReasons for an event type returns reference codes`() {
    whenever(prisonApiClient.getScheduleReasons("APP"))
      .thenReturn(
        listOf(
          appointmentCategoryReferenceCode("AC1", "Appointment Category 1"),
          appointmentCategoryReferenceCode("AC2", "Appointment Category 2"),
          appointmentCategoryReferenceCode("AC3", "Appointment Category 3"),
        ),
      )

    val locations = referenceCodeService.getScheduleReasons(ScheduleReasonEventType.APPOINTMENT)

    assertThat(locations).containsExactly(
      appointmentCategoryReferenceCode("AC1", "Appointment Category 1"),
      appointmentCategoryReferenceCode("AC2", "Appointment Category 2"),
      appointmentCategoryReferenceCode("AC3", "Appointment Category 3"),
    )
  }

  @Test
  fun `getScheduleReasonsMap  for an event type returns mapped reference codes`() {
    whenever(prisonApiClient.getScheduleReasons("APP"))
      .thenReturn(
        listOf(
          appointmentCategoryReferenceCode("AC1", "Appointment Category 1"),
          appointmentCategoryReferenceCode("AC2", "Appointment Category 2"),
          appointmentCategoryReferenceCode("AC3", "Appointment Category 3"),
        ),
      )

    val locations = referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT)

    assertThat(locations).isEqualTo(
      mapOf(
        "AC1" to appointmentCategoryReferenceCode("AC1", "Appointment Category 1"),
        "AC2" to appointmentCategoryReferenceCode("AC2", "Appointment Category 2"),
        "AC3" to appointmentCategoryReferenceCode("AC3", "Appointment Category 3"),
      ),
    )
  }
}
