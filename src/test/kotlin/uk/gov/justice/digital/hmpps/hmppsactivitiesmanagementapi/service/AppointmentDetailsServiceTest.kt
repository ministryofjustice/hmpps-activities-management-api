package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
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
import java.util.*

class AppointmentDetailsServiceTest {
  private val appointmentRepository: AppointmentRepository = mock()
  private val locationService: LocationService = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiClient = mock()
  private val prisonApiClient: PrisonApiClient = mock()

  private val service = AppointmentDetailsService(
    appointmentRepository,
    locationService,
    prisonerSearchApiClient,
    prisonApiClient,
  )

  @BeforeEach
  fun setUp() {
    MockitoAnnotations.openMocks(this)
  }

  @Test
  fun `getAppointmentDetailsById returns mapped appointment details for known appointment id`() {
    val entity = appointmentEntity()
    val occurrenceEntity = entity.occurrences().first()
    whenever(appointmentRepository.findById(entity.appointmentId)).thenReturn(Optional.of(entity))
    whenever(locationService.getLocationsForAppointmentsMap(entity.prisonCode, entity.internalLocationIds())!!)
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
    Assertions.assertThat(service.getAppointmentDetailsById(1)).isEqualTo(
      AppointmentDetails(
        entity.appointmentId,
        AppointmentCategorySummary(entity.category.appointmentCategoryId, entity.category.code, entity.category.description),
        entity.prisonCode,
        AppointmentLocationSummary(entity.internalLocationId!!, "TPR", "Test Appointment Location"),
        entity.inCell,
        entity.startDate,
        entity.startTime,
        entity.endTime,
        entity.comment,
        entity.created,
        UserSummary(1, "CREATE.USER", "CREATE", "USER"),
        entity.updated,
        UserSummary(2, "UPDATE.USER", "UPDATE", "USER"),
        occurrences = listOf(
          AppointmentOccurrenceSummary(
            occurrenceEntity.appointmentOccurrenceId,
            AppointmentLocationSummary(occurrenceEntity.internalLocationId!!, "TPR", "Test Appointment Location"),
            occurrenceEntity.inCell,
            occurrenceEntity.startDate,
            occurrenceEntity.startTime,
            occurrenceEntity.endTime,
            "Appointment occurrence level comment",
            isEdited = false,
            isCancelled = false,
            occurrenceEntity.updated,
            UserSummary(2, "UPDATE.USER", "UPDATE", "USER"),
            1,
          ),
        ),
        prisoners = listOf(
          PrisonerSummary("A1234BC", 456, "TEST", "PRISONER", "TPR", "1-2-3"),
        ),
      ),
    )
  }

  @Test
  fun `getAppointmentDetailsById throws entity not found exception for unknown appointment id`() {
    Assertions.assertThatThrownBy { service.getAppointmentDetailsById(-1) }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Appointment -1 not found")
  }
}
