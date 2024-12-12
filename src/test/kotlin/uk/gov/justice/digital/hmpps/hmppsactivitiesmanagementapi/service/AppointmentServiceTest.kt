package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentFrequency
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.CancelAppointmentsJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.UncancelAppointmentsJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.UpdateAppointmentsJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment.AppointmentCancelDomainService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment.AppointmentService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment.AppointmentUpdateDomainService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.ReferenceCodeDomain
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.ReferenceCodeService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.CaseloadAccessException
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.FakeSecurityContext
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.addCaseloadIdToRequestHeader
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.clearCaseloadIdFromRequestHeader
import java.security.Principal
import java.util.*

@ExtendWith(FakeSecurityContext::class)
class AppointmentServiceTest {
  private val appointmentRepository: AppointmentRepository = mock()
  private val referenceCodeService: ReferenceCodeService = mock()
  private val locationService: LocationService = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiClient = mock()
  private val appointmentUpdateDomainService: AppointmentUpdateDomainService = mock()
  private val appointmentCancelDomainService: AppointmentCancelDomainService = mock()
  private val updateAppointmentsJob: UpdateAppointmentsJob = mock()
  private val cancelAppointmentsJob: CancelAppointmentsJob = mock()
  private val uncancelAppointmentsJob: UncancelAppointmentsJob = mock()
  private lateinit var principal: Principal

  private val service = AppointmentService(
    appointmentRepository,
    referenceCodeService,
    locationService,
    prisonerSearchApiClient,
    appointmentUpdateDomainService,
    appointmentCancelDomainService,
    updateAppointmentsJob,
    cancelAppointmentsJob,
    uncancelAppointmentsJob,
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
          category = "H",
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
  fun `getAppointmentDetailsByIds throws an exception if appointments are from different prisons`() {
    addCaseloadIdToRequestHeader("TPR")

    val appointmentSeriesRSI = appointmentSeriesEntity(prisonCode = "RSI", frequency = AppointmentFrequency.DAILY, numberOfAppointments = 1)
    val appointmentSeriesLPI = appointmentSeriesEntity(prisonCode = "LPI", frequency = AppointmentFrequency.DAILY, numberOfAppointments = 1)

    val entityRSI = appointmentSeriesRSI.appointments()[0]
    val entityLPI = appointmentSeriesLPI.appointments()[0]

    whenever(appointmentRepository.findByIds(listOf(entityRSI.appointmentId, entityLPI.appointmentId))).thenReturn(listOf(entityLPI, entityRSI))

    assertThatThrownBy {
      service.getAppointmentDetailsByIds(listOf(1, 2, 3))
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Only one prison code is supported")
  }

  @Test
  fun `getAppointmentDetailsByIds returns appointments for selected ids`() {
    addCaseloadIdToRequestHeader("TPR")

    val appointmentSeries = appointmentSeriesEntity(frequency = AppointmentFrequency.DAILY, numberOfAppointments = 3)

    val entities = appointmentSeries.appointments()

    whenever(appointmentRepository.findByIds(listOf(entities[0].appointmentId, entities[1].appointmentId, entities[2].appointmentId))).thenReturn(entities)
    whenever(referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY))
      .thenReturn(mapOf(appointmentSeries.categoryCode to appointmentCategoryReferenceCode(appointmentSeries.categoryCode)))
    whenever(locationService.getLocationsForAppointmentsMap(appointmentSeries.prisonCode))
      .thenReturn(mapOf(entities[0].internalLocationId!! to appointmentLocation(entities[0].internalLocationId!!, "TPR")))
    whenever(prisonerSearchApiClient.findByPrisonerNumbersMap(entities[0].prisonerNumbers())).thenReturn(
      mapOf(
        "A1234BC" to PrisonerSearchPrisonerFixture.instance(
          prisonerNumber = "A1234BC",
          bookingId = 456,
          firstName = "TEST",
          lastName = "PRISONER",
          prisonId = "TPR",
          cellLocation = "1-2-3",
          category = "H",
        ),
      ),
    )

    val appointments = service.getAppointmentDetailsByIds(listOf(1, 2, 3))

    assertThat(appointments).extracting(AppointmentDetails::id, AppointmentDetails::sequenceNumber)
      .containsOnly(
        tuple(1L, 1),
        tuple(2L, 2),
        tuple(3L, 3),
      )
  }
}
