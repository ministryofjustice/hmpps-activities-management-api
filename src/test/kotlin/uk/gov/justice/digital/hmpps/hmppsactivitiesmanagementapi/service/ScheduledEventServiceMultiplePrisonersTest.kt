package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.adjudications.AdjudicationsHearingAdapter
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.adjudications.Hearing
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.adjudications.HearingsResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.adjudications.ManageAdjudicationsApiFacade
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.externalmovementsapi.api.ExternalMovementsApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.externalmovementsapi.model.ExternalMovementsResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nomismapping.api.NomisDpsLocationMapping
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nomismapping.api.NomisMappingAPIClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalTimeRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.PrisonRegime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityFromDbInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.adjudicationHearing
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.externalMovement
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.RolloutPrisonPlan
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PrisonerScheduledActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonApiPrisonerScheduleFixture.activityInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonApiPrisonerScheduleFixture.appointmentInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonApiPrisonerScheduleFixture.courtInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonApiPrisonerScheduleFixture.transferInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonApiPrisonerScheduleFixture.visitInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.EventPriorities
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.Priority
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.PrisonRegimeService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.ADJUDICATION_HEARING_DURATION_TWO_HOURS
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

/*
 Tests for the ScheduledEventService focussing on the multiple prisoner methods and responses.
 */

class ScheduledEventServiceMultiplePrisonersTest {
  private val prisonApiClient: PrisonApiClient = mock()
  private val rolloutPrisonRepository: RolloutPrisonService = mock()
  private val prisonerScheduledActivityRepository: PrisonerScheduledActivityRepository = mock()
  private val appointmentInstanceRepository: AppointmentInstanceRepository = mock()
  private val prisonRegimeService: PrisonRegimeService = mock()
  private val manageAdjudicationsApiFacade: ManageAdjudicationsApiFacade = mock()
  private val nomisMappingAPIClient: NomisMappingAPIClient = mock()
  private val adjudicationsHearingAdapter = AdjudicationsHearingAdapter(
    manageAdjudicationsApiFacade = manageAdjudicationsApiFacade,
    nomisMappingAPIClient = nomisMappingAPIClient,
  )
  private val locationService: LocationService = mock()
  private val externalMovementsApiClient: ExternalMovementsApiClient = mock()

  private val service = ScheduledEventService(
    prisonApiClient,
    rolloutPrisonRepository,
    prisonerScheduledActivityRepository,
    appointmentInstanceRepository,
    prisonRegimeService,
    adjudicationsHearingAdapter,
    locationService,
    externalMovementsApiClient,
  )

  val now: LocalDateTime = LocalDate.now().atStartOfDay().plusHours(4)
  val locationUuid: UUID = UUID.randomUUID()

  private val prisonRegime = PrisonRegime(
    prisonCode = "",
    amStart = now.toLocalTime(),
    amFinish = now.toLocalTime(),
    pmStart = now.plusHours(4).toLocalTime(),
    pmFinish = now.toLocalTime().plusHours(5),
    edStart = now.plusHours(8).toLocalTime(),
    edFinish = now.plusHours(9).toLocalTime(),
    prisonRegimeDaysOfWeek =
    emptyList(),
  )

  @BeforeEach
  fun reset() {
    whenever(prisonRegimeService.getPrisonRegimesByDaysOfWeek(any())).thenReturn(
      mapOf(
        DayOfWeek.entries.toSet() to prisonRegime,
      ),
    )
    reset(
      prisonApiClient,
      rolloutPrisonRepository,
      prisonerScheduledActivityRepository,
      appointmentInstanceRepository,
      prisonRegimeService,
    )
  }

  // --- Private utility functions used to set up the mocked responses ---

  private fun setupRolledOutPrisonMock(
    activitiesRolledOut: Boolean,
    appointmentsRolledOut: Boolean,
    externalActivitiesRolledOut: Boolean,
    prisonLive: Boolean,
  ) {
    val prisonCode = "MDI"

    whenever(rolloutPrisonRepository.getByPrisonCode(prisonCode))
      .thenReturn(
        RolloutPrisonPlan(
          prisonCode = prisonCode,
          activitiesRolledOut = activitiesRolledOut,
          appointmentsRolledOut = appointmentsRolledOut,
          externalActivitiesRolledOut = externalActivitiesRolledOut,
          maxDaysToExpiry = 21,
          prisonLive = prisonLive,
        ),
      )
  }

  private fun setupMultiplePrisonerApiMocks(
    prisonerNumbers: Set<String>,
    date: LocalDate,
    timeSlot: TimeSlot?,
  ) {
    val prisonCode = "MDI"
    val activities = listOf(activityInstance(date = date))
    val appointments = listOf(appointmentInstance(date = date))
    val visits = listOf(visitInstance(date = date))
    val courtEvents = listOf(courtInstance(date = date))
    val transferEvents = listOf(transferInstance(date = date), transferInstance(date = date, startTime = null, endTime = null))
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
            locationUuid = locationUuid,
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

    whenever(nomisMappingAPIClient.getLocationMappingByDpsId(locationUuid))
      .thenReturn(NomisDpsLocationMapping(locationUuid, 1L))
  }

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
    dpsLocationId: UUID? = UUID.fromString("44444444-1111-2222-3333-444444444444"),
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
    dpsLocationId = dpsLocationId,
    customLocation = null,
    inCell = inCell,
    onWing = false,
    offWing = true,
    appointmentDate = appointmentDate,
    startTime = startTime,
    endTime = endTime,
    unlockNotes = null,
    extraInformation = comment,
    prisonerExtraInformation = null,
    createdTime = created,
    createdBy = createdBy,
    isCancelled = false,
    updatedTime = updated,
    updatedBy = updatedBy,
    seriesCancellationStartDate = null,
    seriesCancellationStartTime = null,
    seriesFrequency = null,
  )

  private fun appointmentCategories() = mapOf("TEST" to appointmentCategory("TEST"))

  @Nested
  @DisplayName("Scheduled events - multiple prisoners - activities rolled out, appointments are not")
  inner class MultipleWithActivitiesActiveAndAppointmentsNotActive {

    @BeforeEach
    fun `init`() {
      whenever(prisonRegimeService.getPrisonRegimesByDaysOfWeek(any())).thenReturn(
        mapOf(
          DayOfWeek.entries.toSet() to prisonRegime,
        ),
      )
    }

    @Test
    fun `Events for today - including transfers - success`() {
      val prisonCode = "MDI"
      val bookingId = 900001L
      val prisonerNumbers = setOf("G4793VF", "G1234GK")
      val today = LocalDate.now()
      val timeSlot: TimeSlot = TimeSlot.AM

      setupMultiplePrisonerApiMocks(prisonerNumbers, today, timeSlot)
      setupRolledOutPrisonMock(true, false, false, true)

      val activityEntity1 = activityFromDbInstance(sessionDate = today)
      val activityEntity2 = activityFromDbInstance(sessionDate = today, prisonerNumber = "B2222BB", attendanceStatus = null)

      whenever(
        prisonerScheduledActivityRepository.getScheduledActivitiesForPrisonerListAndDate(
          prisonCode,
          prisonerNumbers,
          today,
          timeSlot,
        ),
      )
        .thenReturn(listOf(activityEntity1, activityEntity2))

      whenever(prisonRegimeService.getEventPrioritiesForPrison(prisonCode))
        .thenReturn(EventPriorities(EventType.entries.associateWith { listOf(Priority(it.defaultPriority)) }))

      val scheduledEvents = service.getScheduledEventsForMultiplePrisoners(
        prisonCode,
        prisonerNumbers,
        today,
        timeSlot,
        appointmentCategories(),
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
          assertThat(it.scheduledInstanceId).isEqualTo(activityEntity1.scheduledInstanceId)
          assertThat(it.prisonerNumber).isIn(prisonerNumbers)
          assertThat(it.bookingId).isEqualTo(activityEntity1.bookingId)
          assertThat(it.cancelled).isEqualTo(activityEntity1.cancelled)
          assertThat(it.suspended).isEqualTo(activityEntity1.suspended)
          assertThat(it.internalLocationId).isEqualTo(activityEntity1.internalLocationId?.toLong())
          assertThat(it.internalLocationCode).isEqualTo(activityEntity1.internalLocationCode)
          assertThat(it.internalLocationDescription).isEqualTo(activityEntity1.internalLocationDescription)
          assertThat(it.summary).isEqualTo(activityEntity1.scheduleDescription)
          assertThat(it.date).isEqualTo(activityEntity1.sessionDate)
          assertThat(it.startTime).isEqualTo(activityEntity1.startTime)
          assertThat(it.endTime).isEqualTo(activityEntity1.endTime)
          assertThat(it.priority).isEqualTo(EventType.ACTIVITY.defaultPriority)
        }

        assertThat(externalTransfers).extracting("date", "startTime", "endTime").containsExactly(
          Tuple(today, LocalTime.of(0, 0), LocalTime.of(12, 0)),
          Tuple(today, null, null),
        )

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
          assertThat(it.internalLocationId).isEqualTo(1L)
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
      setupRolledOutPrisonMock(true, false, false, true)

      val activityEntity = activityFromDbInstance(sessionDate = tomorrow)
      whenever(
        prisonerScheduledActivityRepository.getScheduledActivitiesForPrisonerListAndDate(
          prisonCode,
          prisonerNumbers,
          tomorrow,
          timeSlot,
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
        appointmentCategories(),
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
      whenever(prisonRegimeService.getPrisonRegimesByDaysOfWeek(any())).thenReturn(
        mapOf(
          DayOfWeek.entries.toSet() to prisonRegime,
        ),
      )
    }

    @Test
    fun `Events for today - success - with appointments rolled out`() {
      val prisonCode = "MDI"
      val bookingId = 900001L
      val prisonerNumbers = setOf("G4793VF", "G1234GK")
      val today = LocalDate.now()
      val timeSlot: TimeSlot = TimeSlot.AM

      setupMultiplePrisonerApiMocks(prisonerNumbers, today, timeSlot)
      setupRolledOutPrisonMock(true, true, false, true)

      val activityEntity = activityFromDbInstance(sessionDate = today)
      whenever(
        prisonerScheduledActivityRepository.getScheduledActivitiesForPrisonerListAndDate(
          prisonCode,
          prisonerNumbers,
          today,
          timeSlot,
        ),
      )
        .thenReturn(listOf(activityEntity))

      val appointmentEntity = appointmentFromDbInstance(prisonerNumber = prisonerNumbers.first(), bookingId = bookingId)
      whenever(appointmentInstanceRepository.findByPrisonCodeAndPrisonerNumberAndDateAndTime(any(), any(), any(), any(), any()))
        .thenReturn(listOf(appointmentEntity))

      whenever(prisonRegimeService.getEventPrioritiesForPrison(prisonCode))
        .thenReturn(EventPriorities(EventType.entries.associateWith { listOf(Priority(it.defaultPriority)) }))

      whenever(locationService.getLocationDetailsForAppointmentsMap(prisonCode)).thenReturn(emptyMap())

      val scheduledEvents = service.getScheduledEventsForMultiplePrisoners(
        prisonCode,
        prisonerNumbers,
        today,
        timeSlot,
        appointmentCategories(),
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
          assertThat(it.internalLocationDescription).isEqualTo("No information available")
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
          assertThat(it.bookingId).isEqualTo(activityEntity.bookingId)
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

        assertThat(externalTransfers).extracting("date", "startTime", "endTime").containsExactly(
          Tuple(today, LocalTime.of(0, 0), LocalTime.of(12, 0)),
          Tuple(today, null, null),
        )

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
      setupRolledOutPrisonMock(true, true, false, true)
      whenever(prisonRegimeService.getPrisonRegimesByDaysOfWeek(any())).thenReturn(
        mapOf(
          DayOfWeek.entries.toSet() to prisonRegime,
        ),
      )
      val activityEntity = activityFromDbInstance(sessionDate = tomorrow)
      whenever(
        prisonerScheduledActivityRepository.getScheduledActivitiesForPrisonerListAndDate(
          prisonCode,
          prisonerNumbers,
          tomorrow,
          timeSlot,
        ),
      )
        .thenReturn(listOf(activityEntity))

      val appointmentEntity = appointmentFromDbInstance(prisonerNumber = prisonerNumbers.first(), bookingId = bookingId)
      whenever(appointmentInstanceRepository.findByPrisonCodeAndPrisonerNumberAndDateAndTime(any(), any(), any(), any(), any()))
        .thenReturn(listOf(appointmentEntity))

      whenever(prisonRegimeService.getEventPrioritiesForPrison(prisonCode))
        .thenReturn(EventPriorities(EventType.entries.associateWith { listOf(Priority(it.defaultPriority)) }))
    }

    @Test
    fun `Should not fetch sensitive future events`() {
      setupMultiplePrisonerApiMocks(prisonerNumbers, tomorrow, timeSlot)

      service.getScheduledEventsForMultiplePrisoners(
        prisonCode,
        prisonerNumbers,
        tomorrow,
        timeSlot,
        appointmentCategories(),
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

  @Nested
  @DisplayName("External movements from External Movements API for Unlock list")
  inner class ExternalMovementsFromExternalMovementsApiForUnlockList {
    private val prisonCode = "MDI"
    private val prisonerNumbers = setOf("G4793VF", "G1234GK")
    private val today = LocalDate.now()
    private val timeSlot: TimeSlot = TimeSlot.AM

    @BeforeEach
    fun beforeEach() {
      whenever(prisonRegimeService.getPrisonRegimesByDaysOfWeek(any())).thenReturn(
        mapOf(DayOfWeek.entries.toSet() to prisonRegime),
      )
      whenever(prisonRegimeService.getEventPrioritiesForPrison(prisonCode))
        .thenReturn(EventPriorities(EventType.entries.associateWith { listOf(Priority(it.defaultPriority)) }))
      setupMultiplePrisonerApiMocks(prisonerNumbers, today, timeSlot)
    }

    @ParameterizedTest(name = "includeExternalMovements = {0}")
    @ValueSource(booleans = [true, false])
    fun `outside work activities from DB are always filtered out for EA enabled prisons regardless of includeExternalMovements`(includeExternalMovements: Boolean) {
      setupRolledOutPrisonMock(
        activitiesRolledOut = true,
        appointmentsRolledOut = true,
        externalActivitiesRolledOut = true,
        prisonLive = true,
      )

      if (includeExternalMovements) {
        externalMovementsApiClient.stub {
          on {
            getExternalMovements(any(), any(), any(), any())
          } doReturn ExternalMovementsResponse(content = emptyList())
        }
      }

      val internalActivity = activityFromDbInstance(sessionDate = today, outsideWork = false)
      val externalActivity = activityFromDbInstance(sessionDate = today, outsideWork = true, scheduledInstanceId = 2)

      whenever(
        prisonerScheduledActivityRepository.getScheduledActivitiesForPrisonerListAndDate(
          prisonCode,
          prisonerNumbers,
          today,
          timeSlot,
        ),
      ).thenReturn(listOf(internalActivity, externalActivity))

      val activities = service.getScheduledEventsForMultiplePrisoners(
        prisonCode,
        prisonerNumbers,
        today,
        timeSlot,
        appointmentCategories(),
        includeExternalMovements = includeExternalMovements,
      )!!.activities!!

      with(activities.single { it.eventSource == "SAA" }) {
        assertThat(scheduledInstanceId).isEqualTo(internalActivity.scheduledInstanceId)
      }

      assertThat(
        activities.none { it.scheduledInstanceId == externalActivity.scheduledInstanceId },
      ).isTrue()
    }

    @Test
    fun `returns internal activities plus external activities from external movement API when EA is enabled and includeExternalMovements is true`() {
      setupRolledOutPrisonMock(
        activitiesRolledOut = true,
        appointmentsRolledOut = true,
        externalActivitiesRolledOut = true,
        prisonLive = true,
      )

      val internalActivity = activityFromDbInstance(sessionDate = today, outsideWork = false)
      whenever(
        prisonerScheduledActivityRepository.getScheduledActivitiesForPrisonerListAndDate(
          prisonCode,
          prisonerNumbers,
          today,
          timeSlot,
        ),
      ).thenReturn(listOf(internalActivity))

      val externalMovement = externalMovement()

      externalMovementsApiClient.stub {
        on {
          getExternalMovements(any(), any(), any(), any())
        } doReturn ExternalMovementsResponse(content = listOf(externalMovement))
      }

      val activities = service.getScheduledEventsForMultiplePrisoners(
        prisonCode,
        prisonerNumbers,
        today,
        timeSlot,
        appointmentCategories(),
        includeExternalMovements = true,
      )!!.activities!!

      val external = activities.single { it.eventSource == "EXTERNAL_MOVEMENTS_API" }
      with(external) {
        assertThat(categoryDescription).isNull()
        assertThat(categoryCode).isEqualTo("FB")
        assertThat(outsidePrison).isTrue()
        assertThat(startTime).isEqualTo(LocalTime.of(9, 0))
        assertThat(endTime).isEqualTo(LocalTime.of(17, 0))
        assertThat(status).isEqualTo("Scheduled")
      }

      with(activities.single { it.eventSource == "SAA" }) {
        assertThat(outsidePrison).isFalse()
      }

      verifyBlocking(externalMovementsApiClient) {
        getExternalMovements(
          prisonCode,
          prisonerNumbers,
          today.atStartOfDay(),
          today.plusDays(1).atStartOfDay(),
        )
      }
    }

    @Test
    fun `returns only internal activities when includeExternalMovements is false even if EA is enabled`() {
      setupRolledOutPrisonMock(
        activitiesRolledOut = true,
        appointmentsRolledOut = true,
        externalActivitiesRolledOut = true,
        prisonLive = true,
      )

      val internalActivity = activityFromDbInstance(sessionDate = today, outsideWork = false)
      whenever(
        prisonerScheduledActivityRepository.getScheduledActivitiesForPrisonerListAndDate(
          prisonCode,
          prisonerNumbers,
          today,
          timeSlot,
        ),
      ).thenReturn(listOf(internalActivity))

      val activities = service.getScheduledEventsForMultiplePrisoners(
        prisonCode,
        prisonerNumbers,
        today,
        timeSlot,
        appointmentCategories(),
        includeExternalMovements = false,
      )!!.activities!!

      val internal = activities.single { it.eventSource == "SAA" }
      with(internal) {
        assertThat(eventSource).isEqualTo("SAA")
        assertThat(outsidePrison).isFalse()
      }

      verifyNoInteractions(externalMovementsApiClient)
    }

    @Test
    fun `external activities are requested with AM time slot boundaries`() {
      setupRolledOutPrisonMock(
        activitiesRolledOut = true,
        appointmentsRolledOut = true,
        externalActivitiesRolledOut = true,
        prisonLive = true,
      )

      whenever(
        prisonerScheduledActivityRepository.getScheduledActivitiesForPrisonerListAndDate(
          prisonCode,
          prisonerNumbers,
          today,
          TimeSlot.AM,
        ),
      ).thenReturn(emptyList())

      whenever(prisonRegimeService.getTimeRangeForPrisonAndTimeSlot(prisonCode, TimeSlot.AM, today.dayOfWeek))
        .thenReturn(LocalTimeRange(LocalTime.of(0, 0), LocalTime.of(12, 0)))

      val amMovement = externalMovement(start = today.atTime(8, 30))

      externalMovementsApiClient.stub {
        on { getExternalMovements(any(), any(), any(), any()) } doReturn ExternalMovementsResponse(
          content = listOf(amMovement),
        )
      }

      service.getScheduledEventsForMultiplePrisoners(
        prisonCode,
        prisonerNumbers,
        today,
        TimeSlot.AM,
        appointmentCategories(),
        includeExternalMovements = true,
      )

      verifyBlocking(externalMovementsApiClient) {
        getExternalMovements(
          prisonCode,
          prisonerNumbers,
          today.atStartOfDay(),
          today.atTime(LocalTime.of(12, 0)),
        )
      }
    }

    @Test
    fun `external activities are requested with PM time slot boundaries`() {
      setupRolledOutPrisonMock(
        activitiesRolledOut = true,
        appointmentsRolledOut = true,
        externalActivitiesRolledOut = true,
        prisonLive = true,
      )

      setupMultiplePrisonerApiMocks(prisonerNumbers, today, TimeSlot.PM)

      whenever(
        prisonerScheduledActivityRepository.getScheduledActivitiesForPrisonerListAndDate(
          prisonCode,
          prisonerNumbers,
          today,
          TimeSlot.PM,
        ),
      ).thenReturn(emptyList())

      whenever(prisonRegimeService.getTimeRangeForPrisonAndTimeSlot(prisonCode, TimeSlot.PM, today.dayOfWeek))
        .thenReturn(LocalTimeRange(LocalTime.of(12, 0), LocalTime.of(17, 0)))

      val pmMovement = externalMovement(start = today.atTime(14, 0), code = "YOTR")

      externalMovementsApiClient.stub {
        on { getExternalMovements(any(), any(), any(), any()) } doReturn ExternalMovementsResponse(
          content = listOf(pmMovement),
        )
      }

      service.getScheduledEventsForMultiplePrisoners(
        prisonCode,
        prisonerNumbers,
        today,
        TimeSlot.PM,
        appointmentCategories(),
        includeExternalMovements = true,
      )

      verifyBlocking(externalMovementsApiClient) {
        getExternalMovements(
          prisonCode,
          prisonerNumbers,
          today.atTime(LocalTime.of(12, 0)),
          today.atTime(LocalTime.of(17, 0)),
        )
      }
    }

    @Test
    fun `external activities are requested with full day boundaries when no time slot specified`() {
      setupRolledOutPrisonMock(
        activitiesRolledOut = true,
        appointmentsRolledOut = true,
        externalActivitiesRolledOut = true,
        prisonLive = true,
      )

      setupMultiplePrisonerApiMocks(prisonerNumbers, today, null)

      whenever(
        prisonerScheduledActivityRepository.getScheduledActivitiesForPrisonerListAndDate(
          prisonCode,
          prisonerNumbers,
          today,
          null,
        ),
      ).thenReturn(emptyList())

      val amMovement = externalMovement(start = today.atTime(8, 30))
      val pmMovement = externalMovement(start = today.atTime(14, 0), code = "YOTR")
      val edMovement = externalMovement(start = today.atTime(18, 0), code = "20")

      externalMovementsApiClient.stub {
        on { getExternalMovements(any(), any(), any(), any()) } doReturn ExternalMovementsResponse(
          content = listOf(amMovement, pmMovement, edMovement),
        )
      }

      val activities = service.getScheduledEventsForMultiplePrisoners(
        prisonCode,
        prisonerNumbers,
        today,
        null,
        appointmentCategories(),
        includeExternalMovements = true,
      )!!.activities!!

      assertThat(activities).hasSize(3)

      verifyBlocking(externalMovementsApiClient) {
        getExternalMovements(
          prisonCode,
          prisonerNumbers,
          today.atStartOfDay(),
          today.plusDays(1).atStartOfDay(),
        )
      }
    }

    @Test
    fun `external activities request uses start and end times from prison regime`() {
      setupRolledOutPrisonMock(
        activitiesRolledOut = true,
        appointmentsRolledOut = true,
        externalActivitiesRolledOut = true,
        prisonLive = true,
      )

      setupMultiplePrisonerApiMocks(prisonerNumbers, today, TimeSlot.ED)

      whenever(
        prisonerScheduledActivityRepository.getScheduledActivitiesForPrisonerListAndDate(
          prisonCode,
          prisonerNumbers,
          today,
          TimeSlot.ED,
        ),
      ).thenReturn(emptyList())

      whenever(prisonRegimeService.getTimeRangeForPrisonAndTimeSlot(prisonCode, TimeSlot.ED, today.dayOfWeek))
        .thenReturn(LocalTimeRange(LocalTime.of(17, 0), LocalTime.of(21, 30)))

      externalMovementsApiClient.stub {
        on { getExternalMovements(any(), any(), any(), any()) } doReturn ExternalMovementsResponse(
          content = emptyList(),
        )
      }

      service.getScheduledEventsForMultiplePrisoners(
        prisonCode,
        prisonerNumbers,
        today,
        TimeSlot.ED,
        appointmentCategories(),
        includeExternalMovements = true,
      )

      verifyBlocking(externalMovementsApiClient) {
        getExternalMovements(
          prisonCode,
          prisonerNumbers,
          today.atTime(LocalTime.of(17, 0)),
          today.atTime(LocalTime.of(21, 30)),
        )
      }
    }

    @ParameterizedTest(name = "includeExternalMovements = {0}")
    @ValueSource(booleans = [true, false])
    fun `external movements API is not called and outside work activities from DB are included when EA is not rolled out`(
      includeExternalMovements: Boolean,
    ) {
      setupRolledOutPrisonMock(
        activitiesRolledOut = true,
        appointmentsRolledOut = true,
        externalActivitiesRolledOut = false,
        prisonLive = true,
      )

      val internalActivity = activityFromDbInstance(sessionDate = today, outsideWork = false)
      val outsideWorkActivity = activityFromDbInstance(sessionDate = today, outsideWork = true, scheduledInstanceId = 2)

      whenever(
        prisonerScheduledActivityRepository.getScheduledActivitiesForPrisonerListAndDate(
          prisonCode,
          prisonerNumbers,
          today,
          timeSlot,
        ),
      ).thenReturn(listOf(internalActivity, outsideWorkActivity))

      val activities = service.getScheduledEventsForMultiplePrisoners(
        prisonCode,
        prisonerNumbers,
        today,
        timeSlot,
        appointmentCategories(),
        includeExternalMovements = includeExternalMovements,
      )!!.activities!!

      assertThat(activities.map { it.scheduledInstanceId }).containsExactlyInAnyOrder(
        internalActivity.scheduledInstanceId,
        outsideWorkActivity.scheduledInstanceId,
      )

      verifyNoInteractions(externalMovementsApiClient)
    }
  }

  @Nested
  @DisplayName("getExternalMovementsForMultiplePrisonersForMovementList")
  inner class GetExternalMovementsForMultiplePrisonersForMovementList {

    private val prisonCode = "MDI"
    private val prisonerNumbers = setOf("G4793VF", "A1234AA")
    private val today = LocalDate.now()
    private val timeSlot: TimeSlot = TimeSlot.AM

    @BeforeEach
    fun beforeEach() {
      whenever(prisonRegimeService.getEventPrioritiesForPrison(prisonCode))
        .thenReturn(EventPriorities(EventType.entries.associateWith { listOf(Priority(it.defaultPriority)) }))
      setupMultiplePrisonerApiMocks(prisonerNumbers, today, timeSlot)
    }

    @Test
    fun `returns external movements wrapped in LocationEvents`() {
      val externalMovement = externalMovement(prisonerNumber = "G4793VF")

      externalMovementsApiClient.stub {
        on {
          getExternalMovements(any(), any(), any(), any())
        } doReturn ExternalMovementsResponse(content = listOf(externalMovement))
      }

      val result = service.getExternalMovementsForMultiplePrisoners(prisonCode, prisonerNumbers, today, null)

      with(result.single()) {
        assertThat(id).isNull()
        assertThat(dpsLocationId).isNull()
        assertThat(prisonCode).isEqualTo("MDI")
        assertThat(code).isEqualTo("OUTSIDE")
        assertThat(description).isEqualTo("Outside")
        with(events.single()) {
          assertThat(prisonerNumber).isEqualTo("G4793VF")
          assertThat(eventSource).isEqualTo("EXTERNAL_MOVEMENTS_API")
          assertThat(outsidePrison).isTrue()
          assertThat(categoryDescription).isNull()
          assertThat(categoryCode).isEqualTo("FB")
          assertThat(summary).isEqualTo("Accommodation-related ROTL")
          assertThat(date).isEqualTo(today)
          assertThat(startTime).isEqualTo(LocalTime.of(9, 0))
          assertThat(endTime).isEqualTo(LocalTime.of(17, 0))
          assertThat(status).isEqualTo("Scheduled")
        }
      }
    }

    @Test
    fun `groups multiple external movements under one outside location`() {
      val movement1 = externalMovement(
        prisonerNumber = "G4793VF",
        start = today.atTime(9, 0),
        end = today.atTime(11, 0),
      )

      val movement2 = externalMovement(
        prisonerNumber = "A1234AA",
        start = today.atTime(13, 0),
        end = today.atTime(15, 0),
      )

      externalMovementsApiClient.stub {
        on {
          getExternalMovements(any(), any(), any(), any())
        } doReturn ExternalMovementsResponse(content = listOf(movement1, movement2))
      }

      val externalMovements = service.getExternalMovementsForMultiplePrisoners(
        prisonCode,
        prisonerNumbers,
        today,
        null,
      )

      with(externalMovements.single()) {
        assertThat(code).isEqualTo("OUTSIDE")
        assertThat(description).isEqualTo("Outside")
        assertThat(events).hasSize(2)

        assertThat(events.map { it.prisonerNumber })
          .containsExactlyInAnyOrder("G4793VF", "A1234AA")
      }
    }

    @Test
    fun `returns empty set when no external movements found`() {
      externalMovementsApiClient.stub {
        on {
          getExternalMovements(any(), any(), any(), any())
        } doReturn ExternalMovementsResponse(content = emptyList())
      }

      val externalMovements = service.getExternalMovementsForMultiplePrisoners(prisonCode, prisonerNumbers, today, null)

      assertThat(externalMovements).isEmpty()
    }

    @Test
    fun `external movements are requested with AM time slot boundaries when provided`() {
      whenever(prisonRegimeService.getTimeRangeForPrisonAndTimeSlot(prisonCode, TimeSlot.AM, today.dayOfWeek))
        .thenReturn(LocalTimeRange(LocalTime.of(8, 0), LocalTime.of(12, 0)))

      val amMovement = externalMovement(prisonerNumber = "G4793VF", start = today.atTime(9, 0), end = today.atTime(11, 0))

      externalMovementsApiClient.stub {
        on {
          getExternalMovements(any(), any(), any(), any())
        } doReturn ExternalMovementsResponse(content = listOf(amMovement))
      }

      val externalMovements = service.getExternalMovementsForMultiplePrisoners(prisonCode, prisonerNumbers, today, TimeSlot.AM)

      assertThat(externalMovements.single().events.single().prisonerNumber).isEqualTo("G4793VF")

      verifyBlocking(externalMovementsApiClient) {
        getExternalMovements(
          prisonCode,
          prisonerNumbers,
          today.atTime(8, 0),
          today.atTime(12, 0),
        )
      }
    }

    @Test
    fun `external movements are requested with PM time slot boundaries when provided`() {
      whenever(prisonRegimeService.getTimeRangeForPrisonAndTimeSlot(prisonCode, TimeSlot.PM, today.dayOfWeek))
        .thenReturn(LocalTimeRange(LocalTime.of(12, 0), LocalTime.of(17, 0)))

      val pmMovement = externalMovement(prisonerNumber = "G4793VF", start = today.atTime(14, 0), end = today.atTime(16, 0))

      externalMovementsApiClient.stub {
        on {
          getExternalMovements(any(), any(), any(), any())
        } doReturn ExternalMovementsResponse(content = listOf(pmMovement))
      }

      val externalMovements = service.getExternalMovementsForMultiplePrisoners(prisonCode, prisonerNumbers, today, TimeSlot.PM)

      assertThat(externalMovements.single().events.single().prisonerNumber).isEqualTo("G4793VF")

      verifyBlocking(externalMovementsApiClient) {
        getExternalMovements(
          prisonCode,
          prisonerNumbers,
          today.atTime(12, 0),
          today.atTime(17, 0),
        )
      }
    }

    @Test
    fun `external movements are requested with ED time slot boundaries when provided`() {
      whenever(prisonRegimeService.getTimeRangeForPrisonAndTimeSlot(prisonCode, TimeSlot.ED, today.dayOfWeek))
        .thenReturn(LocalTimeRange(LocalTime.of(17, 0), LocalTime.of(21, 30)))

      externalMovementsApiClient.stub {
        on {
          getExternalMovements(any(), any(), any(), any())
        } doReturn ExternalMovementsResponse(content = emptyList())
      }

      val externalMovements = service.getExternalMovementsForMultiplePrisoners(prisonCode, prisonerNumbers, today, TimeSlot.ED)

      assertThat(externalMovements).isEmpty()

      verifyBlocking(externalMovementsApiClient) {
        getExternalMovements(
          prisonCode,
          prisonerNumbers,
          today.atTime(17, 0),
          today.atTime(21, 30),
        )
      }
    }

    @Test
    fun `uses full day range when no time slot provided and does not lookup time range`() {
      externalMovementsApiClient.stub {
        on {
          getExternalMovements(any(), any(), any(), any())
        } doReturn ExternalMovementsResponse(content = emptyList())
      }

      service.getExternalMovementsForMultiplePrisoners(prisonCode, prisonerNumbers, today, null)

      verifyBlocking(externalMovementsApiClient) {
        getExternalMovements(
          prisonCode,
          prisonerNumbers,
          today.atStartOfDay(),
          today.plusDays(1).atStartOfDay(),
        )
      }

      verify(prisonRegimeService, never())
        .getTimeRangeForPrisonAndTimeSlot(any(), any(), any())
    }
  }
}
