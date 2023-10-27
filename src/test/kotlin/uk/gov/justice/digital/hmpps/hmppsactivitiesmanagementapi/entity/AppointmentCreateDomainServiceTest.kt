package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.AdditionalAnswers
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCancelledReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentHostPrisonStaff
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentTier2
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.risleyPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentAttendeeRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentCancellationReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentSeriesRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.TransactionHandler
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional

class AppointmentCreateDomainServiceTest {
  private val appointmentSeriesRepository = mock<AppointmentSeriesRepository>()
  private val appointmentRepository = mock<AppointmentRepository>()
  private val appointmentAttendeeRepository = mock<AppointmentAttendeeRepository>()
  private val appointmentCancellationReasonRepository = mock<AppointmentCancellationReasonRepository>()

  private val appointmentCancelledReason = appointmentCancelledReason()

  val service = AppointmentCreateDomainService(
    appointmentSeriesRepository,
    appointmentRepository,
    appointmentAttendeeRepository,
    appointmentCancellationReasonRepository,
    TransactionHandler(),
  )

  @BeforeEach
  fun setUp() {
    whenever(appointmentRepository.saveAndFlush(any())).thenAnswer(AdditionalAnswers.returnsFirstArg<Appointment>())
    whenever(appointmentAttendeeRepository.saveAndFlush(any())).thenAnswer(AdditionalAnswers.returnsFirstArg<AppointmentAttendee>())
    whenever(appointmentCancellationReasonRepository.findById(appointmentCancelledReason.appointmentCancellationReasonId)).thenReturn(
      Optional.of(appointmentCancelledReason),
    )
  }

  @Test
  fun `uses appointment series details when creating child appointment`() {
    val appointmentSeries = appointmentSeriesWithNoAppointment()

    whenever(appointmentSeriesRepository.findById(appointmentSeries.appointmentSeriesId)).thenReturn(Optional.of(appointmentSeries))

    service.createAppointments(appointmentSeries, emptyMap())

    verify(appointmentRepository).saveAndFlush(
      Appointment(
        appointmentSeries = appointmentSeries,
        sequenceNumber = 1,
        prisonCode = appointmentSeries.prisonCode,
        categoryCode = appointmentSeries.categoryCode,
        customName = appointmentSeries.customName,
        appointmentTier = appointmentSeries.appointmentTier,
        appointmentHost = appointmentSeries.appointmentHost,
        internalLocationId = appointmentSeries.internalLocationId,
        customLocation = appointmentSeries.customLocation,
        inCell = appointmentSeries.inCell,
        onWing = appointmentSeries.onWing,
        offWing = appointmentSeries.offWing,
        startDate = appointmentSeries.startDate,
        startTime = appointmentSeries.startTime,
        endTime = appointmentSeries.endTime,
        unlockNotes = appointmentSeries.unlockNotes,
        extraInformation = appointmentSeries.extraInformation,
        createdTime = appointmentSeries.createdTime,
        createdBy = appointmentSeries.createdBy,
        updatedTime = appointmentSeries.updatedTime,
        updatedBy = appointmentSeries.updatedBy,
      ),
    )
  }

  @Test
  fun `create appointments based on appointment series schedule`() {
    val appointmentSeries = appointmentSeriesWithNoAppointment().apply {
      schedule = AppointmentSeriesSchedule(appointmentSeries = this, frequency = AppointmentFrequency.DAILY, numberOfAppointments = 3)
    }

    whenever(appointmentSeriesRepository.findById(appointmentSeries.appointmentSeriesId)).thenReturn(Optional.of(appointmentSeries))

    service.createAppointments(appointmentSeries, emptyMap())

    val appointmentCaptor = argumentCaptor<Appointment>()
    verify(appointmentRepository, times(3)).saveAndFlush(appointmentCaptor.capture())
    appointmentCaptor.firstValue.startDate isEqualTo LocalDate.now()
    appointmentCaptor.secondValue.startDate isEqualTo LocalDate.now().plusDays(1)
    appointmentCaptor.thirdValue.startDate isEqualTo LocalDate.now().plusDays(2)
  }

  @Test
  fun `only create missing appointments`() {
    val appointmentSeries = appointmentSeriesWithOneAppointment().apply {
      schedule = AppointmentSeriesSchedule(appointmentSeries = this, frequency = AppointmentFrequency.DAILY, numberOfAppointments = 3)
    }

    whenever(appointmentSeriesRepository.findById(appointmentSeries.appointmentSeriesId)).thenReturn(Optional.of(appointmentSeries))

    service.createAppointments(appointmentSeries, emptyMap())

    val appointmentCaptor = argumentCaptor<Appointment>()
    verify(appointmentRepository, times(2)).saveAndFlush(appointmentCaptor.capture())
    appointmentCaptor.firstValue.startDate isEqualTo LocalDate.now().plusDays(1)
    appointmentCaptor.secondValue.startDate isEqualTo LocalDate.now().plusDays(2)
  }

  @Test
  fun `cannot create non migrated appointments in a cancelled state`() {
    assertThrows<IllegalArgumentException>(
      "Only migrated appointments can be created in a cancelled state",
    ) {
      service.createAppointments(appointmentSeriesEntity(isMigrated = false), emptyMap(), true)
      appointmentSeriesEntity(
        appointmentType = AppointmentType.INDIVIDUAL,
        prisonerNumberToBookingIdMap = mapOf("A1234BC" to 456, "B2345CD" to 789),
      )
    }
  }

  @Test
  fun `create migrated appointment in a non cancelled state`() {
    val appointmentSeries = appointmentSeriesWithNoAppointment()

    whenever(appointmentSeriesRepository.findById(appointmentSeries.appointmentSeriesId)).thenReturn(Optional.of(appointmentSeries))

    service.createAppointments(appointmentSeries, emptyMap(), false)

    val appointmentCaptor = argumentCaptor<Appointment>()
    verify(appointmentRepository).saveAndFlush(appointmentCaptor.capture())
    with(appointmentCaptor.firstValue) {
      cancelledTime isEqualTo null
      cancellationReason isEqualTo null
      cancelledBy isEqualTo null
      isDeleted isBool false
    }
  }

  @Test
  fun `create migrated appointment in a cancelled state using created time and created by`() {
    val appointmentSeries = appointmentSeriesWithNoAppointment(isMigrated = true).apply {
      updatedTime = null
      updatedBy = null
    }

    whenever(appointmentSeriesRepository.findById(appointmentSeries.appointmentSeriesId)).thenReturn(Optional.of(appointmentSeries))

    service.createAppointments(appointmentSeries, emptyMap(), true)

    val appointmentCaptor = argumentCaptor<Appointment>()
    verify(appointmentRepository).saveAndFlush(appointmentCaptor.capture())
    with(appointmentCaptor.firstValue) {
      cancelledTime isEqualTo appointmentSeries.createdTime
      cancellationReason isEqualTo appointmentCancelledReason
      cancelledBy isEqualTo appointmentSeries.createdBy
      isDeleted isBool false
    }
  }

  @Test
  fun `create migrated appointment in a cancelled state using updated time and updated by`() {
    val appointmentSeries = appointmentSeriesWithNoAppointment(isMigrated = true)

    whenever(appointmentSeriesRepository.findById(appointmentSeries.appointmentSeriesId)).thenReturn(Optional.of(appointmentSeries))

    service.createAppointments(appointmentSeries, emptyMap(), true)

    val appointmentCaptor = argumentCaptor<Appointment>()
    verify(appointmentRepository).saveAndFlush(appointmentCaptor.capture())
    with(appointmentCaptor.firstValue) {
      cancelledTime isEqualTo appointmentSeries.updatedTime
      cancellationReason isEqualTo appointmentCancelledReason
      cancelledBy isEqualTo appointmentSeries.updatedBy
      isDeleted isBool false
    }
  }

  @Test
  fun `add attendee on creation without populating added time and added by`() {
    val appointmentSeries = appointmentSeriesWithOneAppointment()

    whenever(appointmentSeriesRepository.findById(appointmentSeries.appointmentSeriesId)).thenReturn(Optional.of(appointmentSeries))

    service.createAppointments(appointmentSeries, mapOf("A1234BC" to 1L))

    verify(appointmentAttendeeRepository).saveAndFlush(
      AppointmentAttendee(
        appointment = appointmentSeries.appointments().single(),
        prisonerNumber = "A1234BC",
        bookingId = 1,
        addedTime = null,
        addedBy = null,
        attended = null,
        attendanceRecordedTime = null,
        attendanceRecordedBy = null,
      ).apply {
        removedTime = null
        removalReason = null
        removedBy = null
      },
    )
  }

  @Test
  fun `only create missing attendees`() {
    val appointmentSeries = appointmentSeriesWithOneAppointment().apply {
      appointments().single().apply {
        addAttendee(
          AppointmentAttendee(
            appointment = this,
            prisonerNumber = "A1234BC",
            bookingId = 1,
          ),
        )
      }
    }

    whenever(appointmentSeriesRepository.findById(appointmentSeries.appointmentSeriesId)).thenReturn(Optional.of(appointmentSeries))

    service.createAppointments(appointmentSeries, mapOf("A1234BC" to 1L, "B2345CD" to 2L, "C3456DE" to 3L))

    verify(appointmentAttendeeRepository).saveAndFlush(
      AppointmentAttendee(
        appointment = appointmentSeries.appointments().single(),
        prisonerNumber = "B2345CD",
        bookingId = 2,
      ),
    )
    verify(appointmentAttendeeRepository).saveAndFlush(
      AppointmentAttendee(
        appointment = appointmentSeries.appointments().single(),
        prisonerNumber = "C3456DE",
        bookingId = 3,
      ),
    )
    verifyNoMoreInteractions(appointmentAttendeeRepository)
  }

  private fun appointmentSeriesWithNoAppointment(isMigrated: Boolean = false) =
    // Specify all non default values
    AppointmentSeries(
      appointmentSeriesId = 1,
      appointmentType = AppointmentType.INDIVIDUAL,
      prisonCode = risleyPrisonCode,
      categoryCode = "GYMW",
      customName = "Custom name",
      appointmentTier = appointmentTier2(),
      appointmentHost = appointmentHostPrisonStaff(),
      internalLocationId = 123,
      // Not currently used by the UI. For future features
      customLocation = "Custom location",
      // Not currently used by the UI. For future features
      inCell = true,
      // Not currently used by the UI. For future features
      onWing = true,
      // Not currently used by the UI. For future features
      offWing = false,
      startDate = LocalDate.now(),
      startTime = LocalTime.of(8, 45),
      endTime = LocalTime.of(10, 15),
      // Not currently used by the UI. For future features
      unlockNotes = "Wing officer notes",
      extraInformation = "Extra information for prisoner",
      createdTime = LocalDateTime.now().minusDays(1),
      createdBy = "CREATED_BY_USER",
      updatedTime = LocalDateTime.now().minusHours(1),
      updatedBy = "UPDATED_BY_USER",
      isMigrated = isMigrated,
    )

  private fun appointmentSeriesWithOneAppointment() =
    appointmentSeriesWithNoAppointment().apply {
      addAppointment(
        Appointment(
          appointmentSeries = this,
          sequenceNumber = 1,
          prisonCode = this.prisonCode,
          categoryCode = this.categoryCode,
          customName = this.customName,
          appointmentTier = this.appointmentTier,
          appointmentHost = this.appointmentHost,
          internalLocationId = this.internalLocationId,
          customLocation = this.customLocation,
          inCell = this.inCell,
          onWing = this.onWing,
          offWing = this.offWing,
          startDate = this.startDate,
          startTime = this.startTime,
          endTime = this.endTime,
          unlockNotes = this.unlockNotes,
          extraInformation = this.extraInformation,
          createdTime = this.createdTime,
          createdBy = this.createdBy,
          updatedTime = this.updatedTime,
          updatedBy = this.updatedBy,
        ),
      )
    }
}
