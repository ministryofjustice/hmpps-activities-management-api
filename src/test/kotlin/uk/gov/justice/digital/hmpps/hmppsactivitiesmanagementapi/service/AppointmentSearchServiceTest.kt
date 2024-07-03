package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSearchEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentSearchRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentAttendeeSearchRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentSearchRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentSearchSpecification
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment.AppointmentSearchService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.PrisonRegimeService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.ReferenceCodeDomain
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.ReferenceCodeService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.CATEGORY_CODE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.CREATED_BY_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.END_DATE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EVENT_TIME_MS_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.INTERNAL_LOCATION_ID_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISONER_NUMBER_PROPERTY_KEY
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
    val request = AppointmentSearchRequest(startDate = LocalDate.now())
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
      assertThat(value[USER_PROPERTY_KEY]).isEqualTo(principal.name)
      assertThat(value[PRISON_CODE_PROPERTY_KEY]).isEqualTo("TPR")
      assertThat(value[START_DATE_PROPERTY_KEY]).isEqualTo(request.startDate.toString())
      assertThat(value[END_DATE_PROPERTY_KEY]).isEqualTo("")
      assertThat(value[TIME_SLOT_PROPERTY_KEY]).isEqualTo("[]")
      assertThat(value[CATEGORY_CODE_PROPERTY_KEY]).isEqualTo("")
      assertThat(value[INTERNAL_LOCATION_ID_PROPERTY_KEY]).isEqualTo("")
      assertThat(value[PRISONER_NUMBER_PROPERTY_KEY]).isEqualTo("")
      assertThat(value[CREATED_BY_PROPERTY_KEY]).isEqualTo("")
    }

    with(telemetryMetricsMap) {
      assertThat(value[RESULTS_COUNT_METRIC_KEY]).isEqualTo(1.0)
      assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull()
    }
  }

  @Test
  fun `search by start and end date`() {
    val request = AppointmentSearchRequest(startDate = LocalDate.now(), endDate = LocalDate.now().plusWeeks(1))
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
      assertThat(value[USER_PROPERTY_KEY]).isEqualTo(principal.name)
      assertThat(value[PRISON_CODE_PROPERTY_KEY]).isEqualTo("TPR")
      assertThat(value[START_DATE_PROPERTY_KEY]).isEqualTo(request.startDate.toString())
      assertThat(value[END_DATE_PROPERTY_KEY]).isEqualTo(request.endDate.toString())
      assertThat(value[TIME_SLOT_PROPERTY_KEY]).isEqualTo("[]")
      assertThat(value[CATEGORY_CODE_PROPERTY_KEY]).isEqualTo("")
      assertThat(value[INTERNAL_LOCATION_ID_PROPERTY_KEY]).isEqualTo("")
      assertThat(value[PRISONER_NUMBER_PROPERTY_KEY]).isEqualTo("")
      assertThat(value[CREATED_BY_PROPERTY_KEY]).isEqualTo("")
    }

    with(telemetryMetricsMap) {
      assertThat(value[RESULTS_COUNT_METRIC_KEY]).isEqualTo(1.0)
      assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull()
    }
  }

  @Test
  fun `search by time slot`() {
    val request = AppointmentSearchRequest(startDate = LocalDate.now(), timeSlots = listOf(TimeSlot.AM))
    val result = appointmentSearchEntity()

    whenever(prisonRegimeService.getTimeRangeForPrisonAndTimeSlot("TPR", request.timeSlots?.get(0) ?: TimeSlot.PM))
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
      assertThat(value[USER_PROPERTY_KEY]).isEqualTo(principal.name)
      assertThat(value[PRISON_CODE_PROPERTY_KEY]).isEqualTo("TPR")
      assertThat(value[START_DATE_PROPERTY_KEY]).isEqualTo(request.startDate.toString())
      assertThat(value[END_DATE_PROPERTY_KEY]).isEqualTo("")
      assertThat(value[TIME_SLOT_PROPERTY_KEY]).isEqualTo(request.timeSlots.toString())
      assertThat(value[CATEGORY_CODE_PROPERTY_KEY]).isEqualTo("")
      assertThat(value[INTERNAL_LOCATION_ID_PROPERTY_KEY]).isEqualTo("")
      assertThat(value[PRISONER_NUMBER_PROPERTY_KEY]).isEqualTo("")
      assertThat(value[CREATED_BY_PROPERTY_KEY]).isEqualTo("")
    }

    with(telemetryMetricsMap) {
      assertThat(value[RESULTS_COUNT_METRIC_KEY]).isEqualTo(1.0)
      assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull()
    }
  }

  @Test
  fun `search by multiple time slots`() {
    val request = AppointmentSearchRequest(startDate = LocalDate.now(), timeSlots = listOf(TimeSlot.AM, TimeSlot.PM))
    val result = appointmentSearchEntity()

    whenever(prisonRegimeService.getTimeRangeForPrisonAndTimeSlot("TPR", TimeSlot.AM))
      .thenReturn(LocalTimeRange(LocalTime.of(0, 0), LocalTime.of(13, 0)))
    whenever(prisonRegimeService.getTimeRangeForPrisonAndTimeSlot("TPR", TimeSlot.PM))
      .thenReturn(LocalTimeRange(LocalTime.of(13, 0), LocalTime.of(17, 59)))
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
    verify(appointmentSearchSpecification).startTimeBetween(LocalTime.of(13, 0), LocalTime.of(17, 58))
    verifyNoMoreInteractions(appointmentSearchSpecification)

    verify(telemetryClient).trackEvent(
      eq(TelemetryEvent.APPOINTMENT_SEARCH.value),
      telemetryPropertyMap.capture(),
      telemetryMetricsMap.capture(),
    )

    with(telemetryPropertyMap) {
      assertThat(value[USER_PROPERTY_KEY]).isEqualTo(principal.name)
      assertThat(value[PRISON_CODE_PROPERTY_KEY]).isEqualTo("TPR")
      assertThat(value[START_DATE_PROPERTY_KEY]).isEqualTo(request.startDate.toString())
      assertThat(value[END_DATE_PROPERTY_KEY]).isEqualTo("")
      assertThat(value[TIME_SLOT_PROPERTY_KEY]).isEqualTo(request.timeSlots.toString())
      assertThat(value[CATEGORY_CODE_PROPERTY_KEY]).isEqualTo("")
      assertThat(value[INTERNAL_LOCATION_ID_PROPERTY_KEY]).isEqualTo("")
      assertThat(value[PRISONER_NUMBER_PROPERTY_KEY]).isEqualTo("")
      assertThat(value[CREATED_BY_PROPERTY_KEY]).isEqualTo("")
    }

    with(telemetryMetricsMap) {
      assertThat(value[RESULTS_COUNT_METRIC_KEY]).isEqualTo(1.0)
      assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull()
    }
  }

  @Test
  fun `search by category code`() {
    val request = AppointmentSearchRequest(startDate = LocalDate.now(), categoryCode = "TEST")
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
      assertThat(value[USER_PROPERTY_KEY]).isEqualTo(principal.name)
      assertThat(value[PRISON_CODE_PROPERTY_KEY]).isEqualTo("TPR")
      assertThat(value[START_DATE_PROPERTY_KEY]).isEqualTo(request.startDate.toString())
      assertThat(value[END_DATE_PROPERTY_KEY]).isEqualTo("")
      assertThat(value[TIME_SLOT_PROPERTY_KEY]).isEqualTo("[]")
      assertThat(value[CATEGORY_CODE_PROPERTY_KEY]).isEqualTo(request.categoryCode)
      assertThat(value[INTERNAL_LOCATION_ID_PROPERTY_KEY]).isEqualTo("")
      assertThat(value[PRISONER_NUMBER_PROPERTY_KEY]).isEqualTo("")
      assertThat(value[CREATED_BY_PROPERTY_KEY]).isEqualTo("")
    }

    with(telemetryMetricsMap) {
      assertThat(value[RESULTS_COUNT_METRIC_KEY]).isEqualTo(1.0)
      assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull()
    }
  }

  @Test
  fun `search by internal location id`() {
    val request = AppointmentSearchRequest(startDate = LocalDate.now(), internalLocationId = 123)
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
      assertThat(value[USER_PROPERTY_KEY]).isEqualTo(principal.name)
      assertThat(value[PRISON_CODE_PROPERTY_KEY]).isEqualTo("TPR")
      assertThat(value[START_DATE_PROPERTY_KEY]).isEqualTo(request.startDate.toString())
      assertThat(value[END_DATE_PROPERTY_KEY]).isEqualTo("")
      assertThat(value[TIME_SLOT_PROPERTY_KEY]).isEqualTo("[]")
      assertThat(value[CATEGORY_CODE_PROPERTY_KEY]).isEqualTo("")
      assertThat(value[INTERNAL_LOCATION_ID_PROPERTY_KEY]).isEqualTo(request.internalLocationId.toString())
      assertThat(value[PRISONER_NUMBER_PROPERTY_KEY]).isEqualTo("")
      assertThat(value[CREATED_BY_PROPERTY_KEY]).isEqualTo("")
    }

    with(telemetryMetricsMap) {
      assertThat(value[RESULTS_COUNT_METRIC_KEY]).isEqualTo(1.0)
      assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull()
    }
  }

  @Test
  fun `search by in cell`() {
    val request = AppointmentSearchRequest(startDate = LocalDate.now(), inCell = true)
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
    val request = AppointmentSearchRequest(startDate = LocalDate.now(), prisonerNumbers = listOf("A1234BC"))
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

    verify(telemetryClient).trackEvent(
      eq(TelemetryEvent.APPOINTMENT_SEARCH.value),
      telemetryPropertyMap.capture(),
      telemetryMetricsMap.capture(),
    )

    with(telemetryPropertyMap) {
      assertThat(value[USER_PROPERTY_KEY]).isEqualTo(principal.name)
      assertThat(value[PRISON_CODE_PROPERTY_KEY]).isEqualTo("TPR")
      assertThat(value[START_DATE_PROPERTY_KEY]).isEqualTo(request.startDate.toString())
      assertThat(value[END_DATE_PROPERTY_KEY]).isEqualTo("")
      assertThat(value[TIME_SLOT_PROPERTY_KEY]).isEqualTo("[]")
      assertThat(value[CATEGORY_CODE_PROPERTY_KEY]).isEqualTo("")
      assertThat(value[INTERNAL_LOCATION_ID_PROPERTY_KEY]).isEqualTo("")
      assertThat(value[PRISONER_NUMBER_PROPERTY_KEY]).isEqualTo(request.prisonerNumbers!!.first())
      assertThat(value[CREATED_BY_PROPERTY_KEY]).isEqualTo("")
    }

    with(telemetryMetricsMap) {
      assertThat(value[RESULTS_COUNT_METRIC_KEY]).isEqualTo(1.0)
      assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull()
    }
  }

  @Test
  fun `search by created by`() {
    val request = AppointmentSearchRequest(startDate = LocalDate.now(), createdBy = "CREATE.USER")
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

    verify(telemetryClient).trackEvent(
      eq(TelemetryEvent.APPOINTMENT_SEARCH.value),
      telemetryPropertyMap.capture(),
      telemetryMetricsMap.capture(),
    )

    with(telemetryPropertyMap) {
      assertThat(value[USER_PROPERTY_KEY]).isEqualTo(principal.name)
      assertThat(value[PRISON_CODE_PROPERTY_KEY]).isEqualTo("TPR")
      assertThat(value[START_DATE_PROPERTY_KEY]).isEqualTo(request.startDate.toString())
      assertThat(value[END_DATE_PROPERTY_KEY]).isEqualTo("")
      assertThat(value[TIME_SLOT_PROPERTY_KEY]).isEqualTo("[]")
      assertThat(value[CATEGORY_CODE_PROPERTY_KEY]).isEqualTo("")
      assertThat(value[INTERNAL_LOCATION_ID_PROPERTY_KEY]).isEqualTo("")
      assertThat(value[PRISONER_NUMBER_PROPERTY_KEY]).isEqualTo("")
      assertThat(value[CREATED_BY_PROPERTY_KEY]).isEqualTo(request.createdBy)
    }

    with(telemetryMetricsMap) {
      assertThat(value[RESULTS_COUNT_METRIC_KEY]).isEqualTo(1.0)
      assertThat(value[EVENT_TIME_MS_METRIC_KEY]).isNotNull()
    }
  }

  @Test
  fun `search results exclude individual appointments with no attendees`() {
    val prisonCode = "TPR"
    val request = AppointmentSearchRequest(startDate = LocalDate.now())
    val individualAppointmentOneAttendee = appointmentSearchEntity(1, 1, AppointmentType.INDIVIDUAL, prisonCode, 1)
    val individualAppointmentNoAttendee = appointmentSearchEntity(2, 2, AppointmentType.INDIVIDUAL, prisonCode, 2).apply { attendees = emptyList() }
    val groupAppointmentOneAttendee = appointmentSearchEntity(3, 3, AppointmentType.GROUP, prisonCode, 3)
    val groupAppointmentNoAttendee = appointmentSearchEntity(4, 4, AppointmentType.GROUP, prisonCode, 4).apply { attendees = emptyList() }
    val results = listOf(individualAppointmentOneAttendee, individualAppointmentNoAttendee, groupAppointmentOneAttendee, groupAppointmentNoAttendee)

    whenever(appointmentSearchRepository.findAll(any())).thenReturn(results)
    whenever(appointmentAttendeeSearchRepository.findByAppointmentIds(results.map { it.appointmentId })).thenReturn(results.flatMap { it.attendees })
    whenever(referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY))
      .thenReturn(mapOf("TEST" to appointmentCategoryReferenceCode()))
    whenever(locationService.getLocationsForAppointmentsMap(prisonCode))
      .thenReturn(mapOf(123L to appointmentLocation(123L, prisonCode)))

    val searchResults = service.searchAppointments(prisonCode, request, principal)
    searchResults.map { it.appointmentId }.isEqualTo(listOf(individualAppointmentOneAttendee, groupAppointmentOneAttendee, groupAppointmentNoAttendee).map { it.appointmentId })
    searchResults.filterNot { it.attendees.isEmpty() }.map { it.appointmentId }.isEqualTo(listOf(individualAppointmentOneAttendee, groupAppointmentOneAttendee).map { it.appointmentId })
  }

  @Test
  fun `search throws caseload access exception if caseload id header does not match`() {
    addCaseloadIdToRequestHeader("WRONG")
    val request = AppointmentSearchRequest(startDate = LocalDate.now(), createdBy = "CREATE.USER")
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
