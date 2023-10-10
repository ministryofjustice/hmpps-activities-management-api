package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentAttendanceRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentAttendanceSummaryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.CaseloadAccessException
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.addCaseloadIdToRequestHeader
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.clearCaseloadIdFromRequestHeader
import java.security.Principal
import java.time.LocalDateTime
import java.util.Optional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment as AppointmentModel

class AppointmentAttendanceServiceTest {
  private val appointmentAttendanceSummaryRepository: AppointmentAttendanceSummaryRepository = mock()
  private val appointmentRepository: AppointmentRepository = mock()
  private val referenceCodeService: ReferenceCodeService = mock()
  private val locationService: LocationService = mock()

  private val service = AppointmentAttendanceService(appointmentAttendanceSummaryRepository, appointmentRepository, referenceCodeService, locationService)

  private val principal: Principal = mock()
  private val username = "ATTENDANCE.RECORDED.BY"

  @BeforeEach
  fun setUp() {
    addCaseloadIdToRequestHeader(moorlandPrisonCode)
    whenever(principal.name).thenReturn(username)
  }

  @AfterEach
  fun tearDown() {
    clearCaseloadIdFromRequestHeader()
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
      whenever(entity.prisonCode).thenReturn(moorlandPrisonCode)
      whenever(appointmentRepository.findById(1)).thenReturn(Optional.of(entity))

      assertThatThrownBy { service.markAttendance(1, AppointmentAttendanceRequest(), principal) }
        .isInstanceOf(CaseloadAccessException::class.java)

      verify(appointmentRepository, never()).saveAndFlush(any())
    }

    @Test
    fun `calls appointment mark prisoner attendance function`() {
      val entity = mock<Appointment>()
      whenever(entity.prisonCode).thenReturn(moorlandPrisonCode)
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
}
