package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.rangeTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.RolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.toModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import java.time.LocalDate
import java.time.LocalTime
import javax.persistence.EntityNotFoundException
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.CourtHearings as PrisonApiCourtHearings
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.InmateDetail as PrisonApiInmateDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.ScheduledEvent as PrisonApiScheduledEvent

class ScheduledEventServiceTest {
  private val dateRange = LocalDate.of(2022, 10, 1).rangeTo(LocalDate.of(2022, 11, 5))
  private val prisonApiClient: PrisonApiClient = mock()
  private val rolloutPrisonRepository: RolloutPrisonRepository = mock()
  private val scheduledInstanceService: ScheduledInstanceService = mock()
  private val prisonRegimeService: PrisonRegimeService = mock()
  private val service = ScheduledEventService(prisonApiClient, rolloutPrisonRepository, scheduledInstanceService, prisonRegimeService)

  @Test
  fun `getScheduledEventsForOffenderList (rolled out) - success`() {

    val prisonCode = "MDI"
    val prisonerNumbers = setOf("G4793VF")
    val date = LocalDate.of(2022, 12, 14)
    val timeSlot = TimeSlot.AM
    val schedAppointmentsMono = Mono.just(listOf(PrisonApiPrisonerScheduleFixture.appointmentInstance()))
    val schedVisitsMono = Mono.just(listOf(PrisonApiPrisonerScheduleFixture.visitInstance()))
    val courtEventsMono = Mono.just(listOf(PrisonApiPrisonerScheduleFixture.courtInstance()))

    whenever(prisonApiClient.getScheduledAppointmentsForPrisonerNumbers(prisonCode, prisonerNumbers, date, timeSlot.name)).thenReturn(schedAppointmentsMono)
    whenever(prisonApiClient.getScheduledVisitsForPrisonerNumbers(prisonCode, prisonerNumbers, date, timeSlot.name)).thenReturn(schedVisitsMono)
    whenever(prisonApiClient.getScheduledCourtEventsForPrisonerNumbers(prisonCode, prisonerNumbers, date, timeSlot.name)).thenReturn(courtEventsMono)

    val result = service.getScheduledEventsForOffenderList(prisonCode, prisonerNumbers, date, timeSlot)!!

    verify(prisonRegimeService).getEventPrioritiesForPrison(any())

    with(result) {
      assertThat(prisonerNumbers).contains("G4793VF")
      assertThat(appointments).isNotNull
      assertThat(appointments).hasSize(1)
      with(appointments!![0]) {
        assertThat(prisonCode).isEqualTo("MDI")
        assertThat(eventId).isNull()
        assertThat(bookingId).isNull()
        assertThat(location).isEqualTo("INTERVIEW ROOM")
        assertThat(locationId).isNull()
        assertThat(eventClass).isNull()
        assertThat(eventStatus).isNull()
        assertThat(eventType).isEqualTo("APPOINTMENT")
        assertThat(eventTypeDesc).isNull()
        assertThat(event).isEqualTo("GOVE")
        assertThat(eventDesc).isEqualTo("Governor")
        assertThat(details).isEqualTo("Dont be late")
        assertThat(prisonerNumber).isEqualTo("G4793VF")
        assertThat(date).isEqualTo(LocalDate.of(2022, 12, 14))
        assertThat(startTime).isEqualTo(LocalTime.of(17, 0, 0))
        assertThat(endTime).isEqualTo(LocalTime.of(18, 0, 0))
        assertThat(priority).isEqualTo(4)
      }

      assertThat(activities).isNull()
      assertThat(visits).isNotNull
      assertThat(visits).hasSize(1)
      with(visits!![0]) {
        assertThat(prisonerNumber).isEqualTo("G4793VF")
        assertThat(priority).isEqualTo(2)
        assertThat(prisonCode).isEqualTo("MDI")
        assertThat(eventId).isNull()
        assertThat(bookingId).isNull()
        assertThat(location).isEqualTo("INTERVIEW ROOM")
        assertThat(locationId).isNull()
        assertThat(eventClass).isNull()
        assertThat(eventStatus).isNull()
        assertThat(eventType).isEqualTo("VISIT")
        assertThat(eventTypeDesc).isNull()
        assertThat(event).isEqualTo("VISIT")
        assertThat(eventDesc).isEqualTo("Visit")
        assertThat(details).isNull()
        assertThat(prisonerNumber).isEqualTo("G4793VF")
        assertThat(date).isEqualTo(LocalDate.of(2022, 12, 14))
        assertThat(startTime).isEqualTo(LocalTime.of(14, 30, 0))
        assertThat(endTime).isNull()
        assertThat(priority).isEqualTo(2)
      }

      assertThat(courtHearings).isNotNull
      assertThat(courtHearings).hasSize(1)
      with(courtHearings!![0]) {
        assertThat(prisonerNumber).isEqualTo("G4793VF")
        assertThat(priority).isEqualTo(1)
        assertThat(prisonCode).isEqualTo("MDI")
        assertThat(eventId).isEqualTo(4444333L)
        assertThat(bookingId).isNull()
        assertThat(location).isNull()
        assertThat(locationId).isNull()
        assertThat(eventClass).isNull()
        assertThat(eventStatus).isEqualTo("EXP")
        assertThat(eventType).isEqualTo("COURT_HEARING")
        assertThat(eventTypeDesc).isNull()
        assertThat(event).isEqualTo("CRT")
        assertThat(eventDesc).isEqualTo("Court Appearance")
        assertThat(details).isNull()
        assertThat(prisonerNumber).isEqualTo("G4793VF")
        assertThat(date).isEqualTo(LocalDate.of(2022, 12, 14))
        assertThat(startTime).isEqualTo(LocalTime.of(10, 0, 0))
        assertThat(endTime).isNull()
        assertThat(priority).isEqualTo(1)
      }
    }
  }

  @Test
  fun `getScheduledEventsByDateRange (rolled out) - success`() {

    val schedAppointmentsMono = Mono.just(listOf(PrisonApiScheduledEventFixture.appointmentInstance()))
    val schedVisitsMono = Mono.just(listOf(PrisonApiScheduledEventFixture.visitInstance()))
    val courtHearingsMono = Mono.just(PrisonApiCourtHearingsFixture.instance())
    val prisonerDetailsMono = Mono.just(InmateDetailFixture.instance())

    whenever(
      rolloutPrisonRepository.findByCode("MDI")
    ).thenReturn(
      RolloutPrison(
        rolloutPrisonId = 10,
        code = "MDI",
        description = "Moorland",
        active = true
      )
    )

    whenever(
      prisonRegimeService.getEventPrioritiesForPrison("MDI")
    ).thenReturn(
      EventType.values().associateWith { listOf(Priority(it.defaultPriority)) }
    )

    whenever(
      scheduledInstanceService.getActivityScheduleInstancesByDateRange(
        "MDI",
        "A11111A", dateRange, null
      )
    ).thenReturn(listOf(ScheduledInstanceFixture.instance(id = 1, locationId = 22)).toModel())

    whenever(prisonApiClient.getPrisonerDetails("A11111A")).thenReturn(prisonerDetailsMono)
    whenever(prisonApiClient.getScheduledAppointments(900001, dateRange)).thenReturn(schedAppointmentsMono)
    whenever(prisonApiClient.getScheduledVisits(900001, dateRange)).thenReturn(schedVisitsMono)
    whenever(prisonApiClient.getScheduledCourtHearings(900001, dateRange)).thenReturn(courtHearingsMono)

    val result = service.getScheduledEventsByDateRange("MDI", "A11111A", dateRange)!!

    verify(scheduledInstanceService).getActivityScheduleInstancesByDateRange(any(), any(), any(), eq(null))
    verify(prisonApiClient, never()).getScheduledActivities(any(), any())
    verify(prisonRegimeService).getEventPrioritiesForPrison(any())

    with(result) {
      assertThat(prisonerNumbers).contains("A11111A")
      assertThat(appointments).isNotNull
      assertThat(appointments).hasSize(1)
      assertThat(appointments!![0].prisonerNumber).isEqualTo("A11111A")
      assertThat(appointments!![0].priority).isEqualTo(4)
      assertThat(activities).isNotNull
      assertThat(activities).hasSize(1)
      assertThat(activities!![0].prisonerNumber).isEqualTo("A11111A")
      assertThat(activities!![0].priority).isEqualTo(5)
      assertThat(visits).isNotNull
      assertThat(visits).hasSize(1)
      assertThat(visits!![0].prisonerNumber).isEqualTo("A11111A")
      assertThat(visits!![0].priority).isEqualTo(2)
      assertThat(courtHearings).isNotNull
      assertThat(courtHearings).hasSize(1)
      assertThat(courtHearings!![0].prisonerNumber).isEqualTo("A11111A")
      assertThat(courtHearings!![0].priority).isEqualTo(1)
    }
  }

  @Test
  fun `getScheduledEventsByDateRange (rolled out) - success with explicit default activity priority`() {

    val schedAppointmentsMono = Mono.just(listOf(PrisonApiScheduledEventFixture.appointmentInstance()))
    val schedVisitsMono = Mono.just(listOf(PrisonApiScheduledEventFixture.visitInstance()))
    val courtHearingsMono = Mono.just(PrisonApiCourtHearingsFixture.instance())
    val prisonerDetailsMono = Mono.just(InmateDetailFixture.instance())

    whenever(
      rolloutPrisonRepository.findByCode("MDI")
    ).thenReturn(
      RolloutPrison(
        rolloutPrisonId = 10,
        code = "MDI",
        description = "Moorland",
        active = true
      )
    )

    whenever(
      prisonRegimeService.getEventPrioritiesForPrison("MDI")
    ).thenReturn(
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
      )
    )

    whenever(
      scheduledInstanceService.getActivityScheduleInstancesByDateRange(
        "MDI",
        "A11111A", dateRange, null
      )
    ).thenReturn(listOf(ScheduledInstanceFixture.instance(id = 1, locationId = 22)).toModel())

    whenever(prisonApiClient.getPrisonerDetails("A11111A")).thenReturn(prisonerDetailsMono)
    whenever(prisonApiClient.getScheduledAppointments(900001, dateRange)).thenReturn(schedAppointmentsMono)
    whenever(prisonApiClient.getScheduledVisits(900001, dateRange)).thenReturn(schedVisitsMono)
    whenever(prisonApiClient.getScheduledCourtHearings(900001, dateRange)).thenReturn(courtHearingsMono)

    val result = service.getScheduledEventsByDateRange("MDI", "A11111A", dateRange)!!

    verify(scheduledInstanceService).getActivityScheduleInstancesByDateRange(any(), any(), any(), eq(null))
    verify(prisonApiClient, never()).getScheduledActivities(any(), any())
    verify(prisonRegimeService).getEventPrioritiesForPrison(any())

    with(result) {
      assertThat(appointments!![0].priority).isEqualTo(21)
      assertThat(activities!![0].priority).isEqualTo(8)
      assertThat(visits!![0].priority).isEqualTo(22)
      assertThat(courtHearings!![0].priority).isEqualTo(24)
    }
  }

  @Test
  fun `getScheduledEventsByDateRange (rolled out) - success with default activity priority`() {

    val schedAppointmentsMono = Mono.just(listOf(PrisonApiScheduledEventFixture.appointmentInstance()))
    val schedVisitsMono = Mono.just(listOf(PrisonApiScheduledEventFixture.visitInstance()))
    val courtHearingsMono = Mono.just(PrisonApiCourtHearingsFixture.instance())
    val prisonerDetailsMono = Mono.just(InmateDetailFixture.instance())

    whenever(
      rolloutPrisonRepository.findByCode("MDI")
    ).thenReturn(
      RolloutPrison(
        rolloutPrisonId = 10,
        code = "MDI",
        description = "Moorland",
        active = true
      )
    )

    whenever(
      prisonRegimeService.getEventPrioritiesForPrison("MDI")
    ).thenReturn(
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
      )
    )

    whenever(
      scheduledInstanceService.getActivityScheduleInstancesByDateRange(
        "MDI",
        "A11111A", dateRange, null
      )
    ).thenReturn(listOf(ScheduledInstanceFixture.instance(id = 1, locationId = 22)).toModel())

    whenever(prisonApiClient.getPrisonerDetails("A11111A")).thenReturn(prisonerDetailsMono)
    whenever(prisonApiClient.getScheduledAppointments(900001, dateRange)).thenReturn(schedAppointmentsMono)
    whenever(prisonApiClient.getScheduledVisits(900001, dateRange)).thenReturn(schedVisitsMono)
    whenever(prisonApiClient.getScheduledCourtHearings(900001, dateRange)).thenReturn(courtHearingsMono)

    val result = service.getScheduledEventsByDateRange("MDI", "A11111A", dateRange)!!

    verify(scheduledInstanceService).getActivityScheduleInstancesByDateRange(any(), any(), any(), eq(null))
    verify(prisonApiClient, never()).getScheduledActivities(any(), any())
    verify(prisonRegimeService).getEventPrioritiesForPrison(any())

    with(result) {
      assertThat(appointments!![0].priority).isEqualTo(4) // EventType.APPOINTMENT default
      assertThat(activities!![0].priority).isEqualTo(5) // EventType.ACTIVITY default
      assertThat(visits!![0].priority).isEqualTo(2) // EventType.VISIT default
      assertThat(courtHearings!![0].priority).isEqualTo(1) // EventType.COURT_HEARING default
    }
  }

  @Test
  fun `getScheduledEventsByDateRange (rolled out) - success with category priority`() {

    val schedAppointmentsMono = Mono.just(listOf(PrisonApiScheduledEventFixture.appointmentInstance()))
    val schedVisitsMono = Mono.just(listOf(PrisonApiScheduledEventFixture.visitInstance()))
    val courtHearingsMono = Mono.just(PrisonApiCourtHearingsFixture.instance())
    val prisonerDetailsMono = Mono.just(InmateDetailFixture.instance())

    whenever(
      rolloutPrisonRepository.findByCode("MDI")
    ).thenReturn(
      RolloutPrison(
        rolloutPrisonId = 10,
        code = "MDI",
        description = "Moorland",
        active = true
      )
    )

    whenever(
      prisonRegimeService.getEventPrioritiesForPrison("MDI")
    ).thenReturn(
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
      )
    )

    whenever(
      scheduledInstanceService.getActivityScheduleInstancesByDateRange(
        "MDI",
        "A11111A", dateRange, null
      )
    ).thenReturn(listOf(ScheduledInstanceFixture.instance(id = 1, locationId = 22, activityCategoryCode = "LEI")).toModel())

    whenever(prisonApiClient.getPrisonerDetails("A11111A")).thenReturn(prisonerDetailsMono)
    whenever(prisonApiClient.getScheduledAppointments(900001, dateRange)).thenReturn(schedAppointmentsMono)
    whenever(prisonApiClient.getScheduledVisits(900001, dateRange)).thenReturn(schedVisitsMono)
    whenever(prisonApiClient.getScheduledCourtHearings(900001, dateRange)).thenReturn(courtHearingsMono)

    val result = service.getScheduledEventsByDateRange("MDI", "A11111A", dateRange)!!

    verify(scheduledInstanceService).getActivityScheduleInstancesByDateRange(any(), any(), any(), eq(null))
    verify(prisonApiClient, never()).getScheduledActivities(any(), any())
    verify(prisonRegimeService).getEventPrioritiesForPrison(any())

    with(result) {
      assertThat(appointments!![0].priority).isEqualTo(4) // EventType.APPOINTMENT default
      assertThat(activities!![0].priority).isEqualTo(7) // EventType.LEISURE_SOCIAL
      assertThat(visits!![0].priority).isEqualTo(2) // EventType.VISIT default
      assertThat(courtHearings!![0].priority).isEqualTo(1) // EventType.COURT_HEARING default
    }
  }

  @Test
  fun `getScheduledEventsByDateRange (not rolled out) - success`() {

    val schedAppointmentsMono = Mono.just(listOf(PrisonApiScheduledEventFixture.appointmentInstance()))
    val schedActivitiesMono = Mono.just(listOf(PrisonApiScheduledEventFixture.activityInstance()))
    val schedVisitsMono = Mono.just(listOf(PrisonApiScheduledEventFixture.visitInstance()))
    val courtHearingsMono = Mono.just(PrisonApiCourtHearingsFixture.instance())
    val prisonerDetailsMono = Mono.just(InmateDetailFixture.instance())

    whenever(
      rolloutPrisonRepository.findByCode("MDI")
    ).thenReturn(
      RolloutPrison(
        rolloutPrisonId = 10,
        code = "MDI",
        description = "Moorland",
        active = false
      )
    )

    whenever(
      prisonRegimeService.getEventPrioritiesForPrison("MDI")
    ).thenReturn(
      EventType.values().associateWith { listOf(Priority(it.defaultPriority)) }
    )

    whenever(prisonApiClient.getPrisonerDetails("A11111A")).thenReturn(prisonerDetailsMono)
    whenever(prisonApiClient.getScheduledAppointments(900001, dateRange)).thenReturn(schedAppointmentsMono)
    whenever(prisonApiClient.getScheduledActivities(900001, dateRange)).thenReturn(schedActivitiesMono)
    whenever(prisonApiClient.getScheduledVisits(900001, dateRange)).thenReturn(schedVisitsMono)
    whenever(prisonApiClient.getScheduledCourtHearings(900001, dateRange)).thenReturn(courtHearingsMono)

    val result = service.getScheduledEventsByDateRange("MDI", "A11111A", dateRange)!!

    verify(scheduledInstanceService, never()).getActivityScheduleInstancesByDateRange(any(), any(), any(), eq(null))
    verify(prisonApiClient).getScheduledActivities(any(), any())
    verify(prisonRegimeService).getEventPrioritiesForPrison(any())

    with(result) {
      assertThat(prisonerNumbers).contains("A11111A")
      assertThat(appointments).isNotNull
      assertThat(appointments).hasSize(1)
      assertThat(appointments!![0].prisonerNumber).isEqualTo("A11111A")
      assertThat(appointments!![0].priority).isEqualTo(4)
      assertThat(activities).isNotNull
      assertThat(activities).hasSize(1)
      assertThat(activities!![0].prisonerNumber).isEqualTo("A11111A")
      assertThat(activities!![0].priority).isEqualTo(5)
      assertThat(visits).isNotNull
      assertThat(visits).hasSize(1)
      assertThat(visits!![0].prisonerNumber).isEqualTo("A11111A")
      assertThat(visits!![0].priority).isEqualTo(2)
      assertThat(courtHearings).isNotNull
      assertThat(courtHearings).hasSize(1)
      assertThat(courtHearings!![0].prisonerNumber).isEqualTo("A11111A")
      assertThat(courtHearings!![0].priority).isEqualTo(1)
    }
  }

  @Test
  fun `getScheduledEventsByDateRange (not rolled out) - success with category priority`() {

    val schedAppointmentsMono =
      Mono.just(listOf(PrisonApiScheduledEventFixture.appointmentInstance(eventSubType = "LACO")))
    val schedActivitiesMono = Mono.just(listOf(PrisonApiScheduledEventFixture.activityInstance()))
    val schedVisitsMono = Mono.just(listOf(PrisonApiScheduledEventFixture.visitInstance()))
    val courtHearingsMono = Mono.just(PrisonApiCourtHearingsFixture.instance())
    val prisonerDetailsMono = Mono.just(InmateDetailFixture.instance())

    whenever(
      rolloutPrisonRepository.findByCode("MDI")
    ).thenReturn(
      RolloutPrison(
        rolloutPrisonId = 10,
        code = "MDI",
        description = "Moorland",
        active = false
      )
    )

    whenever(
      prisonRegimeService.getEventPrioritiesForPrison("MDI")
    ).thenReturn(
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
      )
    )

    whenever(prisonApiClient.getPrisonerDetails("A11111A")).thenReturn(prisonerDetailsMono)
    whenever(prisonApiClient.getScheduledAppointments(900001, dateRange)).thenReturn(schedAppointmentsMono)
    whenever(prisonApiClient.getScheduledActivities(900001, dateRange)).thenReturn(schedActivitiesMono)
    whenever(prisonApiClient.getScheduledVisits(900001, dateRange)).thenReturn(schedVisitsMono)
    whenever(prisonApiClient.getScheduledCourtHearings(900001, dateRange)).thenReturn(courtHearingsMono)

    val result = service.getScheduledEventsByDateRange("MDI", "A11111A", dateRange)!!

    verify(scheduledInstanceService, never()).getActivityScheduleInstancesByDateRange(any(), any(), any(), eq(null))
    verify(prisonApiClient).getScheduledActivities(any(), any())
    verify(prisonRegimeService).getEventPrioritiesForPrison(any())

    with(result) {
      assertThat(appointments!![0].priority).isEqualTo(5) // EventType.APPOINTMENT EventCategory.INDUSTRIES "LACO"
      assertThat(activities!![0].priority).isEqualTo(99) // explicit default
      assertThat(visits!![0].priority).isEqualTo(2) // default
      assertThat(courtHearings!![0].priority).isEqualTo(1) // default
    }
  }

  @Test
  fun `getScheduledEventsByDateRange - prisoner details error`() {

    val prisonerDetailsMono: Mono<PrisonApiInmateDetail> = Mono.error(Exception("Error"))

    whenever(prisonApiClient.getPrisonerDetails("A11111A")).thenReturn(prisonerDetailsMono)

    assertThatThrownBy {
      service.getScheduledEventsByDateRange(
        "MDI", "A11111A",
        dateRange
      )
    }
      .isInstanceOf(Exception::class.java)
      .hasMessage("java.lang.Exception: Error")
  }

  @Test
  fun `getScheduledEventsByDateRange - prison code and prisoner number dont match`() {

    val schedAppointmentsMono = Mono.just(listOf(PrisonApiScheduledEventFixture.appointmentInstance()))
    val schedActivitiesMono = Mono.just(listOf(PrisonApiScheduledEventFixture.activityInstance()))
    val schedVisitsMono = Mono.just(listOf(PrisonApiScheduledEventFixture.visitInstance()))
    val courtHearingsMono = Mono.just(PrisonApiCourtHearingsFixture.instance())
    val prisonerDetailsMono = Mono.just(InmateDetailFixture.instance(agencyId = "PVI"))

    whenever(prisonApiClient.getPrisonerDetails("A11111A")).thenReturn(prisonerDetailsMono)
    whenever(prisonApiClient.getScheduledAppointments(900001, dateRange)).thenReturn(schedAppointmentsMono)
    whenever(prisonApiClient.getScheduledActivities(900001, dateRange)).thenReturn(schedActivitiesMono)
    whenever(prisonApiClient.getScheduledVisits(900001, dateRange)).thenReturn(schedVisitsMono)
    whenever(prisonApiClient.getScheduledCourtHearings(900001, dateRange)).thenReturn(courtHearingsMono)

    assertThatThrownBy {
      service.getScheduledEventsByDateRange(
        "MDI", "A11111A",
        dateRange
      )
    }
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Prisoner 'A11111A' not found in prison 'MDI'")
  }

  @Test
  fun `getScheduledEventsByDateRange - prison api appointment details error`() {

    val schedAppointmentsMono: Mono<List<PrisonApiScheduledEvent>> = Mono.error(Exception("Error"))
    val schedActivitiesMono = Mono.just(listOf(PrisonApiScheduledEventFixture.activityInstance()))
    val schedVisitsMono = Mono.just(listOf(PrisonApiScheduledEventFixture.visitInstance()))
    val courtHearingsMono = Mono.just(PrisonApiCourtHearingsFixture.instance())
    val prisonerDetailsMono = Mono.just(InmateDetailFixture.instance())

    whenever(prisonApiClient.getPrisonerDetails("A11111A")).thenReturn(prisonerDetailsMono)
    whenever(prisonApiClient.getScheduledAppointments(900001, dateRange)).thenReturn(schedAppointmentsMono)
    whenever(prisonApiClient.getScheduledActivities(900001, dateRange)).thenReturn(schedActivitiesMono)
    whenever(prisonApiClient.getScheduledVisits(900001, dateRange)).thenReturn(schedVisitsMono)
    whenever(prisonApiClient.getScheduledCourtHearings(900001, dateRange)).thenReturn(courtHearingsMono)

    assertThatThrownBy {
      service.getScheduledEventsByDateRange(
        "MDI", "A11111A",
        dateRange
      )
    }
      .isInstanceOf(Exception::class.java)
      .hasMessage("java.lang.Exception: Error")
  }

  @Test
  fun `getScheduledEventsByDateRange - prison api activities details error`() {

    val schedAppointmentsMono = Mono.just(listOf(PrisonApiScheduledEventFixture.appointmentInstance()))
    val schedActivitiesMono: Mono<List<PrisonApiScheduledEvent>> = Mono.error(Exception("Error"))
    val schedVisitsMono = Mono.just(listOf(PrisonApiScheduledEventFixture.visitInstance()))
    val courtHearingsMono = Mono.just(PrisonApiCourtHearingsFixture.instance())
    val prisonerDetailsMono = Mono.just(InmateDetailFixture.instance())

    whenever(prisonApiClient.getPrisonerDetails("A11111A")).thenReturn(prisonerDetailsMono)
    whenever(prisonApiClient.getScheduledAppointments(900001, dateRange)).thenReturn(schedAppointmentsMono)
    whenever(prisonApiClient.getScheduledActivities(900001, dateRange)).thenReturn(schedActivitiesMono)
    whenever(prisonApiClient.getScheduledVisits(900001, dateRange)).thenReturn(schedVisitsMono)
    whenever(prisonApiClient.getScheduledCourtHearings(900001, dateRange)).thenReturn(courtHearingsMono)

    assertThatThrownBy {
      service.getScheduledEventsByDateRange(
        "MDI", "A11111A",
        dateRange
      )
    }
      .isInstanceOf(Exception::class.java)
      .hasMessage("java.lang.Exception: Error")
  }

  @Test
  fun `getScheduledEventsByDateRange - prison api visit details error`() {

    val schedAppointmentsMono = Mono.just(listOf(PrisonApiScheduledEventFixture.appointmentInstance()))
    val schedActivitiesMono = Mono.just(listOf(PrisonApiScheduledEventFixture.activityInstance()))
    val schedVisitsMono: Mono<List<PrisonApiScheduledEvent>> = Mono.error(Exception("Error"))
    val courtHearingsMono = Mono.just(PrisonApiCourtHearingsFixture.instance())
    val prisonerDetailsMono = Mono.just(InmateDetailFixture.instance())

    whenever(prisonApiClient.getPrisonerDetails("A11111A")).thenReturn(prisonerDetailsMono)
    whenever(prisonApiClient.getScheduledAppointments(900001, dateRange)).thenReturn(schedAppointmentsMono)
    whenever(prisonApiClient.getScheduledActivities(900001, dateRange)).thenReturn(schedActivitiesMono)
    whenever(prisonApiClient.getScheduledVisits(900001, dateRange)).thenReturn(schedVisitsMono)
    whenever(prisonApiClient.getScheduledCourtHearings(900001, dateRange)).thenReturn(courtHearingsMono)

    assertThatThrownBy {
      service.getScheduledEventsByDateRange(
        "MDI", "A11111A",
        dateRange
      )
    }
      .isInstanceOf(Exception::class.java)
      .hasMessage("java.lang.Exception: Error")
  }

  @Test
  fun `getScheduledEventsByDateRange - prison api court hearings error`() {

    val schedAppointmentsMono = Mono.just(listOf(PrisonApiScheduledEventFixture.appointmentInstance()))
    val schedActivitiesMono = Mono.just(listOf(PrisonApiScheduledEventFixture.activityInstance()))
    val schedVisitsMono = Mono.just(listOf(PrisonApiScheduledEventFixture.visitInstance()))
    val courtHearingsMono: Mono<PrisonApiCourtHearings> = Mono.error(Exception("Error"))
    val prisonerDetailsMono: Mono<PrisonApiInmateDetail> = Mono.just(InmateDetailFixture.instance())

    whenever(prisonApiClient.getPrisonerDetails("A11111A")).thenReturn(prisonerDetailsMono)
    whenever(prisonApiClient.getScheduledAppointments(900001, dateRange)).thenReturn(schedAppointmentsMono)
    whenever(prisonApiClient.getScheduledActivities(900001, dateRange)).thenReturn(schedActivitiesMono)
    whenever(prisonApiClient.getScheduledVisits(900001, dateRange)).thenReturn(schedVisitsMono)
    whenever(prisonApiClient.getScheduledCourtHearings(900001, dateRange)).thenReturn(courtHearingsMono)

    assertThatThrownBy {
      service.getScheduledEventsByDateRange(
        "MDI", "A11111A",
        dateRange
      )
    }
      .isInstanceOf(Exception::class.java)
      .hasMessage("java.lang.Exception: Error")
  }
}
