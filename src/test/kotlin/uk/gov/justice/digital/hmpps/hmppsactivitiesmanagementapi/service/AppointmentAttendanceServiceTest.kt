package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventTierType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentAttendanceSummaryEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentAttendanceSummaryModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSearchEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentAttendanceRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentAndAttendee
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentAttendanceSummaryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentAttendeeSearchRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment.AppointmentAttendanceService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.ReferenceCodeDomain
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.ReferenceCodeService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.CaseloadAccessException
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.addCaseloadIdToRequestHeader
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.clearCaseloadIdFromRequestHeader
import java.security.Principal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment as AppointmentModel

class AppointmentAttendanceServiceTest {
  private val appointmentAttendanceSummaryRepository: AppointmentAttendanceSummaryRepository = mock()
  private val appointmentRepository: AppointmentRepository = mock()
  private val referenceCodeService: ReferenceCodeService = mock()
  private val locationService: LocationService = mock()
  private val appointmentAttendeeSearchRepository: AppointmentAttendeeSearchRepository = mock()

  private val service = AppointmentAttendanceService(appointmentAttendanceSummaryRepository, appointmentRepository, referenceCodeService, locationService, appointmentAttendeeSearchRepository)

  private val principal: Principal = mock()
  private val username = "ATTENDANCE.RECORDED.BY"
  private val entity = appointmentAttendanceSummaryEntity()

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
      whenever(appointmentAttendanceSummaryRepository.findByPrisonCodeAndStartDateAndCustomName("RAN", date, "custom")).thenReturn(
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
      whenever(appointmentAttendanceSummaryRepository.findByPrisonCodeAndStartDateAndCategoryCodeAndCustomName("RAN", date, "appointment", "custom")).thenReturn(
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
  inner class AppointmentAttendanceByStatus {

    private val now = LocalTime.now()

    inner class TestData(val toTest: Boolean? = null, val tier: EventTierType = EventTierType.TIER_1) :
      AppointmentAndAttendee {
      override fun getPrisonerNumber(): String = "prisoner"

      override fun getBookingId(): Long = 1

      override fun getAppointmentId(): Long = 2

      override fun getAppointmentAttendeeId(): Long = 3

      override fun getStartDate(): LocalDate = LocalDate.now()

      override fun getStartTime(): LocalTime = now

      override fun getEndTime(): LocalTime = now.plusHours(1)

      override fun getEventTier(): String = tier.name

      override fun getCategoryCode(): String = "CAT"

      override fun getCustomName(): String? = "custom"

      override fun getAttended(): Boolean? = toTest
    }

    private val filterTests = listOf(
      TestData(tier = EventTierType.TIER_2),
      TestData(true),
      TestData(false),
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

      assertThat(response.size).isEqualTo(1)
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

      assertThat(response.size).isEqualTo(1)
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
