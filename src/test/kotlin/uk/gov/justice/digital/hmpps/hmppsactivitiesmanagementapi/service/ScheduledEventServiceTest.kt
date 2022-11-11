package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.CourtHearings as PrisonApiCourtHearings
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.InmateDetail as PrisonApiInmateDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.ScheduledEvent as PrisonApiScheduledEvent

class ScheduledEventServiceTest {
  private val prisonApiClient: PrisonApiClient = mock()
  private val service = ScheduledEventService(prisonApiClient)

  @Test
  fun `getScheduledEventsByDateRange - success`() {

    val dateRange = LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))
    val schedAppointmentsMono: Mono<List<PrisonApiScheduledEvent>> =
      Mono.just(listOf(PrisonApiScheduledEventFixture.instance()))
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
      prisonApiClient.getScheduledCourtHearings(
        900001, dateRange
      )
    ).thenReturn(courtHearingsMono)

    val result = service.getScheduledEventsByDateRange(
      "MDI", "A11111A",
      LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))
    )

    assertThat(result).isNotNull
    assertThat(result?.prisonerNumber).isEqualTo("A11111A")
    assertThat(result?.appointments).isNotNull
    assertThat(result?.appointments).hasSize(1)
    assertThat(result?.appointments!![0].prisonerNumber).isEqualTo("A11111A")
    assertThat(result.courtHearings).isNotNull
    assertThat(result.courtHearings).hasSize(1)
    assertThat(result.courtHearings!![0].prisonerNumber).isEqualTo("A11111A")
  }

  @Test
  fun `getScheduledEventsByDateRange - prisoner details error`() {

    val dateRange = LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))
    val schedAppointmentsMono: Mono<List<PrisonApiScheduledEvent>> =
      Mono.just(listOf(PrisonApiScheduledEventFixture.instance()))
    val courtHearingsMono: Mono<PrisonApiCourtHearings> = Mono.just(PrisonApiCourtHearingsFixture.instance())
    val prisonerDetailsMono: Mono<PrisonApiInmateDetail> = Mono.error(Exception("Error"))

    whenever(
      prisonApiClient.getPrisonerDetails("A11111A")
    ).thenReturn(prisonerDetailsMono)

    whenever(
      prisonApiClient.getScheduledAppointments(
        900001, dateRange
      )
    ).thenReturn(schedAppointmentsMono)

    whenever(
      prisonApiClient.getScheduledCourtHearings(
        900001, dateRange
      )
    ).thenReturn(courtHearingsMono)

    Assertions.assertThatThrownBy {
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
      prisonApiClient.getScheduledCourtHearings(
        900001, dateRange
      )
    ).thenReturn(courtHearingsMono)

    Assertions.assertThatThrownBy {
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
      Mono.just(listOf(PrisonApiScheduledEventFixture.instance()))
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
      prisonApiClient.getScheduledCourtHearings(
        900001, dateRange
      )
    ).thenReturn(courtHearingsMono)

    Assertions.assertThatThrownBy {
      service.getScheduledEventsByDateRange(
        "MDI", "A11111A",
        LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))
      )
    }
      .isInstanceOf(Exception::class.java)
      .hasMessage("java.lang.Exception: Error")
  }
}
