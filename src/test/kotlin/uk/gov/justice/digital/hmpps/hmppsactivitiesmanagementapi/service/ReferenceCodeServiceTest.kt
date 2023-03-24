package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions
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

    val locations = referenceCodeService.getReferenceCodes("INT_SCH_RSN")

    Assertions.assertThat(locations).containsExactly(
      appointmentCategoryReferenceCode("AC1", "Appointment Category 1"),
      appointmentCategoryReferenceCode("AC2", "Appointment Category 2"),
      appointmentCategoryReferenceCode("AC3", "Appointment Category 3"),
    )
  }

  @Test
  fun `getAppointmentCategoryReferenceCodes returns INT_SCH_RSN reference codes`() {
    whenever(prisonApiClient.getReferenceCodes("INT_SCH_RSN"))
      .thenReturn(
        listOf(
          appointmentCategoryReferenceCode("AC1", "Appointment Category 1"),
          appointmentCategoryReferenceCode("AC2", "Appointment Category 2"),
          appointmentCategoryReferenceCode("AC3", "Appointment Category 3"),
        ),
      )

    val locations = referenceCodeService.getAppointmentCategoryReferenceCodes()

    Assertions.assertThat(locations).containsExactly(
      appointmentCategoryReferenceCode("AC1", "Appointment Category 1"),
      appointmentCategoryReferenceCode("AC2", "Appointment Category 2"),
      appointmentCategoryReferenceCode("AC3", "Appointment Category 3"),
    )
  }

  @Test
  fun `getAppointmentCategoryReferenceCodes returns mapped INT_SCH_RSN reference codes`() {
    whenever(prisonApiClient.getReferenceCodes("INT_SCH_RSN"))
      .thenReturn(
        listOf(
          appointmentCategoryReferenceCode("AC1", "Appointment Category 1"),
          appointmentCategoryReferenceCode("AC2", "Appointment Category 2"),
          appointmentCategoryReferenceCode("AC3", "Appointment Category 3"),
        ),
      )

    val locations = referenceCodeService.getAppointmentCategoryReferenceCodesMap()

    Assertions.assertThat(locations).isEqualTo(
      mapOf(
        "AC1" to appointmentCategoryReferenceCode("AC1", "Appointment Category 1"),
        "AC2" to appointmentCategoryReferenceCode("AC2", "Appointment Category 2"),
        "AC3" to appointmentCategoryReferenceCode("AC3", "Appointment Category 3"),
      ),
    )
  }

  @Test
  fun `getScheduleReasons for an event type domain returns reference codes`() {
    whenever(prisonApiClient.getScheduleReasons("APP"))
      .thenReturn(
        listOf(
          appointmentCategoryReferenceCode("AC1", "Appointment Category 1"),
          appointmentCategoryReferenceCode("AC2", "Appointment Category 2"),
          appointmentCategoryReferenceCode("AC3", "Appointment Category 3"),
        ),
      )

    val locations = referenceCodeService.getScheduleReasons("APP")

    Assertions.assertThat(locations).containsExactly(
      appointmentCategoryReferenceCode("AC1", "Appointment Category 1"),
      appointmentCategoryReferenceCode("AC2", "Appointment Category 2"),
      appointmentCategoryReferenceCode("AC3", "Appointment Category 3"),
    )
  }

  @Test
  fun `getScheduleReasons returns APP reference codes`() {
    whenever(prisonApiClient.getScheduleReasons("APP"))
      .thenReturn(
        listOf(
          appointmentCategoryReferenceCode("AC1", "Appointment Category 1"),
          appointmentCategoryReferenceCode("AC2", "Appointment Category 2"),
          appointmentCategoryReferenceCode("AC3", "Appointment Category 3"),
        ),
      )

    val locations = referenceCodeService.getAppointmentScheduleReasons()

    Assertions.assertThat(locations).containsExactly(
      appointmentCategoryReferenceCode("AC1", "Appointment Category 1"),
      appointmentCategoryReferenceCode("AC2", "Appointment Category 2"),
      appointmentCategoryReferenceCode("AC3", "Appointment Category 3"),
    )
  }

  @Test
  fun `getScheduleReasons returns mapped APP reference codes`() {
    whenever(prisonApiClient.getScheduleReasons("APP"))
      .thenReturn(
        listOf(
          appointmentCategoryReferenceCode("AC1", "Appointment Category 1"),
          appointmentCategoryReferenceCode("AC2", "Appointment Category 2"),
          appointmentCategoryReferenceCode("AC3", "Appointment Category 3"),
        ),
      )

    val locations = referenceCodeService.getAppointmentScheduleReasonsMap()

    Assertions.assertThat(locations).isEqualTo(
      mapOf(
        "AC1" to appointmentCategoryReferenceCode("AC1", "Appointment Category 1"),
        "AC2" to appointmentCategoryReferenceCode("AC2", "Appointment Category 2"),
        "AC3" to appointmentCategoryReferenceCode("AC3", "Appointment Category 3"),
      ),
    )
  }
}
