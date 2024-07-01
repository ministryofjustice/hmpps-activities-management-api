package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.ReferenceCodeDomain
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.ReferenceCodeService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.ScheduleReasonEventType
import java.util.Properties

class ReferenceCodeServiceTest {
  private val prisonApiClient: PrisonApiClient = mock()
  private val locationGroupService: LocationGroupService = mock()
  private val groupsProperties: Properties = mock()

  private val locationService = LocationService(prisonApiClient, locationGroupService, groupsProperties)
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

  @Test
  fun `getScheduleLocations for an event type returns locations`() {
    whenever(prisonApiClient.getLocationsForTypeUnrestricted(MOORLAND_PRISON_CODE, "APP"))
      .thenReturn(
        Mono.just(
          listOf(
            appointmentLocation(1, MOORLAND_PRISON_CODE),
            appointmentLocation(2, MOORLAND_PRISON_CODE),
            appointmentLocation(3, MOORLAND_PRISON_CODE),
          ),
        ),
      )

    val locations = locationService.getLocationsForAppointments(MOORLAND_PRISON_CODE)

    assertThat(locations).containsExactly(
      appointmentLocation(1, MOORLAND_PRISON_CODE),
      appointmentLocation(2, MOORLAND_PRISON_CODE),
      appointmentLocation(3, MOORLAND_PRISON_CODE),
    )
  }
}
