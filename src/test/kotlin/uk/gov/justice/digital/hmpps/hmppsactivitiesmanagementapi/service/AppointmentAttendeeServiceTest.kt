package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.AdditionalAnswers
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.MovementType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentAttendee
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.cancelOnTransferAppointmentAttendeeRemovalReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.movement
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.pentonvillePrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.prisonRegime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.prisonerPermanentTransferAppointmentAttendeeRemovalReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.prisonerReleasedAppointmentAttendeeRemovalReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentCancelledOnTransferEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentAttendeeRemovalReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentAttendeeRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.CANCEL_ON_TRANSFER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PRISONER_STATUS_PERMANENT_TRANSFER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PRISONER_STATUS_RELEASED_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PrisonRegimeRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
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
  private val outboundEventsService: OutboundEventsService = mock()
  private val auditService = mock<AuditService>()

  private val appointmentCaptor = argumentCaptor<Appointment>()

  private val service = spy(
    AppointmentAttendeeService(
      appointmentRepository,
      appointmentAttendeeRepository,
      appointmentInstanceRepository,
      appointmentAttendeeRemovalReasonRepository,
      prisonRegimeRepository,
      prisonerSearch,
      prisonApi,
      TransactionHandler(),
      outboundEventsService,
      auditService,
    ),
  )

  @BeforeEach
  fun setUp() {
    whenever(appointmentRepository.saveAndFlush(appointmentCaptor.capture())).thenAnswer(AdditionalAnswers.returnsFirstArg<Appointment>())
  }

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
      val appointmentAttendeeMock = mock<AppointmentAttendee>()

      whenever(appointmentInstance.appointmentAttendeeId).thenReturn(appointmentAttendeeId)
      whenever(appointmentInstance.prisonCode).thenReturn(prisonCode)
      whenever(appointmentInstance.prisonerNumber).thenReturn(prisonerNumber)

      whenever(appointmentAttendeeMock.appointmentAttendeeId).thenReturn(appointmentAttendeeId)
      whenever(
        appointmentAttendeeMock.remove(
          removedTime,
          prisonerReleasedAppointmentAttendeeRemovalReason(),
          removedBy,
        ),
      ).thenReturn(appointmentAttendeeMock)

      whenever(appointmentAttendeeRemovalReasonRepository.findById(PRISONER_STATUS_RELEASED_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID))
        .thenReturn(Optional.of(prisonerReleasedAppointmentAttendeeRemovalReason()))

      whenever(appointmentInstanceRepository.findByPrisonCodeAndPrisonerNumberFromNow(prisonCode, prisonerNumber))
        .thenReturn(listOf(appointmentInstance))

      whenever(appointmentAttendeeRepository.findById(appointmentInstance.appointmentAttendeeId))
        .thenReturn(Optional.of(appointmentAttendeeMock))

      service.removePrisonerFromFutureAppointments(
        prisonCode,
        prisonerNumber,
        removedTime,
        PRISONER_STATUS_RELEASED_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID,
        removedBy,
      )

      verify(appointmentAttendeeMock).remove(
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
        removedTime,
        CANCEL_ON_TRANSFER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID,
        removedBy,
      )

      verifyNoInteractions(appointmentAttendeeRepository)
      verifyNoInteractions(auditService)
    }
  }

  @Nested
  @DisplayName("Manage appointment attendees")
  inner class ManageAppointmentAttendees {
    private val prisonNumber = "A1234BC"

    private val activeInPrisoner = PrisonerSearchPrisonerFixture.instance(
      prisonerNumber = prisonNumber,
      inOutStatus = Prisoner.InOutStatus.IN,
      status = "ACTIVE IN",
      prisonId = moorlandPrisonCode,
      lastMovementType = null,
      confirmedReleaseDate = null,
    )

    private val activeInDifferentPrison = PrisonerSearchPrisonerFixture.instance(
      prisonerNumber = prisonNumber,
      inOutStatus = Prisoner.InOutStatus.IN,
      status = "ACTIVE IN",
      prisonId = pentonvillePrisonCode,
      lastMovementType = null,
      confirmedReleaseDate = null,
    )

    private val activeOutPrisoner = PrisonerSearchPrisonerFixture.instance(
      prisonerNumber = prisonNumber,
      inOutStatus = Prisoner.InOutStatus.OUT,
      status = "ACTIVE OUT",
      prisonId = moorlandPrisonCode,
      lastMovementType = null,
      confirmedReleaseDate = null,
    )

    private val prisonerReleasedToday = PrisonerSearchPrisonerFixture.instance(
      prisonerNumber = prisonNumber,
      inOutStatus = Prisoner.InOutStatus.OUT,
      status = "INACTIVE OUT",
      prisonId = null,
      lastMovementType = MovementType.RELEASE,
      confirmedReleaseDate = LocalDate.now(),
    )

    private val expiredMovement = movement(prisonerNumber = prisonNumber, fromPrisonCode = moorlandPrisonCode, movementDate = TimeSource.yesterday())
    private val nonExpiredMovement = movement(prisonerNumber = prisonNumber, fromPrisonCode = moorlandPrisonCode, movementDate = TimeSource.today())

    @BeforeEach
    fun setup() {
      whenever(prisonRegimeRepository.findByPrisonCode(moorlandPrisonCode)).thenReturn(prisonRegime(prisonCode = moorlandPrisonCode))
      whenever(
        appointmentAttendeeRemovalReasonRepository.findById(
          PRISONER_STATUS_RELEASED_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID,
        ),
      ).thenReturn(
        Optional.of(prisonerReleasedAppointmentAttendeeRemovalReason()),
      )
      whenever(
        appointmentAttendeeRemovalReasonRepository.findById(
          PRISONER_STATUS_PERMANENT_TRANSFER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID,
        ),
      ).thenReturn(
        Optional.of(prisonerPermanentTransferAppointmentAttendeeRemovalReason()),
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
      service.manageAppointmentAttendees(pentonvillePrisonCode, 0)

      verifyNoInteractions(appointmentRepository)
      verifyNoInteractions(prisonerSearch)
      verifyNoInteractions(prisonApi)
      verify(service, never()).removePrisonerFromFutureAppointments(any(), any(), any(), any(), any())
    }

    @Test
    fun `does not remove future appointments for prisoners that have not been released`() {
      val appointmentSeries = appointmentSeriesEntity(startDate = LocalDate.now())
      val appointment = appointmentSeries.appointments().first()

      whenever(appointmentRepository.findAllByPrisonCodeAndStartDate(moorlandPrisonCode, LocalDate.now())).thenReturn(appointmentSeries.appointments())
      whenever(prisonerSearch.findByPrisonerNumbers(appointment.prisonerNumbers())).thenReturn(listOf(activeInPrisoner))

      service.manageAppointmentAttendees(moorlandPrisonCode, 0)

      verify(service, never()).removePrisonerFromFutureAppointments(any(), any(), any(), any(), any())
    }

    @Test
    fun `remove future appointments for released prisoners`() {
      val appointmentSeries = appointmentSeriesEntity(startDate = LocalDate.now())
      val appointment = appointmentSeries.appointments().first()

      whenever(appointmentRepository.findAllByPrisonCodeAndStartDate(moorlandPrisonCode, LocalDate.now())).thenReturn(appointmentSeries.appointments())
      whenever(prisonerSearch.findByPrisonerNumbers(appointment.prisonerNumbers())).thenReturn(listOf(prisonerReleasedToday))

      service.manageAppointmentAttendees(moorlandPrisonCode, 0)

      verify(service).removePrisonerFromFutureAppointments(
        eq(moorlandPrisonCode),
        eq(prisonerReleasedToday.prisonerNumber),
        any<LocalDateTime>(),
        eq(PRISONER_STATUS_RELEASED_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID),
        eq("MANAGE_APPOINTMENT_SERVICE"),
      )
    }

    @Test
    fun `does not remove future appointments for prisoners that transferred to a different prison an unknown amount of time ago`() {
      val appointmentSeries = appointmentSeriesEntity(startDate = LocalDate.now())
      val appointment = appointmentSeries.appointments().first()

      whenever(appointmentRepository.findAllByPrisonCodeAndStartDate(moorlandPrisonCode, LocalDate.now())).thenReturn(appointmentSeries.appointments())
      whenever(prisonerSearch.findByPrisonerNumbers(appointment.prisonerNumbers())).thenReturn(listOf(activeInDifferentPrison))
      whenever(prisonApi.getMovementsForPrisonersFromPrison(moorlandPrisonCode, setOf(activeInDifferentPrison.prisonerNumber))).thenReturn(emptyList())

      service.manageAppointmentAttendees(moorlandPrisonCode, 0)

      verify(service, never()).removePrisonerFromFutureAppointments(any(), any(), any(), any(), any())
    }

    @Test
    fun `does not remove future appointments for prisoners that transferred to a different prison less than expired days ago`() {
      val appointmentSeries = appointmentSeriesEntity(startDate = LocalDate.now())
      val appointment = appointmentSeries.appointments().first()

      whenever(appointmentRepository.findAllByPrisonCodeAndStartDate(moorlandPrisonCode, LocalDate.now())).thenReturn(appointmentSeries.appointments())
      whenever(prisonerSearch.findByPrisonerNumbers(appointment.prisonerNumbers())).thenReturn(listOf(activeInDifferentPrison))
      whenever(prisonApi.getMovementsForPrisonersFromPrison(moorlandPrisonCode, setOf(activeInDifferentPrison.prisonerNumber))).thenReturn(listOf(nonExpiredMovement))

      service.manageAppointmentAttendees(moorlandPrisonCode, 0)

      verify(service, never()).removePrisonerFromFutureAppointments(any(), any(), any(), any(), any())
    }

    @Test
    fun `remove future appointments for prisoners that transferred to a different prison more than expired days ago`() {
      val appointmentSeries = appointmentSeriesEntity(startDate = LocalDate.now())
      val appointment = appointmentSeries.appointments().first()

      whenever(appointmentRepository.findAllByPrisonCodeAndStartDate(moorlandPrisonCode, LocalDate.now())).thenReturn(appointmentSeries.appointments())
      whenever(prisonerSearch.findByPrisonerNumbers(appointment.prisonerNumbers())).thenReturn(listOf(activeInDifferentPrison))
      whenever(prisonApi.getMovementsForPrisonersFromPrison(moorlandPrisonCode, setOf(activeInDifferentPrison.prisonerNumber))).thenReturn(listOf(expiredMovement))

      service.manageAppointmentAttendees(moorlandPrisonCode, 0)

      verify(service).removePrisonerFromFutureAppointments(
        eq(moorlandPrisonCode),
        eq(prisonerReleasedToday.prisonerNumber),
        any<LocalDateTime>(),
        eq(PRISONER_STATUS_PERMANENT_TRANSFER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID),
        eq("MANAGE_APPOINTMENT_SERVICE"),
      )
    }

    @Test
    fun `remove future appointments for prisoners that have been active out from the prison more than expired days ago`() {
      val appointmentSeries = appointmentSeriesEntity(startDate = LocalDate.now())
      val appointment = appointmentSeries.appointments().first()

      whenever(appointmentRepository.findAllByPrisonCodeAndStartDate(moorlandPrisonCode, LocalDate.now())).thenReturn(appointmentSeries.appointments())
      whenever(prisonerSearch.findByPrisonerNumbers(appointment.prisonerNumbers())).thenReturn(listOf(activeOutPrisoner))
      whenever(prisonApi.getMovementsForPrisonersFromPrison(moorlandPrisonCode, setOf(activeOutPrisoner.prisonerNumber))).thenReturn(listOf(expiredMovement))

      service.manageAppointmentAttendees(moorlandPrisonCode, 0)

      verify(service).removePrisonerFromFutureAppointments(
        eq(moorlandPrisonCode),
        eq(prisonerReleasedToday.prisonerNumber),
        any<LocalDateTime>(),
        eq(PRISONER_STATUS_PERMANENT_TRANSFER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID),
        eq("MANAGE_APPOINTMENT_SERVICE"),
      )
    }

    @Test
    fun `do not remove future appointments using permanent transfer reason for released prisoners `() {
      val appointmentSeries = appointmentSeriesEntity(startDate = LocalDate.now())
      val appointment = appointmentSeries.appointments().first()

      whenever(appointmentRepository.findAllByPrisonCodeAndStartDate(moorlandPrisonCode, LocalDate.now())).thenReturn(appointmentSeries.appointments())
      whenever(prisonerSearch.findByPrisonerNumbers(appointment.prisonerNumbers())).thenReturn(listOf(prisonerReleasedToday))
      // Add an expired movement prior to prisoner being released
      whenever(prisonApi.getMovementsForPrisonersFromPrison(moorlandPrisonCode, setOf(activeInDifferentPrison.prisonerNumber))).thenReturn(listOf(expiredMovement))

      service.manageAppointmentAttendees(moorlandPrisonCode, 0)

      // Verify the released prisoner is removed from all future appointments using released removal reason
      verify(service).removePrisonerFromFutureAppointments(
        eq(moorlandPrisonCode),
        eq(prisonerReleasedToday.prisonerNumber),
        any<LocalDateTime>(),
        eq(PRISONER_STATUS_RELEASED_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID),
        eq("MANAGE_APPOINTMENT_SERVICE"),
      )

      // Verify the released prisoner is not also removed from all future appointments using permanent transfer removal reason
      verify(service, never()).removePrisonerFromFutureAppointments(
        eq(moorlandPrisonCode),
        eq(prisonerReleasedToday.prisonerNumber),
        any<LocalDateTime>(),
        eq(PRISONER_STATUS_PERMANENT_TRANSFER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID),
        eq("MANAGE_APPOINTMENT_SERVICE"),
      )
    }
  }
}
