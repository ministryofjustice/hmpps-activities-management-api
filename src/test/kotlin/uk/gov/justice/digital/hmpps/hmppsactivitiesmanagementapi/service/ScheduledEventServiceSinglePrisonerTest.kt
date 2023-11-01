package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerScheduledActivity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.RolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.adjudicationHearing
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PrisonerScheduledActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/*
 Tests for the ScheduledEventService focussing on the single prisoner methods and responses.
 */

class ScheduledEventServiceSinglePrisonerTest {
  private val prisonApiClient: PrisonApiClient = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiClient = mock()
  private val rolloutPrisonRepository: RolloutPrisonRepository = mock()
  private val prisonerScheduledActivityRepository: PrisonerScheduledActivityRepository = mock()
  private val appointmentInstanceRepository: AppointmentInstanceRepository = mock()
  private val prisonRegimeService: PrisonRegimeService = mock()

  private val service = ScheduledEventService(
    prisonApiClient,
    prisonerSearchApiClient,
    rolloutPrisonRepository,
    prisonerScheduledActivityRepository,
    appointmentInstanceRepository,
    prisonRegimeService,
  )

  @BeforeEach
  fun reset() {
    reset(
      prisonApiClient,
      prisonerSearchApiClient,
      rolloutPrisonRepository,
      prisonerScheduledActivityRepository,
      appointmentInstanceRepository,
      prisonRegimeService,
    )
  }

  // --- Private utility functions used to set up the mocked responses ---

  private fun setupRolledOutPrisonMock(activitiesRolloutDate: LocalDate, appointmentsRolloutDate: LocalDate) {
    val prisonCode = "MDI"
    whenever(rolloutPrisonRepository.findByCode(prisonCode))
      .thenReturn(
        RolloutPrison(
          rolloutPrisonId = 10,
          code = prisonCode,
          description = "Description",
          activitiesToBeRolledOut = true,
          activitiesRolloutDate = activitiesRolloutDate,
          appointmentsToBeRolledOut = true,
          appointmentsRolloutDate = appointmentsRolloutDate,
        ),
      )
  }

  // Stubs a response for each type of event that can come from the prison API
  // (activity, appointment, visit, adjudication hearing, court hearing, transfer)

  private fun setupSinglePrisonerApiMocks(
    prisonCode: String,
    prisonerNumber: String,
    dateRange: LocalDateRange,
    withAppointmentSubType: String? = null,
    withPrisonerDetailsException: Boolean = false,
    prisonOverride: String = prisonCode,
  ) {
    val appointments = if (!withAppointmentSubType.isNullOrEmpty()) {
      listOf(PrisonApiScheduledEventFixture.appointmentInstance(eventSubType = withAppointmentSubType))
    } else {
      listOf(PrisonApiScheduledEventFixture.appointmentInstance())
    }
    val activities = listOf(PrisonApiScheduledEventFixture.activityInstance())
    val visits = listOf(PrisonApiScheduledEventFixture.visitInstance())
    val courtHearings = PrisonApiCourtHearingsFixture.instance()
    val adjudications = listOf(adjudicationHearing(prisonCode, prisonerNumber))
    val transferEventsToday = listOf(PrisonApiPrisonerScheduleFixture.transferInstance(date = LocalDate.now()))

    val sensitiveEventDateRange = LocalDateRange(
      dateRange.start,
      if (LocalDate.now() < dateRange.endInclusive) LocalDate.now() else dateRange.endInclusive,
    )

    if (withPrisonerDetailsException) {
      prisonerSearchApiClient.stub {
        on {
          runBlocking {
            prisonerSearchApiClient.findByPrisonerNumbersAsync(listOf(prisonerNumber))
          }
        } doThrow RuntimeException("Error")
      }
    } else {
      val prisonerDetails = listOf(PrisonerSearchPrisonerFixture.instance(prisonId = prisonOverride))
      prisonerSearchApiClient.stub {
        on {
          runBlocking {
            prisonerSearchApiClient.findByPrisonerNumbersAsync(listOf(prisonerNumber))
          }
        } doReturn prisonerDetails
      }
    }

    prisonApiClient.stub {
      on {
        runBlocking {
          prisonApiClient.getScheduledActivitiesAsync(900001, dateRange)
        }
      } doReturn activities

      on {
        runBlocking {
          prisonApiClient.getScheduledAppointmentsAsync(900001, dateRange)
        }
      } doReturn appointments

      on {
        runBlocking {
          prisonApiClient.getScheduledVisitsAsync(900001, dateRange)
        }
      } doReturn visits

      on {
        runBlocking {
          prisonApiClient.getScheduledCourtHearingsAsync(900001, sensitiveEventDateRange)
        }
      } doReturn courtHearings

      on {
        runBlocking {
          prisonApiClient.getExternalTransfersOnDateAsync(prisonCode, setOf(prisonerNumber), LocalDate.now())
        }
      } doReturn transferEventsToday

      on {
        runBlocking {
          prisonApiClient.getOffenderAdjudications(prisonCode, dateRange, setOf(prisonerNumber))
        }
      } doReturn adjudications
    }
  }

  private fun appointmentCategoryMap() = mapOf("TEST" to appointmentCategoryReferenceCode("TEST"))
  private fun appointmentLocationMap() = mapOf(101L to appointmentLocation(101L, "MDI"))

  private fun activityFromDbInstance(
    scheduledInstanceId: Long = 1,
    allocationId: Long = 1,
    prisonCode: String = "MDI",
    sessionDate: LocalDate = LocalDate.of(2022, 12, 14),
    startTime: LocalTime? = LocalTime.of(10, 0),
    endTime: LocalTime? = LocalTime.of(11, 30),
    prisonerNumber: String = "G4793VF",
    bookingId: Int = 900001,
    inCell: Boolean = false,
    onWing: Boolean = false,
    offWing: Boolean = false,
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
    inCell = inCell,
    onWing = onWing,
    offWing = offWing,
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

  private fun appointmentFromDbInstance(
    appointmentInstanceId: Long = 1L,
    appointmentSeriesId: Long = 1L,
    appointmentId: Long = 1L,
    appointmentAttendeeId: Long = 1L,
    appointmentType: AppointmentType = AppointmentType.INDIVIDUAL,
    prisonCode: String = "MDI",
    prisonerNumber: String,
    bookingId: Long,
    categoryCode: String = "TEST",
    customName: String? = null,
    internalLocationId: Long? = 101,
    inCell: Boolean = false,
    appointmentDate: LocalDate = LocalDate.now(),
    startTime: LocalTime = LocalTime.of(10, 0, 0),
    endTime: LocalTime? = LocalTime.of(11, 0, 0),
    comment: String? = null,
    created: LocalDateTime = LocalDateTime.now(),
    createdBy: String = "",
    updated: LocalDateTime? = null,
    updatedBy: String? = null,
  ) = AppointmentInstance(
    appointmentInstanceId,
    appointmentSeriesId,
    appointmentId,
    appointmentAttendeeId,
    appointmentType = appointmentType,
    prisonCode = prisonCode,
    bookingId = bookingId,
    appointmentDate = appointmentDate,
    categoryCode = categoryCode,
    customName = customName,
    internalLocationId = internalLocationId,
    customLocation = null,
    inCell = inCell,
    onWing = false,
    offWing = true,
    prisonerNumber = prisonerNumber,
    startTime = startTime,
    endTime = endTime,
    unlockNotes = null,
    extraInformation = comment,
    createdTime = created,
    createdBy = createdBy,
    isCancelled = false,
    updatedTime = updated,
    updatedBy = updatedBy,
  )

  @Nested
  @DisplayName("Scheduled events - activities are active, appointments are not active")
  inner class ActivitiesActiveAppointmentsNot {

    @Test
    fun `Success - including external transfers`() {
      val prisonCode = "MDI"
      val prisonerNumber = "G4793VF"
      val bookingId = 900001L
      val startDate = LocalDate.now().plusDays(-1)
      val endDate = LocalDate.now().plusDays(10)
      val dateRangeOverlappingTodaysDate = LocalDateRange(startDate, endDate)
      val timeSlot: TimeSlot = TimeSlot.AM

      setupSinglePrisonerApiMocks(prisonCode, prisonerNumber, dateRangeOverlappingTodaysDate)

      setupRolledOutPrisonMock(
        activitiesRolloutDate = LocalDate.of(2022, 12, 22),
        appointmentsRolloutDate = LocalDate.of(2600, 12, 22),
      )

      whenever(prisonRegimeService.getEventPrioritiesForPrison(prisonCode))
        .thenReturn(EventPriorities(EventType.values().associateWith { listOf(Priority(it.defaultPriority)) }))

      whenever(
        prisonerScheduledActivityRepository.getScheduledActivitiesForPrisonerAndDateRange(
          prisonCode,
          prisonerNumber,
          startDate,
          endDate,
        ),
      )
        .thenReturn(listOf(activityFromDbInstance()))

      val result = service.getScheduledEventsForSinglePrisoner(
        prisonCode,
        prisonerNumber,
        LocalDateRange(startDate, endDate),
        timeSlot,
        appointmentCategoryMap(),
        appointmentLocationMap(),
      )

      verifyBlocking(prisonApiClient, never()) { getScheduledActivitiesAsync(any(), any()) }
      verifyBlocking(prisonApiClient) { getScheduledAppointmentsAsync(any(), any()) }

      with(result!!) {
        assertThat(prisonerNumbers).containsExactlyInAnyOrder(prisonerNumber)

        assertThat(appointments).isNotNull
        assertThat(appointments).hasSize(1)

        appointments!!.map {
          assertThat(it.eventSource).isEqualTo("NOMIS")
          assertThat(it.prisonCode).isEqualTo("MDI")
          assertThat(it.eventType).isEqualTo(EventType.APPOINTMENT.name)
          assertThat(it.prisonerNumber).isIn(prisonerNumbers)
          assertThat(it.bookingId).isEqualTo(bookingId)
          assertThat(it.eventId).isEqualTo(1)
          assertThat(it.appointmentSeriesId).isNull()
          assertThat(it.appointmentId).isNull()
          assertThat(it.appointmentAttendeeId).isNull()
          assertThat(it.cancelled).isFalse
          assertThat(it.categoryCode).isEqualTo("GOVE")
          assertThat(it.categoryDescription).isEqualTo("Governor")
          assertThat(it.summary).isEqualTo("Appointment Governor")
          assertThat(it.cancelled).isFalse
          assertThat(it.internalLocationId).isEqualTo(1L)
          assertThat(it.internalLocationCode).isEqualTo("GOVERNORS OFFICE")
          assertThat(it.internalLocationDescription).isEqualTo("GOVERNORS OFFICE")
          assertThat(it.date).isEqualTo(LocalDate.of(2022, 12, 14))
          assertThat(it.startTime).isEqualTo(LocalTime.of(17, 0, 0))
          assertThat(it.endTime).isEqualTo(LocalTime.of(18, 0, 0))
          assertThat(it.priority).isEqualTo(EventType.APPOINTMENT.defaultPriority)
        }

        assertThat(visits).isNotNull
        assertThat(visits).hasSize(1)

        visits!!.map {
          assertThat(it.eventSource).isEqualTo("NOMIS")
          assertThat(it.eventType).isEqualTo(EventType.VISIT.name)
          assertThat(it.prisonerNumber).isIn(prisonerNumbers)
          assertThat(it.bookingId).isEqualTo(bookingId)
          assertThat(it.internalLocationId).isEqualTo(1)
          assertThat(it.internalLocationCode).isEqualTo("VISITS ROOM")
          assertThat(it.internalLocationDescription).isEqualTo("VISITS ROOM")
          assertThat(it.categoryCode).isEqualTo("VISIT")
          assertThat(it.categoryDescription).isEqualTo("Visits")
          assertThat(it.eventId).isEqualTo(1)
          assertThat(it.cancelled).isFalse
          assertThat(it.summary).isEqualTo("Visit Visits")
          assertThat(it.date).isEqualTo(LocalDate.of(2022, 12, 14))
          assertThat(it.startTime).isEqualTo(LocalTime.of(17, 0, 0))
          assertThat(it.endTime).isEqualTo(LocalTime.of(18, 0, 0))
          assertThat(it.priority).isEqualTo(EventType.VISIT.defaultPriority)
        }

        assertThat(courtHearings).isNotNull
        assertThat(courtHearings).hasSize(1)

        courtHearings!!.map {
          assertThat(it.eventSource).isEqualTo("NOMIS")
          assertThat(it.eventType).isEqualTo(EventType.COURT_HEARING.name)
          assertThat(it.eventId).isEqualTo(1L)
          assertThat(it.prisonerNumber).isIn(prisonerNumbers)
          assertThat(it.bookingId).isEqualTo(bookingId)
          assertThat(it.internalLocationId).isNull()
          assertThat(it.internalLocationCode).isNull()
          assertThat(it.internalLocationDescription).isEqualTo("Aberdeen Sheriff's Court (abdshf)")
          assertThat(it.summary).isEqualTo("Court hearing")
          assertThat(it.date).isEqualTo(LocalDate.of(2022, 12, 14))
          assertThat(it.startTime).isEqualTo(LocalTime.of(10, 0, 0))
          assertThat(it.endTime).isNull()
          assertThat(it.priority).isEqualTo(EventType.COURT_HEARING.defaultPriority)
        }

        assertThat(activities).isNotNull
        assertThat(activities).hasSize(1)

        activities!!.map {
          assertThat(it.eventSource).isEqualTo("SAA")
          assertThat(it.eventType).isEqualTo(EventType.ACTIVITY.name)
          assertThat(it.eventId).isNull()
          assertThat(it.scheduledInstanceId).isEqualTo(1)
          assertThat(it.prisonerNumber).isIn(prisonerNumbers)
          assertThat(it.bookingId).isEqualTo(bookingId)
          assertThat(it.internalLocationId).isEqualTo(1)
          assertThat(it.internalLocationCode).isEqualTo("MDI-EDU_ROOM1")
          assertThat(it.internalLocationDescription).isEqualTo("Education room 1")
          assertThat(it.categoryCode).isEqualTo("Education")
          assertThat(it.categoryDescription).isEqualTo("Education")
          assertThat(it.summary).isEqualTo("HB1 AM")
          assertThat(it.comments).isNull()
          assertThat(it.date).isEqualTo(LocalDate.of(2022, 12, 14))
          assertThat(it.startTime).isEqualTo(LocalTime.of(10, 0, 0))
          assertThat(it.endTime).isEqualTo(LocalTime.of(11, 30, 0))
          assertThat(it.priority).isEqualTo(EventType.ACTIVITY.defaultPriority)
        }

        assertThat(adjudications).isNotNull
        assertThat(adjudications).hasSize(1)

        adjudications!!.map {
          assertThat(it.eventSource).isEqualTo("NOMIS")
          assertThat(it.eventType).isEqualTo(EventType.ADJUDICATION_HEARING.name)
          assertThat(it.oicHearingId).isEqualTo(-1L)
          assertThat(it.eventId).isNull()
          assertThat(it.prisonerNumber).isIn(prisonerNumbers)
          assertThat(it.internalLocationId).isEqualTo(-2L)
          assertThat(it.internalLocationCode).isNull()
          assertThat(it.internalLocationDescription).isEqualTo("Adjudication room")
          assertThat(it.categoryCode).isNull()
          assertThat(it.categoryDescription).isEqualTo("Governor's Hearing Adult")
          assertThat(it.summary).isEqualTo("Governor's Hearing Adult")
          assertThat(it.date).isEqualTo(LocalDate.now())
          assertThat(it.startTime).isEqualTo(LocalTime.of(0, 0, 0))
          assertThat(it.endTime).isEqualTo(LocalTime.of(2, 0, 0))
          assertThat(it.priority).isEqualTo(EventType.ADJUDICATION_HEARING.defaultPriority)
        }

        assertThat(externalTransfers).isNotNull
        assertThat(externalTransfers).hasSize(1)

        externalTransfers!!.map {
          assertThat(it.eventSource).isEqualTo("NOMIS")
          assertThat(it.eventType).isEqualTo(EventType.EXTERNAL_TRANSFER.name)
          assertThat(it.eventId).isEqualTo(1)
          assertThat(it.prisonerNumber).isIn(prisonerNumbers)
          assertThat(it.bookingId).isEqualTo(900001)
          assertThat(it.categoryCode).isEqualTo("TRANSFER")
          assertThat(it.categoryDescription).isNull()
          assertThat(it.internalLocationDescription).isEqualTo("External")
          assertThat(it.summary).isEqualTo("Transfer")
          assertThat(it.date).isEqualTo(LocalDate.now())
          assertThat(it.startTime).isEqualTo(LocalTime.of(0, 0, 0))
          assertThat(it.endTime).isEqualTo(LocalTime.of(12, 0, 0))
          assertThat(it.priority).isEqualTo(EventType.EXTERNAL_TRANSFER.defaultPriority)
        }
      }
    }

    @Test
    fun `Success - excluding external transfers - date range excludes today`() {
      val prisonCode = "MDI"
      val prisonerNumber = "G4793VF"
      val startDate = LocalDate.now().plusDays(-10)
      val endDate = LocalDate.now().plusDays(-1)
      val dateRangeNotOverlappingTodaysDate = LocalDateRange(startDate, endDate)
      val timeSlot: TimeSlot = TimeSlot.AM

      setupSinglePrisonerApiMocks(prisonCode, prisonerNumber, dateRangeNotOverlappingTodaysDate)

      setupRolledOutPrisonMock(
        activitiesRolloutDate = LocalDate.of(2022, 12, 22),
        appointmentsRolloutDate = LocalDate.of(2600, 12, 22),
      )

      whenever(prisonRegimeService.getEventPrioritiesForPrison(prisonCode))
        .thenReturn(EventPriorities(EventType.values().associateWith { listOf(Priority(it.defaultPriority)) }))

      whenever(
        prisonerScheduledActivityRepository.getScheduledActivitiesForPrisonerAndDateRange(
          prisonCode,
          prisonerNumber,
          startDate,
          endDate,
        ),
      )
        .thenReturn(listOf(activityFromDbInstance()))

      val result = service.getScheduledEventsForSinglePrisoner(
        prisonCode,
        prisonerNumber,
        LocalDateRange(startDate, endDate),
        timeSlot,
        appointmentCategoryMap(),
        appointmentLocationMap(),
      )

      verifyBlocking(prisonApiClient, never()) { getScheduledActivitiesAsync(any(), any()) }
      verifyBlocking(prisonApiClient) { getScheduledAppointmentsAsync(any(), any()) }

      with(result!!) {
        assertThat(prisonerNumbers).containsExactlyInAnyOrder(prisonerNumber)

        assertThat(appointments).isNotNull
        assertThat(appointments).hasSize(1)

        assertThat(visits).isNotNull
        assertThat(visits).hasSize(1)

        assertThat(courtHearings).isNotNull
        assertThat(courtHearings).hasSize(1)

        assertThat(activities).isNotNull
        assertThat(activities).hasSize(1)

        assertThat(adjudications).isNotNull
        assertThat(adjudications).hasSize(1)

        assertThat(externalTransfers).isEmpty()
      }
    }

    @Test
    fun `Specific event priorities`() {
      val prisonCode = "MDI"
      val prisonerNumber = "A1111AA"
      val startDate = LocalDate.of(2022, 12, 14)
      val endDate = LocalDate.of(2022, 12, 15)
      val dateRange = LocalDateRange(startDate, endDate)
      val timeSlot: TimeSlot = TimeSlot.AM

      setupSinglePrisonerApiMocks(prisonCode, prisonerNumber, dateRange)

      setupRolledOutPrisonMock(
        activitiesRolloutDate = LocalDate.of(2022, 12, 22),
        appointmentsRolloutDate = LocalDate.of(2600, 12, 22),
      )

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
        EventPriorities(
          mapOf(
            EventType.ACTIVITY to listOf(
              Priority(1, EventCategory.EDUCATION),
              Priority(2, EventCategory.PRISON_JOBS),
              Priority(3, EventCategory.GYM_SPORTS_FITNESS),
              Priority(4, EventCategory.INDUCTION),
              Priority(5, EventCategory.INDUSTRIES),
              Priority(6, EventCategory.INTERVENTIONS),
              Priority(7, EventCategory.OTHER),
              Priority(8), // Will default to this because event category in test data does not match
            ),
            EventType.APPOINTMENT to listOf(Priority(21)),
            EventType.VISIT to listOf(Priority(22)),
            EventType.ADJUDICATION_HEARING to listOf(Priority(23)),
            EventType.COURT_HEARING to listOf(Priority(24)),
          ),
        ),
      )

      val result = service.getScheduledEventsForSinglePrisoner(
        prisonCode,
        prisonerNumber,
        LocalDateRange(startDate, endDate),
        timeSlot,
        appointmentCategoryMap(),
        appointmentLocationMap(),
      )

      verifyBlocking(prisonApiClient, never()) { getScheduledActivitiesAsync(any(), any()) }
      verifyBlocking(prisonApiClient) { getScheduledAppointmentsAsync(any(), any()) }

      with(result!!) {
        assertThat(activities!![0].priority).isEqualTo(8) // Unmatched category defaults to 8
        assertThat(appointments!![0].priority).isEqualTo(21)
        assertThat(visits!![0].priority).isEqualTo(22)
        assertThat(adjudications!![0].priority).isEqualTo(23)
        assertThat(courtHearings!![0].priority).isEqualTo(24)
      }
    }

    @Test
    fun `Success with default event priorities`() {
      val prisonCode = "MDI"
      val prisonerNumber = "A1111AA"
      val startDate = LocalDate.of(2022, 12, 14)
      val endDate = LocalDate.of(2022, 12, 15)
      val dateRange = LocalDateRange(startDate, endDate)
      val timeSlot: TimeSlot = TimeSlot.AM

      setupSinglePrisonerApiMocks(prisonCode, prisonerNumber, dateRange)

      setupRolledOutPrisonMock(
        activitiesRolloutDate = LocalDate.of(2022, 12, 22),
        appointmentsRolloutDate = LocalDate.of(2600, 12, 22),
      )

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
        EventPriorities(
          mapOf(
            EventType.ACTIVITY to listOf(
              Priority(1, EventCategory.EDUCATION),
              Priority(2, EventCategory.OTHER),
              Priority(3, EventCategory.GYM_SPORTS_FITNESS),
              Priority(4, EventCategory.INDUCTION),
              Priority(5, EventCategory.INDUSTRIES),
              Priority(6, EventCategory.INTERVENTIONS),
              Priority(7, EventCategory.PRISON_JOBS),
            ),
            EventType.APPOINTMENT to listOf(Priority(9, EventCategory.EDUCATION)),
            EventType.VISIT to listOf(Priority(10, EventCategory.EDUCATION)),
            EventType.ADJUDICATION_HEARING to listOf(Priority(11, EventCategory.EDUCATION)),
            EventType.COURT_HEARING to listOf(Priority(12, EventCategory.EDUCATION)),
          ),
        ),
      )

      val result = service.getScheduledEventsForSinglePrisoner(
        prisonCode,
        prisonerNumber,
        LocalDateRange(startDate, endDate),
        timeSlot,
        appointmentCategoryMap(),
        appointmentLocationMap(),
      )

      verifyBlocking(prisonApiClient, never()) { getScheduledActivitiesAsync(any(), any()) }
      verifyBlocking(prisonApiClient) { getScheduledAppointmentsAsync(any(), any()) }

      with(result!!) {
        assertThat(courtHearings!![0].priority).isEqualTo(EventType.COURT_HEARING.defaultPriority)
        assertThat(visits!![0].priority).isEqualTo(EventType.VISIT.defaultPriority)
        assertThat(adjudications!![0].priority).isEqualTo(EventType.ADJUDICATION_HEARING.defaultPriority)
        assertThat(appointments!![0].priority).isEqualTo(EventType.APPOINTMENT.defaultPriority)
        assertThat(activities!![0].priority).isEqualTo(EventType.ACTIVITY.defaultPriority)
      }
    }

    @Test
    fun `Success with activity category priority`() {
      val prisonCode = "MDI"
      val prisonerNumber = "A1111AA"
      val startDate = LocalDate.of(2022, 12, 14)
      val endDate = LocalDate.of(2022, 12, 15)
      val dateRange = LocalDateRange(startDate, endDate)
      val timeSlot: TimeSlot = TimeSlot.AM

      setupSinglePrisonerApiMocks(prisonCode, prisonerNumber, dateRange)

      setupRolledOutPrisonMock(
        activitiesRolloutDate = LocalDate.of(2022, 12, 22),
        appointmentsRolloutDate = LocalDate.of(2600, 12, 22),
      )

      whenever(prisonRegimeService.getEventPrioritiesForPrison(prisonCode)).thenReturn(
        EventPriorities(
          mapOf(
            EventType.ACTIVITY to listOf(
              Priority(1, EventCategory.EDUCATION),
              Priority(2, EventCategory.FAITH_SPIRITUALITY),
              Priority(3, EventCategory.GYM_SPORTS_FITNESS),
              Priority(4, EventCategory.INDUCTION),
              Priority(5, EventCategory.INDUSTRIES),
              Priority(6, EventCategory.INTERVENTIONS),
              Priority(7, EventCategory.NOT_IN_WORK),
              Priority(8, EventCategory.PRISON_JOBS),
              Priority(9, EventCategory.OTHER),
            ),
            EventType.APPOINTMENT to listOf(Priority(10, EventCategory.EDUCATION)),
            EventType.VISIT to listOf(Priority(11, EventCategory.EDUCATION)),
            EventType.ADJUDICATION_HEARING to listOf(Priority(12, EventCategory.EDUCATION)),
            EventType.COURT_HEARING to listOf(Priority(13, EventCategory.EDUCATION)),
          ),
        ),
      )

      // With activity category set to NOT IN WORK
      whenever(
        prisonerScheduledActivityRepository.getScheduledActivitiesForPrisonerAndDateRange(
          prisonCode,
          prisonerNumber,
          startDate,
          endDate,
        ),
      )
        .thenReturn(listOf(activityFromDbInstance(activityCategory = "NOT_IN_WORK")))

      val result = service.getScheduledEventsForSinglePrisoner(
        prisonCode,
        prisonerNumber,
        LocalDateRange(startDate, endDate),
        timeSlot,
        appointmentCategoryMap(),
        appointmentLocationMap(),
      )

      verifyBlocking(prisonApiClient, never()) { getScheduledActivitiesAsync(any(), any()) }

      with(result!!) {
        assertThat(activities!![0].priority).isEqualTo(7) // EventCategory.NOT_IN_WORK
        assertThat(adjudications!![0].priority).isEqualTo(4) // EventType.ADJUDICATION_HEARING default
        assertThat(appointments!![0].priority).isEqualTo(5) // EventType.APPOINTMENT default
        assertThat(courtHearings!![0].priority).isEqualTo(1) // EventType.COURT_HEARING default
        assertThat(visits!![0].priority).isEqualTo(3) // EventType.VISIT default
      }
    }
  }

  @Nested
  @DisplayName("Scheduled events - appointments are active, activities are not active")
  inner class AppointmentsActiveActivitiesNot {

    @Test
    fun `Success - appointments active, activities from NOMIS`() {
      val prisonCode = "MDI"
      val prisonerNumber = "A1111AA"
      val bookingId = 900001L
      val startDate = LocalDate.of(2022, 12, 14)
      val endDate = LocalDate.of(2022, 12, 15)
      val dateRange = LocalDateRange(startDate, endDate)
      val timeSlot: TimeSlot = TimeSlot.AM

      setupSinglePrisonerApiMocks(prisonCode, prisonerNumber, dateRange)

      setupRolledOutPrisonMock(
        activitiesRolloutDate = LocalDate.of(2600, 12, 22),
        appointmentsRolloutDate = LocalDate.of(2022, 12, 22),
      )

      whenever(prisonRegimeService.getEventPrioritiesForPrison(prisonCode))
        .thenReturn(EventPriorities(EventType.values().associateWith { listOf(Priority(it.defaultPriority)) }))

      val appointmentEntity = appointmentFromDbInstance(
        prisonerNumber = prisonerNumber,
        bookingId = bookingId,
        customName = "Meeting with the governor",
      )
      whenever(appointmentInstanceRepository.findByBookingIdAndDateRange(any(), any(), any()))
        .thenReturn(listOf(appointmentEntity))

      val result = service.getScheduledEventsForSinglePrisoner(
        prisonCode,
        prisonerNumber,
        LocalDateRange(startDate, endDate),
        timeSlot,
        appointmentCategoryMap(),
        appointmentLocationMap(),
      )

      verify(prisonerScheduledActivityRepository, never())
        .getScheduledActivitiesForPrisonerAndDateRange(prisonCode, prisonerNumber, startDate, endDate)

      verify(appointmentInstanceRepository)
        .findByBookingIdAndDateRange(bookingId, startDate, endDate)

      verifyBlocking(prisonApiClient) { getScheduledActivitiesAsync(any(), any()) }
      verifyBlocking(prisonApiClient, never()) { getScheduledAppointmentsAsync(any(), any()) }

      with(result!!) {
        assertThat(this.prisonerNumbers).containsExactly("A1111AA")

        assertThat(appointments).isNotNull
        assertThat(appointments).hasSize(1)

        appointments!!.map {
          assertThat(it.eventSource).isEqualTo("SAA")
          assertThat(it.prisonCode).isEqualTo("MDI")
          assertThat(it.prisonerNumber).isIn(prisonerNumbers)
          assertThat(it.bookingId).isEqualTo(bookingId)
          assertThat(it.eventId).isNull()
          assertThat(it.eventType).isEqualTo(EventType.APPOINTMENT.name)
          assertThat(it.appointmentSeriesId).isEqualTo(appointmentEntity.appointmentSeriesId)
          assertThat(it.appointmentId).isEqualTo(appointmentEntity.appointmentId)
          assertThat(it.appointmentAttendeeId).isEqualTo(appointmentEntity.appointmentAttendeeId)
          assertThat(it.categoryCode).isEqualTo(appointmentEntity.categoryCode)
          assertThat(it.cancelled).isFalse
          assertThat(it.internalLocationId).isEqualTo(appointmentEntity.internalLocationId)
          assertThat(it.priority).isEqualTo(EventType.APPOINTMENT.defaultPriority)
        }

        assertThat(activities).isNotNull
        assertThat(activities).hasSize(1)

        activities!!.map {
          assertThat(it.eventSource).isEqualTo("NOMIS")
          assertThat(it.eventType).isEqualTo(EventType.ACTIVITY.name)
          assertThat(it.eventId).isEqualTo(1L)
          assertThat(it.scheduledInstanceId).isNull()
          assertThat(it.internalLocationDescription).isEqualTo("WORKSHOP 10 - BRICKS")
          assertThat(it.categoryCode).isEqualTo("PA")
          assertThat(it.categoryDescription).isEqualTo("Prison Activities")
          assertThat(it.summary).isEqualTo("Prison Activities")
          assertThat(it.date).isEqualTo(LocalDate.of(2022, 12, 14))
          assertThat(it.startTime).isEqualTo(LocalTime.of(13, 15, 0))
          assertThat(it.endTime).isEqualTo(LocalTime.of(16, 15, 0))
          assertThat(it.priority).isEqualTo(EventType.ACTIVITY.defaultPriority)
        }

        assertThat(visits).isNotNull
        assertThat(visits).hasSize(1)

        assertThat(courtHearings).isNotNull
        assertThat(courtHearings).hasSize(1)

        assertThat(adjudications).isNotNull
        assertThat(adjudications).hasSize(1)
      }
    }
  }

  @Nested
  @DisplayName("Scheduled events - both activities and appointments active")
  inner class ActivitiesAndAppointmentsActive {

    @Test
    fun `Success - both active`() {
      val prisonCode = "MDI"
      val prisonerNumber = "G4793VF"
      val bookingId = 900001L
      val startDate = LocalDate.now().plusDays(-1)
      val endDate = LocalDate.now().plusDays(10)
      val dateRangeOverlappingTodaysDate = LocalDateRange(startDate, endDate)
      val timeSlot: TimeSlot = TimeSlot.AM

      setupSinglePrisonerApiMocks(prisonCode, prisonerNumber, dateRangeOverlappingTodaysDate)

      setupRolledOutPrisonMock(
        activitiesRolloutDate = LocalDate.of(2022, 12, 22),
        appointmentsRolloutDate = LocalDate.of(2022, 12, 22),
      )

      whenever(prisonRegimeService.getEventPrioritiesForPrison(prisonCode))
        .thenReturn(EventPriorities(EventType.values().associateWith { listOf(Priority(it.defaultPriority)) }))

      val activityEntity = activityFromDbInstance()
      whenever(
        prisonerScheduledActivityRepository.getScheduledActivitiesForPrisonerAndDateRange(
          prisonCode,
          prisonerNumber,
          startDate,
          endDate,
        ),
      )
        .thenReturn(listOf(activityEntity))

      val appointmentEntity = appointmentFromDbInstance(prisonerNumber = prisonerNumber, bookingId = bookingId)
      whenever(appointmentInstanceRepository.findByBookingIdAndDateRange(any(), any(), any()))
        .thenReturn(listOf(appointmentEntity))

      val result = service.getScheduledEventsForSinglePrisoner(
        prisonCode,
        prisonerNumber,
        LocalDateRange(startDate, endDate),
        timeSlot,
        appointmentCategoryMap(),
        appointmentLocationMap(),
      )

      verifyBlocking(prisonApiClient, never()) { getScheduledActivitiesAsync(any(), any()) }
      verifyBlocking(prisonApiClient, never()) { getScheduledAppointmentsAsync(any(), any()) }

      with(result!!) {
        assertThat(prisonerNumbers).containsExactlyInAnyOrder(prisonerNumber)

        assertThat(appointments).isNotNull
        assertThat(appointments).hasSize(1)

        appointments!!.map {
          assertThat(it.eventSource).isEqualTo("SAA")
          assertThat(it.prisonCode).isEqualTo(prisonCode)
          assertThat(it.prisonerNumber).isIn(prisonerNumbers)
          assertThat(it.bookingId).isEqualTo(bookingId)
          assertThat(it.eventId).isNull()
          assertThat(it.eventType).isEqualTo(EventType.APPOINTMENT.name)
          assertThat(it.appointmentSeriesId).isEqualTo(appointmentEntity.appointmentSeriesId)
          assertThat(it.appointmentId).isEqualTo(appointmentEntity.appointmentId)
          assertThat(it.appointmentAttendeeId).isEqualTo(appointmentEntity.appointmentInstanceId)
          assertThat(it.summary).isEqualTo("Test Category")
          assertThat(it.categoryCode).isEqualTo(appointmentEntity.categoryCode)
          assertThat(it.categoryDescription).isEqualTo("Test Category")
          assertThat(it.cancelled).isFalse
          assertThat(it.internalLocationId).isEqualTo(appointmentEntity.internalLocationId)
          assertThat(it.internalLocationCode).isEqualTo("No information available")
          assertThat(it.internalLocationDescription).isEqualTo("Test Appointment Location User Description")
          assertThat(it.priority).isEqualTo(EventType.APPOINTMENT.defaultPriority)
        }

        assertThat(visits).isNotNull
        assertThat(visits).hasSize(1)

        visits!!.map {
          assertThat(it.eventSource).isEqualTo("NOMIS")
          assertThat(it.eventType).isEqualTo(EventType.VISIT.name)
          assertThat(it.priority).isEqualTo(EventType.VISIT.defaultPriority)
        }

        assertThat(courtHearings).isNotNull
        assertThat(courtHearings).hasSize(1)

        courtHearings!!.map {
          assertThat(it.eventSource).isEqualTo("NOMIS")
          assertThat(it.eventType).isEqualTo(EventType.COURT_HEARING.name)
          assertThat(it.priority).isEqualTo(EventType.COURT_HEARING.defaultPriority)
        }

        assertThat(activities).isNotNull
        assertThat(activities).hasSize(1)

        activities!!.map {
          assertThat(it.eventSource).isEqualTo("SAA")
          assertThat(it.eventType).isEqualTo(EventType.ACTIVITY.name)
          assertThat(it.eventId).isNull()
          assertThat(it.scheduledInstanceId).isEqualTo(activityEntity.scheduledInstanceId)
          assertThat(it.summary).isEqualTo(activityEntity.scheduleDescription)
          assertThat(it.priority).isEqualTo(EventType.ACTIVITY.defaultPriority)
        }

        assertThat(externalTransfers).isNotNull
        assertThat(externalTransfers).hasSize(1)

        externalTransfers!!.map {
          assertThat(it.eventSource).isEqualTo("NOMIS")
          assertThat(it.eventType).isEqualTo(EventType.EXTERNAL_TRANSFER.name)
          assertThat(it.priority).isEqualTo(EventType.EXTERNAL_TRANSFER.defaultPriority)
        }
      }
    }
  }

  @Nested
  @DisplayName("Scheduled events - neither activities nor appointments are active")
  inner class NeitherActivitiesNorAppointmentsActive {
    @Test
    fun `Success - with sub-category priority`() {
      val prisonCode = "MDI"
      val prisonerNumber = "A1111AA"
      val startDate = LocalDate.of(2022, 12, 14)
      val endDate = LocalDate.of(2022, 12, 15)
      val dateRange = LocalDateRange(startDate, endDate)
      val timeSlot: TimeSlot = TimeSlot.AM

      // Setup prison API mocks - with a specific APPOINTMENT type
      setupSinglePrisonerApiMocks(prisonCode, prisonerNumber, dateRange, withAppointmentSubType = "LACO")

      setupRolledOutPrisonMock(
        activitiesRolloutDate = LocalDate.of(2600, 12, 22),
        appointmentsRolloutDate = LocalDate.of(2600, 12, 22),
      )

      whenever(prisonRegimeService.getEventPrioritiesForPrison(prisonCode)).thenReturn(
        EventPriorities(
          mapOf(
            EventType.ACTIVITY to listOf(
              Priority(99),
            ),
            EventType.APPOINTMENT to listOf(
              Priority(1, EventCategory.EDUCATION),
              Priority(2, EventCategory.NOT_IN_WORK),
              Priority(3, EventCategory.GYM_SPORTS_FITNESS),
              Priority(4, EventCategory.INDUCTION),
              Priority(5, EventCategory.INDUSTRIES),
              Priority(6, EventCategory.INTERVENTIONS),
              Priority(7, EventCategory.OTHER),
            ),
            EventType.VISIT to listOf(Priority(10, EventCategory.EDUCATION)),
            EventType.ADJUDICATION_HEARING to listOf(Priority(11, EventCategory.EDUCATION)),
            EventType.COURT_HEARING to listOf(Priority(12, EventCategory.EDUCATION)),
          ),
        ),
      )

      val result = service.getScheduledEventsForSinglePrisoner(
        prisonCode,
        prisonerNumber,
        LocalDateRange(startDate, endDate),
        timeSlot,
        appointmentCategoryMap(),
        appointmentLocationMap(),
      )

      verify(prisonerScheduledActivityRepository, never())
        .getScheduledActivitiesForPrisonerAndDateRange(prisonCode, prisonerNumber, startDate, endDate)

      verifyBlocking(prisonApiClient) { getScheduledActivitiesAsync(any(), any()) }
      verifyBlocking(prisonApiClient) { getScheduledAppointmentsAsync(any(), any()) }

      with(result!!) {
        assertThat(appointments!![0].priority).isEqualTo(5) // EventType.APPOINTMENT EventCategory.INDUSTRIES "LACO"
        assertThat(activities!![0].priority).isEqualTo(99) // explicit default
        assertThat(visits!![0].priority).isEqualTo(3) // default
        assertThat(courtHearings!![0].priority).isEqualTo(1) // default
      }
    }

    @Test
    fun `Prisoner details error`() {
      val prisonCode = "MDI"
      val prisonerNumber = "A1111AA"
      val startDate = LocalDate.of(2022, 12, 14)
      val endDate = LocalDate.of(2022, 12, 15)
      val dateRange = LocalDateRange(startDate, endDate)

      // Setup prison API mocks  - with an error on prisoner details
      setupSinglePrisonerApiMocks(prisonCode, prisonerNumber, dateRange, withPrisonerDetailsException = true)

      setupRolledOutPrisonMock(
        activitiesRolloutDate = LocalDate.of(2600, 12, 22),
        appointmentsRolloutDate = LocalDate.of(2600, 12, 22),
      )

      assertThatThrownBy {
        service.getScheduledEventsForSinglePrisoner(
          prisonCode,
          prisonerNumber,
          dateRange,
          null,
          appointmentCategoryMap(),
          appointmentLocationMap(),
        )
      }
        .isInstanceOf(Exception::class.java)
        .hasMessage("Error")
    }

    @Test
    fun `Prison code and prisoner number do not match`() {
      val prisonCode = "MDI"
      val prisonerNumber = "A1111AA"
      val startDate = LocalDate.of(2022, 12, 14)
      val endDate = LocalDate.of(2022, 12, 15)
      val dateRange = LocalDateRange(startDate, endDate)

      // Setup prison API mocks - with the prisoner at a different prison
      setupSinglePrisonerApiMocks(prisonCode, prisonerNumber, dateRange, prisonOverride = "PVI")

      setupRolledOutPrisonMock(
        activitiesRolloutDate = LocalDate.of(2600, 12, 22),
        appointmentsRolloutDate = LocalDate.of(2600, 12, 22),
      )

      assertThatThrownBy {
        service.getScheduledEventsForSinglePrisoner(
          prisonCode,
          prisonerNumber,
          dateRange,
          null,
          appointmentCategoryMap(),
          appointmentLocationMap(),
        )
      }
        .isInstanceOf(EntityNotFoundException::class.java)
        .hasMessage("Prisoner 'A1111AA' not found in prison 'MDI'")
    }

    @Test
    fun `Appointments exception`() {
      val prisonCode = "MDI"
      val prisonerNumber = "A1111AA"
      val startDate = LocalDate.of(2022, 12, 14)
      val endDate = LocalDate.of(2022, 12, 15)
      val dateRange = LocalDateRange(startDate, endDate)

      setupSinglePrisonerApiMocks(prisonCode, prisonerNumber, dateRange)

      setupRolledOutPrisonMock(
        activitiesRolloutDate = LocalDate.of(2600, 12, 22),
        appointmentsRolloutDate = LocalDate.of(2600, 12, 22),
      )

      // Simulate appointment error from prison API
      prisonApiClient.stub {
        on {
          runBlocking {
            prisonApiClient.getScheduledAppointmentsAsync(900001, dateRange)
          }
        } doThrow RuntimeException("Error")
      }

      assertThatThrownBy {
        service.getScheduledEventsForSinglePrisoner(
          prisonCode,
          prisonerNumber,
          dateRange,
          null,
          appointmentCategoryMap(),
          appointmentLocationMap(),
        )
      }
        .isInstanceOf(Exception::class.java)
        .hasMessage("Error")
    }

    @Test
    fun `Activities exception`() {
      val prisonCode = "MDI"
      val prisonerNumber = "A1111AA"
      val startDate = LocalDate.of(2022, 12, 14)
      val endDate = LocalDate.of(2022, 12, 15)
      val dateRange = LocalDateRange(startDate, endDate)

      setupSinglePrisonerApiMocks(prisonCode, prisonerNumber, dateRange)

      setupRolledOutPrisonMock(
        activitiesRolloutDate = LocalDate.of(2600, 12, 22),
        appointmentsRolloutDate = LocalDate.of(2600, 12, 22),
      )

      // Simulate activities error from prison API
      prisonApiClient.stub {
        on {
          runBlocking {
            prisonApiClient.getScheduledActivitiesAsync(
              900001,
              dateRange,
            )
          }
        } doThrow RuntimeException("Error")
      }

      assertThatThrownBy {
        service.getScheduledEventsForSinglePrisoner(
          prisonCode,
          prisonerNumber,
          dateRange,
          null,
          appointmentCategoryMap(),
          appointmentLocationMap(),
        )
      }
        .isInstanceOf(Exception::class.java)
        .hasMessage("Error")
    }

    @Test
    fun `Visits exception`() {
      val prisonCode = "MDI"
      val prisonerNumber = "A1111AA"
      val startDate = LocalDate.of(2022, 12, 14)
      val endDate = LocalDate.of(2022, 12, 15)
      val dateRange = LocalDateRange(startDate, endDate)

      setupSinglePrisonerApiMocks(prisonCode, prisonerNumber, dateRange)

      setupRolledOutPrisonMock(
        activitiesRolloutDate = LocalDate.of(2600, 12, 22),
        appointmentsRolloutDate = LocalDate.of(2600, 12, 22),
      )

      // Simulate visits error from prison API
      prisonApiClient.stub {
        on {
          runBlocking {
            prisonApiClient.getScheduledVisitsAsync(
              900001,
              dateRange,
            )
          }
        } doThrow RuntimeException("Error")
      }

      assertThatThrownBy {
        service.getScheduledEventsForSinglePrisoner(
          prisonCode,
          prisonerNumber,
          dateRange,
          null,
          appointmentCategoryMap(),
          appointmentLocationMap(),
        )
      }
        .isInstanceOf(Exception::class.java)
        .hasMessage("Error")
    }

    @Test
    fun `Court hearings exception`() {
      val prisonCode = "MDI"
      val prisonerNumber = "A1111AA"
      val startDate = LocalDate.of(2022, 12, 14)
      val endDate = LocalDate.of(2022, 12, 15)
      val dateRange = LocalDateRange(startDate, endDate)

      setupSinglePrisonerApiMocks(prisonCode, prisonerNumber, dateRange)
      setupRolledOutPrisonMock(
        activitiesRolloutDate = LocalDate.of(2600, 12, 22),
        appointmentsRolloutDate = LocalDate.of(2600, 12, 22),
      )

      // Simulate court hearings error from prison API
      prisonApiClient.stub {
        on {
          runBlocking {
            prisonApiClient.getScheduledCourtHearingsAsync(900001, dateRange)
          }
        } doThrow RuntimeException("Error")
      }

      assertThatThrownBy {
        service.getScheduledEventsForSinglePrisoner(
          prisonCode,
          prisonerNumber,
          dateRange,
          null,
          appointmentCategoryMap(),
          appointmentLocationMap(),
        )
      }
        .isInstanceOf(Exception::class.java)
        .hasMessage("Error")
    }
  }

  @Nested
  @DisplayName("Scheduled events - hide sensitive future events")
  inner class ShowHideSensitiveEvents {
    val prisonCode = "MDI"
    val prisonerNumber = "G4793VF"
    val startDate: LocalDate = LocalDate.now().minusDays(1)
    val endDate: LocalDate = LocalDate.now().plusDays(10)
    private val dateRangeOverlappingTodaysDate = LocalDateRange(startDate, endDate)
    val timeSlot: TimeSlot = TimeSlot.AM

    @BeforeEach
    fun beforeEach() {
      setupRolledOutPrisonMock(
        activitiesRolloutDate = LocalDate.of(2022, 12, 22),
        appointmentsRolloutDate = LocalDate.of(2600, 12, 22),
      )

      whenever(prisonRegimeService.getEventPrioritiesForPrison(prisonCode))
        .thenReturn(EventPriorities(EventType.values().associateWith { listOf(Priority(it.defaultPriority)) }))

      whenever(
        prisonerScheduledActivityRepository.getScheduledActivitiesForPrisonerAndDateRange(
          prisonCode,
          prisonerNumber,
          startDate,
          endDate,
        ),
      ).thenReturn(listOf(activityFromDbInstance()))
    }

    @Test
    fun `Should not fetch sensitive future events`() {
      setupSinglePrisonerApiMocks(prisonCode, prisonerNumber, dateRangeOverlappingTodaysDate)

      service.getScheduledEventsForSinglePrisoner(
        prisonCode,
        prisonerNumber,
        LocalDateRange(startDate, endDate),
        timeSlot,
        appointmentCategoryMap(),
        appointmentLocationMap(),
      )

      // Should not retrieve sensitive events with future date ranges
      verifyBlocking(prisonApiClient) {
        getScheduledCourtHearingsAsync(any(), eq(LocalDateRange(startDate, LocalDate.now())))
      }
      verifyBlocking(prisonApiClient) {
        getExternalTransfersOnDateAsync(any(), any(), eq(LocalDate.now()))
      }
    }
  }
}
