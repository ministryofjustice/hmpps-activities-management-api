package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentOccurrenceDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.userDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.CaseloadAccessException
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.addCaseloadIdToRequestHeader
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.clearCaseloadIdFromRequestHeader
import java.util.Optional

class AppointmentOccurrenceDetailsServiceTest {
  private val appointmentOccurrenceRepository: AppointmentOccurrenceRepository = mock()
  private val referenceCodeService: ReferenceCodeService = mock()
  private val locationService: LocationService = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiClient = mock()
  private val prisonApiClient: PrisonApiClient = mock()

  private val service = AppointmentOccurrenceDetailsService(
    appointmentOccurrenceRepository,
    referenceCodeService,
    locationService,
    prisonerSearchApiClient,
    prisonApiClient,
  )

  @AfterEach
  fun tearDown() {
    clearCaseloadIdFromRequestHeader()
  }

  @Test
  fun `getAppointmentOccurrenceDetailsById returns mapped appointment details for known appointment id`() {
    addCaseloadIdToRequestHeader("TPR")
    val appointmentSeries = appointmentSeriesEntity()
    val entity = appointmentSeries.occurrences().first()
    whenever(appointmentOccurrenceRepository.findById(entity.appointmentOccurrenceId)).thenReturn(Optional.of(entity))
    whenever(referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY))
      .thenReturn(mapOf(appointmentSeries.categoryCode to appointmentCategoryReferenceCode(appointmentSeries.categoryCode)))
    whenever(locationService.getLocationsForAppointmentsMap(appointmentSeries.prisonCode))
      .thenReturn(mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR")))
    whenever(prisonApiClient.getUserDetailsList(appointmentSeries.usernames())).thenReturn(
      listOf(
        userDetail(1, "CREATE.USER", "CREATE", "USER"),
        userDetail(2, "UPDATE.USER", "UPDATE", "USER"),
      ),
    )
    whenever(prisonerSearchApiClient.findByPrisonerNumbersMap(entity.prisonerNumbers())).thenReturn(
      mapOf(
        "A1234BC" to PrisonerSearchPrisonerFixture.instance(
          prisonerNumber = "A1234BC",
          bookingId = 456,
          firstName = "TEST",
          lastName = "PRISONER",
          prisonId = "TPR",
          cellLocation = "1-2-3",
        ),
      ),
    )
    assertThat(service.getAppointmentOccurrenceDetailsById(1)).isEqualTo(
      appointmentOccurrenceDetails(
        entity.appointmentOccurrenceId,
        appointmentSeries.appointmentSeriesId,
        sequenceNumber = entity.sequenceNumber,
        appointmentDescription = "Appointment description",
        created = appointmentSeries.created,
        updated = entity.updated,
      ),
    )
  }

  @Test
  fun `getAppointmentOccurrenceDetailsById throws entity not found exception for unknown appointment id`() {
    assertThatThrownBy { service.getAppointmentOccurrenceDetailsById(-1) }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Appointment Occurrence -1 not found")
  }

  @Test
  fun `getAppointmentOccurrenceDetailsById throws caseload access exception if caseload id header does not match`() {
    addCaseloadIdToRequestHeader("WRONG")
    val appointmentSeries = appointmentSeriesEntity()
    val entity = appointmentSeries.occurrences().first()
    whenever(appointmentOccurrenceRepository.findById(entity.appointmentOccurrenceId)).thenReturn(Optional.of(entity))
    assertThatThrownBy { service.getAppointmentOccurrenceDetailsById(entity.appointmentOccurrenceId) }.isInstanceOf(CaseloadAccessException::class.java)
  }
}
