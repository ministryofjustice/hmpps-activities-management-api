package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.RolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transformActivityScheduleInstances
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.CourtHearings as PrisonApiCourtHearings
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.InmateDetail as PrisonApiInmateDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.ScheduledEvent as PrisonApiScheduledEvent

class ScheduledEventServiceTest {
  private val prisonApiClient: PrisonApiClient = mock()
  private val rolloutPrisonRepository: RolloutPrisonRepository = mock()
  private val scheduledInstanceService: ScheduledInstanceService = mock()
  private val service = ScheduledEventService(prisonApiClient, rolloutPrisonRepository, scheduledInstanceService)

  @Test
  fun `getScheduledEventsByDateRange (rolled out) - success`() {

    val dateRange = LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))
    val schedAppointmentsMono: Mono<List<PrisonApiScheduledEvent>> =
      Mono.just(listOf(PrisonApiScheduledEventFixture.appointmentInstance()))
    val schedVisitsMono: Mono<List<PrisonApiScheduledEvent>> =
      Mono.just(listOf(PrisonApiScheduledEventFixture.visitInstance()))
    val courtHearingsMono: Mono<PrisonApiCourtHearings> = Mono.just(PrisonApiCourtHearingsFixture.instance())
    val prisonerDetailsMono: Mono<PrisonApiInmateDetail> = Mono.just(InmateDetailFixture.instance())

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
      scheduledInstanceService.getActivityScheduleInstancesByDateRange(
        "MDI",
        "A11111A", dateRange
      )
    ).thenReturn(transformActivityScheduleInstances(listOf(ScheduledInstanceFixture.instance(id = 1, locationId = 22))))

    whenever(
      prisonApiClient.getPrisonerDetails("A11111A")
    ).thenReturn(prisonerDetailsMono)

    whenever(
      prisonApiClient.getScheduledAppointments(
        900001, dateRange
      )
    ).thenReturn(schedAppointmentsMono)

    whenever(
      prisonApiClient.getScheduledVisits(
        900001, dateRange
      )
    ).thenReturn(schedVisitsMono)

    whenever(
      prisonApiClient.getScheduledCourtHearings(
        900001, dateRange
      )
    ).thenReturn(courtHearingsMono)

    val result = service.getScheduledEventsByDateRange(
      "MDI", "A11111A", dateRange
    )

    verify(scheduledInstanceService, times(1)).getActivityScheduleInstancesByDateRange(any(), any(), any())
    verify(prisonApiClient, times(0)).getScheduledActivities(any(), any())

    with(result!!) {
      assertThat(prisonerNumber).isEqualTo("A11111A")
      assertThat(appointments).isNotNull
      assertThat(appointments).hasSize(1)
      assertThat(appointments!![0].prisonerNumber).isEqualTo("A11111A")
      assertThat(activities).isNotNull
      assertThat(activities).hasSize(1)
      assertThat(activities!![0].prisonerNumber).isEqualTo("A11111A")
      assertThat(visits).isNotNull
      assertThat(visits).hasSize(1)
      assertThat(visits!![0].prisonerNumber).isEqualTo("A11111A")
      assertThat(courtHearings).isNotNull
      assertThat(courtHearings).hasSize(1)
      assertThat(courtHearings!![0].prisonerNumber).isEqualTo("A11111A")
    }
  }

  @Test
  fun `getScheduledEventsByDateRange (not rolled out) - success`() {

    val dateRange = LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))
    val schedAppointmentsMono: Mono<List<PrisonApiScheduledEvent>> =
      Mono.just(listOf(PrisonApiScheduledEventFixture.appointmentInstance()))
    val schedActivitiesMono: Mono<List<PrisonApiScheduledEvent>> =
      Mono.just(listOf(PrisonApiScheduledEventFixture.activityInstance()))
    val schedVisitsMono: Mono<List<PrisonApiScheduledEvent>> =
      Mono.just(listOf(PrisonApiScheduledEventFixture.visitInstance()))
    val courtHearingsMono: Mono<PrisonApiCourtHearings> = Mono.just(PrisonApiCourtHearingsFixture.instance())
    val prisonerDetailsMono: Mono<PrisonApiInmateDetail> = Mono.just(InmateDetailFixture.instance())

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
      prisonApiClient.getPrisonerDetails("A11111A")
    ).thenReturn(prisonerDetailsMono)

    whenever(
      prisonApiClient.getScheduledAppointments(
        900001, dateRange
      )
    ).thenReturn(schedAppointmentsMono)

    whenever(
      prisonApiClient.getScheduledActivities(
        900001, dateRange
      )
    ).thenReturn(schedActivitiesMono)

    whenever(
      prisonApiClient.getScheduledVisits(
        900001, dateRange
      )
    ).thenReturn(schedVisitsMono)

    whenever(
      prisonApiClient.getScheduledCourtHearings(
        900001, dateRange
      )
    ).thenReturn(courtHearingsMono)

    val result = service.getScheduledEventsByDateRange(
      "MDI", "A11111A",
      LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))
    )

    verify(scheduledInstanceService, times(0)).getActivityScheduleInstancesByDateRange(any(), any(), any())
    verify(prisonApiClient, times(1)).getScheduledActivities(any(), any())

    with(result!!) {
      assertThat(prisonerNumber).isEqualTo("A11111A")
      assertThat(appointments).isNotNull
      assertThat(appointments).hasSize(1)
      assertThat(appointments!![0].prisonerNumber).isEqualTo("A11111A")
      assertThat(activities).isNotNull
      assertThat(activities).hasSize(1)
      assertThat(activities!![0].prisonerNumber).isEqualTo("A11111A")
      assertThat(visits).isNotNull
      assertThat(visits).hasSize(1)
      assertThat(visits!![0].prisonerNumber).isEqualTo("A11111A")
      assertThat(courtHearings).isNotNull
      assertThat(courtHearings).hasSize(1)
      assertThat(courtHearings!![0].prisonerNumber).isEqualTo("A11111A")
    }
  }

  @Test
  fun `getScheduledEventsByDateRange - prisoner details error`() {

    val prisonerDetailsMono: Mono<PrisonApiInmateDetail> = Mono.error(Exception("Error"))

    whenever(
      prisonApiClient.getPrisonerDetails("A11111A")
    ).thenReturn(prisonerDetailsMono)

    assertThatThrownBy {
      service.getScheduledEventsByDateRange(
        "MDI", "A11111A",
        LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))
      )
    }
      .isInstanceOf(Exception::class.java)
      .hasMessage("java.lang.Exception: Error")
  }

  @Test
  fun `getScheduledEventsByDateRange - prison api appointment details error`() {

    val dateRange = LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))
    val schedAppointmentsMono: Mono<List<PrisonApiScheduledEvent>> = Mono.error(Exception("Error"))
    val schedActivitiesMono: Mono<List<PrisonApiScheduledEvent>> =
      Mono.just(listOf(PrisonApiScheduledEventFixture.activityInstance()))
    val schedVisitsMono: Mono<List<PrisonApiScheduledEvent>> =
      Mono.just(listOf(PrisonApiScheduledEventFixture.visitInstance()))
    val courtHearingsMono: Mono<PrisonApiCourtHearings> = Mono.just(PrisonApiCourtHearingsFixture.instance())
    val prisonerDetailsMono: Mono<PrisonApiInmateDetail> = Mono.just(InmateDetailFixture.instance())

    whenever(
      prisonApiClient.getPrisonerDetails("A11111A")
    ).thenReturn(prisonerDetailsMono)

    whenever(
      prisonApiClient.getScheduledAppointments(
        900001, dateRange
      )
    ).thenReturn(schedAppointmentsMono)

    whenever(
      prisonApiClient.getScheduledActivities(
        900001, dateRange
      )
    ).thenReturn(schedActivitiesMono)

    whenever(
      prisonApiClient.getScheduledVisits(
        900001, dateRange
      )
    ).thenReturn(schedVisitsMono)

    whenever(
      prisonApiClient.getScheduledCourtHearings(
        900001, dateRange
      )
    ).thenReturn(courtHearingsMono)

    assertThatThrownBy {
      service.getScheduledEventsByDateRange(
        "MDI", "A11111A",
        LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))
      )
    }
      .isInstanceOf(Exception::class.java)
      .hasMessage("java.lang.Exception: Error")
  }

  @Test
  fun `getScheduledEventsByDateRange - prison api activities details error`() {

    val dateRange = LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))
    val schedAppointmentsMono: Mono<List<PrisonApiScheduledEvent>> =
      Mono.just(listOf(PrisonApiScheduledEventFixture.appointmentInstance()))
    val schedActivitiesMono: Mono<List<PrisonApiScheduledEvent>> = Mono.error(Exception("Error"))
    val schedVisitsMono: Mono<List<PrisonApiScheduledEvent>> =
      Mono.just(listOf(PrisonApiScheduledEventFixture.visitInstance()))
    val courtHearingsMono: Mono<PrisonApiCourtHearings> = Mono.just(PrisonApiCourtHearingsFixture.instance())
    val prisonerDetailsMono: Mono<PrisonApiInmateDetail> = Mono.just(InmateDetailFixture.instance())

    whenever(
      prisonApiClient.getPrisonerDetails("A11111A")
    ).thenReturn(prisonerDetailsMono)

    whenever(
      prisonApiClient.getScheduledAppointments(
        900001, dateRange
      )
    ).thenReturn(schedAppointmentsMono)

    whenever(
      prisonApiClient.getScheduledActivities(
        900001, dateRange
      )
    ).thenReturn(schedActivitiesMono)

    whenever(
      prisonApiClient.getScheduledVisits(
        900001, dateRange
      )
    ).thenReturn(schedVisitsMono)

    whenever(
      prisonApiClient.getScheduledCourtHearings(
        900001, dateRange
      )
    ).thenReturn(courtHearingsMono)

    assertThatThrownBy {
      service.getScheduledEventsByDateRange(
        "MDI", "A11111A",
        LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))
      )
    }
      .isInstanceOf(Exception::class.java)
      .hasMessage("java.lang.Exception: Error")
  }

  @Test
  fun `getScheduledEventsByDateRange - prison api visit details error`() {

    val dateRange = LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))
    val schedAppointmentsMono: Mono<List<PrisonApiScheduledEvent>> =
      Mono.just(listOf(PrisonApiScheduledEventFixture.appointmentInstance()))
    val schedActivitiesMono: Mono<List<PrisonApiScheduledEvent>> =
      Mono.just(listOf(PrisonApiScheduledEventFixture.activityInstance()))
    val schedVisitsMono: Mono<List<PrisonApiScheduledEvent>> = Mono.error(Exception("Error"))
    val courtHearingsMono: Mono<PrisonApiCourtHearings> = Mono.just(PrisonApiCourtHearingsFixture.instance())
    val prisonerDetailsMono: Mono<PrisonApiInmateDetail> = Mono.just(InmateDetailFixture.instance())

    whenever(
      prisonApiClient.getPrisonerDetails("A11111A")
    ).thenReturn(prisonerDetailsMono)

    whenever(
      prisonApiClient.getScheduledAppointments(
        900001, dateRange
      )
    ).thenReturn(schedAppointmentsMono)

    whenever(
      prisonApiClient.getScheduledActivities(
        900001, dateRange
      )
    ).thenReturn(schedActivitiesMono)

    whenever(
      prisonApiClient.getScheduledVisits(
        900001, dateRange
      )
    ).thenReturn(schedVisitsMono)

    whenever(
      prisonApiClient.getScheduledCourtHearings(
        900001, dateRange
      )
    ).thenReturn(courtHearingsMono)

    assertThatThrownBy {
      service.getScheduledEventsByDateRange(
        "MDI", "A11111A",
        LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))
      )
    }
      .isInstanceOf(Exception::class.java)
      .hasMessage("java.lang.Exception: Error")
  }

  @Test
  fun `getScheduledEventsByDateRange - prison api court hearings error`() {

    val dateRange = LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))
    val schedAppointmentsMono: Mono<List<PrisonApiScheduledEvent>> =
      Mono.just(listOf(PrisonApiScheduledEventFixture.appointmentInstance()))
    val schedActivitiesMono: Mono<List<PrisonApiScheduledEvent>> =
      Mono.just(listOf(PrisonApiScheduledEventFixture.activityInstance()))
    val schedVisitsMono: Mono<List<PrisonApiScheduledEvent>> =
      Mono.just(listOf(PrisonApiScheduledEventFixture.visitInstance()))
    val courtHearingsMono: Mono<PrisonApiCourtHearings> = Mono.error(Exception("Error"))
    val prisonerDetailsMono: Mono<PrisonApiInmateDetail> = Mono.just(InmateDetailFixture.instance())

    whenever(
      prisonApiClient.getPrisonerDetails("A11111A")
    ).thenReturn(prisonerDetailsMono)

    whenever(
      prisonApiClient.getScheduledAppointments(
        900001, dateRange
      )
    ).thenReturn(schedAppointmentsMono)

    whenever(
      prisonApiClient.getScheduledActivities(
        900001, dateRange
      )
    ).thenReturn(schedActivitiesMono)

    whenever(
      prisonApiClient.getScheduledVisits(
        900001, dateRange
      )
    ).thenReturn(schedVisitsMono)

    whenever(
      prisonApiClient.getScheduledCourtHearings(
        900001, dateRange
      )
    ).thenReturn(courtHearingsMono)

    assertThatThrownBy {
      service.getScheduledEventsByDateRange(
        "MDI", "A11111A",
        LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))
      )
    }
      .isInstanceOf(Exception::class.java)
      .hasMessage("java.lang.Exception: Error")
  }
}
