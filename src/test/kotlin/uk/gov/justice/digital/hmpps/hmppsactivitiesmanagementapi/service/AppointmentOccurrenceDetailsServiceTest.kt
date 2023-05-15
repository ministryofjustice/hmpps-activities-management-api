package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.userDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentOccurrenceDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.UserSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceRepository
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

  @Test
  fun `getAppointmentOccurrenceDetailsById returns mapped appointment details for known appointment id`() {
    val appointment = appointmentEntity()
    val entity = appointment.occurrences().first()
    whenever(appointmentOccurrenceRepository.findById(entity.appointmentOccurrenceId)).thenReturn(Optional.of(entity))
    whenever(referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY))
      .thenReturn(mapOf(appointment.categoryCode to appointmentCategoryReferenceCode(appointment.categoryCode)))
    whenever(locationService.getLocationsForAppointmentsMap(appointment.prisonCode))
      .thenReturn(mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR")))
    whenever(prisonApiClient.getUserDetailsList(appointment.usernames())).thenReturn(
      listOf(
        userDetail(1, "CREATE.USER", "CREATE", "USER"),
        userDetail(2, "UPDATE.USER", "UPDATE", "USER"),
      ),
    )
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(entity.prisonerNumbers())).thenReturn(
      Mono.just(
        listOf(
          PrisonerSearchPrisonerFixture.instance(
            prisonerNumber = "A1234BC",
            bookingId = 456,
            firstName = "TEST",
            lastName = "PRISONER",
            prisonId = "TPR",
            cellLocation = "1-2-3",
          ),
        ),
      ),
    )
    assertThat(service.getAppointmentOccurrenceDetailsById(1)).isEqualTo(
      AppointmentOccurrenceDetails(
        entity.appointmentOccurrenceId,
        appointment.appointmentId,
        AppointmentType.INDIVIDUAL,
        entity.sequenceNumber,
        appointment.prisonCode,
        prisoners = listOf(
          PrisonerSummary("A1234BC", 456, "TEST", "PRISONER", "TPR", "1-2-3"),
        ),
        AppointmentCategorySummary(appointment.categoryCode, "Test Category"),
        "Appointment description",
        AppointmentLocationSummary(entity.internalLocationId!!, appointment.prisonCode, "Test Appointment Location User Description"),
        entity.inCell,
        entity.startDate,
        entity.startTime,
        entity.endTime,
        entity.comment ?: appointment.comment,
        null,
        true,
        false,
        appointment.created,
        UserSummary(1, "CREATE.USER", "CREATE", "USER"),
        entity.updated,
        UserSummary(2, "UPDATE.USER", "UPDATE", "USER"),
      ),
    )
  }

  @Test
  fun `getAppointmentOccurrenceDetailsById throws entity not found exception for unknown appointment id`() {
    assertThatThrownBy { service.getAppointmentOccurrenceDetailsById(-1) }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Appointment Occurrence -1 not found")
  }
}
