package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.MovementType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentAttendee
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentInstanceEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.cancelOnTransferAppointmentAttendeeRemovalReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.movement
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.pentonvillePrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.prisonRegime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.prisonerReleasedAppointmentAttendeeRemovalReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentCancelledOnTransferEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentAttendeeRemovalReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentAttendeeRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.CANCEL_ON_TRANSFER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PRISONER_STATUS_RELEASED_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PrisonRegimeRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.FakeSecurityContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(FakeSecurityContext::class)
class AppointmentAttendeeServiceTest {
  private val appointmentRepository = mock<AppointmentRepository>()
  private val appointmentAttendeeRepository = mock<AppointmentAttendeeRepository>()
  private val appointmentInstanceRepository = mock<AppointmentInstanceRepository>()
  private val appointmentAttendeeRemovalReasonRepository = mock<AppointmentAttendeeRemovalReasonRepository>()
  private val prisonRegimeRepository = mock<PrisonRegimeRepository>()
  private val prisonerSearch = mock<PrisonerSearchApiApplicationClient>()
  private val prisonApi = mock<PrisonApiApplicationClient>()
  private val auditService = mock<AuditService>()

  private val service = AppointmentAttendeeService(
    appointmentRepository,
    appointmentAttendeeRepository,
    appointmentInstanceRepository,
    appointmentAttendeeRemovalReasonRepository,
    prisonRegimeRepository,
    prisonerSearch,
    prisonApi,
    TransactionHandler(),
    auditService,
  )

  @Nested
  @DisplayName("Remove prisoner from future appointments")
  inner class RemovePrisonerFromFutureAppointments {
    @Test
    fun `removes prisoner from future appointments`() {
      val appointmentAttendeeId = 42L
      val prisonCode = "PVI"
      val prisonerNumber = "ABC123"
      val removedTime = LocalDateTime.now()
      val removedBy = "OFFENDER_RELEASED_EVENT"
      val appointmentInstance = mock<AppointmentInstance>()
      val appointmentAttendee = mock<AppointmentAttendee>()

      whenever(appointmentInstance.appointmentAttendeeId).thenReturn(appointmentAttendeeId)
      whenever(appointmentInstance.prisonCode).thenReturn(prisonCode)
      whenever(appointmentInstance.prisonerNumber).thenReturn(prisonerNumber)

      whenever(appointmentAttendeeRemovalReasonRepository.findById(PRISONER_STATUS_RELEASED_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID))
        .thenReturn(Optional.of(prisonerReleasedAppointmentAttendeeRemovalReason()))

      whenever(appointmentInstanceRepository.findByPrisonCodeAndPrisonerNumberFromNow(prisonCode, prisonerNumber))
        .thenReturn(listOf(appointmentInstance))

      whenever(appointmentAttendeeRepository.findById(appointmentAttendeeId)).thenReturn(Optional.of(appointmentAttendee))

      service.removePrisonerFromFutureAppointments(
        prisonCode,
        prisonerNumber,
        PRISONER_STATUS_RELEASED_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID,
        removedTime,
        removedBy,
      )

      verify(appointmentAttendee).remove(
        removedTime,
        prisonerReleasedAppointmentAttendeeRemovalReason(),
        removedBy,
      )

      verify(auditService).logEvent(any<AppointmentCancelledOnTransferEvent>())
    }

    @Test
    fun `does not remove anything if there are no future appointments`() {
      val prisonCode = "PVI"
      val prisonerNumber = "ABC123"
      val removedTime = LocalDateTime.now()
      val removedBy = "OFFENDER_RELEASED_EVENT"

      whenever(appointmentAttendeeRemovalReasonRepository.findById(CANCEL_ON_TRANSFER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID))
        .thenReturn(Optional.of(cancelOnTransferAppointmentAttendeeRemovalReason()))

      whenever(appointmentInstanceRepository.findByPrisonCodeAndPrisonerNumberFromNow(prisonCode, prisonerNumber))
        .thenReturn(listOf())

      service.removePrisonerFromFutureAppointments(
        prisonCode,
        prisonerNumber,
        CANCEL_ON_TRANSFER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID,
        removedTime,
        removedBy,
      )

      verifyNoInteractions(appointmentAttendeeRepository)
      verifyNoInteractions(auditService)
    }
  }

  @Nested
  @DisplayName("Manage appointment attendees")
  inner class ManageAppointmentAttendees {
    private val activeInPrisoner = PrisonerSearchPrisonerFixture.instance(
      prisonerNumber = "A1234BC",
      inOutStatus = Prisoner.InOutStatus.IN,
      status = "ACTIVE IN",
      prisonId = moorlandPrisonCode,
      lastMovementType = null,
      confirmedReleaseDate = LocalDate.now().plusDays(2),
    )

    private val activeInDifferentPrison = PrisonerSearchPrisonerFixture.instance(
      prisonerNumber = "A1234BC",
      inOutStatus = Prisoner.InOutStatus.IN,
      status = "ACTIVE IN",
      prisonId = pentonvillePrisonCode,
      lastMovementType = null,
      confirmedReleaseDate = LocalDate.now().plusDays(2),
    )

    private val activeOutPrisoner = PrisonerSearchPrisonerFixture.instance(
      prisonerNumber = "A1234BC",
      inOutStatus = Prisoner.InOutStatus.OUT,
      status = "ACTIVE OUT",
      prisonId = moorlandPrisonCode,
      lastMovementType = null,
      confirmedReleaseDate = LocalDate.now().plusDays(2),
    )

    private val prisonerReleasedToday = PrisonerSearchPrisonerFixture.instance(
      prisonerNumber = "A1234BC",
      inOutStatus = Prisoner.InOutStatus.OUT,
      status = "INACTIVE OUT",
      prisonId = moorlandPrisonCode,
      lastMovementType = MovementType.RELEASE,
      confirmedReleaseDate = LocalDate.now(),
    )

    private val expiredMovement = movement(prisonerNumber = "A1234BC", fromPrisonCode = moorlandPrisonCode, movementDate = TimeSource.yesterday())
    private val nonExpiredMovement = movement(prisonerNumber = "A1234BC", fromPrisonCode = moorlandPrisonCode, movementDate = TimeSource.today())

    @BeforeEach
    fun setup() {
      whenever(prisonRegimeRepository.findByPrisonCode(moorlandPrisonCode)).thenReturn(prisonRegime())
      whenever(
        appointmentAttendeeRemovalReasonRepository.findById(
          PRISONER_STATUS_RELEASED_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID,
        ),
      ).thenReturn(
        Optional.of(prisonerReleasedAppointmentAttendeeRemovalReason()),
      )
    }

    @Test
    fun `days after now cannot be more than 60 days`() {
      assertThatThrownBy {
        service.manageAppointmentAttendees(moorlandPrisonCode, 61)
      }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Supplied days after now must be at least one day and less than 61 days")
    }

    @Test
    fun `days after now cannot be negative`() {
      assertThatThrownBy {
        service.manageAppointmentAttendees(moorlandPrisonCode, -1)
      }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Supplied days after now must be at least one day and less than 61 days")
    }

    @Test
    fun `prison regime must exist`() {
      assertThatThrownBy {
        service.manageAppointmentAttendees(pentonvillePrisonCode, 0)
      }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Rolled out prison $pentonvillePrisonCode is missing a prison regime.")
    }

    @Test
    fun `do not remove attendee records for prisoners that have not been released`() {
      val appointmentSeries = appointmentSeriesEntity(startDate = LocalDate.now())
      val appointment = appointmentSeries.appointments().first()
      val appointmentAttendee = appointment.attendees().first()
      val appointmentInstance = appointmentInstanceEntity()

      whenever(appointmentRepository.findAllByPrisonCodeAndStartDate(moorlandPrisonCode, LocalDate.now())).thenReturn(appointmentSeries.appointments())
      whenever(prisonerSearch.findByPrisonerNumbers(appointment.prisonerNumbers())).thenReturn(Mono.just(listOf(activeInPrisoner)))
      whenever(appointmentInstanceRepository.findByPrisonCodeAndPrisonerNumberFromNow(moorlandPrisonCode, activeInPrisoner.prisonerNumber)).thenReturn(listOf(appointmentInstance))
      whenever(appointmentAttendeeRepository.findById(appointmentInstance.appointmentAttendeeId)).thenReturn(Optional.of(appointmentAttendee))

      service.manageAppointmentAttendees(moorlandPrisonCode, 0)

      assertThat(appointment.attendees()).isNotEmpty()

      with(appointmentAttendee) {
        removedTime isEqualTo null
        removalReason isEqualTo null
        removedBy isEqualTo null
        isRemoved() isBool false
        isDeleted isBool false
      }

      verifyNoInteractions(appointmentInstanceRepository)
      verifyNoInteractions(appointmentAttendeeRepository)
    }

    @Test
    fun `do not remove attendee records for appointments with start dates prior to prisoner confirmed release date`() {
      val appointmentSeries = appointmentSeriesEntity(startDate = LocalDate.now().minusDays(1))
      val appointment = appointmentSeries.appointments().first()
      val appointmentAttendee = appointment.attendees().first()
      val appointmentInstance = appointmentInstanceEntity()

      whenever(appointmentRepository.findAllByPrisonCodeAndStartDate(moorlandPrisonCode, LocalDate.now().minusDays(1))).thenReturn(appointmentSeries.appointments())
      whenever(prisonerSearch.findByPrisonerNumbers(appointment.prisonerNumbers())).thenReturn(Mono.just(listOf(prisonerReleasedToday)))
      whenever(appointmentInstanceRepository.findByPrisonCodeAndPrisonerNumberFromNow(moorlandPrisonCode, activeInPrisoner.prisonerNumber)).thenReturn(listOf(appointmentInstance))
      whenever(appointmentAttendeeRepository.findById(appointmentInstance.appointmentAttendeeId)).thenReturn(Optional.of(appointmentAttendee))

      service.manageAppointmentAttendees(moorlandPrisonCode, 0)

      assertThat(appointment.attendees()).isNotEmpty()

      with(appointmentAttendee) {
        removedTime isEqualTo null
        removalReason isEqualTo null
        removedBy isEqualTo null
        isRemoved() isBool false
        isDeleted isBool false
      }

      verifyNoInteractions(appointmentInstanceRepository)
      verifyNoInteractions(appointmentAttendeeRepository)
    }

    @Test
    fun `remove attendee records for appointments with start dates on or after prisoner confirmed release date`() {
      val appointmentSeries = appointmentSeriesEntity(startDate = LocalDate.now())
      val appointment = appointmentSeries.appointments().first()
      val appointmentAttendee = appointment.attendees().first()
      val appointmentInstance = appointmentInstanceEntity()

      whenever(appointmentRepository.findAllByPrisonCodeAndStartDate(moorlandPrisonCode, LocalDate.now())).thenReturn(appointmentSeries.appointments())
      whenever(prisonerSearch.findByPrisonerNumbers(appointment.prisonerNumbers())).thenReturn(Mono.just(listOf(prisonerReleasedToday)))
      whenever(appointmentInstanceRepository.findByPrisonCodeAndPrisonerNumberFromNow(moorlandPrisonCode, activeInPrisoner.prisonerNumber)).thenReturn(listOf(appointmentInstance))
      whenever(appointmentAttendeeRepository.findById(appointmentInstance.appointmentAttendeeId)).thenReturn(Optional.of(appointmentAttendee))

      service.manageAppointmentAttendees(moorlandPrisonCode, 0)

      assertThat(appointment.attendees()).isEmpty()

      with(appointmentAttendee) {
        removedTime isCloseTo LocalDateTime.now()
        removalReason isEqualTo prisonerReleasedAppointmentAttendeeRemovalReason()
        removedBy isEqualTo "MANAGE_APPOINTMENT_SERVICE"
        isRemoved() isBool false
        isDeleted isBool true
      }
    }
  }
}
