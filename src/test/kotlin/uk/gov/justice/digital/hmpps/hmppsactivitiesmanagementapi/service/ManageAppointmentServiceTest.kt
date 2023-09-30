package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.MovementType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentAttendeeDeletedReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentAttendeeRemovalReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PRISONER_STATUS_RELEASED_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID
import java.time.LocalDate
import java.util.Optional

class ManageAppointmentServiceTest {
  private val appointmentRepository: AppointmentRepository = mock()
  private val appointmentAttendeeRemovalReasonRepository: AppointmentAttendeeRemovalReasonRepository = mock()
  private val prisonerSearch: PrisonerSearchApiApplicationClient = mock()

  private val service = ManageAppointmentService(
    appointmentRepository,
    appointmentAttendeeRemovalReasonRepository,
    prisonerSearch,
  )

  @Nested
  @DisplayName("Manage appointment attendees")
  inner class ManageAppointmentAttendees {
    private val activeInPrisoner = PrisonerSearchPrisonerFixture.instance(
      prisonerNumber = "A1234BC",
      inOutStatus = Prisoner.InOutStatus.IN,
      status = "ACTIVE IN",
      lastMovementType = null,
      confirmedReleaseDate = LocalDate.now().plusDays(2),
    )

    private val prisonerReleasedToday = PrisonerSearchPrisonerFixture.instance(
      prisonerNumber = "A1234BC",
      inOutStatus = Prisoner.InOutStatus.OUT,
      status = "INACTIVE OUT",
      lastMovementType = MovementType.RELEASE,
      confirmedReleaseDate = LocalDate.now(),
    )

    @BeforeEach
    fun setup() {
      whenever(
        appointmentAttendeeRemovalReasonRepository.findById(
          PRISONER_STATUS_RELEASED_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID,
        ),
      ).thenReturn(
        Optional.of(appointmentAttendeeDeletedReason()),
      )
    }

    @Test
    fun `do not remove attendee records for prisoners that have not been released`() {
      val appointmentSeries = appointmentSeriesEntity(startDate = LocalDate.now())
      val appointment = appointmentSeries.appointments().first()

      whenever(appointmentRepository.findAllByStartDate(LocalDate.now())).thenReturn(appointmentSeries.appointments())
      whenever(prisonerSearch.findByPrisonerNumbers(appointment.prisonerNumbers())).thenReturn(Mono.just(listOf(activeInPrisoner)))

      service.manageAppointmentAttendees(LocalDateRange(LocalDate.now(), LocalDate.now()))

      with(appointment.attendees()) {
        this hasSize 1
        onEach { it.isRemoved() isEqualTo false }
        onEach { it.isDeleted isEqualTo false }
      }
    }

    @Test
    fun `do not remove attendee records for appointments with start dates prior to prisoner confirmed release date`() {
      val appointmentSeries = appointmentSeriesEntity(startDate = LocalDate.now().minusDays(1))
      val appointment = appointmentSeries.appointments().first()

      whenever(appointmentRepository.findAllByStartDate(LocalDate.now().minusDays(1))).thenReturn(appointmentSeries.appointments())
      whenever(prisonerSearch.findByPrisonerNumbers(appointment.prisonerNumbers())).thenReturn(Mono.just(listOf(prisonerReleasedToday)))

      service.manageAppointmentAttendees(LocalDateRange(LocalDate.now().minusDays(1), LocalDate.now().minusDays(1)))

      with(appointment.attendees()) {
        this hasSize 1
        onEach { it.isRemoved() isEqualTo false }
        onEach { it.isDeleted isEqualTo false }
      }
    }

    @Test
    fun `remove attendee records for appointments with start dates on or after prisoner confirmed release date`() {
      val appointmentSeries = appointmentSeriesEntity(startDate = LocalDate.now())
      val appointment = appointmentSeries.appointments().first()

      whenever(appointmentRepository.findAllByStartDate(LocalDate.now())).thenReturn(appointmentSeries.appointments())
      whenever(prisonerSearch.findByPrisonerNumbers(appointment.prisonerNumbers())).thenReturn(Mono.just(listOf(prisonerReleasedToday)))

      service.manageAppointmentAttendees(LocalDateRange(LocalDate.now(), LocalDate.now()))

      assertThat(appointment.attendees()).isEmpty()
    }
  }
}
