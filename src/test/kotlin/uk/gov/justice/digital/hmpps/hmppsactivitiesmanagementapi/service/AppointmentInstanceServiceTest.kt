package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.PrisonerSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.ScheduledEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalTimeRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.RolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentInstanceEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.locations
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.prisonerSchedules
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.prisoners
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.scheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentInstanceRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Optional

class AppointmentInstanceServiceTest {

  private val rolloutPrison: RolloutPrison = mock()
  private val prisonApiClient: PrisonApiClient = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiClient = mock()
  private val referenceCodeService: ReferenceCodeService = mock()
  private val prisonRegimeService: PrisonRegimeService = mock()
  private val locationService: LocationService = mock()
  private val appointmentInstanceRepository: AppointmentInstanceRepository = mock()
  private val appointmentInstanceService = AppointmentInstanceService(
    prisonApiClient,
    prisonerSearchApiClient,
    referenceCodeService,
    locationService,
    appointmentInstanceRepository,
    prisonRegimeService,
  )

  @Nested
  @DisplayName("getAppointmentInstanceById")
  inner class GetAppointmentInstanceById {
    @Test
    fun `returns an appointment instance for known appointment instance id`() {
      val entity = appointmentInstanceEntity()
      whenever(appointmentInstanceRepository.findById(1)).thenReturn(Optional.of(entity))
      assertThat(appointmentInstanceService.getAppointmentInstanceById(1)).isEqualTo(entity.toModel())
    }

    @Test
    fun `throws entity not found exception for unknown appointment instance id`() {
      assertThatThrownBy { appointmentInstanceService.getAppointmentInstanceById(0) }
        .isInstanceOf(EntityNotFoundException::class.java)
        .hasMessage("Appointment Instance 0 not found")
    }
  }

  @Nested
  @DisplayName("getScheduledEvents")
  inner class GetScheduledEvents {

    @Test
    fun `fetches data from the Prison API when isAppointmentsEnabled is false`() {
      val bookingId = 900001L
      val startDate = LocalDate.of(2022, 12, 14)
      val endDate = LocalDate.of(2022, 12, 15)
      val dateRange = LocalDateRange(startDate, endDate)
      val expectedScheduledEvents = scheduledEvents()

      whenever(rolloutPrison.isAppointmentsEnabled()).thenReturn(false)
      whenever(prisonApiClient.getScheduledAppointments(bookingId, dateRange)).thenReturn(Mono.just(expectedScheduledEvents))

      val actualScheduledEvents = appointmentInstanceService.getScheduledEvents(rolloutPrison, bookingId, dateRange)

      assertThat(actualScheduledEvents).isEqualTo(expectedScheduledEvents)
      verify(appointmentInstanceRepository, never()).findByBookingIdAndDateRange(any(), any(), any())
      verify(prisonApiClient).getScheduledAppointments(bookingId, dateRange)
    }

    @Test
    fun `fetches data from the Appointment Instance Repository when when isAppointmentsEnabled is true`() {
      val bookingId = 456L
      val startDate = LocalDate.of(2022, 12, 14)
      val startTime = LocalTime.of(9, 0)
      val endDate = LocalDate.of(2022, 12, 15)
      val endTime = LocalTime.of(10, 30)
      val dateRange = LocalDateRange(startDate, endDate)
      val expectedScheduledEvents = listOf(
        ScheduledEvent(
          bookingId = bookingId,
          startTime = LocalDateTime.of(startDate, startTime).format(DateTimeFormatter.ISO_DATE_TIME),
          endTime = LocalDateTime.of(startDate, endTime).format(DateTimeFormatter.ISO_DATE_TIME),
          eventType = "APP",
          eventTypeDesc = "Appointment",
          eventClass = "INT_MOV",
          eventId = 2,
          eventStatus = "SCH",
          eventDate = startDate,
          eventSource = "APP",
          eventSubType = "TEST",
          eventSubTypeDesc = "Test Category",
          agencyId = "TPR",
        ),
      )

      whenever(rolloutPrison.isAppointmentsEnabled()).thenReturn(true)
      whenever(appointmentInstanceRepository.findByBookingIdAndDateRange(bookingId, startDate, endDate))
        .thenReturn(listOf(appointmentInstanceEntity(appointmentDate = startDate)))
      whenever(referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY)).thenReturn(mapOf("TEST" to appointmentCategoryReferenceCode()))

      val actualScheduledEvents = appointmentInstanceService.getScheduledEvents(rolloutPrison, bookingId, dateRange)

      assertThat(actualScheduledEvents).isEqualTo(expectedScheduledEvents)
      verify(prisonApiClient, never()).getScheduledAppointments(any(), any())
      verify(appointmentInstanceRepository).findByBookingIdAndDateRange(bookingId, startDate, endDate)
    }
  }

  @Nested
  @DisplayName("getPrisonerSchedules")
  inner class GetPrisonerSchedules {

    @Test
    fun `fetches data from the Prison API when isAppointmentsEnabled is false`() {
      val prisonCode = "PBI"
      val prisonerNumbers = setOf("P123", "P456")
      val startDate = LocalDate.of(2022, 12, 14)
      val timeSlot = TimeSlot.AM

      val expectedPrisonerSchedules = prisonerSchedules()

      whenever(rolloutPrison.isAppointmentsEnabled()).thenReturn(false)
      whenever(prisonApiClient.getScheduledAppointmentsForPrisonerNumbers(prisonCode, prisonerNumbers, startDate, timeSlot)).thenReturn(Mono.just(expectedPrisonerSchedules))

      val actualPrisonerSchedules = appointmentInstanceService.getPrisonerSchedules(prisonCode, prisonerNumbers, rolloutPrison, startDate, timeSlot)

      assertThat(actualPrisonerSchedules).isEqualTo(expectedPrisonerSchedules)
      verify(appointmentInstanceRepository, never()).findByBookingIdAndDateRange(any(), any(), any())
      verify(prisonApiClient).getScheduledAppointmentsForPrisonerNumbers(prisonCode, prisonerNumbers, startDate, timeSlot)
    }

    @Test
    fun `fetches data from the Appointment Instance Repository when when isAppointmentsEnabled is true`() {
      val prisonCode = "PBI"
      val prisonerNumbers = setOf("P123")
      val startDate = LocalDate.of(2022, 12, 14)
      val earliestTime = LocalTime.of(0, 0)
      val latestTime = LocalTime.of(11, 59)
      val timeSlot = TimeSlot.AM

      val expectedPrisonerSchedules = listOf(
        PrisonerSchedule(
          cellLocation = "A-1-002",
          comment = "Appointment instance level comment",
          event = "TEST",
          eventDescription = "Test Category",
          eventLocation = "User Description",
          eventStatus = "SCH",
          eventType = "APP",
          firstName = "John",
          lastName = "Smith",
          locationId = 123,
          offenderNo = prisonerNumbers.first(),
          startTime = LocalDateTime.of(startDate, LocalTime.of(9, 0)).format(DateTimeFormatter.ISO_DATE_TIME),
          endTime = LocalDateTime.of(startDate, LocalTime.of(10, 30)).format(DateTimeFormatter.ISO_DATE_TIME),
        ),
      )

      whenever(prisonRegimeService.getTimeRangeForPrisonAndTimeSlot(prisonCode, timeSlot)).thenReturn(LocalTimeRange(earliestTime, latestTime))
      whenever(referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY)).thenReturn(mapOf("TEST" to appointmentCategoryReferenceCode()))
      whenever(locationService.getLocationsForAppointmentsMap(prisonCode)).thenReturn(locations())
      whenever(prisonerSearchApiClient.findByPrisonerNumbers(prisonerNumbers.toList())).thenReturn(Mono.just(prisoners(prisonerNumber = prisonerNumbers.first())))
      whenever(rolloutPrison.isAppointmentsEnabled()).thenReturn(true)
      whenever(appointmentInstanceRepository.findByPrisonCodeAndPrisonerNumberAndDateAndTime(eq(prisonCode), eq(prisonerNumbers), eq(startDate), any(), any()))
        .thenReturn(listOf(appointmentInstanceEntity(appointmentDate = startDate, prisonerNumber = "P123", bookingId = 456)))

      val actualPrisonerSchedules = appointmentInstanceService.getPrisonerSchedules(prisonCode, prisonerNumbers, rolloutPrison, startDate, timeSlot)

      assertThat(actualPrisonerSchedules).isEqualTo(expectedPrisonerSchedules)
      verify(prisonApiClient, never()).getScheduledAppointmentsForPrisonerNumbers(any(), any(), any(), any())
      verify(appointmentInstanceRepository).findByPrisonCodeAndPrisonerNumberAndDateAndTime(eq(prisonCode), eq(prisonerNumbers), eq(startDate), any(), any())
    }
  }
}
