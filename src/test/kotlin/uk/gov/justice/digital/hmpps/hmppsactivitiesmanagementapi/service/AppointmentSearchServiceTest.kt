package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalTimeRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSearchEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceSearchRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentAttendeeSearchRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentSearchRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentSearchSpecification
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.CATEGORY_CODE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.END_DATE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EVENT_TIME_MS_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.INTERNAL_LOCATION_ID_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISON_CODE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.RESULTS_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.START_DATE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TIME_SLOT_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.USER_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.CaseloadAccessException
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.FakeSecurityContext
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.addCaseloadIdToRequestHeader
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.clearCaseloadIdFromRequestHeader
import java.security.Principal
import java.time.LocalDate
import java.time.LocalTime

@ExtendWith(FakeSecurityContext::class)
class AppointmentSearchServiceTest {
  private val appointmentSearchRepository: AppointmentSearchRepository = mock()
  private val appointmentAttendeeSearchRepository: AppointmentAttendeeSearchRepository = mock()
  private val appointmentSearchSpecification: AppointmentSearchSpecification = spy()
  private val prisonRegimeService: PrisonRegimeService = mock()
  private val referenceCodeService: ReferenceCodeService = mock()
  private val locationService: LocationService = mock()
  private val telemetryClient: TelemetryClient = mock()

  private val principal: Principal = mock()

  @Captor
  private lateinit var telemetryPropertyMap: ArgumentCaptor<Map<String, String>>

  @Captor
  private lateinit var telemetryMetricsMap: ArgumentCaptor<Map<String, Double>>

  private val service = AppointmentSearchService(
    appointmentSearchRepository,
    appointmentAttendeeSearchRepository,
    appointmentSearchSpecification,
    prisonRegimeService,
    referenceCodeService,
    locationService,
    telemetryClient,
  )

  @BeforeEach
  fun setup() {
    MockitoAnnotations.openMocks(this)
    addCaseloadIdToRequestHeader("TPR")
  }

  @AfterEach
  fun tearDown() {
    clearCaseloadIdFromRequestHeader()
  }

  @Test
  fun `search by start date`() {
    val request = AppointmentOccurrenceSearchRequest(startDate = LocalDate.now())
    val result = appointmentSearchEntity()

    whenever(appointmentSearchRepository.findAll(any())).thenReturn(listOf(result))
    whenever(appointmentAttendeeSearchRepository.findByAppointmentIds(listOf(result.appointmentId))).thenReturn(result.attendees)
    whenever(referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY))
      .thenReturn(mapOf(result.categoryCode to appointmentCategoryReferenceCode(result.categoryCode)))
    whenever(locationService.getLocationsForAppointmentsMap(result.prisonCode))
      .thenReturn(mapOf(result.internalLocationId!! to appointmentLocation(result.internalLocationId!!, "TPR")))

    service.searchAppointments("TPR", request, principal)

    verify(appointmentSearchSpecification).prisonCodeEquals("TPR")
    verify(appointmentSearchSpecification).startDateEquals(request.startDate!!)
    verifyNoMoreInteractions(appointmentSearchSpecification)

    verify(telemetryClient).trackEvent(
      eq(TelemetryEvent.APPOINTMENT_SEARCH.value),
      telemetryPropertyMap.capture(),
      telemetryMetricsMap.capture(),
    )

    with(telemetryPropertyMap) {
      Assertions.assertThat(value[USER_PROPERTY_KEY]).isEqualTo(principal.name)
      Assertions.assertThat(value[PRISON_CODE_PROPERTY_KEY]).isEqualTo("TPR")
      Assertions.assertThat(value[START_DATE_PROPERTY_KEY]).isEqualTo(request.startDate.toString())
      Assertions.assertThat(value[END_DATE_PROPERTY_KEY]).isEqualTo("")
      Assertions.assertThat(value[TIME_SLOT_PROPERTY_KEY]).isEqualTo("")
      Assertions.assertThat(value[CATEGORY_CODE_PROPERTY_KEY]).isEqualTo("")
      Assertions.assertThat(value[INTERNAL_LOCATION_ID_PROPERTY_KEY]).isEqualTo("")
    }

    with(telemetryMetricsMap) {
      Assertions.assertThat(value[RESULTS_COUNT_METRIC_KEY]).isEqualTo(1.0)
      Assertions.assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull()
    }
  }

  @Test
  fun `search by start and end date`() {
    val request = AppointmentOccurrenceSearchRequest(startDate = LocalDate.now(), endDate = LocalDate.now().plusWeeks(1))
    val result = appointmentSearchEntity()

    whenever(appointmentSearchRepository.findAll(any())).thenReturn(listOf(result))
    whenever(appointmentAttendeeSearchRepository.findByAppointmentIds(listOf(result.appointmentId))).thenReturn(result.attendees)
    whenever(referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY))
      .thenReturn(mapOf(result.categoryCode to appointmentCategoryReferenceCode(result.categoryCode)))
    whenever(locationService.getLocationsForAppointmentsMap(result.prisonCode))
      .thenReturn(mapOf(result.internalLocationId!! to appointmentLocation(result.internalLocationId!!, "TPR")))

    service.searchAppointments("TPR", request, principal)

    verify(appointmentSearchSpecification).prisonCodeEquals("TPR")
    verify(appointmentSearchSpecification).startDateBetween(request.startDate!!, request.endDate!!)
    verifyNoMoreInteractions(appointmentSearchSpecification)

    verify(telemetryClient).trackEvent(
      eq(TelemetryEvent.APPOINTMENT_SEARCH.value),
      telemetryPropertyMap.capture(),
      telemetryMetricsMap.capture(),
    )

    with(telemetryPropertyMap) {
      Assertions.assertThat(value[USER_PROPERTY_KEY]).isEqualTo(principal.name)
      Assertions.assertThat(value[PRISON_CODE_PROPERTY_KEY]).isEqualTo("TPR")
      Assertions.assertThat(value[START_DATE_PROPERTY_KEY]).isEqualTo(request.startDate.toString())
      Assertions.assertThat(value[END_DATE_PROPERTY_KEY]).isEqualTo(request.endDate.toString())
      Assertions.assertThat(value[TIME_SLOT_PROPERTY_KEY]).isEqualTo("")
      Assertions.assertThat(value[CATEGORY_CODE_PROPERTY_KEY]).isEqualTo("")
      Assertions.assertThat(value[INTERNAL_LOCATION_ID_PROPERTY_KEY]).isEqualTo("")
    }

    with(telemetryMetricsMap) {
      Assertions.assertThat(value[RESULTS_COUNT_METRIC_KEY]).isEqualTo(1.0)
      Assertions.assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull()
    }
  }

  @Test
  fun `search by time slot`() {
    val request = AppointmentOccurrenceSearchRequest(startDate = LocalDate.now(), timeSlot = TimeSlot.AM)
    val result = appointmentSearchEntity()

    whenever(prisonRegimeService.getTimeRangeForPrisonAndTimeSlot("TPR", request.timeSlot!!))
      .thenReturn(LocalTimeRange(LocalTime.of(0, 0), LocalTime.of(13, 0)))
    whenever(appointmentSearchRepository.findAll(any())).thenReturn(listOf(result))
    whenever(appointmentAttendeeSearchRepository.findByAppointmentIds(listOf(result.appointmentId))).thenReturn(result.attendees)
    whenever(referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY))
      .thenReturn(mapOf(result.categoryCode to appointmentCategoryReferenceCode(result.categoryCode)))
    whenever(locationService.getLocationsForAppointmentsMap(result.prisonCode))
      .thenReturn(mapOf(result.internalLocationId!! to appointmentLocation(result.internalLocationId!!, "TPR")))

    service.searchAppointments("TPR", request, principal)

    verify(appointmentSearchSpecification).prisonCodeEquals("TPR")
    verify(appointmentSearchSpecification).startDateEquals(request.startDate!!)
    verify(appointmentSearchSpecification).startTimeBetween(LocalTime.of(0, 0), LocalTime.of(12, 59))
    verifyNoMoreInteractions(appointmentSearchSpecification)

    verify(telemetryClient).trackEvent(
      eq(TelemetryEvent.APPOINTMENT_SEARCH.value),
      telemetryPropertyMap.capture(),
      telemetryMetricsMap.capture(),
    )

    with(telemetryPropertyMap) {
      Assertions.assertThat(value[USER_PROPERTY_KEY]).isEqualTo(principal.name)
      Assertions.assertThat(value[PRISON_CODE_PROPERTY_KEY]).isEqualTo("TPR")
      Assertions.assertThat(value[START_DATE_PROPERTY_KEY]).isEqualTo(request.startDate.toString())
      Assertions.assertThat(value[END_DATE_PROPERTY_KEY]).isEqualTo("")
      Assertions.assertThat(value[TIME_SLOT_PROPERTY_KEY]).isEqualTo(request.timeSlot.toString())
      Assertions.assertThat(value[CATEGORY_CODE_PROPERTY_KEY]).isEqualTo("")
      Assertions.assertThat(value[INTERNAL_LOCATION_ID_PROPERTY_KEY]).isEqualTo("")
    }

    with(telemetryMetricsMap) {
      Assertions.assertThat(value[RESULTS_COUNT_METRIC_KEY]).isEqualTo(1.0)
      Assertions.assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull()
    }
  }

  @Test
  fun `search by category code`() {
    val request = AppointmentOccurrenceSearchRequest(startDate = LocalDate.now(), categoryCode = "TEST")
    val result = appointmentSearchEntity()

    whenever(appointmentSearchRepository.findAll(any())).thenReturn(listOf(result))
    whenever(appointmentAttendeeSearchRepository.findByAppointmentIds(listOf(result.appointmentId))).thenReturn(result.attendees)
    whenever(referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY))
      .thenReturn(mapOf(result.categoryCode to appointmentCategoryReferenceCode(result.categoryCode)))
    whenever(locationService.getLocationsForAppointmentsMap(result.prisonCode))
      .thenReturn(mapOf(result.internalLocationId!! to appointmentLocation(result.internalLocationId!!, "TPR")))

    service.searchAppointments("TPR", request, principal)

    verify(appointmentSearchSpecification).prisonCodeEquals("TPR")
    verify(appointmentSearchSpecification).startDateEquals(request.startDate!!)
    verify(appointmentSearchSpecification).categoryCodeEquals(request.categoryCode!!)
    verifyNoMoreInteractions(appointmentSearchSpecification)

    verify(telemetryClient).trackEvent(
      eq(TelemetryEvent.APPOINTMENT_SEARCH.value),
      telemetryPropertyMap.capture(),
      telemetryMetricsMap.capture(),
    )

    with(telemetryPropertyMap) {
      Assertions.assertThat(value[USER_PROPERTY_KEY]).isEqualTo(principal.name)
      Assertions.assertThat(value[PRISON_CODE_PROPERTY_KEY]).isEqualTo("TPR")
      Assertions.assertThat(value[START_DATE_PROPERTY_KEY]).isEqualTo(request.startDate.toString())
      Assertions.assertThat(value[END_DATE_PROPERTY_KEY]).isEqualTo("")
      Assertions.assertThat(value[TIME_SLOT_PROPERTY_KEY]).isEqualTo("")
      Assertions.assertThat(value[CATEGORY_CODE_PROPERTY_KEY]).isEqualTo(request.categoryCode)
      Assertions.assertThat(value[INTERNAL_LOCATION_ID_PROPERTY_KEY]).isEqualTo("")
    }

    with(telemetryMetricsMap) {
      Assertions.assertThat(value[RESULTS_COUNT_METRIC_KEY]).isEqualTo(1.0)
      Assertions.assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull()
    }
  }

  @Test
  fun `search by internal location id`() {
    val request = AppointmentOccurrenceSearchRequest(startDate = LocalDate.now(), internalLocationId = 123)
    val result = appointmentSearchEntity()

    whenever(appointmentSearchRepository.findAll(any())).thenReturn(listOf(result))
    whenever(appointmentAttendeeSearchRepository.findByAppointmentIds(listOf(result.appointmentId))).thenReturn(result.attendees)
    whenever(referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY))
      .thenReturn(mapOf(result.categoryCode to appointmentCategoryReferenceCode(result.categoryCode)))
    whenever(locationService.getLocationsForAppointmentsMap(result.prisonCode))
      .thenReturn(mapOf(result.internalLocationId!! to appointmentLocation(result.internalLocationId!!, "TPR")))

    service.searchAppointments("TPR", request, principal)

    verify(appointmentSearchSpecification).prisonCodeEquals("TPR")
    verify(appointmentSearchSpecification).startDateEquals(request.startDate!!)
    verify(appointmentSearchSpecification).internalLocationIdEquals(request.internalLocationId!!)
    verifyNoMoreInteractions(appointmentSearchSpecification)

    verify(telemetryClient).trackEvent(
      eq(TelemetryEvent.APPOINTMENT_SEARCH.value),
      telemetryPropertyMap.capture(),
      telemetryMetricsMap.capture(),
    )

    with(telemetryPropertyMap) {
      Assertions.assertThat(value[USER_PROPERTY_KEY]).isEqualTo(principal.name)
      Assertions.assertThat(value[PRISON_CODE_PROPERTY_KEY]).isEqualTo("TPR")
      Assertions.assertThat(value[START_DATE_PROPERTY_KEY]).isEqualTo(request.startDate.toString())
      Assertions.assertThat(value[END_DATE_PROPERTY_KEY]).isEqualTo("")
      Assertions.assertThat(value[TIME_SLOT_PROPERTY_KEY]).isEqualTo("")
      Assertions.assertThat(value[CATEGORY_CODE_PROPERTY_KEY]).isEqualTo("")
      Assertions.assertThat(value[INTERNAL_LOCATION_ID_PROPERTY_KEY]).isEqualTo(request.internalLocationId.toString())
    }

    with(telemetryMetricsMap) {
      Assertions.assertThat(value[RESULTS_COUNT_METRIC_KEY]).isEqualTo(1.0)
      Assertions.assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull()
    }
  }

  @Test
  fun `search by in cell`() {
    val request = AppointmentOccurrenceSearchRequest(startDate = LocalDate.now(), inCell = true)
    val result = appointmentSearchEntity(inCell = true)

    whenever(appointmentSearchRepository.findAll(any())).thenReturn(listOf(result))
    whenever(appointmentAttendeeSearchRepository.findByAppointmentIds(listOf(result.appointmentId))).thenReturn(result.attendees)
    whenever(referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY))
      .thenReturn(mapOf(result.categoryCode to appointmentCategoryReferenceCode(result.categoryCode)))
    whenever(locationService.getLocationsForAppointmentsMap(result.prisonCode))
      .thenReturn(emptyMap())

    service.searchAppointments("TPR", request, principal)

    verify(appointmentSearchSpecification).prisonCodeEquals("TPR")
    verify(appointmentSearchSpecification).startDateEquals(request.startDate!!)
    verify(appointmentSearchSpecification).inCellEquals(request.inCell!!)
    verifyNoMoreInteractions(appointmentSearchSpecification)
  }

  @Test
  fun `search by prisoner numbers`() {
    val request = AppointmentOccurrenceSearchRequest(startDate = LocalDate.now(), prisonerNumbers = listOf("A1234BC"))
    val result = appointmentSearchEntity()

    whenever(appointmentSearchRepository.findAll(any())).thenReturn(listOf(result))
    whenever(appointmentAttendeeSearchRepository.findByAppointmentIds(listOf(result.appointmentId))).thenReturn(result.attendees)
    whenever(referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY))
      .thenReturn(mapOf(result.categoryCode to appointmentCategoryReferenceCode(result.categoryCode)))
    whenever(locationService.getLocationsForAppointmentsMap(result.prisonCode))
      .thenReturn(mapOf(result.internalLocationId!! to appointmentLocation(result.internalLocationId!!, "TPR")))

    service.searchAppointments("TPR", request, principal)

    verify(appointmentSearchSpecification).prisonCodeEquals("TPR")
    verify(appointmentSearchSpecification).startDateEquals(request.startDate!!)
    verify(appointmentSearchSpecification).prisonerNumbersIn(request.prisonerNumbers!!)
    verifyNoMoreInteractions(appointmentSearchSpecification)
  }

  @Test
  fun `search by created by`() {
    val request = AppointmentOccurrenceSearchRequest(startDate = LocalDate.now(), createdBy = "CREATE.USER")
    val result = appointmentSearchEntity()

    whenever(appointmentSearchRepository.findAll(any())).thenReturn(listOf(result))
    whenever(appointmentAttendeeSearchRepository.findByAppointmentIds(listOf(result.appointmentId))).thenReturn(result.attendees)
    whenever(referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY))
      .thenReturn(mapOf(result.categoryCode to appointmentCategoryReferenceCode(result.categoryCode)))
    whenever(locationService.getLocationsForAppointmentsMap(result.prisonCode))
      .thenReturn(mapOf(result.internalLocationId!! to appointmentLocation(result.internalLocationId!!, "TPR")))

    service.searchAppointments("TPR", request, principal)

    verify(appointmentSearchSpecification).prisonCodeEquals("TPR")
    verify(appointmentSearchSpecification).startDateEquals(request.startDate!!)
    verify(appointmentSearchSpecification).createdByEquals(request.createdBy!!)
    verifyNoMoreInteractions(appointmentSearchSpecification)
  }

  @Test
  fun `search throws caseload access exception if caseload id header does not match`() {
    addCaseloadIdToRequestHeader("WRONG")
    val request = AppointmentOccurrenceSearchRequest(startDate = LocalDate.now(), createdBy = "CREATE.USER")
    val result = appointmentSearchEntity()

    whenever(appointmentSearchRepository.findAll(any())).thenReturn(listOf(result))
    whenever(appointmentAttendeeSearchRepository.findByAppointmentIds(listOf(result.appointmentId))).thenReturn(result.attendees)
    whenever(referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY))
      .thenReturn(mapOf(result.categoryCode to appointmentCategoryReferenceCode(result.categoryCode)))
    whenever(locationService.getLocationsForAppointmentsMap(result.prisonCode))
      .thenReturn(mapOf(result.internalLocationId!! to appointmentLocation(result.internalLocationId!!, "TPR")))

    assertThatThrownBy { service.searchAppointments("TPR", request, principal) }
      .isInstanceOf(CaseloadAccessException::class.java)
  }
}
