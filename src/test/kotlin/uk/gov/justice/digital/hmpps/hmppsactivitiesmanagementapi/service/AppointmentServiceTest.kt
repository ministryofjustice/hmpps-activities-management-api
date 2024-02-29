package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentCancelDomainService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentUpdateDomainService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.CancelAppointmentsJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.UpdateAppointmentsJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.CaseloadAccessException
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.FakeSecurityContext
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.addCaseloadIdToRequestHeader
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.clearCaseloadIdFromRequestHeader
import java.security.Principal
import java.util.Optional

@ExtendWith(FakeSecurityContext::class)
class AppointmentServiceTest {
  private val appointmentRepository: AppointmentRepository = mock()
  private val referenceCodeService: ReferenceCodeService = mock()
  private val locationService: LocationService = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiClient = mock()
  private val prisonApiClient: PrisonApiClient = mock()
  private val appointmentUpdateDomainService: AppointmentUpdateDomainService = mock()
  private val appointmentCancelDomainService: AppointmentCancelDomainService = mock()
  private val updateAppointmentsJob: UpdateAppointmentsJob = mock()
  private val cancelAppointmentsJob: CancelAppointmentsJob = mock()
  private lateinit var principal: Principal

  private val service = AppointmentService(
    appointmentRepository,
    referenceCodeService,
    locationService,
    prisonerSearchApiClient,
    prisonApiClient,
    appointmentUpdateDomainService,
    appointmentCancelDomainService,
    updateAppointmentsJob,
    cancelAppointmentsJob,
  )

  @BeforeEach
  fun setUp() {
    principal = SecurityContextHolder.getContext().authentication
  }

  @AfterEach
  fun tearDown() {
    clearCaseloadIdFromRequestHeader()
  }

  @Test
  fun `getAppointmentDetailsById returns mapped appointment details for known appointment id`() {
    addCaseloadIdToRequestHeader("TPR")
    val appointmentSeries = appointmentSeriesEntity()
    val entity = appointmentSeries.appointments().first()
    whenever(appointmentRepository.findById(entity.appointmentId)).thenReturn(Optional.of(entity))
    whenever(referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY))
      .thenReturn(mapOf(appointmentSeries.categoryCode to appointmentCategoryReferenceCode(appointmentSeries.categoryCode)))
    whenever(locationService.getLocationsForAppointmentsMap(appointmentSeries.prisonCode))
      .thenReturn(mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR")))
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
    assertThat(service.getAppointmentDetailsById(1)).isEqualTo(
      appointmentDetails(
        entity.appointmentId,
        appointmentSeries.appointmentSeriesId,
        sequenceNumber = entity.sequenceNumber,
        customName = "Appointment description",
        createdTime = appointmentSeries.createdTime,
        updatedTime = entity.updatedTime,
      ),
    )
  }

  @Test
  fun `getAppointmentDetailsById throws entity not found exception for unknown appointment id`() {
    assertThatThrownBy { service.getAppointmentDetailsById(-1) }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Appointment -1 not found")
  }

  @Test
  fun `getAppointmentDetailsById throws caseload access exception if caseload id header does not match`() {
    addCaseloadIdToRequestHeader("WRONG")
    val appointmentSeries = appointmentSeriesEntity()
    val entity = appointmentSeries.appointments().first()
    whenever(appointmentRepository.findById(entity.appointmentId)).thenReturn(Optional.of(entity))
    assertThatThrownBy { service.getAppointmentDetailsById(entity.appointmentId) }.isInstanceOf(CaseloadAccessException::class.java)
  }

  @Test
  fun `failIfCategoryIsVideoLinkAndMissingExtraInfo should throw exception when category is VLB and extraInformation is empty`() {
    val request = AppointmentUpdateRequest(categoryCode = "VLB", extraInformation = "")

    assertThrows<IllegalArgumentException> {
      service.updateAppointment(1L, request, principal)
    }
  }

  @Test
  fun `failIfCategoryIsVideoLinkAndMissingExtraInfo should not throw exception when category is VLB and extraInformation is not empty`() {
    addCaseloadIdToRequestHeader("TPR")
    val request = AppointmentUpdateRequest(categoryCode = "VLB", extraInformation = "Video Link Session Court")
    val appointmentSeries = appointmentSeriesEntity()
    val entity = appointmentSeries.appointments().first()
    whenever(appointmentRepository.findById(entity.appointmentId)).thenReturn(Optional.of(entity))
    whenever(referenceCodeService.getScheduleReasonsMap(ScheduleReasonEventType.APPOINTMENT)).thenReturn(mapOf("VLB" to appointmentCategoryReferenceCode("")))

    assertDoesNotThrow {
      service.updateAppointment(1L, request, principal)
    }
  }
}
