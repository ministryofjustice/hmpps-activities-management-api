package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Job
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.movement
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.rolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.JobEventMessage
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.JobsSqsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.ManageAppointmentAttendeesJobEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.PRISONER_STATUS_PERMANENT_TRANSFER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.PRISONER_STATUS_RELEASED_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment.AppointmentAttendeeService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService
import java.time.LocalDate
import java.time.LocalDateTime

class ManageAppointmentAttendeesServiceTest {

  private val rolloutPrisonService: RolloutPrisonService = mock { on { getRolloutPrisons() } doReturn listOf(rolloutPrison(MOORLAND_PRISON_CODE)) }
  private val prisonerSearchApiClient: PrisonerSearchApiClient = mock()
  private val appointmentAttendeeService: AppointmentAttendeeService = mock()
  private val appointmentRepository: AppointmentRepository = mock()
  private val prisonApiClient: PrisonApiClient = mock()
  private val jobsSqsService: JobsSqsService = mock()
  private val jobService: JobService = mock()

  val service = ManageAppointmentAttendeesService(
    rolloutPrisonService,
    prisonerSearchApiClient,
    appointmentAttendeeService,
    appointmentRepository,
    prisonApiClient,
    jobsSqsService,
    jobService,
  )

  private val prisonNumber = "A1234BC"

  private val activeInPrisoner = activeInMoorlandPrisoner.copy(prisonerNumber = prisonNumber)

  private val activeOutPrisoner = activeOutMoorlandPrisoner.copy(prisonerNumber = prisonNumber)

  private val prisonerReleasedToday = permanentlyReleasedPrisonerToday.copy(prisonerNumber = prisonNumber)

  private val activeInDifferentPrison = activeInPentonvillePrisoner.copy(prisonerNumber = prisonNumber)

  private val expiredMovement = movement(prisonerNumber = prisonNumber, fromPrisonCode = MOORLAND_PRISON_CODE, movementDate = TimeSource.yesterday())

  private val nonExpiredMovement = movement(prisonerNumber = prisonNumber, fromPrisonCode = MOORLAND_PRISON_CODE, movementDate = TimeSource.today())

  @BeforeEach
  fun setup() {
    whenever(rolloutPrisonService.getByPrisonCode(MOORLAND_PRISON_CODE)).thenReturn(rolloutPrison(prisonCode = MOORLAND_PRISON_CODE))
  }

  @Test
  fun `days after now cannot be more than 60 days`() {
    assertThatThrownBy {
      service.manageAttendees(61)
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Supplied days after now must be at least one day and less than 61 days")
  }

  @Test
  fun `days after now cannot be negative`() {
    assertThatThrownBy {
      service.manageAttendees(-1)
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Supplied days after now must be at least one day and less than 61 days")
  }

  @Test
  fun `does not remove future appointments for prisoners that have not been released`() {
    val appointmentSeries = appointmentSeriesEntity(startDate = LocalDate.now())
    val appointment = appointmentSeries.appointments().first()

    whenever(appointmentRepository.findAllByPrisonCodeAndStartDate(MOORLAND_PRISON_CODE, LocalDate.now())).thenReturn(appointmentSeries.appointments())
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(appointment.prisonerNumbers())).thenReturn(listOf(activeInPrisoner))

    service.manageAttendees(0)

    verifyNoInteractions(appointmentAttendeeService)
  }

  @Test
  fun `remove future appointments for released prisoners`() {
    val appointmentSeries = appointmentSeriesEntity(startDate = LocalDate.now())
    val appointment = appointmentSeries.appointments().first()

    whenever(appointmentRepository.findAllByPrisonCodeAndStartDate(MOORLAND_PRISON_CODE, LocalDate.now())).thenReturn(appointmentSeries.appointments())
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(appointment.prisonerNumbers())).thenReturn(listOf(prisonerReleasedToday))

    service.manageAttendees(0)

    verify(appointmentAttendeeService).removePrisonerFromFutureAppointments(
      eq(MOORLAND_PRISON_CODE),
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

    whenever(appointmentRepository.findAllByPrisonCodeAndStartDate(MOORLAND_PRISON_CODE, LocalDate.now())).thenReturn(appointmentSeries.appointments())
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(appointment.prisonerNumbers())).thenReturn(listOf(activeInDifferentPrison))
    whenever(prisonApiClient.getMovementsForPrisonersFromPrison(MOORLAND_PRISON_CODE, setOf(activeInDifferentPrison.prisonerNumber))).thenReturn(emptyList())

    service.manageAttendees(0)

    verifyNoInteractions(appointmentAttendeeService)
  }

  @Test
  fun `does not remove future appointments for prisoners that transferred to a different prison less than expired days ago`() {
    val appointmentSeries = appointmentSeriesEntity(startDate = LocalDate.now())
    val appointment = appointmentSeries.appointments().first()

    whenever(appointmentRepository.findAllByPrisonCodeAndStartDate(MOORLAND_PRISON_CODE, LocalDate.now())).thenReturn(appointmentSeries.appointments())
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(appointment.prisonerNumbers())).thenReturn(listOf(activeInDifferentPrison))
    whenever(prisonApiClient.getMovementsForPrisonersFromPrison(MOORLAND_PRISON_CODE, setOf(activeInDifferentPrison.prisonerNumber))).thenReturn(listOf(nonExpiredMovement))

    service.manageAttendees(0)

    verifyNoInteractions(appointmentAttendeeService)
  }

  @Test
  fun `remove future appointments for prisoners that transferred to a different prison more than expired days ago`() {
    val appointmentSeries = appointmentSeriesEntity(startDate = LocalDate.now())
    val appointment = appointmentSeries.appointments().first()

    whenever(appointmentRepository.findAllByPrisonCodeAndStartDate(MOORLAND_PRISON_CODE, LocalDate.now())).thenReturn(appointmentSeries.appointments())
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(appointment.prisonerNumbers())).thenReturn(listOf(activeInDifferentPrison))
    whenever(prisonApiClient.getMovementsForPrisonersFromPrison(MOORLAND_PRISON_CODE, setOf(activeInDifferentPrison.prisonerNumber))).thenReturn(listOf(expiredMovement))

    service.manageAttendees(0)

    verify(appointmentAttendeeService).removePrisonerFromFutureAppointments(
      eq(MOORLAND_PRISON_CODE),
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

    whenever(appointmentRepository.findAllByPrisonCodeAndStartDate(MOORLAND_PRISON_CODE, LocalDate.now())).thenReturn(appointmentSeries.appointments())
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(appointment.prisonerNumbers())).thenReturn(listOf(activeOutPrisoner))
    whenever(prisonApiClient.getMovementsForPrisonersFromPrison(MOORLAND_PRISON_CODE, setOf(activeOutPrisoner.prisonerNumber))).thenReturn(listOf(expiredMovement))

    service.manageAttendees(0)

    verify(appointmentAttendeeService).removePrisonerFromFutureAppointments(
      eq(MOORLAND_PRISON_CODE),
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

    whenever(appointmentRepository.findAllByPrisonCodeAndStartDate(MOORLAND_PRISON_CODE, LocalDate.now())).thenReturn(appointmentSeries.appointments())
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(appointment.prisonerNumbers())).thenReturn(listOf(prisonerReleasedToday))
    // Add an expired movement prior to prisoner being released
    whenever(prisonApiClient.getMovementsForPrisonersFromPrison(MOORLAND_PRISON_CODE, setOf(activeInDifferentPrison.prisonerNumber))).thenReturn(listOf(expiredMovement))

    service.manageAttendees(0)

    // Verify the released prisoner is removed from all future appointments using released removal reason
    verify(appointmentAttendeeService).removePrisonerFromFutureAppointments(
      eq(MOORLAND_PRISON_CODE),
      eq(prisonerReleasedToday.prisonerNumber),
      any<LocalDateTime>(),
      eq(PRISONER_STATUS_RELEASED_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID),
      eq("MANAGE_APPOINTMENT_SERVICE"),
    )

    verifyNoMoreInteractions(appointmentAttendeeService)
  }

  @Test
  fun `should send events to queue for each prison`() {
    val prisons = listOf(rolloutPrison(MOORLAND_PRISON_CODE), rolloutPrison(PENTONVILLE_PRISON_CODE))

    whenever(rolloutPrisonService.getRolloutPrisons()).doReturn(prisons)

    service.sendEvents(Job(123, JobType.MANAGE_APPOINTMENT_ATTENDEES), 12)

    verify(jobService).initialiseCounts(123, 2)

    prisons.forEach {
      verify(jobsSqsService).sendJobEvent(JobEventMessage(123, JobType.MANAGE_APPOINTMENT_ATTENDEES, ManageAppointmentAttendeesJobEvent(it.prisonCode, 12)))
    }

    verifyNoMoreInteractions(jobsSqsService)
  }

  @Test
  fun `handleEvent - days after now cannot be more than 60 days`() {
    assertThatThrownBy {
      service.handleEvent(123, MOORLAND_PRISON_CODE, 61)
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Supplied days after now must be at least one day and less than 61 days")
  }

  @Test
  fun `handleEvent - days after now cannot be negative`() {
    assertThatThrownBy {
      service.handleEvent(123, MOORLAND_PRISON_CODE, -1)
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Supplied days after now must be at least one day and less than 61 days")
  }

  @Test
  fun `handleEvent - does not remove future appointments for prisoners that have not been released`() {
    val appointmentSeries = appointmentSeriesEntity(startDate = LocalDate.now())
    val appointment = appointmentSeries.appointments().first()

    whenever(appointmentRepository.findAllByPrisonCodeAndStartDate(MOORLAND_PRISON_CODE, LocalDate.now())).thenReturn(appointmentSeries.appointments())
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(appointment.prisonerNumbers())).thenReturn(listOf(activeInPrisoner))

    service.handleEvent(123, MOORLAND_PRISON_CODE, 12)

    verifyNoInteractions(appointmentAttendeeService)
  }

  @Test
  fun `handleEvents - remove future appointments for released prisoners`() {
    val appointmentSeries = appointmentSeriesEntity(startDate = LocalDate.now())
    val appointment = appointmentSeries.appointments().first()

    whenever(appointmentRepository.findAllByPrisonCodeAndStartDate(MOORLAND_PRISON_CODE, LocalDate.now())).thenReturn(appointmentSeries.appointments())
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(appointment.prisonerNumbers())).thenReturn(listOf(prisonerReleasedToday))

    service.handleEvent(123, MOORLAND_PRISON_CODE, 12)

    verify(appointmentAttendeeService).removePrisonerFromFutureAppointments(
      eq(MOORLAND_PRISON_CODE),
      eq(prisonerReleasedToday.prisonerNumber),
      any<LocalDateTime>(),
      eq(PRISONER_STATUS_RELEASED_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID),
      eq("MANAGE_APPOINTMENT_SERVICE"),
    )
  }

  @Test
  fun `handleEvents - does not remove future appointments for prisoners that transferred to a different prison an unknown amount of time ago`() {
    val appointmentSeries = appointmentSeriesEntity(startDate = LocalDate.now())
    val appointment = appointmentSeries.appointments().first()

    whenever(appointmentRepository.findAllByPrisonCodeAndStartDate(MOORLAND_PRISON_CODE, LocalDate.now())).thenReturn(appointmentSeries.appointments())
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(appointment.prisonerNumbers())).thenReturn(listOf(activeInDifferentPrison))
    whenever(prisonApiClient.getMovementsForPrisonersFromPrison(MOORLAND_PRISON_CODE, setOf(activeInDifferentPrison.prisonerNumber))).thenReturn(emptyList())

    service.handleEvent(123, MOORLAND_PRISON_CODE, 12)

    verifyNoInteractions(appointmentAttendeeService)
  }

  @Test
  fun `handleEvents - does not remove future appointments for prisoners that transferred to a different prison less than expired days ago`() {
    val appointmentSeries = appointmentSeriesEntity(startDate = LocalDate.now())
    val appointment = appointmentSeries.appointments().first()

    whenever(appointmentRepository.findAllByPrisonCodeAndStartDate(MOORLAND_PRISON_CODE, LocalDate.now())).thenReturn(appointmentSeries.appointments())
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(appointment.prisonerNumbers())).thenReturn(listOf(activeInDifferentPrison))
    whenever(prisonApiClient.getMovementsForPrisonersFromPrison(MOORLAND_PRISON_CODE, setOf(activeInDifferentPrison.prisonerNumber))).thenReturn(listOf(nonExpiredMovement))

    service.handleEvent(123, MOORLAND_PRISON_CODE, 12)

    verifyNoInteractions(appointmentAttendeeService)
  }

  @Test
  fun `handleEvents - remove future appointments for prisoners that transferred to a different prison more than expired days ago`() {
    val appointmentSeries = appointmentSeriesEntity(startDate = LocalDate.now())
    val appointment = appointmentSeries.appointments().first()

    whenever(appointmentRepository.findAllByPrisonCodeAndStartDate(MOORLAND_PRISON_CODE, LocalDate.now())).thenReturn(appointmentSeries.appointments())
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(appointment.prisonerNumbers())).thenReturn(listOf(activeInDifferentPrison))
    whenever(prisonApiClient.getMovementsForPrisonersFromPrison(MOORLAND_PRISON_CODE, setOf(activeInDifferentPrison.prisonerNumber))).thenReturn(listOf(expiredMovement))

    service.handleEvent(123, MOORLAND_PRISON_CODE, 12)

    verify(appointmentAttendeeService).removePrisonerFromFutureAppointments(
      eq(MOORLAND_PRISON_CODE),
      eq(prisonerReleasedToday.prisonerNumber),
      any<LocalDateTime>(),
      eq(PRISONER_STATUS_PERMANENT_TRANSFER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID),
      eq("MANAGE_APPOINTMENT_SERVICE"),
    )
  }

  @Test
  fun `handleEvents - remove future appointments for prisoners that have been active out from the prison more than expired days ago`() {
    val appointmentSeries = appointmentSeriesEntity(startDate = LocalDate.now())
    val appointment = appointmentSeries.appointments().first()

    whenever(appointmentRepository.findAllByPrisonCodeAndStartDate(MOORLAND_PRISON_CODE, LocalDate.now())).thenReturn(appointmentSeries.appointments())
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(appointment.prisonerNumbers())).thenReturn(listOf(activeOutPrisoner))
    whenever(prisonApiClient.getMovementsForPrisonersFromPrison(MOORLAND_PRISON_CODE, setOf(activeOutPrisoner.prisonerNumber))).thenReturn(listOf(expiredMovement))

    service.handleEvent(123, MOORLAND_PRISON_CODE, 12)

    verify(appointmentAttendeeService).removePrisonerFromFutureAppointments(
      eq(MOORLAND_PRISON_CODE),
      eq(prisonerReleasedToday.prisonerNumber),
      any<LocalDateTime>(),
      eq(PRISONER_STATUS_PERMANENT_TRANSFER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID),
      eq("MANAGE_APPOINTMENT_SERVICE"),
    )
  }

  @Test
  fun `handleEvents - do not remove future appointments using permanent transfer reason for released prisoners `() {
    val appointmentSeries = appointmentSeriesEntity(startDate = LocalDate.now())
    val appointment = appointmentSeries.appointments().first()

    whenever(appointmentRepository.findAllByPrisonCodeAndStartDate(MOORLAND_PRISON_CODE, LocalDate.now())).thenReturn(appointmentSeries.appointments())
    whenever(prisonerSearchApiClient.findByPrisonerNumbers(appointment.prisonerNumbers())).thenReturn(listOf(prisonerReleasedToday))
    // Add an expired movement prior to prisoner being released
    whenever(prisonApiClient.getMovementsForPrisonersFromPrison(MOORLAND_PRISON_CODE, setOf(activeInDifferentPrison.prisonerNumber))).thenReturn(listOf(expiredMovement))

    service.handleEvent(123, MOORLAND_PRISON_CODE, 12)

    // Verify the released prisoner is removed from all future appointments using released removal reason
    verify(appointmentAttendeeService).removePrisonerFromFutureAppointments(
      eq(MOORLAND_PRISON_CODE),
      eq(prisonerReleasedToday.prisonerNumber),
      any<LocalDateTime>(),
      eq(PRISONER_STATUS_RELEASED_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID),
      eq("MANAGE_APPOINTMENT_SERVICE"),
    )

    verifyNoMoreInteractions(appointmentAttendeeService)
  }
}
