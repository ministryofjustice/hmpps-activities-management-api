package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock.PrisonApiMockServer
import java.time.LocalDate

class PrisonApiClientTest {

  private lateinit var prisonApiClient: PrisonApiClient

  companion object {
    @JvmField
    internal val prisonApiMockServer = PrisonApiMockServer()

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      prisonApiMockServer.start()
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      prisonApiMockServer.stop()
    }
  }

  @BeforeEach
  fun resetStubs() {
    prisonApiMockServer.resetAll()
    val webClient = WebClient.create("http://localhost:${prisonApiMockServer.port()}")
    prisonApiClient = PrisonApiClient(webClient)
  }

  @Test
  fun `getPrisonerDetails - success`() {
    val prisonerNumber = "G4793VF"

    prisonApiMockServer.stubGetPrisonerDetails(prisonerNumber)
    val prisonerDetails = prisonApiClient.getPrisonerDetails(prisonerNumber).block()
    assertThat(prisonerDetails).isNotNull
    assertThat(prisonerDetails!!.bookingId).isEqualTo(1200993)
  }

  @Test
  fun `getPrisonerDetails - not found`() {
    val prisonerNumber = "AAAAA"
    prisonApiMockServer.stubGetPrisonerDetailsNotFound(prisonerNumber)
    Assertions.assertThatThrownBy { prisonApiClient.getPrisonerDetails(prisonerNumber).block() }
      .isInstanceOf(WebClientResponseException::class.java)
      .hasMessage("404 Not Found from GET http://localhost:8999/api/bookings/offenderNo/AAAAA?fullInfo=true")
  }

  @Test
  fun `getScheduledAppointments - success`() {
    val bookingId = 10001L
    val dateRange = LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))
    prisonApiMockServer.stubGetScheduledAppointments(bookingId, dateRange.start, dateRange.endInclusive)
    val scheduledAppointments = prisonApiClient.getScheduledAppointments(bookingId, dateRange).block()
    assertThat(scheduledAppointments).hasSize(1)
    assertThat(scheduledAppointments!![0].bookingId).isEqualTo(1200993L)
  }

  @Test
  fun `getScheduledAppointments - not found`() {
    val bookingId = 0L
    val dateRange = LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))

    prisonApiMockServer.stubGetScheduledAppointmentsNotFound(bookingId, dateRange.start, dateRange.endInclusive)
    Assertions.assertThatThrownBy { prisonApiClient.getScheduledAppointments(bookingId, dateRange).block() }
      .isInstanceOf(WebClientResponseException::class.java)
      .hasMessage("404 Not Found from GET http://localhost:8999/api/bookings/0/appointments?startDate=2022-10-01&endDate=2022-11-05")
  }

  @Test
  fun `getCourtHearings - success`() {
    val bookingId = 10001L
    val dateRange = LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))

    prisonApiMockServer.stubGetCourtHearings(bookingId, dateRange.start, dateRange.endInclusive)
    val courtHearings = prisonApiClient.getScheduledCourtHearings(bookingId, dateRange).block()
    assertThat(courtHearings).isNotNull
    assertThat(courtHearings?.hearings).hasSize(4)
    assertThat(courtHearings?.hearings!![0].id).isEqualTo(460421101L)
  }

  @Test
  fun `getCourtHearings - not found`() {
    val bookingId = 0L
    val dateRange = LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))

    prisonApiMockServer.stubGetCourtHearingsNotFound(bookingId, dateRange.start, dateRange.endInclusive)
    Assertions.assertThatThrownBy { prisonApiClient.getScheduledCourtHearings(bookingId, dateRange).block() }
      .isInstanceOf(WebClientResponseException::class.java)
      .hasMessage("404 Not Found from GET http://localhost:8999/api/bookings/0/court-hearings?startDate=2022-10-01&endDate=2022-11-05")
  }
}
