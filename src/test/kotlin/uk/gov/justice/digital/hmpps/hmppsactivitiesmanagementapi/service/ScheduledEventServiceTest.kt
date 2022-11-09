package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.InmateDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.ScheduledEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import java.time.LocalDate

class ScheduledEventServiceTest {
  private val prisonApiClient: PrisonApiClient = mock()
  private val service = ScheduledEventService(prisonApiClient)

  @Test
  fun `getScheduledEventsByDateRange - success`() {

    val dateRange = LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))
    val schedAppointmentsMono: Mono<List<ScheduledEvent>> = Mono.just(listOf(PrisonApiScheduledEventFixture.instance()))
    val prisonerDetailsMono: Mono<InmateDetail> = Mono.just(InmateDetailFixture.instance())

    whenever(
      prisonApiClient.getPrisonerDetails("A11111A")
    ).thenReturn(prisonerDetailsMono)

    whenever(
      prisonApiClient.getScheduledAppointments(
        900001, dateRange
      )
    ).thenReturn(schedAppointmentsMono)

    val results = service.getScheduledEventsByDateRange(
      "MDI", "A11111A",
      LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))
    )

    assertThat(results).hasSize(1)
    assertThat(results[0].prisonerNumber).isEqualTo("A11111A")
  }
}
