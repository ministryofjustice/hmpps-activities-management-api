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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentOccurrenceSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.UserSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import java.util.Optional

class AppointmentDetailsServiceTest {
  private val appointmentRepository: AppointmentRepository = mock()
  private val referenceCodeService: ReferenceCodeService = mock()
  private val locationService: LocationService = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiClient = mock()
  private val prisonApiClient: PrisonApiClient = mock()

  private val service = AppointmentDetailsService(
    appointmentRepository,
    referenceCodeService,
    locationService,
    prisonerSearchApiClient,
    prisonApiClient,
  )

  @Test
  fun `getAppointmentDetailsById returns mapped appointment details for known appointment id`() {
    val entity = appointmentEntity()
    val occurrenceEntity = entity.occurrences().first()
    whenever(appointmentRepository.findById(entity.appointmentId)).thenReturn(Optional.of(entity))
    whenever(referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY))
      .thenReturn(mapOf(entity.categoryCode to appointmentCategoryReferenceCode(entity.categoryCode)))
    whenever(locationService.getLocationsForAppointmentsMap(entity.prisonCode))
      .thenReturn(mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR")))
    whenever(prisonApiClient.getUserDetailsList(entity.usernames())).thenReturn(
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
    assertThat(service.getAppointmentDetailsById(1)).isEqualTo(
      AppointmentDetails(
        entity.appointmentId,
        AppointmentType.INDIVIDUAL,
        entity.prisonCode,
        prisoners = listOf(
          PrisonerSummary("A1234BC", 456, "TEST", "PRISONER", "TPR", "1-2-3"),
        ),
        AppointmentCategorySummary(entity.categoryCode, "Test Category"),
        "Appointment description",
        AppointmentLocationSummary(entity.internalLocationId!!, "TPR", "Test Appointment Location User Description"),
        entity.inCell,
        entity.startDate,
        entity.startTime,
        entity.endTime,
        null,
        entity.comment,
        entity.created,
        UserSummary(1, "CREATE.USER", "CREATE", "USER"),
        entity.updated,
        UserSummary(2, "UPDATE.USER", "UPDATE", "USER"),
        occurrences = listOf(
          AppointmentOccurrenceSummary(
            occurrenceEntity.appointmentOccurrenceId,
            1,
            1,
            AppointmentLocationSummary(occurrenceEntity.internalLocationId!!, "TPR", "Test Appointment Location User Description"),
            occurrenceEntity.inCell,
            occurrenceEntity.startDate,
            occurrenceEntity.startTime,
            occurrenceEntity.endTime,
            "Appointment occurrence level comment",
            isEdited = false,
            isCancelled = false,
            occurrenceEntity.updated,
            UserSummary(2, "UPDATE.USER", "UPDATE", "USER"),
          ),
        ),
      ),
    )
  }

  @Test
  fun `getAppointmentDetailsById throws entity not found exception for unknown appointment id`() {
    assertThatThrownBy { service.getAppointmentDetailsById(-1) }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Appointment -1 not found")
  }
}
