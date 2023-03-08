package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerScheduledActivity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.RolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentsDataSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PrisonerScheduledActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import java.time.LocalDate
import java.time.LocalTime

class ScheduledEventServiceTest {
  private val prisonApiClient: PrisonApiClient = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiClient = mock()
  private val rolloutPrisonRepository: RolloutPrisonRepository = mock()
  private val prisonerScheduledActivityRepository: PrisonerScheduledActivityRepository = mock()
  private val prisonRegimeService: PrisonRegimeService = mock()
  private val appointmentInstanceService: AppointmentInstanceService = mock()

  private val service = ScheduledEventService(
    prisonApiClient,
    prisonerSearchApiClient,
    rolloutPrisonRepository,
    prisonerScheduledActivityRepository,
    prisonRegimeService,
    appointmentInstanceService,

  )

  @BeforeEach
  fun reset() {
    reset(
      prisonApiClient,
      rolloutPrisonRepository,
      prisonerScheduledActivityRepository,
      prisonRegimeService,
    )
  }

  // --- Private utility functions used to set up the mocked responses ---

  private fun setupRolledOutPrisonMock(prisonCode: String, active: Boolean, rolloutDate: LocalDate, dataSource: AppointmentsDataSource) {
    whenever(rolloutPrisonRepository.findByCode(prisonCode))
      .thenReturn(
        RolloutPrison(
          rolloutPrisonId = 10,
          code = prisonCode,
          description = "Description",
          active = active,
          rolloutDate = rolloutDate,
          appointmentsDataSource = dataSource,
        ),
      )
  }

  private fun setupMultiplePrisonerApiMocks(
    prisonCode: String,
    prisonerNumbers: Set<String>,
    date: LocalDate,
    timeSlot: TimeSlot?,
  ) {
    val scheduledAppointmentsMono = listOf(PrisonApiPrisonerScheduleFixture.appointmentInstance())
    val scheduledVisitsMono = Mono.just(listOf(PrisonApiPrisonerScheduleFixture.visitInstance()))
    val courtEventsMono = Mono.just(listOf(PrisonApiPrisonerScheduleFixture.courtInstance()))

    // Produces mocked responses of type Mono.just(List<PrisonerSchedule>) for each of appointments, visits and court hearings
    whenever(appointmentInstanceService.getPrisonerSchedules(eq(prisonCode), eq(prisonerNumbers), any(), eq(date), eq(timeSlot)))
      .thenReturn(scheduledAppointmentsMono)

    whenever(prisonApiClient.getScheduledVisitsForPrisonerNumbers(prisonCode, prisonerNumbers, date, timeSlot))
      .thenReturn(scheduledVisitsMono)

    whenever(prisonApiClient.getScheduledCourtEventsForPrisonerNumbers(prisonCode, prisonerNumbers, date, timeSlot))
      .thenReturn(courtEventsMono)
  }

  private fun setupSinglePrisonerApiMocks(
    prisonCode: String,
    prisonerNumber: String,
    dateRange: LocalDateRange,
    appointmentsException: Boolean = false,
    courtHearingsException: Boolean = false,
    visitsException: Boolean = false,
    withAppointmentSubType: String? = null,
    withPrisonerDetailsException: Boolean = false,
    prisonOverride: String = prisonCode,
  ) {
    val scheduledAppointments = if (!withAppointmentSubType.isNullOrEmpty()) {
      listOf(PrisonApiScheduledEventFixture.appointmentInstance(eventSubType = withAppointmentSubType))
    } else {
      listOf(PrisonApiScheduledEventFixture.appointmentInstance())
    }

    val scheduledVisitsMono = if (visitsException) {
      Mono.error(Exception("Error"))
    } else {
      Mono.just(listOf(PrisonApiScheduledEventFixture.visitInstance()))
    }

    val courtHearingsMono = if (courtHearingsException) {
      Mono.error(Exception("Error"))
    } else {
      Mono.just(PrisonApiCourtHearingsFixture.instance())
    }

    val prisonerDetailsMono = if (withPrisonerDetailsException) {
      Mono.error(Exception("Error"))
    } else {
      Mono.just(listOf(PrisonerSearchPrisonerFixture.instance(prisonId = prisonOverride)))
    }

    whenever(prisonerSearchApiClient.findByPrisonerNumbers(listOf(prisonerNumber)))
      .thenReturn(prisonerDetailsMono)

    if (appointmentsException) {
      whenever(appointmentInstanceService.getScheduledEvents(any(), eq(900001), eq(dateRange)))
        .thenThrow(RuntimeException("Error"))
    } else {
      whenever(appointmentInstanceService.getScheduledEvents(any(), eq(900001), eq(dateRange)))
        .thenReturn(scheduledAppointments)
    }

    whenever(prisonApiClient.getScheduledVisits(900001, dateRange))
      .thenReturn(scheduledVisitsMono)

    whenever(prisonApiClient.getScheduledCourtHearings(900001, dateRange))
      .thenReturn(courtHearingsMono)
  }

  private fun activityFromDbInstance(
    scheduledInstanceId: Long = 1,
    allocationId: Long = 1,
    prisonCode: String = "MDI",
    sessionDate: LocalDate = LocalDate.of(2022, 12, 14),
    startTime: LocalTime? = LocalTime.of(10, 0),
    endTime: LocalTime? = LocalTime.of(11, 30),
    prisonerNumber: String = "G4793VF",
    bookingId: Int = 900001,
    internalLocationId: Int? = 1,
    internalLocationCode: String? = "MDI-EDU_ROOM1",
    internalLocationDescription: String? = "Education room 1",
    scheduleDescription: String? = "HB1 AM",
    activityId: Int = 1,
    activityCategory: String = "Education",
    activitySummary: String? = "English level 1",
    cancelled: Boolean = false,
    suspended: Boolean = false,
  ) = PrisonerScheduledActivity(
    scheduledInstanceId = scheduledInstanceId,
    allocationId = allocationId,
    prisonCode = prisonCode,
    sessionDate = sessionDate,
    startTime = startTime,
    endTime = endTime,
    prisonerNumber = prisonerNumber,
    bookingId = bookingId,
    internalLocationId = internalLocationId,
    internalLocationCode = internalLocationCode,
    internalLocationDescription = internalLocationDescription,
    scheduleDescription = scheduleDescription,
    activityId = activityId,
    activityCategory = activityCategory,
    activitySummary = activitySummary,
    cancelled = cancelled,
    suspended = suspended,
  )

  @Test
  fun `get scheduled events - multiple prisoners - rolled out prison - success`() {
    val prisonCode = "MDI"
    val prisonerNumbers = setOf("G4793VF", "G1234GK")
    val date = LocalDate.of(2022, 12, 14)
    val timeSlot: TimeSlot = TimeSlot.AM

    // Set up the Prison API mocks for multiple prisoners - each return Mono.just(List<PrisonerSchedule>)
    setupMultiplePrisonerApiMocks(prisonCode, prisonerNumbers, date, timeSlot)
    setupRolledOutPrisonMock(prisonCode, active = true, rolloutDate = LocalDate.of(2022, 12, 22), AppointmentsDataSource.PRISON_API)

    // Returns List<PrisonerScheduledEvent> from the DB views
    whenever(prisonerScheduledActivityRepository.getScheduledActivitiesForPrisonerListAndDate(prisonCode, prisonerNumbers, date))
      .thenReturn(listOf(activityFromDbInstance()))

    val result = service.getScheduledEventsByPrisonAndPrisonersAndDateRange(prisonCode, prisonerNumbers, date, timeSlot)

    // Should not be called - this is a rolled-out prison
    verify(prisonApiClient, never()).getScheduledActivities(any(), any())

    with(result!!) {
      assertThat(this.prisonerNumbers).contains("G4793VF")
      assertThat(appointments).isNotNull
      assertThat(appointments).hasSize(1)

      appointments!!.map {
        assertThat(it.prisonCode).isEqualTo("MDI")
        assertThat(it.location).isEqualTo("INTERVIEW ROOM")
        assertThat(it.eventType).isEqualTo("APPOINTMENT")
        assertThat(it.event).isEqualTo("GOVE")
        assertThat(it.eventDesc).isEqualTo("Governor")
        assertThat(it.details).isEqualTo("Dont be late")
        assertThat(it.prisonerNumber).isIn(prisonerNumbers)
        assertThat(it.date).isEqualTo(LocalDate.of(2022, 12, 14))
        assertThat(it.startTime).isEqualTo(LocalTime.of(17, 0, 0))
        assertThat(it.endTime).isEqualTo(LocalTime.of(18, 0, 0))
        assertThat(it.priority).isEqualTo(4)
      }

      assertThat(visits).isNotNull
      assertThat(visits).hasSize(1)

      visits!!.map {
        assertThat(it.prisonerNumber).isIn(prisonerNumbers)
        assertThat(it.prisonCode).isEqualTo("MDI")
        assertThat(it.bookingId).isEqualTo(900001)
        assertThat(it.location).isEqualTo("INTERVIEW ROOM")
        assertThat(it.eventType).isEqualTo("VISIT")
        assertThat(it.event).isEqualTo("VISIT")
        assertThat(it.eventDesc).isEqualTo("Visit")
        assertThat(it.date).isEqualTo(LocalDate.of(2022, 12, 14))
        assertThat(it.startTime).isEqualTo(LocalTime.of(14, 30, 0))
        assertThat(it.priority).isEqualTo(2)
      }

      assertThat(courtHearings).isNotNull
      assertThat(courtHearings).hasSize(1)

      courtHearings!!.map {
        assertThat(it.prisonCode).isEqualTo("MDI")
        assertThat(it.prisonerNumber).isIn(prisonerNumbers)
        assertThat(it.bookingId).isEqualTo(900001)
        assertThat(it.eventId).isEqualTo(1L)
        assertThat(it.eventType).isEqualTo("COURT_HEARING")
        assertThat(it.event).isEqualTo("CRT")
        assertThat(it.eventDesc).isEqualTo("Court Appearance")
        assertThat(it.date).isEqualTo(LocalDate.of(2022, 12, 14))
        assertThat(it.startTime).isEqualTo(LocalTime.of(10, 0, 0))
        assertThat(it.priority).isEqualTo(1)
      }

      assertThat(activities).isNotNull
      assertThat(activities).hasSize(1)
      activities!!.map {
        assertThat(it.prisonerNumber).isIn(prisonerNumbers)
        assertThat(it.prisonCode).isEqualTo("MDI")
        assertThat(it.bookingId).isEqualTo(900001)
        assertThat(it.eventId).isEqualTo(1L)
        assertThat(it.eventStatus).isNull()
        assertThat(it.eventClass).isEqualTo("INT_MOV")
        assertThat(it.eventType).isEqualTo("PRISON_ACT")
        assertThat(it.eventTypeDesc).isEqualTo("Education")
        assertThat(it.locationId).isEqualTo(1)
        assertThat(it.location).isEqualTo("Education room 1")
        assertThat(it.event).isEqualTo("English level 1")
        assertThat(it.eventDesc).isEqualTo("HB1 AM")
        assertThat(it.details).isEqualTo("English level 1: HB1 AM")
        assertThat(it.date).isEqualTo(LocalDate.of(2022, 12, 14))
        assertThat(it.startTime).isEqualTo(LocalTime.of(10, 0, 0))
        assertThat(it.endTime).isEqualTo(LocalTime.of(11, 30, 0))
        assertThat(it.priority).isEqualTo(EventType.ACTIVITY.defaultPriority)
      }
    }
  }

  @Test
  fun `get scheduled events - single prisoner - rolled out prison - success`() {
    val prisonCode = "MDI"
    val prisonerNumber = "G4793VF"
    val startDate = LocalDate.of(2022, 12, 14)
    val endDate = LocalDate.of(2022, 12, 15)
    val dateRange = LocalDateRange(startDate, endDate)
    val timeSlot: TimeSlot = TimeSlot.AM

    setupSinglePrisonerApiMocks(prisonCode, prisonerNumber, dateRange)
    setupRolledOutPrisonMock(prisonCode, active = true, rolloutDate = LocalDate.of(2022, 12, 22), AppointmentsDataSource.PRISON_API)

    // Uses the default event priorities for all types of event
    whenever(prisonRegimeService.getEventPrioritiesForPrison(prisonCode))
      .thenReturn(EventType.values().associateWith { listOf(Priority(it.defaultPriority)) })

    // Activities from the database view
    whenever(prisonerScheduledActivityRepository.getScheduledActivitiesForPrisonerAndDateRange(prisonCode, prisonerNumber, startDate, endDate))
      .thenReturn(listOf(activityFromDbInstance()))

    val result = service.getScheduledEventsByPrisonAndPrisonerAndDateRange(
      prisonCode,
      prisonerNumber,
      LocalDateRange(startDate, endDate),
      timeSlot,
    )

    // Should not be called - this is a rolled-out prison
    verify(prisonApiClient, never()).getScheduledActivities(any(), any())

    with(result!!) {
      assertThat(prisonerNumbers).containsExactlyInAnyOrder(prisonerNumber)
      assertThat(appointments).isNotNull
      assertThat(appointments).hasSize(1)

      appointments!!.map {
        assertThat(it.prisonCode).isEqualTo("MDI")
        assertThat(it.prisonerNumber).isIn(prisonerNumbers)
        assertThat(it.bookingId).isEqualTo(900001)
        assertThat(it.eventId).isEqualTo(1)
        assertThat(it.eventStatus).isEqualTo("SCH")
        assertThat(it.eventClass).isEqualTo("INT_MOV")
        assertThat(it.eventType).isEqualTo("APPOINTMENT")
        assertThat(it.event).isEqualTo("GOVE")
        assertThat(it.locationId).isEqualTo(1)
        assertThat(it.location).isEqualTo("GOVERNORS OFFICE")
        assertThat(it.eventDesc).isEqualTo("Governor")
        assertThat(it.details).isEqualTo("Dont be late")
        assertThat(it.date).isEqualTo(LocalDate.of(2022, 12, 14))
        assertThat(it.startTime).isEqualTo(LocalTime.of(17, 0, 0))
        assertThat(it.endTime).isEqualTo(LocalTime.of(18, 0, 0))
        assertThat(it.priority).isEqualTo(4)
      }

      assertThat(visits).isNotNull
      assertThat(visits).hasSize(1)

      visits!!.map {
        assertThat(it.prisonCode).isEqualTo("MDI")
        assertThat(it.prisonerNumber).isIn(prisonerNumbers)
        assertThat(it.bookingId).isEqualTo(900001)
        assertThat(it.locationId).isEqualTo(1)
        assertThat(it.location).isEqualTo("VISITS ROOM")
        assertThat(it.eventClass).isEqualTo("INT_MOV")
        assertThat(it.eventId).isEqualTo(1)
        assertThat(it.eventType).isEqualTo("VISIT")
        assertThat(it.eventTypeDesc).isEqualTo("Visit")
        assertThat(it.eventStatus).isEqualTo("SCH")
        assertThat(it.event).isEqualTo("VISIT")
        assertThat(it.eventDesc).isEqualTo("Visits")
        assertThat(it.details).isEqualTo("Social Contact")
        assertThat(it.date).isEqualTo(LocalDate.of(2022, 12, 14))
        assertThat(it.startTime).isEqualTo(LocalTime.of(17, 0, 0))
        assertThat(it.endTime).isEqualTo(LocalTime.of(18, 0, 0))
        assertThat(it.priority).isEqualTo(2)
      }

      assertThat(courtHearings).isNotNull
      assertThat(courtHearings).hasSize(1)

      courtHearings!!.map {
        assertThat(it.prisonerNumber).isIn(prisonerNumbers)
        assertThat(it.prisonCode).isEqualTo("MDI")
        assertThat(it.bookingId).isEqualTo(900001)
        assertThat(it.eventId).isEqualTo(1L)
        assertThat(it.eventType).isEqualTo("COURT_HEARING")
        assertThat(it.location).isEqualTo("Aberdeen Sheriff's Court (abdshf)")
        assertThat(it.date).isEqualTo(LocalDate.of(2022, 12, 14))
        assertThat(it.startTime).isEqualTo(LocalTime.of(10, 0, 0))
        assertThat(it.priority).isEqualTo(1)
      }

      assertThat(activities).isNotNull
      assertThat(activities).hasSize(1)
      activities!!.map {
        assertThat(it.prisonCode).isEqualTo("MDI")
        assertThat(it.prisonerNumber).isIn(prisonerNumbers)
        assertThat(it.bookingId).isEqualTo(900001)
        assertThat(it.eventId).isEqualTo(1L)
        assertThat(it.eventClass).isEqualTo("INT_MOV")
        assertThat(it.eventType).isEqualTo("PRISON_ACT")
        assertThat(it.event).isEqualTo("English level 1")
        assertThat(it.eventDesc).isEqualTo("HB1 AM")
        assertThat(it.date).isEqualTo(LocalDate.of(2022, 12, 14))
        assertThat(it.startTime).isEqualTo(LocalTime.of(10, 0, 0))
        assertThat(it.endTime).isEqualTo(LocalTime.of(11, 30, 0))
        assertThat(it.priority).isEqualTo(5)
      }
    }
  }

  @Test
  fun `get scheduled events - single prisoner - rolled out prison - success with default activity priority`() {
    val prisonCode = "MDI"
    val prisonerNumber = "A1111AA"
    val startDate = LocalDate.of(2022, 12, 14)
    val endDate = LocalDate.of(2022, 12, 15)
    val dateRange = LocalDateRange(startDate, endDate)
    val timeSlot: TimeSlot = TimeSlot.AM

    setupSinglePrisonerApiMocks(prisonCode, prisonerNumber, dateRange)
    setupRolledOutPrisonMock(prisonCode, active = true, rolloutDate = LocalDate.of(2022, 12, 22), AppointmentsDataSource.PRISON_API)

    // Mocked activities from the database view
    whenever(prisonerScheduledActivityRepository.getScheduledActivitiesForPrisonerAndDateRange(prisonCode, prisonerNumber, startDate, endDate))
      .thenReturn(listOf(activityFromDbInstance()))

    // Set specific priorities for ACTIVITIES for this test
    whenever(prisonRegimeService.getEventPrioritiesForPrison(prisonCode)).thenReturn(
      mapOf(
        EventType.ACTIVITY to listOf(
          Priority(1, EventCategory.EDUCATION),
          Priority(2, EventCategory.SERVICES),
          Priority(3, EventCategory.GYM_SPORTS_FITNESS),
          Priority(4, EventCategory.INDUCTION),
          Priority(5, EventCategory.INDUSTRIES),
          Priority(6, EventCategory.INTERVENTIONS),
          Priority(7, EventCategory.LEISURE_SOCIAL),
          Priority(8), // Will default to this because event category doesn't match
        ),
        EventType.APPOINTMENT to listOf(Priority(21)),
        EventType.VISIT to listOf(Priority(22)),
        EventType.ADJUDICATION_HEARING to listOf(Priority(23)),
        EventType.COURT_HEARING to listOf(Priority(24)),
      ),
    )

    val result = service.getScheduledEventsByPrisonAndPrisonerAndDateRange(
      prisonCode,
      prisonerNumber,
      LocalDateRange(startDate, endDate),
      timeSlot,
    )

    // Should not be called - this is a rolled-out prison
    verify(prisonApiClient, never()).getScheduledActivities(any(), any())

    with(result!!) {
      assertThat(appointments!![0].priority).isEqualTo(21)
      assertThat(activities!![0].priority).isEqualTo(8)
      assertThat(visits!![0].priority).isEqualTo(22)
      assertThat(courtHearings!![0].priority).isEqualTo(24)
    }
  }

  @Test
  fun `get scheduled event - single prisoner - rolled out prison - success with default activity priority`() {
    val prisonCode = "MDI"
    val prisonerNumber = "A1111AA"
    val startDate = LocalDate.of(2022, 12, 14)
    val endDate = LocalDate.of(2022, 12, 15)
    val dateRange = LocalDateRange(startDate, endDate)
    val timeSlot: TimeSlot = TimeSlot.AM

    setupSinglePrisonerApiMocks(prisonCode, prisonerNumber, dateRange)
    setupRolledOutPrisonMock(prisonCode, active = true, rolloutDate = LocalDate.of(2022, 12, 22), AppointmentsDataSource.PRISON_API)

    // Mocked activities from the database view
    whenever(prisonerScheduledActivityRepository.getScheduledActivitiesForPrisonerAndDateRange(prisonCode, prisonerNumber, startDate, endDate))
      .thenReturn(listOf(activityFromDbInstance()))

    // Specific priorities for ACTIVITY types for the test
    whenever(prisonRegimeService.getEventPrioritiesForPrison(prisonCode)).thenReturn(
      mapOf(
        EventType.ACTIVITY to listOf(
          Priority(1, EventCategory.EDUCATION),
          Priority(2, EventCategory.SERVICES),
          Priority(3, EventCategory.GYM_SPORTS_FITNESS),
          Priority(4, EventCategory.INDUCTION),
          Priority(5, EventCategory.INDUSTRIES),
          Priority(6, EventCategory.INTERVENTIONS),
          Priority(7, EventCategory.LEISURE_SOCIAL),
        ),
        EventType.APPOINTMENT to listOf(Priority(9, EventCategory.EDUCATION)),
        EventType.VISIT to listOf(Priority(10, EventCategory.EDUCATION)),
        EventType.ADJUDICATION_HEARING to listOf(Priority(11, EventCategory.EDUCATION)),
        EventType.COURT_HEARING to listOf(Priority(12, EventCategory.EDUCATION)),
      ),
    )

    val result = service.getScheduledEventsByPrisonAndPrisonerAndDateRange(
      prisonCode,
      prisonerNumber,
      LocalDateRange(startDate, endDate),
      timeSlot,
    )

    // Should not be called - this is a rolled-out prison
    verify(prisonApiClient, never()).getScheduledActivities(any(), any())

    with(result!!) {
      assertThat(appointments!![0].priority).isEqualTo(4) // EventType.APPOINTMENT default
      assertThat(activities!![0].priority).isEqualTo(5) // EventType.ACTIVITY default
      assertThat(visits!![0].priority).isEqualTo(2) // EventType.VISIT default
      assertThat(courtHearings!![0].priority).isEqualTo(1) // EventType.COURT_HEARING default
    }
  }

  @Test
  fun `get scheduled events - single prisoner - rolled out prison - success with category priority`() {
    val prisonCode = "MDI"
    val prisonerNumber = "A1111AA"
    val startDate = LocalDate.of(2022, 12, 14)
    val endDate = LocalDate.of(2022, 12, 15)
    val dateRange = LocalDateRange(startDate, endDate)
    val timeSlot: TimeSlot = TimeSlot.AM

    setupSinglePrisonerApiMocks(prisonCode, prisonerNumber, dateRange)
    setupRolledOutPrisonMock(prisonCode, active = true, rolloutDate = LocalDate.of(2022, 12, 22), AppointmentsDataSource.PRISON_API)

    whenever(prisonRegimeService.getEventPrioritiesForPrison(prisonCode)).thenReturn(
      mapOf(
        EventType.ACTIVITY to listOf(
          Priority(1, EventCategory.EDUCATION),
          Priority(2, EventCategory.SERVICES),
          Priority(3, EventCategory.GYM_SPORTS_FITNESS),
          Priority(4, EventCategory.INDUCTION),
          Priority(5, EventCategory.INDUSTRIES),
          Priority(6, EventCategory.INTERVENTIONS),
          Priority(7, EventCategory.LEISURE_SOCIAL),
        ),
        EventType.APPOINTMENT to listOf(Priority(9, EventCategory.EDUCATION)),
        EventType.VISIT to listOf(Priority(10, EventCategory.EDUCATION)),
        EventType.ADJUDICATION_HEARING to listOf(Priority(11, EventCategory.EDUCATION)),
        EventType.COURT_HEARING to listOf(Priority(12, EventCategory.EDUCATION)),
      ),
    )

    // Mocked activities from the database view - with category set to LEISURE
    whenever(prisonerScheduledActivityRepository.getScheduledActivitiesForPrisonerAndDateRange(prisonCode, prisonerNumber, startDate, endDate))
      .thenReturn(listOf(activityFromDbInstance(activityCategory = "LEI")))

    val result = service.getScheduledEventsByPrisonAndPrisonerAndDateRange(
      prisonCode,
      prisonerNumber,
      LocalDateRange(startDate, endDate),
      timeSlot,
    )

    // Should not be called - this is a rolled-out prison
    verify(prisonApiClient, never()).getScheduledActivities(any(), any())

    with(result!!) {
      assertThat(appointments!![0].priority).isEqualTo(4) // EventType.APPOINTMENT default
      assertThat(activities!![0].priority).isEqualTo(7) // EventType.LEISURE_SOCIAL
      assertThat(visits!![0].priority).isEqualTo(2) // EventType.VISIT default
      assertThat(courtHearings!![0].priority).isEqualTo(1) // EventType.COURT_HEARING default
    }
  }

  @Test
  fun `get scheduled events - single prisoner - not rolled out prison - success`() {
    val prisonCode = "MDI"
    val prisonerNumber = "A1111AA"
    val startDate = LocalDate.of(2022, 12, 14)
    val endDate = LocalDate.of(2022, 12, 15)
    val dateRange = LocalDateRange(startDate, endDate)
    val timeSlot: TimeSlot = TimeSlot.AM

    setupSinglePrisonerApiMocks(prisonCode, prisonerNumber, dateRange)
    setupRolledOutPrisonMock(prisonCode, active = false, rolloutDate = LocalDate.of(2023, 12, 22), AppointmentsDataSource.PRISON_API)

    // Mock response for activities from Prison API for this prisoner
    val scheduledActivitiesMono = Mono.just(listOf(PrisonApiScheduledEventFixture.activityInstance()))
    whenever(prisonApiClient.getScheduledActivities(900001, dateRange)).thenReturn(scheduledActivitiesMono)

    whenever(prisonRegimeService.getEventPrioritiesForPrison(prisonCode))
      .thenReturn(EventType.values().associateWith { listOf(Priority(it.defaultPriority)) })

    val result = service.getScheduledEventsByPrisonAndPrisonerAndDateRange(
      prisonCode,
      prisonerNumber,
      LocalDateRange(startDate, endDate),
      timeSlot,
    )

    // Should not be called - NOT a rolled out prison
    verify(prisonerScheduledActivityRepository, never())
      .getScheduledActivitiesForPrisonerAndDateRange(prisonCode, prisonerNumber, startDate, endDate)

    // Should be called - this is NOT a rolled out prison
    verify(prisonApiClient).getScheduledActivities(any(), any())

    with(result!!) {
      assertThat(this.prisonerNumbers).containsExactly("A1111AA")
      assertThat(appointments).isNotNull
      assertThat(appointments).hasSize(1)
      assertThat(appointments!![0].prisonerNumber).isEqualTo("A1111AA")
      assertThat(appointments!![0].priority).isEqualTo(4)
      assertThat(activities).isNotNull
      assertThat(activities).hasSize(1)
      assertThat(activities!![0].prisonerNumber).isEqualTo("A1111AA")
      assertThat(activities!![0].priority).isEqualTo(5)
      assertThat(visits).isNotNull
      assertThat(visits).hasSize(1)
      assertThat(visits!![0].prisonerNumber).isEqualTo("A1111AA")
      assertThat(visits!![0].priority).isEqualTo(2)
      assertThat(courtHearings).isNotNull
      assertThat(courtHearings).hasSize(1)
      assertThat(courtHearings!![0].prisonerNumber).isEqualTo("A1111AA")
      assertThat(courtHearings!![0].priority).isEqualTo(1)
    }
  }

  @Test
  fun `get scheduled events - single prisoner - not rolled out - success with sub-category priority`() {
    val prisonCode = "MDI"
    val prisonerNumber = "A1111AA"
    val startDate = LocalDate.of(2022, 12, 14)
    val endDate = LocalDate.of(2022, 12, 15)
    val dateRange = LocalDateRange(startDate, endDate)
    val timeSlot: TimeSlot = TimeSlot.AM

    // Setup prison API mocks for court, appointments, visits - with a specific APPOINTMENT type
    setupSinglePrisonerApiMocks(prisonCode, prisonerNumber, dateRange, withAppointmentSubType = "LACO")

    // Set this prison to be NOT rolled out onto new activities
    setupRolledOutPrisonMock(prisonCode, active = false, rolloutDate = LocalDate.of(2023, 12, 22), AppointmentsDataSource.PRISON_API)

    // Mock response for activities from Prison API for this prisoner
    val scheduledActivitiesMono = Mono.just(listOf(PrisonApiScheduledEventFixture.activityInstance()))
    whenever(prisonApiClient.getScheduledActivities(900001, dateRange))
      .thenReturn(scheduledActivitiesMono)

    whenever(prisonRegimeService.getEventPrioritiesForPrison(prisonCode)).thenReturn(
      mapOf(
        EventType.ACTIVITY to listOf(
          Priority(99),
        ),
        EventType.APPOINTMENT to listOf(
          Priority(1, EventCategory.EDUCATION),
          Priority(2, EventCategory.SERVICES),
          Priority(3, EventCategory.GYM_SPORTS_FITNESS),
          Priority(4, EventCategory.INDUCTION),
          Priority(5, EventCategory.INDUSTRIES),
          Priority(6, EventCategory.INTERVENTIONS),
          Priority(7, EventCategory.LEISURE_SOCIAL),
        ),
        EventType.VISIT to listOf(Priority(10, EventCategory.EDUCATION)),
        EventType.ADJUDICATION_HEARING to listOf(Priority(11, EventCategory.EDUCATION)),
        EventType.COURT_HEARING to listOf(Priority(12, EventCategory.EDUCATION)),
      ),
    )

    val result = service.getScheduledEventsByPrisonAndPrisonerAndDateRange(
      prisonCode,
      prisonerNumber,
      LocalDateRange(startDate, endDate),
      timeSlot,
    )

    // Should not be called - prison is NOT rolled out
    verify(prisonerScheduledActivityRepository, never())
      .getScheduledActivitiesForPrisonerAndDateRange(prisonCode, prisonerNumber, startDate, endDate)

    // Should be called - NOT a rolled out prison
    verify(prisonApiClient).getScheduledActivities(any(), any())

    with(result!!) {
      assertThat(appointments!![0].priority).isEqualTo(5) // EventType.APPOINTMENT EventCategory.INDUSTRIES "LACO"
      assertThat(activities!![0].priority).isEqualTo(99) // explicit default
      assertThat(visits!![0].priority).isEqualTo(2) // default
      assertThat(courtHearings!![0].priority).isEqualTo(1) // default
    }
  }

  @Test
  fun `get scheduled events - single prisoner - not rolled out - prisoner details error`() {
    val prisonCode = "MDI"
    val prisonerNumber = "A1111AA"
    val startDate = LocalDate.of(2022, 12, 14)
    val endDate = LocalDate.of(2022, 12, 15)
    val dateRange = LocalDateRange(startDate, endDate)

    // Setup prison API mocks for court, appointments, visits - with an error on prisoner details
    setupSinglePrisonerApiMocks(prisonCode, prisonerNumber, dateRange, withPrisonerDetailsException = true)

    setupRolledOutPrisonMock(prisonCode, active = false, rolloutDate = LocalDate.of(2023, 12, 22), AppointmentsDataSource.PRISON_API)

    assertThatThrownBy {
      service.getScheduledEventsByPrisonAndPrisonerAndDateRange(
        prisonCode,
        prisonerNumber,
        dateRange,
      )
    }
      .isInstanceOf(Exception::class.java)
      .hasMessage("java.lang.Exception: Error")
  }

  @Test
  fun `get scheduled events - single prisoner - not rolled out - prison code and prisoner number do not match`() {
    val prisonCode = "MDI"
    val prisonerNumber = "A1111AA"
    val startDate = LocalDate.of(2022, 12, 14)
    val endDate = LocalDate.of(2022, 12, 15)
    val dateRange = LocalDateRange(startDate, endDate)

    // Setup prison API mocks for court, appointments, visits - with the prisoner at a different prison
    setupSinglePrisonerApiMocks(prisonCode, prisonerNumber, dateRange, prisonOverride = "PVI")

    setupRolledOutPrisonMock(prisonCode, active = false, rolloutDate = LocalDate.of(2023, 12, 22), AppointmentsDataSource.PRISON_API)

    assertThatThrownBy {
      service.getScheduledEventsByPrisonAndPrisonerAndDateRange(
        prisonCode,
        prisonerNumber,
        dateRange,
      )
    }
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Prisoner 'A1111AA' not found in prison 'MDI'")
  }

  @Test
  fun `get scheduled events - single prisoner - not rolled out - appointments exception`() {
    val prisonCode = "MDI"
    val prisonerNumber = "A1111AA"
    val startDate = LocalDate.of(2022, 12, 14)
    val endDate = LocalDate.of(2022, 12, 15)
    val dateRange = LocalDateRange(startDate, endDate)

    // Setup prison API mocks for court, appointments, visits - with and appointments error
    setupSinglePrisonerApiMocks(prisonCode, prisonerNumber, dateRange, appointmentsException = true)

    setupRolledOutPrisonMock(prisonCode, active = false, rolloutDate = LocalDate.of(2023, 12, 22), AppointmentsDataSource.PRISON_API)

    assertThatThrownBy {
      service.getScheduledEventsByPrisonAndPrisonerAndDateRange(
        prisonCode,
        prisonerNumber,
        dateRange,
      )
    }
      .isInstanceOf(Exception::class.java)
      .hasMessage("Error")
  }

  @Test
  fun `get scheduled events - single prisoner - not rolled out - activities exception`() {
    val prisonCode = "MDI"
    val prisonerNumber = "A1111AA"
    val startDate = LocalDate.of(2022, 12, 14)
    val endDate = LocalDate.of(2022, 12, 15)
    val dateRange = LocalDateRange(startDate, endDate)

    setupSinglePrisonerApiMocks(prisonCode, prisonerNumber, dateRange)
    setupRolledOutPrisonMock(prisonCode, active = false, rolloutDate = LocalDate.of(2023, 12, 22), AppointmentsDataSource.PRISON_API)

    // Simulate activities error from prison API
    whenever(prisonApiClient.getScheduledActivities(900001, dateRange))
      .thenReturn(Mono.error(Exception("Error")))

    assertThatThrownBy {
      service.getScheduledEventsByPrisonAndPrisonerAndDateRange(
        prisonCode,
        prisonerNumber,
        dateRange,
      )
    }
      .isInstanceOf(Exception::class.java)
      .hasMessage("java.lang.Exception: Error")
  }

  @Test
  fun `get scheduled events - single prisoner - not rolled out - visits exception`() {
    val prisonCode = "MDI"
    val prisonerNumber = "A1111AA"
    val startDate = LocalDate.of(2022, 12, 14)
    val endDate = LocalDate.of(2022, 12, 15)
    val dateRange = LocalDateRange(startDate, endDate)

    setupSinglePrisonerApiMocks(prisonCode, prisonerNumber, dateRange, visitsException = true)
    setupRolledOutPrisonMock(prisonCode, active = false, rolloutDate = LocalDate.of(2023, 12, 22), AppointmentsDataSource.PRISON_API)

    assertThatThrownBy {
      service.getScheduledEventsByPrisonAndPrisonerAndDateRange(
        prisonCode,
        prisonerNumber,
        dateRange,
      )
    }
      .isInstanceOf(Exception::class.java)
      .hasMessage("java.lang.Exception: Error")
  }

  @Test
  fun `get scheduled events - single prisoner - not rolled out - court hearings exception`() {
    val prisonCode = "MDI"
    val prisonerNumber = "A1111AA"
    val startDate = LocalDate.of(2022, 12, 14)
    val endDate = LocalDate.of(2022, 12, 15)
    val dateRange = LocalDateRange(startDate, endDate)

    setupSinglePrisonerApiMocks(prisonCode, prisonerNumber, dateRange, courtHearingsException = true)
    setupRolledOutPrisonMock(prisonCode, active = false, rolloutDate = LocalDate.of(2023, 12, 22), AppointmentsDataSource.PRISON_API)

    assertThatThrownBy {
      service.getScheduledEventsByPrisonAndPrisonerAndDateRange(
        prisonCode,
        prisonerNumber,
        dateRange,
      )
    }
      .isInstanceOf(Exception::class.java)
      .hasMessage("java.lang.Exception: Error")
  }
}
