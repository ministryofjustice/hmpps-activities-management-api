package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import com.microsoft.applicationinsights.TelemetryClient
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.AdditionalAnswers
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentAttendee
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentFrequency
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentSeries
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentSeriesSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.RISLEY_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCancelledReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.eventOrganiser
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.eventTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentSeriesRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.AppointmentCancellationReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AuditService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.TransactionHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment.AppointmentCreateDomainService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional

class AppointmentCreateDomainServiceTest {
  private val appointmentSeriesRepository = mock<AppointmentSeriesRepository>()
  private val appointmentRepository = mock<AppointmentRepository>()
  private val appointmentCancellationReasonRepository = mock<AppointmentCancellationReasonRepository>()
  private val outboundEventsService: OutboundEventsService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val auditService: AuditService = mock()

  private val appointmentCaptor = argumentCaptor<Appointment>()

  private val appointmentCancelledReason = appointmentCancelledReason()

  val service = AppointmentCreateDomainService(
    appointmentSeriesRepository,
    appointmentRepository,
    appointmentCancellationReasonRepository,
    TransactionHandler(),
    outboundEventsService,
    telemetryClient,
    auditService,
  )

  @BeforeEach
  fun setUp() {
    whenever(appointmentRepository.saveAndFlush(appointmentCaptor.capture())).thenAnswer(AdditionalAnswers.returnsFirstArg<Appointment>())
    whenever(appointmentCancellationReasonRepository.findById(appointmentCancelledReason.appointmentCancellationReasonId)).thenReturn(
      Optional.of(appointmentCancelledReason),
    )
  }

  @Test
  fun `uses appointment series details when creating child appointment`() {
    val appointmentSeries = appointmentSeriesWithNoAppointment()

    service.createAppointments(appointmentSeries, emptyMap())

    appointmentCaptor.firstValue isEqualTo Appointment(
      appointmentSeries = appointmentSeries,
      sequenceNumber = 1,
      prisonCode = appointmentSeries.prisonCode,
      categoryCode = appointmentSeries.categoryCode,
      customName = appointmentSeries.customName,
      appointmentTier = appointmentSeries.appointmentTier,
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
    ).apply {
      appointmentOrganiser = appointmentSeries.appointmentOrganiser
    }
  }

  @Test
  fun `create appointments based on appointment series schedule`() {
    val appointmentSeries = appointmentSeriesWithNoAppointment().apply {
      schedule = AppointmentSeriesSchedule(appointmentSeries = this, frequency = AppointmentFrequency.DAILY, numberOfAppointments = 3)
    }

    service.createAppointments(appointmentSeries, emptyMap())

    appointmentCaptor.firstValue.startDate isEqualTo LocalDate.now()
    appointmentCaptor.secondValue.startDate isEqualTo LocalDate.now().plusDays(1)
    appointmentCaptor.thirdValue.startDate isEqualTo LocalDate.now().plusDays(2)
  }

  @Test
  fun `only create missing appointments`() {
    val appointmentSeries = appointmentSeriesWithOneAppointment().apply {
      schedule = AppointmentSeriesSchedule(appointmentSeries = this, frequency = AppointmentFrequency.DAILY, numberOfAppointments = 3)
    }

    service.createAppointments(appointmentSeries, emptyMap())

    appointmentCaptor.firstValue.startDate isEqualTo LocalDate.now().plusDays(1)
    appointmentCaptor.secondValue.startDate isEqualTo LocalDate.now().plusDays(2)
  }

  @Test
  fun `cannot create non migrated appointments in a cancelled state`() {
    assertThrows<IllegalArgumentException>(
      "Only migrated appointments can be created in a cancelled state",
    ) {
      service.createAppointments(
        appointmentSeriesEntity(isMigrated = false),
        emptyMap(),
        createFirstAppointmentOnly = false,
        isCancelled = true,
      )
    }
  }

  @Test
  fun `create migrated appointment in a non cancelled state`() {
    val appointmentSeries = appointmentSeriesWithNoAppointment()

    service.createAppointments(appointmentSeries, emptyMap(), createFirstAppointmentOnly = false, isCancelled = false)

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

    service.createAppointments(appointmentSeries, emptyMap(), createFirstAppointmentOnly = false, isCancelled = true)

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

    service.createAppointments(appointmentSeries, emptyMap(), createFirstAppointmentOnly = false, isCancelled = true)

    with(appointmentCaptor.firstValue) {
      cancelledTime isEqualTo appointmentSeries.updatedTime
      cancellationReason isEqualTo appointmentCancelledReason
      cancelledBy isEqualTo appointmentSeries.updatedBy
      isDeleted isBool false
    }
  }

  @Test
  fun `add attendee on creation without populating added time and added by`() {
    val appointmentSeries = appointmentSeriesWithNoAppointment()

    service.createAppointments(appointmentSeries, mapOf("A1234BC" to 1L))

    appointmentCaptor.firstValue.attendees().single() isEqualTo AppointmentAttendee(
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
    }
  }

  @Test
  fun `creating an appointment should raise sync events for each appointment instance`() {
    val appointmentSeries = appointmentSeriesWithNoAppointment().apply {
      schedule = AppointmentSeriesSchedule(appointmentSeries = this, frequency = AppointmentFrequency.DAILY, numberOfAppointments = 2)
    }

    service.createAppointments(appointmentSeries, mapOf("A1234BC" to 1L, "A2345BC" to 2L, "A3456BC" to 3L))

    verify(outboundEventsService, times(6)).send(eq(OutboundEvent.APPOINTMENT_INSTANCE_CREATED), any(), eq(null))
    verifyNoMoreInteractions(outboundEventsService)
  }

  private fun appointmentSeriesWithNoAppointment(isMigrated: Boolean = false) =
    // Specify all non default values
    AppointmentSeries(
      appointmentSeriesId = 1,
      appointmentType = AppointmentType.GROUP,
      prisonCode = RISLEY_PRISON_CODE,
      categoryCode = "GYMW",
      customName = "Custom name",
      appointmentTier = eventTier(),
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
    ).apply {
      appointmentOrganiser = eventOrganiser()
    }

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
        ).also {
          it.appointmentOrganiser = this.appointmentOrganiser
        },
      )
    }
}
