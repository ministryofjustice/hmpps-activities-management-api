package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.rangeTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentsDataSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerScheduledActivity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.RolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.adjudicationHearing
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ScheduledEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PrisonerScheduledActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.ADJUDICATION_HEARING_DURATION_TWO_HOURS
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
    val appointments = listOf(PrisonApiPrisonerScheduleFixture.appointmentInstance(date = date))
    val visits = listOf(PrisonApiPrisonerScheduleFixture.visitInstance(date = date))
    val courtEvents = listOf(PrisonApiPrisonerScheduleFixture.courtInstance(date = date))
    val transferEvents = listOf(PrisonApiPrisonerScheduleFixture.transferInstance(date = date))
    val adjudications = prisonerNumbers.map { adjudicationHearing(prisonCode, it) }

    prisonApiClient.stub {
      on {
        runBlocking {
          getScheduledVisitsForPrisonerNumbersAsync(
            prisonCode,
            prisonerNumbers,
            date,
            timeSlot,
          )
        }
      } doReturn visits
      on {
        runBlocking {
          getScheduledCourtEventsForPrisonerNumbersAsync(
            prisonCode,
            prisonerNumbers,
            date,
            timeSlot,
          )
        }
      } doReturn courtEvents
      on { runBlocking { getExternalTransfersOnDateAsync(prisonCode, prisonerNumbers, date) } } doReturn transferEvents
      on { runBlocking { prisonApiClient.getOffenderAdjudications(prisonCode, date.rangeTo(date.plusDays(1)), prisonerNumbers, timeSlot) } } doReturn adjudications
    }

    whenever(
      appointmentInstanceService.getPrisonerSchedules(
        eq(prisonCode),
        eq(prisonerNumbers),
        any(),
        eq(date),
        eq(timeSlot),
      ),
    )
      .thenReturn(appointments)
  }

  private fun setupSinglePrisonerApiMocks(
    prisonCode: String,
    prisonerNumber: String,
    dateRange: LocalDateRange,
    appointmentsException: Boolean = false,
    withAppointmentSubType: String? = null,
    withPrisonerDetailsException: Boolean = false,
    prisonOverride: String = prisonCode,
  ) {
    val scheduledAppointments = if (!withAppointmentSubType.isNullOrEmpty()) {
      listOf(PrisonApiScheduledEventFixture.appointmentInstance(eventSubType = withAppointmentSubType))
    } else {
      listOf(PrisonApiScheduledEventFixture.appointmentInstance())
    }

    val scheduledVisits = listOf(PrisonApiScheduledEventFixture.visitInstance())
    val courtHearings = PrisonApiCourtHearingsFixture.instance()
    val adjudications = listOf(adjudicationHearing(prisonCode, prisonerNumber))

    if (withPrisonerDetailsException) {
      prisonerSearchApiClient.stub {
        on { runBlocking { prisonerSearchApiClient.findByPrisonerNumbersAsync(listOf(prisonerNumber)) } } doThrow RuntimeException("Error")
      }
    } else {
      val prisonerDetails = listOf(PrisonerSearchPrisonerFixture.instance(prisonId = prisonOverride))
      prisonerSearchApiClient.stub {
        on { runBlocking { prisonerSearchApiClient.findByPrisonerNumbersAsync(listOf(prisonerNumber)) } } doReturn prisonerDetails
      }
    }

    if (appointmentsException) {
      whenever(appointmentInstanceService.getScheduledEvents(any(), eq(900001), eq(dateRange)))
        .thenThrow(RuntimeException("Error"))
    } else {
      whenever(appointmentInstanceService.getScheduledEvents(any(), eq(900001), eq(dateRange)))
        .thenReturn(scheduledAppointments)
    }

    val transferEventsToday = listOf(PrisonApiPrisonerScheduleFixture.transferInstance(date = LocalDate.now()))

    prisonApiClient.stub {
      on { runBlocking { prisonApiClient.getScheduledVisitsAsync(900001, dateRange) } } doReturn scheduledVisits
      on { runBlocking { prisonApiClient.getScheduledCourtHearingsAsync(900001, dateRange) } } doReturn courtHearings
      on { runBlocking { prisonApiClient.getExternalTransfersOnDateAsync(prisonCode, setOf(prisonerNumber), LocalDate.now()) } } doReturn transferEventsToday
      on { runBlocking { prisonApiClient.getOffenderAdjudications(prisonCode, dateRange, setOf(prisonerNumber)) } } doReturn adjudications
    }
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
  fun `get scheduled events for today - multiple prisoners - rolled out prison - success`() {
    val prisonCode = "MDI"
    val prisonerNumbers = setOf("G4793VF", "G1234GK")
    val today = LocalDate.now()
    val timeSlot: TimeSlot = TimeSlot.AM

    setupMultiplePrisonerApiMocks(prisonCode, prisonerNumbers, today, timeSlot)
    setupRolledOutPrisonMock(
      prisonCode,
      active = true,
      rolloutDate = LocalDate.of(2022, 12, 22),
      AppointmentsDataSource.PRISON_API,
    )

    whenever(
      prisonerScheduledActivityRepository.getScheduledActivitiesForPrisonerListAndDate(
        prisonCode,
        prisonerNumbers,
        today,
      ),
    )
      .thenReturn(listOf(activityFromDbInstance(sessionDate = today)))

    val scheduledEvents =
      service.getScheduledEventsByPrisonAndPrisonersAndDateRange(prisonCode, prisonerNumbers, today, timeSlot)

    // Should not be called - this is a rolled-out prison
    verifyBlocking(prisonApiClient, never()) { getScheduledActivitiesAsync(any(), any()) }

    with(scheduledEvents!!) {
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
        assertThat(it.date).isEqualTo(today)
        assertThat(it.startTime).isEqualTo(LocalTime.MIDNIGHT)
        assertThat(it.endTime).isEqualTo(LocalTime.NOON)
        assertThat(it.priority).isEqualTo(EventType.APPOINTMENT.defaultPriority)
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
        assertThat(it.date).isEqualTo(today)
        assertThat(it.startTime).isEqualTo(LocalTime.MIDNIGHT)
        assertThat(it.priority).isEqualTo(EventType.VISIT.defaultPriority)
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
        assertThat(it.date).isEqualTo(today)
        assertThat(it.startTime).isEqualTo(LocalTime.MIDNIGHT)
        assertThat(it.priority).isEqualTo(EventType.COURT_HEARING.defaultPriority)
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
        assertThat(it.date).isEqualTo(today)
        assertThat(it.startTime).isEqualTo(LocalTime.of(10, 0, 0))
        assertThat(it.endTime).isEqualTo(LocalTime.of(11, 30, 0))
        assertThat(it.priority).isEqualTo(EventType.ACTIVITY.defaultPriority)
      }

      assertThat(externalTransfers).containsExactly(
        ScheduledEvent(
          prisonCode = "MDI",
          prisonerNumber = "G4793VF",
          eventId = 1,
          bookingId = 900001,
          location = "External",
          locationId = null,
          eventClass = "TRANSFER",
          eventType = "EXTERNAL_TRANSFER",
          eventStatus = "SCH",
          eventTypeDesc = "EXTERNAL_TRANSFER",
          event = "TRANSFER",
          eventDesc = "",
          startTime = LocalTime.MIDNIGHT,
          endTime = LocalTime.NOON,
          details = "",
          date = LocalDate.now(),
          priority = EventType.EXTERNAL_TRANSFER.defaultPriority,
        ),
      )

      assertThat(adjudications).containsExactlyInAnyOrder(
        ScheduledEvent(
          prisonCode = moorlandPrisonCode,
          eventId = -1,
          bookingId = null,
          locationId = -2,
          location = "Adjudication room",
          eventClass = null,
          eventStatus = "SCH",
          eventType = EventType.ADJUDICATION_HEARING.name,
          eventTypeDesc = "Governor's Hearing Adult",
          event = null,
          eventDesc = null,
          details = null,
          prisonerNumber = "G4793VF",
          date = LocalDate.now(),
          startTime = LocalDate.now().atStartOfDay().toLocalTime(),
          endTime = LocalDate.now().atStartOfDay().toLocalTime().plusHours(ADJUDICATION_HEARING_DURATION_TWO_HOURS),
          priority = EventType.ADJUDICATION_HEARING.defaultPriority,
        ),
        ScheduledEvent(
          prisonCode = moorlandPrisonCode,
          eventId = -1,
          bookingId = null,
          locationId = -2,
          location = "Adjudication room",
          eventClass = null,
          eventStatus = "SCH",
          eventType = EventType.ADJUDICATION_HEARING.name,
          eventTypeDesc = "Governor's Hearing Adult",
          event = null,
          eventDesc = null,
          details = null,
          prisonerNumber = "G1234GK",
          date = LocalDate.now(),
          startTime = LocalDate.now().atStartOfDay().toLocalTime(),
          endTime = LocalDate.now().atStartOfDay().toLocalTime().plusHours(ADJUDICATION_HEARING_DURATION_TWO_HOURS),
          priority = EventType.ADJUDICATION_HEARING.defaultPriority,
        ),
      )
    }
  }

  @Test
  fun `get scheduled events for tomorrow excludes transfers - multiple prisoners - rolled out prison - success`() {
    val prisonCode = "MDI"
    val prisonerNumbers = setOf("G4793VF", "G1234GK")
    val tomorrow = LocalDate.now().plusDays(1)
    val timeSlot: TimeSlot = TimeSlot.AM

    setupMultiplePrisonerApiMocks(prisonCode, prisonerNumbers, tomorrow, timeSlot)
    setupRolledOutPrisonMock(
      prisonCode,
      active = true,
      rolloutDate = LocalDate.of(2022, 12, 22),
      AppointmentsDataSource.PRISON_API,
    )

    whenever(
      prisonerScheduledActivityRepository.getScheduledActivitiesForPrisonerListAndDate(
        prisonCode,
        prisonerNumbers,
        tomorrow,
      ),
    )
      .thenReturn(listOf(activityFromDbInstance(sessionDate = tomorrow)))

    val scheduledEvents = service.getScheduledEventsByPrisonAndPrisonersAndDateRange(prisonCode, prisonerNumbers, tomorrow, timeSlot)

    // Should not be called - this is a rolled-out prison
    verifyBlocking(prisonApiClient, never()) { getScheduledActivitiesAsync(any(), any()) }

    with(scheduledEvents!!) {
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
        assertThat(it.date).isEqualTo(tomorrow)
        assertThat(it.startTime).isEqualTo(LocalTime.MIDNIGHT)
        assertThat(it.endTime).isEqualTo(LocalTime.NOON)
        assertThat(it.priority).isEqualTo(EventType.APPOINTMENT.defaultPriority)
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
        assertThat(it.date).isEqualTo(tomorrow)
        assertThat(it.startTime).isEqualTo(LocalTime.MIDNIGHT)
        assertThat(it.priority).isEqualTo(EventType.VISIT.defaultPriority)
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
        assertThat(it.date).isEqualTo(tomorrow)
        assertThat(it.startTime).isEqualTo(LocalTime.MIDNIGHT)
        assertThat(it.priority).isEqualTo(EventType.COURT_HEARING.defaultPriority)
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
        assertThat(it.date).isEqualTo(tomorrow)
        assertThat(it.startTime).isEqualTo(LocalTime.of(10, 0, 0))
        assertThat(it.endTime).isEqualTo(LocalTime.of(11, 30, 0))
        assertThat(it.priority).isEqualTo(EventType.ACTIVITY.defaultPriority)
      }

      assertThat(externalTransfers).isEmpty()
    }
  }

  @Test
  fun `get scheduled events (including transfers occurring today) - single prisoner - rolled out prison - success`() {
    val prisonCode = "MDI"
    val prisonerNumber = "G4793VF"
    val startDate = LocalDate.of(2022, 12, 14)
    val endDate = LocalDate.now().plusDays(10)
    val dateRangeOverlappingTodaysDate = LocalDateRange(startDate, endDate)
    val timeSlot: TimeSlot = TimeSlot.AM

    setupSinglePrisonerApiMocks(prisonCode, prisonerNumber, dateRangeOverlappingTodaysDate)
    setupRolledOutPrisonMock(
      prisonCode,
      active = true,
      rolloutDate = LocalDate.of(2022, 12, 22),
      AppointmentsDataSource.PRISON_API,
    )

    // Uses the default event priorities for all types of event
    whenever(prisonRegimeService.getEventPrioritiesForPrison(prisonCode))
      .thenReturn(EventType.values().associateWith { listOf(Priority(it.defaultPriority)) })

    // Activities from the database view
    whenever(
      prisonerScheduledActivityRepository.getScheduledActivitiesForPrisonerAndDateRange(
        prisonCode,
        prisonerNumber,
        startDate,
        endDate,
      ),
    )
      .thenReturn(listOf(activityFromDbInstance()))

    val result = service.getScheduledEventsByPrisonAndPrisonerAndDateRange(
      prisonCode,
      prisonerNumber,
      LocalDateRange(startDate, endDate),
      timeSlot,
    )

    // Should not be called - this is a rolled-out prison
    verifyBlocking(prisonApiClient, never()) { getScheduledActivitiesAsync(any(), any()) }

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
        assertThat(it.priority).isEqualTo(5)
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
        assertThat(it.priority).isEqualTo(3)
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
        assertThat(it.priority).isEqualTo(6)
      }

      assertThat(externalTransfers).containsExactly(
        ScheduledEvent(
          prisonCode = "MDI",
          prisonerNumber = "G4793VF",
          eventId = 1,
          bookingId = 900001,
          location = "External",
          locationId = null,
          eventClass = "TRANSFER",
          eventType = "EXTERNAL_TRANSFER",
          eventStatus = "SCH",
          eventTypeDesc = "EXTERNAL_TRANSFER",
          event = "TRANSFER",
          eventDesc = "",
          startTime = LocalTime.MIDNIGHT,
          endTime = LocalTime.NOON,
          details = "",
          date = LocalDate.now(),
          priority = EventType.EXTERNAL_TRANSFER.defaultPriority,
        ),
      )
    }
  }

  @Test
  fun `get scheduled events (excluding transfers) - single prisoner - rolled out prison - success`() {
    val prisonCode = "MDI"
    val prisonerNumber = "G4793VF"
    val startDate = LocalDate.of(2022, 12, 14)
    val endDate = LocalDate.now().minusDays(1)
    val dateRangeNotOverlappingTodaysDate = LocalDateRange(startDate, endDate)
    val timeSlot: TimeSlot = TimeSlot.AM

    setupSinglePrisonerApiMocks(prisonCode, prisonerNumber, dateRangeNotOverlappingTodaysDate)
    setupRolledOutPrisonMock(
      prisonCode,
      active = true,
      rolloutDate = LocalDate.of(2022, 12, 22),
      AppointmentsDataSource.PRISON_API,
    )

    // Uses the default event priorities for all types of event
    whenever(prisonRegimeService.getEventPrioritiesForPrison(prisonCode))
      .thenReturn(EventType.values().associateWith { listOf(Priority(it.defaultPriority)) })

    // Activities from the database view
    whenever(
      prisonerScheduledActivityRepository.getScheduledActivitiesForPrisonerAndDateRange(
        prisonCode,
        prisonerNumber,
        startDate,
        endDate,
      ),
    )
      .thenReturn(listOf(activityFromDbInstance()))

    val result = service.getScheduledEventsByPrisonAndPrisonerAndDateRange(
      prisonCode,
      prisonerNumber,
      LocalDateRange(startDate, endDate),
      timeSlot,
    )

    // Should not be called - this is a rolled-out prison
    verifyBlocking(prisonApiClient, never()) { getScheduledActivitiesAsync(any(), any()) }

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
        assertThat(it.priority).isEqualTo(5)
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
        assertThat(it.priority).isEqualTo(3)
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
        assertThat(it.priority).isEqualTo(6)
      }

      assertThat(externalTransfers).isEmpty()
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
    setupRolledOutPrisonMock(
      prisonCode,
      active = true,
      rolloutDate = LocalDate.of(2022, 12, 22),
      AppointmentsDataSource.PRISON_API,
    )

    // Mocked activities from the database view
    whenever(
      prisonerScheduledActivityRepository.getScheduledActivitiesForPrisonerAndDateRange(
        prisonCode,
        prisonerNumber,
        startDate,
        endDate,
      ),
    )
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
    verifyBlocking(prisonApiClient, never()) { getScheduledActivitiesAsync(any(), any()) }

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
    setupRolledOutPrisonMock(
      prisonCode,
      active = true,
      rolloutDate = LocalDate.of(2022, 12, 22),
      AppointmentsDataSource.PRISON_API,
    )

    // Mocked activities from the database view
    whenever(
      prisonerScheduledActivityRepository.getScheduledActivitiesForPrisonerAndDateRange(
        prisonCode,
        prisonerNumber,
        startDate,
        endDate,
      ),
    )
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
    verifyBlocking(prisonApiClient, never()) { getScheduledActivitiesAsync(any(), any()) }

    with(result!!) {
      assertThat(appointments!![0].priority).isEqualTo(5) // EventType.APPOINTMENT default
      assertThat(activities!![0].priority).isEqualTo(6) // EventType.ACTIVITY default
      assertThat(visits!![0].priority).isEqualTo(3) // EventType.VISIT default
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
    setupRolledOutPrisonMock(
      prisonCode,
      active = true,
      rolloutDate = LocalDate.of(2022, 12, 22),
      AppointmentsDataSource.PRISON_API,
    )

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
    whenever(
      prisonerScheduledActivityRepository.getScheduledActivitiesForPrisonerAndDateRange(
        prisonCode,
        prisonerNumber,
        startDate,
        endDate,
      ),
    )
      .thenReturn(listOf(activityFromDbInstance(activityCategory = "LEI")))

    val result = service.getScheduledEventsByPrisonAndPrisonerAndDateRange(
      prisonCode,
      prisonerNumber,
      LocalDateRange(startDate, endDate),
      timeSlot,
    )

    // Should not be called - this is a rolled-out prison
    verifyBlocking(prisonApiClient, never()) { getScheduledActivitiesAsync(any(), any()) }

    with(result!!) {
      assertThat(appointments!![0].priority).isEqualTo(5) // EventType.APPOINTMENT default
      assertThat(activities!![0].priority).isEqualTo(7) // EventType.LEISURE_SOCIAL
      assertThat(visits!![0].priority).isEqualTo(3) // EventType.VISIT default
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
    setupRolledOutPrisonMock(
      prisonCode,
      active = false,
      rolloutDate = LocalDate.of(2023, 12, 22),
      AppointmentsDataSource.PRISON_API,
    )

    // Mock response for activities from Prison API for this prisoner
    val scheduledActivities = listOf(PrisonApiScheduledEventFixture.activityInstance())
    prisonApiClient.stub {
      on { runBlocking { prisonApiClient.getScheduledActivitiesAsync(900001, dateRange) } } doReturn scheduledActivities
    }

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
    verifyBlocking(prisonApiClient) { getScheduledActivitiesAsync(any(), any()) }

    with(result!!) {
      assertThat(this.prisonerNumbers).containsExactly("A1111AA")
      assertThat(appointments).isNotNull
      assertThat(appointments).hasSize(1)
      assertThat(appointments!![0].prisonerNumber).isEqualTo("A1111AA")
      assertThat(appointments!![0].priority).isEqualTo(5)
      assertThat(activities).isNotNull
      assertThat(activities).hasSize(1)
      assertThat(activities!![0].prisonerNumber).isEqualTo("A1111AA")
      assertThat(activities!![0].priority).isEqualTo(6)
      assertThat(visits).isNotNull
      assertThat(visits).hasSize(1)
      assertThat(visits!![0].prisonerNumber).isEqualTo("A1111AA")
      assertThat(visits!![0].priority).isEqualTo(3)
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
    setupRolledOutPrisonMock(
      prisonCode,
      active = false,
      rolloutDate = LocalDate.of(2023, 12, 22),
      AppointmentsDataSource.PRISON_API,
    )

    // Mock response for activities from Prison API for this prisoner
    val scheduledActivities = listOf(PrisonApiScheduledEventFixture.activityInstance())
    prisonApiClient.stub {
      on { runBlocking { prisonApiClient.getScheduledActivitiesAsync(900001, dateRange) } } doReturn scheduledActivities
    }

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
    verifyBlocking(prisonApiClient) { getScheduledActivitiesAsync(any(), any()) }

    with(result!!) {
      assertThat(appointments!![0].priority).isEqualTo(5) // EventType.APPOINTMENT EventCategory.INDUSTRIES "LACO"
      assertThat(activities!![0].priority).isEqualTo(99) // explicit default
      assertThat(visits!![0].priority).isEqualTo(3) // default
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

    setupRolledOutPrisonMock(
      prisonCode,
      active = false,
      rolloutDate = LocalDate.of(2023, 12, 22),
      AppointmentsDataSource.PRISON_API,
    )

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
  fun `get scheduled events - single prisoner - not rolled out - prison code and prisoner number do not match`() {
    val prisonCode = "MDI"
    val prisonerNumber = "A1111AA"
    val startDate = LocalDate.of(2022, 12, 14)
    val endDate = LocalDate.of(2022, 12, 15)
    val dateRange = LocalDateRange(startDate, endDate)

    // Setup prison API mocks for court, appointments, visits - with the prisoner at a different prison
    setupSinglePrisonerApiMocks(prisonCode, prisonerNumber, dateRange, prisonOverride = "PVI")

    setupRolledOutPrisonMock(
      prisonCode,
      active = false,
      rolloutDate = LocalDate.of(2023, 12, 22),
      AppointmentsDataSource.PRISON_API,
    )

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

    setupRolledOutPrisonMock(
      prisonCode,
      active = false,
      rolloutDate = LocalDate.of(2023, 12, 22),
      AppointmentsDataSource.PRISON_API,
    )

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
    setupRolledOutPrisonMock(
      prisonCode,
      active = false,
      rolloutDate = LocalDate.of(2023, 12, 22),
      AppointmentsDataSource.PRISON_API,
    )

    // Simulate activities error from prison API
    prisonApiClient.stub {
      on { runBlocking { prisonApiClient.getScheduledActivitiesAsync(900001, dateRange) } } doThrow RuntimeException("Error")
    }

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
  fun `get scheduled events - single prisoner - not rolled out - visits exception`() {
    val prisonCode = "MDI"
    val prisonerNumber = "A1111AA"
    val startDate = LocalDate.of(2022, 12, 14)
    val endDate = LocalDate.of(2022, 12, 15)
    val dateRange = LocalDateRange(startDate, endDate)

    setupSinglePrisonerApiMocks(prisonCode, prisonerNumber, dateRange)
    setupRolledOutPrisonMock(
      prisonCode,
      active = false,
      rolloutDate = LocalDate.of(2023, 12, 22),
      AppointmentsDataSource.PRISON_API,
    )

    // Simulate visits error from prison API
    prisonApiClient.stub {
      on { runBlocking { prisonApiClient.getScheduledVisitsAsync(900001, dateRange) } } doThrow RuntimeException("Error")
    }

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
  fun `get scheduled events - single prisoner - not rolled out - court hearings exception`() {
    val prisonCode = "MDI"
    val prisonerNumber = "A1111AA"
    val startDate = LocalDate.of(2022, 12, 14)
    val endDate = LocalDate.of(2022, 12, 15)
    val dateRange = LocalDateRange(startDate, endDate)

    setupSinglePrisonerApiMocks(prisonCode, prisonerNumber, dateRange)
    setupRolledOutPrisonMock(
      prisonCode,
      active = false,
      rolloutDate = LocalDate.of(2023, 12, 22),
      AppointmentsDataSource.PRISON_API,
    )

    // Simulate court hearings error from prison API
    prisonApiClient.stub {
      on { runBlocking { prisonApiClient.getScheduledCourtHearingsAsync(900001, dateRange) } } doThrow RuntimeException("Error")
    }

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
}
