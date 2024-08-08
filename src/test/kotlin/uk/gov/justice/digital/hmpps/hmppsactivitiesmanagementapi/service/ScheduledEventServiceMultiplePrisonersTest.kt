package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.adjudications.AdjudicationsHearingAdapter
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.adjudications.Hearing
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.adjudications.HearingsResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.adjudications.ManageAdjudicationsApiFacade
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.rangeTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerScheduledActivity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.RolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.adjudicationHearing
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PrisonerScheduledActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.EventPriorities
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.Priority
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.PrisonRegimeService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.ADJUDICATION_HEARING_DURATION_TWO_HOURS
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/*
 Tests for the ScheduledEventService focussing on the multiple prisoner methods and responses.
 */

class ScheduledEventServiceMultiplePrisonersTest {
  private val prisonApiClient: PrisonApiClient = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiClient = mock()
  private val rolloutPrisonRepository: RolloutPrisonRepository = mock()
  private val prisonerScheduledActivityRepository: PrisonerScheduledActivityRepository = mock()
  private val appointmentInstanceRepository: AppointmentInstanceRepository = mock()
  private val prisonRegimeService: PrisonRegimeService = mock()
  private val manageAdjudicationsApiFacade: ManageAdjudicationsApiFacade = mock()
  private val adjudicationsHearingAdapter = AdjudicationsHearingAdapter(
    manageAdjudicationsApiFacade = manageAdjudicationsApiFacade,
    prisonRegimeService = prisonRegimeService,
  )

  private val service = ScheduledEventService(
    prisonApiClient,
    prisonerSearchApiClient,
    rolloutPrisonRepository,
    prisonerScheduledActivityRepository,
    appointmentInstanceRepository,
    prisonRegimeService,
    adjudicationsHearingAdapter,
  )

  @BeforeEach
  fun reset() {
    whenever(prisonRegimeService.getPrisonRegimeSlotForDayAndTime(any(), any(), any())).thenReturn(TimeSlot.AM)
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
    val active = true
    whenever(rolloutPrisonRepository.findByCode(prisonCode))
      .thenReturn(
        RolloutPrison(
          rolloutPrisonId = 10,
          code = prisonCode,
          description = "Description",
          activitiesToBeRolledOut = active,
          activitiesRolloutDate = activitiesRolloutDate,
          appointmentsToBeRolledOut = active,
          appointmentsRolloutDate = appointmentsRolloutDate,
          maxDaysToExpiry = 21,
        ),
      )
  }

  private fun setupMultiplePrisonerApiMocks(
    prisonerNumbers: Set<String>,
    date: LocalDate,
    timeSlot: TimeSlot?,
  ) {
    val prisonCode = "MDI"
    val activities = listOf(PrisonApiPrisonerScheduleFixture.activityInstance(date = date))
    val appointments = listOf(PrisonApiPrisonerScheduleFixture.appointmentInstance(date = date))
    val visits = listOf(PrisonApiPrisonerScheduleFixture.visitInstance(date = date))
    val courtEvents = listOf(PrisonApiPrisonerScheduleFixture.courtInstance(date = date))
    val transferEvents = listOf(PrisonApiPrisonerScheduleFixture.transferInstance(date = date))
    val adjudications = prisonerNumbers.map { adjudicationHearing(prisonCode, it) }

    prisonApiClient.stub {
      on {
        runBlocking {
          getScheduledActivitiesForPrisonerNumbersAsync(
            prisonCode,
            prisonerNumbers,
            date,
            timeSlot,
          )
        }
      } doReturn activities

      on {
        runBlocking {
          getScheduledAppointmentsForPrisonerNumbersAsync(
            prisonCode,
            prisonerNumbers,
            date,
            timeSlot,
          )
        }
      } doReturn appointments

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

      on {
        runBlocking {
          getExternalTransfersOnDateAsync(
            prisonCode,
            prisonerNumbers,
            date,
          )
        }
      } doReturn transferEvents

      on {
        runBlocking {
          manageAdjudicationsApiFacade.getAdjudicationHearings(
            prisonCode,
            date,
            date,
            prisonerNumbers,
          )
        }
      } doReturn adjudications.map {
        HearingsResponse(
          prisonerNumber = it.offenderNo,
          hearing = Hearing(
            id = it.hearingId,
            locationId = it.internalLocationId,
            dateTimeOfHearing = LocalDateTime.parse(it.startTime!!),
            agencyId = it.agencyId,
            oicHearingType = it.hearingType!!,
          ),
        )
      }

      on {
        runBlocking {
          prisonApiClient.getEventLocationsForPrison(any())
        }
      } doReturn emptyMap()
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
    bookingId: Long = 900001,
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
    startTime: LocalTime = LocalTime.now(),
    endTime: LocalTime? = null,
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
    prisonerNumber = prisonerNumber,
    bookingId = bookingId,
    categoryCode = categoryCode,
    customName = customName,
    internalLocationId = internalLocationId,
    customLocation = null,
    inCell = inCell,
    onWing = false,
    offWing = true,
    appointmentDate = appointmentDate,
    startTime = startTime,
    endTime = endTime,
    unlockNotes = null,
    extraInformation = comment,
    createdTime = created,
    createdBy = createdBy,
    isCancelled = false,
    updatedTime = updated,
    updatedBy = updatedBy,
    seriesCancellationStartDate = null,
    seriesCancellationStartTime = null,
    seriesFrequency = null,
  )

  private fun appointmentCategoryMap() = mapOf("TEST" to appointmentCategoryReferenceCode("TEST"))
  private fun appointmentLocationMap() = mapOf(101L to appointmentLocation(101L, "MDI"))

  @Nested
  @DisplayName("Scheduled events - multiple prisoners - activities rolled out, appointments are not")
  inner class MultipleWithActivitiesActiveAndAppointmentsNotActive {

    @BeforeEach
    fun `init`() {
      whenever(prisonRegimeService.getPrisonRegimeSlotForDayAndTime(any(), any(), any())).thenReturn(TimeSlot.AM)
    }

    @Test
    fun `Events for today - including transfers - success`() {
      val prisonCode = "MDI"
      val bookingId = 900001L
      val prisonerNumbers = setOf("G4793VF", "G1234GK")
      val today = LocalDate.now()
      val timeSlot: TimeSlot = TimeSlot.AM

      setupMultiplePrisonerApiMocks(prisonerNumbers, today, timeSlot)
      setupRolledOutPrisonMock(LocalDate.of(2022, 12, 22), LocalDate.of(2600, 12, 22))

      val activityEntity = activityFromDbInstance(sessionDate = today)
      whenever(
        prisonerScheduledActivityRepository.getScheduledActivitiesForPrisonerListAndDate(
          prisonCode,
          prisonerNumbers,
          today,
        ),
      )
        .thenReturn(listOf(activityEntity))

      whenever(prisonRegimeService.getEventPrioritiesForPrison(prisonCode))
        .thenReturn(EventPriorities(EventType.entries.associateWith { listOf(Priority(it.defaultPriority)) }))

      val scheduledEvents = service.getScheduledEventsForMultiplePrisoners(
        prisonCode,
        prisonerNumbers,
        today,
        timeSlot,
        appointmentCategoryMap(),
        appointmentLocationMap(),
      )

      verifyBlocking(prisonApiClient, never()) {
        getScheduledActivitiesForPrisonerNumbersAsync(any(), any(), any(), anyOrNull())
      }
      verifyBlocking(prisonApiClient) {
        getScheduledAppointmentsForPrisonerNumbersAsync(any(), any(), any(), anyOrNull())
      }

      with(scheduledEvents!!) {
        assertThat(this.prisonerNumbers).contains("G4793VF")
        assertThat(appointments).isNotNull
        assertThat(appointments).hasSize(1)

        appointments!!.map {
          assertThat(it.eventSource).isEqualTo("NOMIS")
          assertThat(it.prisonCode).isEqualTo("MDI")
          assertThat(it.prisonerNumber).isIn(prisonerNumbers)
          assertThat(it.bookingId).isEqualTo(bookingId)
          assertThat(it.eventId).isEqualTo(1)
          assertThat(it.eventType).isEqualTo(EventType.APPOINTMENT.name)
          assertThat(it.categoryCode).isEqualTo("GOVE")
          assertThat(it.cancelled).isFalse
          assertThat(it.summary).isEqualTo("Governor")
          assertThat(it.internalLocationDescription).isEqualTo("INTERVIEW ROOM")
          assertThat(it.date).isEqualTo(LocalDate.now())
          assertThat(it.startTime).isEqualTo(LocalTime.of(0, 0, 0))
          assertThat(it.endTime).isEqualTo(LocalTime.of(12, 0, 0))
          assertThat(it.priority).isEqualTo(EventType.APPOINTMENT.defaultPriority)
        }

        assertThat(visits).isNotNull
        assertThat(visits).hasSize(1)

        visits!!.map {
          assertThat(it.eventSource).isEqualTo("NOMIS")
          assertThat(it.eventType).isEqualTo(EventType.VISIT.name)
          assertThat(it.prisonCode).isEqualTo("MDI")
          assertThat(it.prisonerNumber).isIn(prisonerNumbers)
          assertThat(it.bookingId).isEqualTo(bookingId)
          assertThat(it.internalLocationId).isEqualTo(1)
          assertThat(it.internalLocationDescription).isEqualTo("INTERVIEW ROOM")
          assertThat(it.eventId).isEqualTo(1)
          assertThat(it.summary).isEqualTo("Visit")
          assertThat(it.date).isEqualTo(LocalDate.now())
          assertThat(it.startTime).isEqualTo(LocalTime.of(0, 0, 0))
          assertThat(it.endTime).isNull()
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
          assertThat(it.internalLocationId).isEqualTo(1)
          assertThat(it.internalLocationDescription).isEqualTo("Leeds Crown Court")
          assertThat(it.summary).isEqualTo("Court Appearance")
          assertThat(it.date).isEqualTo(LocalDate.now())
          assertThat(it.startTime).isEqualTo(LocalTime.of(0, 0, 0))
          assertThat(it.endTime).isNull()
          assertThat(it.priority).isEqualTo(EventType.COURT_HEARING.defaultPriority)
        }

        assertThat(activities).isNotNull
        assertThat(activities).hasSize(1)

        activities!!.map {
          assertThat(it.eventSource).isEqualTo("SAA")
          assertThat(it.eventType).isEqualTo(EventType.ACTIVITY.name)
          assertThat(it.eventId).isNull()
          assertThat(it.scheduledInstanceId).isEqualTo(activityEntity.scheduledInstanceId)
          assertThat(it.prisonerNumber).isIn(prisonerNumbers)
          assertThat(it.bookingId).isEqualTo(activityEntity.bookingId.toLong())
          assertThat(it.cancelled).isEqualTo(activityEntity.cancelled)
          assertThat(it.suspended).isEqualTo(activityEntity.suspended)
          assertThat(it.internalLocationId).isEqualTo(activityEntity.internalLocationId?.toLong())
          assertThat(it.internalLocationCode).isEqualTo(activityEntity.internalLocationCode)
          assertThat(it.internalLocationDescription).isEqualTo(activityEntity.internalLocationDescription)
          assertThat(it.summary).isEqualTo(activityEntity.scheduleDescription)
          assertThat(it.date).isEqualTo(activityEntity.sessionDate)
          assertThat(it.startTime).isEqualTo(activityEntity.startTime)
          assertThat(it.endTime).isEqualTo(activityEntity.endTime)
          assertThat(it.priority).isEqualTo(EventType.ACTIVITY.defaultPriority)
        }

        assertThat(externalTransfers).isNotNull
        assertThat(externalTransfers).hasSize(1)

        externalTransfers!!.map {
          assertThat(it.eventSource).isEqualTo("NOMIS")
          assertThat(it.eventType).isEqualTo(EventType.EXTERNAL_TRANSFER.name)
          assertThat(it.eventId).isEqualTo(1)
          assertThat(it.prisonerNumber).isIn(prisonerNumbers)
          assertThat(it.bookingId).isEqualTo(bookingId)
          assertThat(it.summary).isEqualTo("Transfer")
          assertThat(it.date).isEqualTo(LocalDate.now())
          assertThat(it.priority).isEqualTo(EventType.EXTERNAL_TRANSFER.defaultPriority)
        }

        adjudications!!.map {
          assertThat(it.eventSource).isEqualTo("NOMIS")
          assertThat(it.eventType).isEqualTo(EventType.ADJUDICATION_HEARING.name)
          assertThat(it.prisonerNumber).isIn(prisonerNumbers)
          assertThat(it.internalLocationId).isEqualTo(-2)
          assertThat(it.oicHearingId).isIn(-1L, -2L)
          assertThat(it.internalLocationDescription).isEqualTo("Adjudication room")
          assertThat(it.categoryDescription).isEqualTo("Governor's Hearing Adult")
          assertThat(it.summary).isEqualTo("Governor's Hearing Adult")
          assertThat(it.date).isEqualTo(LocalDate.now())
          assertThat(it.priority).isEqualTo(EventType.ADJUDICATION_HEARING.defaultPriority)
          assertThat(it.startTime).isEqualTo(LocalDate.now().atStartOfDay().toLocalTime())
          assertThat(it.endTime).isEqualTo(
            LocalDate.now().atStartOfDay().toLocalTime().plusHours(ADJUDICATION_HEARING_DURATION_TWO_HOURS),
          )
        }
      }
    }

    @Test
    fun `Events for tomorrow - excludes external transfers and court events - success`() {
      val prisonCode = "MDI"
      val prisonerNumbers = setOf("G4793VF", "G1234GK")
      val tomorrow = LocalDate.now().plusDays(1)
      val timeSlot: TimeSlot = TimeSlot.AM

      setupMultiplePrisonerApiMocks(prisonerNumbers, tomorrow, timeSlot)
      setupRolledOutPrisonMock(LocalDate.of(2022, 12, 22), LocalDate.of(2600, 12, 22))

      val activityEntity = activityFromDbInstance(sessionDate = tomorrow)
      whenever(
        prisonerScheduledActivityRepository.getScheduledActivitiesForPrisonerListAndDate(
          prisonCode,
          prisonerNumbers,
          tomorrow,
        ),
      )
        .thenReturn(listOf(activityEntity))

      whenever(prisonRegimeService.getEventPrioritiesForPrison(prisonCode))
        .thenReturn(EventPriorities(EventType.entries.associateWith { listOf(Priority(it.defaultPriority)) }))

      val scheduledEvents = service.getScheduledEventsForMultiplePrisoners(
        prisonCode,
        prisonerNumbers,
        tomorrow,
        timeSlot,
        appointmentCategoryMap(),
        appointmentLocationMap(),
      )

      verifyBlocking(prisonApiClient, never()) {
        getScheduledActivitiesForPrisonerNumbersAsync(any(), any(), any(), anyOrNull())
      }
      verifyBlocking(prisonApiClient) {
        getScheduledAppointmentsForPrisonerNumbersAsync(any(), any(), any(), anyOrNull())
      }

      with(scheduledEvents!!) {
        assertThat(this.prisonerNumbers).contains("G4793VF")

        assertThat(appointments).isNotNull
        assertThat(appointments).hasSize(1)
        appointments!!.map {
          assertThat(it.eventSource).isEqualTo("NOMIS")
          assertThat(it.eventType).isEqualTo(EventType.APPOINTMENT.name)
          assertThat(it.date).isEqualTo(tomorrow)
          assertThat(it.startTime).isEqualTo(LocalTime.MIDNIGHT)
          assertThat(it.priority).isEqualTo(EventType.APPOINTMENT.defaultPriority)
        }

        assertThat(visits).isNotNull
        assertThat(visits).hasSize(1)

        visits!!.map {
          assertThat(it.eventSource).isEqualTo("NOMIS")
          assertThat(it.eventType).isEqualTo(EventType.VISIT.name)
          assertThat(it.date).isEqualTo(tomorrow)
          assertThat(it.startTime).isEqualTo(LocalTime.MIDNIGHT)
          assertThat(it.priority).isEqualTo(EventType.VISIT.defaultPriority)
        }

        assertThat(courtHearings).isNotNull
        assertThat(courtHearings).hasSize(0)

        courtHearings!!.map {
          assertThat(it.eventSource).isEqualTo("NOMIS")
          assertThat(it.eventType).isEqualTo(EventType.COURT_HEARING.name)
          assertThat(it.date).isEqualTo(tomorrow)
          assertThat(it.date).isEqualTo(tomorrow)
          assertThat(it.startTime).isEqualTo(LocalTime.MIDNIGHT)
          assertThat(it.priority).isEqualTo(EventType.COURT_HEARING.defaultPriority)
        }

        assertThat(activities).isNotNull
        assertThat(activities).hasSize(1)

        activities!!.map {
          assertThat(it.eventSource).isEqualTo("SAA")
          assertThat(it.eventType).isEqualTo(EventType.ACTIVITY.name)
          assertThat(it.date).isEqualTo(tomorrow)
          assertThat(it.startTime).isEqualTo(LocalTime.of(10, 0, 0))
          assertThat(it.endTime).isEqualTo(LocalTime.of(11, 30, 0))
          assertThat(it.priority).isEqualTo(EventType.ACTIVITY.defaultPriority)
        }

        assertThat(adjudications).isNotNull
        assertThat(adjudications).hasSize(2)

        adjudications!!.map {
          assertThat(it.eventSource).isEqualTo("NOMIS")
          assertThat(it.eventType).isEqualTo(EventType.ADJUDICATION_HEARING.name)
          assertThat(it.date).isEqualTo(LocalDate.now())
          assertThat(it.startTime).isEqualTo(LocalDate.now().atStartOfDay().toLocalTime())
          assertThat(it.endTime).isEqualTo(
            LocalDate.now().atStartOfDay().toLocalTime().plusHours(ADJUDICATION_HEARING_DURATION_TWO_HOURS),
          )
          assertThat(it.priority).isEqualTo(EventType.ADJUDICATION_HEARING.defaultPriority)
        }

        assertThat(externalTransfers).isEmpty()
      }
    }
  }

  @Nested
  @DisplayName("Scheduled events - multiple prisoners - both activities and appointments are rolled out")
  inner class MultipleActivitiesBothActive {

    @BeforeEach
    fun `init`() {
      whenever(prisonRegimeService.getPrisonRegimeSlotForDayAndTime(any(), any(), any())).thenReturn(TimeSlot.AM)
    }

    @Test
    fun `Events for today - success - with appointments rolled out`() {
      val prisonCode = "MDI"
      val bookingId = 900001L
      val prisonerNumbers = setOf("G4793VF", "G1234GK")
      val today = LocalDate.now()
      val timeSlot: TimeSlot = TimeSlot.AM

      setupMultiplePrisonerApiMocks(prisonerNumbers, today, timeSlot)
      setupRolledOutPrisonMock(LocalDate.of(2022, 12, 22), LocalDate.of(2022, 12, 22))

      val activityEntity = activityFromDbInstance(sessionDate = today)
      whenever(
        prisonerScheduledActivityRepository.getScheduledActivitiesForPrisonerListAndDate(
          prisonCode,
          prisonerNumbers,
          today,
        ),
      )
        .thenReturn(listOf(activityEntity))

      val appointmentEntity = appointmentFromDbInstance(prisonerNumber = prisonerNumbers.first(), bookingId = bookingId)
      whenever(appointmentInstanceRepository.findByPrisonCodeAndPrisonerNumberAndDateAndTime(any(), any(), any(), any(), any()))
        .thenReturn(listOf(appointmentEntity))

      whenever(prisonRegimeService.getEventPrioritiesForPrison(prisonCode))
        .thenReturn(EventPriorities(EventType.values().associateWith { listOf(Priority(it.defaultPriority)) }))

      val scheduledEvents = service.getScheduledEventsForMultiplePrisoners(
        prisonCode,
        prisonerNumbers,
        today,
        timeSlot,
        appointmentCategoryMap(),
        appointmentLocationMap(),
      )

      verifyBlocking(prisonApiClient, never()) { getScheduledActivitiesForPrisonerNumbersAsync(any(), any(), any(), anyOrNull()) }
      verifyBlocking(prisonApiClient, never()) { getScheduledAppointmentsForPrisonerNumbersAsync(any(), any(), any(), anyOrNull()) }

      with(scheduledEvents!!) {
        assertThat(this.prisonerNumbers).contains("G4793VF")
        assertThat(appointments).isNotNull
        assertThat(appointments).hasSize(1)

        appointments!!.map {
          assertThat(it.eventSource).isEqualTo("SAA")
          assertThat(it.prisonCode).isEqualTo("MDI")
          assertThat(it.eventType).isEqualTo(EventType.APPOINTMENT.name)
          assertThat(it.prisonerNumber).isIn(prisonerNumbers)
          assertThat(it.eventId).isNull()
          assertThat(it.appointmentSeriesId).isEqualTo(appointmentEntity.appointmentSeriesId)
          assertThat(it.appointmentId).isEqualTo(appointmentEntity.appointmentId)
          assertThat(it.appointmentAttendeeId).isEqualTo(appointmentEntity.appointmentInstanceId)
          assertThat(it.categoryCode).isEqualTo(appointmentEntity.categoryCode)
          assertThat(it.categoryDescription).isEqualTo("Test Category")
          assertThat(it.internalLocationId).isEqualTo(101L)
          assertThat(it.internalLocationCode).isEqualTo("No information available")
          assertThat(it.internalLocationDescription).isEqualTo("Test Appointment Location User Description")
          assertThat(it.cancelled).isFalse
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
          assertThat(it.prisonerNumber).isIn(prisonerNumbers)
          assertThat(it.bookingId).isEqualTo(activityEntity.bookingId.toLong())
          assertThat(it.cancelled).isFalse
          assertThat(it.internalLocationId).isEqualTo(activityEntity.internalLocationId?.toLong())
          assertThat(it.internalLocationCode).isEqualTo(activityEntity.internalLocationCode)
          assertThat(it.internalLocationDescription).isEqualTo(activityEntity.internalLocationDescription)
          assertThat(it.summary).isEqualTo(activityEntity.scheduleDescription)
          assertThat(it.date).isEqualTo(activityEntity.sessionDate)
          assertThat(it.startTime).isEqualTo(LocalTime.of(10, 0, 0))
          assertThat(it.endTime).isEqualTo(LocalTime.of(11, 30, 0))
          assertThat(it.priority).isEqualTo(EventType.ACTIVITY.defaultPriority)
        }

        assertThat(externalTransfers).isNotNull
        assertThat(externalTransfers).hasSize(1)

        externalTransfers!!.map {
          assertThat(it.eventSource).isEqualTo("NOMIS")
          assertThat(it.eventType).isEqualTo(EventType.EXTERNAL_TRANSFER.name)
          assertThat(it.priority).isEqualTo(EventType.EXTERNAL_TRANSFER.defaultPriority)
        }

        assertThat(adjudications).isNotNull
        assertThat(adjudications).hasSize(2)

        adjudications!!.map {
          assertThat(it.eventSource).isEqualTo("NOMIS")
          assertThat(it.eventType).isEqualTo(EventType.ADJUDICATION_HEARING.name)
          assertThat(it.priority).isEqualTo(EventType.ADJUDICATION_HEARING.defaultPriority)
        }
      }
    }
  }

  @Nested
  @DisplayName("Scheduled events - hide sensitive future events")
  inner class ShowHideSensitiveEvents {
    val prisonCode = "MDI"
    val bookingId = 900001L
    val prisonerNumbers = setOf("G4793VF", "G1234GK")
    val tomorrow: LocalDate = LocalDate.now().plusDays(1)
    val timeSlot: TimeSlot = TimeSlot.AM

    @BeforeEach
    fun beforeEach() {
      setupRolledOutPrisonMock(LocalDate.of(2022, 12, 22), LocalDate.of(2022, 12, 22))

      val activityEntity = activityFromDbInstance(sessionDate = tomorrow)
      whenever(
        prisonerScheduledActivityRepository.getScheduledActivitiesForPrisonerListAndDate(
          prisonCode,
          prisonerNumbers,
          tomorrow,
        ),
      )
        .thenReturn(listOf(activityEntity))

      val appointmentEntity = appointmentFromDbInstance(prisonerNumber = prisonerNumbers.first(), bookingId = bookingId)
      whenever(appointmentInstanceRepository.findByPrisonCodeAndPrisonerNumberAndDateAndTime(any(), any(), any(), any(), any()))
        .thenReturn(listOf(appointmentEntity))

      whenever(prisonRegimeService.getEventPrioritiesForPrison(prisonCode))
        .thenReturn(EventPriorities(EventType.values().associateWith { listOf(Priority(it.defaultPriority)) }))
    }

    @Test
    fun `Should not fetch sensitive future events`() {
      setupMultiplePrisonerApiMocks(prisonerNumbers, tomorrow, timeSlot)

      service.getScheduledEventsForMultiplePrisoners(
        prisonCode,
        prisonerNumbers,
        tomorrow,
        timeSlot,
        appointmentCategoryMap(),
        appointmentLocationMap(),
      )

      // Should not retrieve sensitive events with future date ranges
      verifyBlocking(prisonApiClient, never()) {
        getScheduledCourtEventsForPrisonerNumbersAsync(prisonCode, prisonerNumbers, tomorrow, timeSlot)
      }
      verifyBlocking(prisonApiClient, never()) {
        getExternalTransfersOnDateAsync(prisonCode, prisonerNumbers, tomorrow)
      }
    }
  }
}
