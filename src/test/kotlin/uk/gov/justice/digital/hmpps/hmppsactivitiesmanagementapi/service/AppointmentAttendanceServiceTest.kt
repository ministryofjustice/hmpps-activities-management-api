package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.firstValue
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.secondValue
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentSeries
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventTierType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentAttendanceSummaryEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentAttendanceSummaryModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSearchEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentAttendanceRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.MultipleAppointmentAttendanceRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentAndAttendee
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentAttendanceSummaryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentAttendeeSearchRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment.AppointmentAttendanceService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment.AttendanceAction
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.ReferenceCodeDomain
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.ReferenceCodeService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_ID_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EVENT_TIME_MS_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISONERS_ATTENDANCE_CHANGED_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISONERS_ATTENDED_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISONERS_NON_ATTENDED_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISON_CODE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.USER_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.CaseloadAccessException
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.addCaseloadIdToRequestHeader
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.clearCaseloadIdFromRequestHeader
import java.security.Principal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment as AppointmentModel

@ExtendWith(MockitoExtension::class)
class AppointmentAttendanceServiceTest {
  private val appointmentAttendanceSummaryRepository: AppointmentAttendanceSummaryRepository = mock()
  private val appointmentRepository: AppointmentRepository = mock()
  private val referenceCodeService: ReferenceCodeService = mock()
  private val locationService: LocationService = mock()
  private val appointmentAttendeeSearchRepository: AppointmentAttendeeSearchRepository = mock()
  private val telemetryClient: TelemetryClient = mock()

  private val service = AppointmentAttendanceService(appointmentAttendanceSummaryRepository, appointmentRepository, referenceCodeService, locationService, appointmentAttendeeSearchRepository, telemetryClient, TransactionHandler())

  private val principal: Principal = mock()
  private val username = "ATTENDANCE.RECORDED.BY"
  private val entity = appointmentAttendanceSummaryEntity()

  @Captor
  private lateinit var appointmentCaptor: ArgumentCaptor<Appointment>

  @Captor
  private lateinit var telemetryPropertyMapCaptor: ArgumentCaptor<Map<String, String>>

  @Captor
  private lateinit var telemetryMetricsMapCaptor: ArgumentCaptor<Map<String, Double>>

  @BeforeEach
  fun setUp() {
    addCaseloadIdToRequestHeader(MOORLAND_PRISON_CODE)
    whenever(principal.name).thenReturn(username)
  }

  @AfterEach
  fun tearDown() {
    clearCaseloadIdFromRequestHeader()
  }

  @Nested
  @DisplayName("get appointment attendance summaries")
  inner class GetAppointmentAttendanceSummaries {

    private val date = LocalDate.now()

    @BeforeEach
    fun `init`() {
      val appointmentSearch = appointmentSearchEntity(appointmentId = 1)
      val referenceCodeMap = mapOf(
        entity.categoryCode to appointmentCategoryReferenceCode(entity.categoryCode, "Chaplaincy"),
        "TEST_CAT" to appointmentCategoryReferenceCode("TEST_CAT", "appointment"),
      )
      val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, entity.prisonCode, userDescription = "Chapel"))

      whenever(appointmentAttendanceSummaryRepository.findByPrisonCodeAndStartDate(MOORLAND_PRISON_CODE, date)).thenReturn(listOf(entity))
      whenever(referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY)).thenReturn(referenceCodeMap)
      whenever(locationService.getLocationsForAppointmentsMap(any())).thenReturn(locationMap)
      whenever(appointmentAttendeeSearchRepository.findByAppointmentIds(listOf(1))).thenReturn(appointmentSearch.attendees)
    }

    @Test
    fun `throws caseload access exception if caseload id header does not match`() {
      addCaseloadIdToRequestHeader("WRONG")
      assertThatThrownBy { service.getAppointmentAttendanceSummaries(prisonCode = MOORLAND_PRISON_CODE, date = LocalDate.now()) }
        .isInstanceOf(CaseloadAccessException::class.java)
      verify(appointmentRepository, never()).saveAndFlush(any())
    }

    @Test
    fun `returns appointment attendance summaries`() {
      val summaries = service.getAppointmentAttendanceSummaries(MOORLAND_PRISON_CODE, date)

      assertThat(summaries).isEqualTo(listOf(appointmentAttendanceSummaryModel()))
    }

    @Test
    fun `filters values by custom name`() {
      addCaseloadIdToRequestHeader("RAN")
      whenever(appointmentAttendanceSummaryRepository.findByPrisonCodeAndStartDateAndCustomNameIgnoreCase("RAN", date, "custom")).thenReturn(
        listOf(appointmentAttendanceSummaryEntity(customName = "custom")),
      )

      val summaries = service.getAppointmentAttendanceSummaries(prisonCode = "RAN", date = LocalDate.now(), customName = "custom")
      assertThat(summaries.all { it.appointmentName.contains("custom") }).isTrue()
    }

    @Test
    fun `filters values by category code`() {
      addCaseloadIdToRequestHeader("RAN")
      whenever(appointmentAttendanceSummaryRepository.findByPrisonCodeAndStartDateAndCategoryCode("RAN", date, "appointment")).thenReturn(
        listOf(appointmentAttendanceSummaryEntity(categoryCode = "TEST_CAT")),
      )

      val summaries = service.getAppointmentAttendanceSummaries(prisonCode = "RAN", date = LocalDate.now(), categoryCode = "TEST_CAT")
      assertThat(summaries.all { it.appointmentName.contains("appointment") }).isTrue()
    }

    @Test
    fun `filters by category and custom name`() {
      addCaseloadIdToRequestHeader("RAN")
      whenever(appointmentAttendanceSummaryRepository.findByPrisonCodeAndStartDateAndCategoryCodeAndCustomNameIgnoreCase("RAN", date, "appointment", "custom")).thenReturn(
        listOf(appointmentAttendanceSummaryEntity(categoryCode = "TEST_CAT")),
      )

      val summaries = service.getAppointmentAttendanceSummaries(prisonCode = "RAN", date = LocalDate.now(), categoryCode = "TEST_CAT", customName = "custom")
      assertThat(summaries.all { it.appointmentName.contains("appointment") && it.appointmentName.contains("custom") }).isTrue()
    }
  }

  @Nested
  @DisplayName("mark attendance")
  inner class GetAppointmentInstanceById {
    @Test
    fun `throws entity not found exception for unknown appointment id`() {
      assertThatThrownBy { service.markAttendance(-1, AppointmentAttendanceRequest(), principal) }
        .isInstanceOf(EntityNotFoundException::class.java)
        .hasMessage("Appointment -1 not found")

      verify(appointmentRepository, never()).saveAndFlush(any())
    }

    @Test
    fun `throws caseload access exception if caseload id header does not match`() {
      addCaseloadIdToRequestHeader("WRONG")
      val entity = mock<Appointment>()
      whenever(entity.prisonCode).thenReturn(MOORLAND_PRISON_CODE)
      whenever(appointmentRepository.findById(1)).thenReturn(Optional.of(entity))

      assertThatThrownBy { service.markAttendance(1, AppointmentAttendanceRequest(), principal) }
        .isInstanceOf(CaseloadAccessException::class.java)

      verify(appointmentRepository, never()).saveAndFlush(any())
    }

    @Test
    fun `calls appointment mark prisoner attendance function`() {
      val entity = mock<Appointment>()
      whenever(entity.prisonCode).thenReturn(MOORLAND_PRISON_CODE)
      whenever(entity.toModel()).thenReturn(mock<AppointmentModel>())
      whenever(appointmentRepository.findById(1)).thenReturn(Optional.of(entity))
      whenever(appointmentRepository.saveAndFlush(entity)).thenReturn(entity)
      val request = AppointmentAttendanceRequest(
        attendedPrisonNumbers = listOf("A1234BC", "C3456DE"),
        nonAttendedPrisonNumbers = listOf("B2345CD"),
      )

      val appointment = service.markAttendance(1, request, principal)

      verify(entity).markPrisonerAttendance(eq(request.attendedPrisonNumbers), eq(request.nonAttendedPrisonNumbers), any<LocalDateTime>(), eq(username))
      verify(appointmentRepository).saveAndFlush(entity)
      assertThat(appointment).isInstanceOf(AppointmentModel::class.java)
    }
  }

  @Nested
  @DisplayName("Mark Multiple Attendances")
  inner class MarkMultipleAttendances {
    @ParameterizedTest
    @EnumSource(AttendanceAction::class)
    fun `throws caseload access exception if caseload id header does not match`(action: AttendanceAction) {
      val appointment1 = appointmentEntity(mockAppointmentSeries("RSI"), appointmentId = 1, sequenceNumber = 1, prisonerNumberToBookingIdMap = mapOf("AA1111A" to 1))
      val appointment2 = appointmentEntity(mockAppointmentSeries(), appointmentId = 2, sequenceNumber = 2, prisonerNumberToBookingIdMap = mapOf("CC1111C" to 3))

      whenever(appointmentRepository.findByIds(listOf(1, 2))).thenReturn((listOf(appointment1, appointment2)))

      val appointment1Request = MultipleAppointmentAttendanceRequest(1, listOf("AA1111A"))
      val appointment2Request = MultipleAppointmentAttendanceRequest(2, listOf("CC1111C"))

      assertThatThrownBy { service.markMultipleAttendances(listOf(appointment1Request, appointment2Request), action, principal) }
        .isInstanceOf(CaseloadAccessException::class.java)

      verify(appointmentRepository, never()).saveAndFlush(any())
      verifyNoInteractions(telemetryClient)
    }

    @Test
    fun `Will mark attendances for appointments that exist`() {
      val appointment1 = appointmentEntity(mockAppointmentSeries(), appointmentId = 1, sequenceNumber = 1, prisonerNumberToBookingIdMap = mapOf("AA1111A" to 1, "BB1111B" to 2))
      val appointment2 = appointmentEntity(mockAppointmentSeries(), appointmentId = 2, sequenceNumber = 2, prisonerNumberToBookingIdMap = mapOf("CC1111C" to 3))

      whenever(appointmentRepository.findByIds(listOf(1, 2, 3))).thenReturn((listOf(appointment1, appointment2)))

      val appointment1Request = MultipleAppointmentAttendanceRequest(1, listOf("AA1111A", "BB1111B"))
      val appointment2Request = MultipleAppointmentAttendanceRequest(2, listOf("CC1111C", "DD1111D"))
      val appointment3Request = MultipleAppointmentAttendanceRequest(3, listOf("EE1111E"))

      val startTimeInMs = System.currentTimeMillis()

      service.markMultipleAttendances(
        listOf(appointment1Request, appointment2Request, appointment3Request),
        AttendanceAction.ATTENDED,
        principal,
      )

      verify(appointmentRepository, times(2)).saveAndFlush<Appointment>(appointmentCaptor.capture())

      val savedEntities = appointmentCaptor.allValues

      val recordedTime = savedEntities.first().attendees().first().attendanceRecordedTime!!

      with(savedEntities.first { appointment -> appointment.appointmentId == appointment1.appointmentId }) {
        with(attendees().first { attendee -> attendee.prisonerNumber == "AA1111A" }) {
          prisonerNumber isEqualTo "AA1111A"
          attendanceRecordedTime isEqualTo recordedTime
          attendanceRecordedBy isEqualTo username
          attended = true
        }
        with(attendees().first { attendee -> attendee.prisonerNumber == "BB1111B" }) {
          prisonerNumber isEqualTo "BB1111B"
          attendanceRecordedTime isEqualTo recordedTime
          attendanceRecordedBy isEqualTo username
          attended = true
        }
      }

      with(savedEntities.first { appointment -> appointment.appointmentId == appointment2.appointmentId }) {
        with(attendees().first { attendee -> attendee.prisonerNumber == "CC1111C" }) {
          prisonerNumber isEqualTo "CC1111C"
          attendanceRecordedTime isEqualTo recordedTime
          attendanceRecordedBy isEqualTo username
          attended = true
        }
      }

      verify(telemetryClient, times(2)).trackEvent(
        eq(TelemetryEvent.APPOINTMENT_ATTENDANCE_MARKED_METRICS.value),
        telemetryPropertyMapCaptor.capture(),
        telemetryMetricsMapCaptor.capture(),
      )

      verifyTelemetryProperties(telemetryPropertyMapCaptor.firstValue, appointment1.appointmentId)
      verifyTelemetryMetrics(telemetryMetricsMapCaptor.firstValue, startTimeInMs, 2, 0, 0)

      verifyTelemetryProperties(telemetryPropertyMapCaptor.secondValue, appointment2.appointmentId)
      verifyTelemetryMetrics(telemetryMetricsMapCaptor.secondValue, startTimeInMs, 1, 0, 0)
    }

    @Test
    fun `Will mark attendances where attendance has already been set`() {
      val appointment1 = appointmentEntity(mockAppointmentSeries(), appointmentId = 1, sequenceNumber = 1, prisonerNumberToBookingIdMap = mapOf("AA1111A" to 1, "BB1111B" to 2))

      appointment1.attendees().first().attended = false

      whenever(appointmentRepository.findByIds(listOf(1))).thenReturn((listOf(appointment1)))

      val appointment1Request = MultipleAppointmentAttendanceRequest(1, listOf("AA1111A", "BB1111B"))

      val startTimeInMs = System.currentTimeMillis()

      service.markMultipleAttendances(listOf(appointment1Request), AttendanceAction.ATTENDED, principal)

      verify(appointmentRepository).saveAndFlush<Appointment>(appointmentCaptor.capture())

      with(appointmentCaptor.value) {
        val recordedTime = attendees().first().attendanceRecordedTime!!

        with(attendees().first { attendee -> attendee.prisonerNumber == "AA1111A" }) {
          prisonerNumber isEqualTo "AA1111A"
          attendanceRecordedTime isEqualTo recordedTime
          attendanceRecordedBy isEqualTo username
          attended = true
        }
        with(attendees().first { attendee -> attendee.prisonerNumber == "BB1111B" }) {
          prisonerNumber isEqualTo "BB1111B"
          attendanceRecordedTime isEqualTo recordedTime
          attendanceRecordedBy isEqualTo username
          attended = true
        }
      }

      verify(telemetryClient).trackEvent(
        eq(TelemetryEvent.APPOINTMENT_ATTENDANCE_MARKED_METRICS.value),
        telemetryPropertyMapCaptor.capture(),
        telemetryMetricsMapCaptor.capture(),
      )

      verifyTelemetryProperties(telemetryPropertyMapCaptor.value, appointment1.appointmentId)
      verifyTelemetryMetrics(telemetryMetricsMapCaptor.value, startTimeInMs, 2, 0, 1)
    }

    @Test
    fun `Will mark non-attendances for appointments that exist`() {
      val appointment1 = appointmentEntity(mockAppointmentSeries(), appointmentId = 1, sequenceNumber = 1, prisonerNumberToBookingIdMap = mapOf("AA1111A" to 1, "BB1111B" to 2))

      appointment1.attendees().first().attended = true

      whenever(appointmentRepository.findByIds(listOf(1))).thenReturn((listOf(appointment1)))

      val appointment1Request = MultipleAppointmentAttendanceRequest(1, listOf("AA1111A", "BB1111B"))

      val startTimeInMs = System.currentTimeMillis()

      service.markMultipleAttendances(listOf(appointment1Request), AttendanceAction.NOT_ATTENDED, principal)

      verify(appointmentRepository).saveAndFlush<Appointment>(appointmentCaptor.capture())

      with(appointmentCaptor.value) {
        val recordedTime = attendees().first().attendanceRecordedTime!!

        with(attendees().first { attendee -> attendee.prisonerNumber == "AA1111A" }) {
          prisonerNumber isEqualTo "AA1111A"
          attendanceRecordedTime isEqualTo recordedTime
          attendanceRecordedBy isEqualTo username
          attended = false
        }
        with(attendees().first { attendee -> attendee.prisonerNumber == "BB1111B" }) {
          prisonerNumber isEqualTo "BB1111B"
          attendanceRecordedTime isEqualTo recordedTime
          attendanceRecordedBy isEqualTo username
          attended = false
        }
      }

      verify(telemetryClient).trackEvent(
        eq(TelemetryEvent.APPOINTMENT_ATTENDANCE_MARKED_METRICS.value),
        telemetryPropertyMapCaptor.capture(),
        telemetryMetricsMapCaptor.capture(),
      )

      verifyTelemetryProperties(telemetryPropertyMapCaptor.value, appointment1.appointmentId)
      verifyTelemetryMetrics(telemetryMetricsMapCaptor.value, startTimeInMs, 0, 2, 1)
    }

    @Test
    fun `Will reset attendances for appointments that exist`() {
      val appointment1 = appointmentEntity(mockAppointmentSeries(), appointmentId = 1, sequenceNumber = 1, prisonerNumberToBookingIdMap = mapOf("AA1111A" to 1, "BB1111B" to 2))

      appointment1.attendees().first().attended = true

      whenever(appointmentRepository.findByIds(listOf(1))).thenReturn((listOf(appointment1)))

      val appointment1Request = MultipleAppointmentAttendanceRequest(1, listOf("AA1111A", "BB1111B"))

      val startTimeInMs = System.currentTimeMillis()

      service.markMultipleAttendances(listOf(appointment1Request), AttendanceAction.RESET, principal)

      verify(appointmentRepository).saveAndFlush<Appointment>(appointmentCaptor.capture())

      with(appointmentCaptor.value) {
        with(attendees().first { attendee -> attendee.prisonerNumber == "AA1111A" }) {
          prisonerNumber isEqualTo "AA1111A"
          attendanceRecordedTime isEqualTo null
          attendanceRecordedBy isEqualTo null
          attended = null
        }
        with(attendees().first { attendee -> attendee.prisonerNumber == "BB1111B" }) {
          prisonerNumber isEqualTo "BB1111B"
          attendanceRecordedTime isEqualTo null
          attendanceRecordedBy isEqualTo null
          attended = null
        }
      }

      verify(telemetryClient).trackEvent(
        eq(TelemetryEvent.APPOINTMENT_ATTENDANCE_MARKED_METRICS.value),
        telemetryPropertyMapCaptor.capture(),
        telemetryMetricsMapCaptor.capture(),
      )

      verifyTelemetryProperties(telemetryPropertyMapCaptor.value, appointment1.appointmentId)
      verifyTelemetryMetrics(telemetryMetricsMapCaptor.value, startTimeInMs, 0, 0, 1)
    }

    private fun mockAppointmentSeries(prisonCode: String = MOORLAND_PRISON_CODE): AppointmentSeries {
      val appointmentSeries = mock<AppointmentSeries>()
      whenever(appointmentSeries.startTime).thenReturn(LocalTime.now())
      whenever(appointmentSeries.prisonCode).thenReturn(prisonCode)
      whenever(appointmentSeries.categoryCode).thenReturn("CANT")
      whenever(appointmentSeries.createdTime).thenReturn(LocalDateTime.now())
      whenever(appointmentSeries.createdBy).thenReturn("A_USER")
      return appointmentSeries
    }

    private fun verifyTelemetryProperties(properties: Map<String, String>, appointmentId: Long) {
      properties[USER_PROPERTY_KEY] isEqualTo username
      properties[PRISON_CODE_PROPERTY_KEY] isEqualTo MOORLAND_PRISON_CODE
      properties[APPOINTMENT_ID_PROPERTY_KEY] isEqualTo appointmentId.toString()
    }

    private fun verifyTelemetryMetrics(metrics: Map<String, Double>, startTimeInMs: Long, numAttended: Long, numNonAttended: Long, numChanged: Long) {
      metrics[PRISONERS_ATTENDED_COUNT_METRIC_KEY] isEqualTo numAttended.toDouble()
      metrics[PRISONERS_NON_ATTENDED_COUNT_METRIC_KEY] isEqualTo numNonAttended.toDouble()
      metrics[PRISONERS_ATTENDANCE_CHANGED_COUNT_METRIC_KEY] isEqualTo numChanged.toDouble()
      assertThat(metrics[EVENT_TIME_MS_METRIC_KEY]).isCloseTo((System.currentTimeMillis() - startTimeInMs).toDouble(), within(1000.0))
    }
  }

  @Nested
  inner class AppointmentAttendanceByStatus {

    private val now = LocalTime.now()

    inner class TestData(val didTheyAttend: Boolean? = null, val tier: EventTierType = EventTierType.TIER_1) : AppointmentAndAttendee {
      override fun getPrisonerNumber(): String = "prisoner"

      override fun getBookingId(): Long = 1

      override fun getAppointmentId(): Long = 2

      override fun getAppointmentAttendeeId(): Long = 3

      override fun getStartDate(): LocalDate = LocalDate.now()

      override fun getStartTime(): LocalTime = now

      override fun getEndTime(): LocalTime = now.plusHours(1)

      override fun getEventTier(): String = tier.name

      override fun getCategoryCode(): String = "CAT"

      override fun getCustomName(): String = "custom"

      override fun getAttended(): Boolean? = didTheyAttend
    }

    private val filterTests = listOf(
      TestData(didTheyAttend = true, tier = EventTierType.TIER_2),
      TestData(true),
      TestData(),
      TestData(false),
      TestData(didTheyAttend = false, tier = EventTierType.TIER_2),
    )

    @BeforeEach
    fun `init`() {
      val referenceCodeMap = mapOf(
        entity.categoryCode to appointmentCategoryReferenceCode(entity.categoryCode, "Chaplaincy"),
        "TEST_CAT" to appointmentCategoryReferenceCode("TEST_CAT", "appointment"),
      )
      whenever(referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY)).thenReturn(referenceCodeMap)
    }

    @Test
    fun `throws exception if status is EVENT_TIER and no EventTierType supplied`() {
      assertThrows<ValidationException> {
        service.getAppointmentAttendanceByStatus(
          prisonCode = "MDI",
          date = LocalDate.now(),
          status = AttendanceStatus.EVENT_TIER,
        )
      }
    }

    @Test
    fun `verify maps all fields`() {
      val testData = TestData(true)
      whenever(appointmentRepository.getAppointmentsWithAttendees(any(), any(), anyOrNull(), anyOrNull(), anyOrNull(), any(), anyOrNull()))
        .thenReturn(listOf(testData))

      val response = service.getAppointmentAttendanceByStatus(
        prisonCode = "MDI",
        date = LocalDate.now(),
        status = AttendanceStatus.ATTENDED,
      ).first()

      assertThat(response.appointmentId).isEqualTo(testData.getAppointmentId())
      assertThat(response.appointmentAttendeeId).isEqualTo(testData.getAppointmentAttendeeId())
      assertThat(response.bookingId).isEqualTo(testData.getBookingId())
      assertThat(response.prisonerNumber).isEqualTo(testData.getPrisonerNumber())
      assertThat(response.startDate).isEqualTo(testData.getStartDate())
      assertThat(response.startTime).isEqualTo(testData.getStartTime())
      assertThat(response.endTime).isEqualTo(testData.getEndTime())
      assertThat(response.appointmentName).isEqualTo("custom (CAT)")
    }

    @Test
    fun `filter ATTENDED`() {
      whenever(appointmentRepository.getAppointmentsWithAttendees(any(), any(), anyOrNull(), anyOrNull(), anyOrNull(), any(), anyOrNull()))
        .thenReturn(filterTests)

      val response = service.getAppointmentAttendanceByStatus(
        prisonCode = "MDI",
        date = LocalDate.now(),
        status = AttendanceStatus.ATTENDED,
      )

      assertThat(response.size).isEqualTo(2)
    }

    @Test
    fun `filter NOT ATTENDED`() {
      whenever(appointmentRepository.getAppointmentsWithAttendees(any(), any(), anyOrNull(), anyOrNull(), anyOrNull(), any(), anyOrNull()))
        .thenReturn(filterTests)

      val response = service.getAppointmentAttendanceByStatus(
        prisonCode = "MDI",
        date = LocalDate.now(),
        status = AttendanceStatus.NOT_ATTENDED,
      )

      assertThat(response.size).isEqualTo(2)
    }

    @Test
    fun `filter NOT RECORDED`() {
      whenever(appointmentRepository.getAppointmentsWithAttendees(any(), any(), anyOrNull(), anyOrNull(), anyOrNull(), any(), anyOrNull()))
        .thenReturn(filterTests)

      val response = service.getAppointmentAttendanceByStatus(
        prisonCode = "MDI",
        date = LocalDate.now(),
        status = AttendanceStatus.NOT_RECORDED,
      )

      assertThat(response.size).isEqualTo(1)
    }

    @Test
    fun `filter EVENT TIER`() {
      whenever(appointmentRepository.getAppointmentsWithAttendees(any(), any(), anyOrNull(), anyOrNull(), anyOrNull(), any(), anyOrNull()))
        .thenReturn(filterTests)

      val response = service.getAppointmentAttendanceByStatus(
        prisonCode = "MDI",
        date = LocalDate.now(),
        status = AttendanceStatus.EVENT_TIER,
        eventTier = EventTierType.TIER_2,
      )

      assertThat(response.size).isEqualTo(1)
    }
  }
}
