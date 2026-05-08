package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.externalmovementsapi

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.RetryApiService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.externalmovementsapi.api.ExternalMovementsApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.externalmovementsapi.model.ExternalMovement
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.externalmovementsapi.model.ExternalMovementDescription
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.externalmovementsapi.model.ExternalMovementStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.externalmovementsapi.model.ExternalMovementsResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock.ExternalMovementsApiMockServer
import java.time.LocalDateTime

class ExternalMovementsApiClientTest {
  private lateinit var externalMovementsApiClient: ExternalMovementsApiClient

  companion object {
    @JvmField
    internal val externalMovementsApiMockServer = ExternalMovementsApiMockServer()

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      externalMovementsApiMockServer.start()
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      externalMovementsApiMockServer.stop()
    }
  }

  @BeforeEach
  fun resetStubs() {
    externalMovementsApiMockServer.resetAll()
    val webClient = WebClient.create("http://localhost:${externalMovementsApiMockServer.port()}")
    externalMovementsApiClient = ExternalMovementsApiClient(webClient, RetryApiService(3, 250))
  }

  @Test
  fun `getExternalMovements - success`(): Unit = runBlocking {
    val prisonCode = "NSI"
    val prisonerNumbers = listOf("A1234AA")
    val start = LocalDateTime.of(2026, 5, 1, 12, 0)
    val end = LocalDateTime.of(2026, 6, 30, 12, 0)

    val expectedResponse = ExternalMovementsResponse(
      content = listOf(
        ExternalMovement(
          id = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
          prisonerNumber = "A1234AA",
          description = ExternalMovementDescription(full = "Standard ROTL", short = "Accomodation-related", code = "FB"),
          start = LocalDateTime.of(2026, 5, 10, 9, 0),
          end = LocalDateTime.of(2026, 5, 10, 17, 0),
          status = ExternalMovementStatus(code = "SCHEDULED", description = "Scheduled"),
        ),
      ),
    )

    externalMovementsApiMockServer.stubGetExternalMovements(prisonCode, prisonerNumbers, start, end, expectedResponse)

    val response = externalMovementsApiClient.getExternalMovements(prisonCode, prisonerNumbers, start, end)

    with(response.content.single()) {
      prisonerNumber isEqualTo "A1234AA"
      id isEqualTo "3fa85f64-5717-4562-b3fc-2c963f66afa6"
      description.full isEqualTo "Standard ROTL"
      description.short isEqualTo "Accomodation-related"
      description.code isEqualTo "FB"
      this.start isEqualTo LocalDateTime.of(2026, 5, 10, 9, 0)
      this.end isEqualTo LocalDateTime.of(2026, 5, 10, 17, 0)
      status.code isEqualTo "SCHEDULED"
    }
  }

  @Test
  fun `getExternalMovements - empty response`(): Unit = runBlocking {
    val prisonCode = "NSI"
    val prisonerNumbers = listOf("A1234AA")
    val start = LocalDateTime.of(2026, 5, 1, 12, 0)
    val end = LocalDateTime.of(2026, 6, 30, 12, 0)

    externalMovementsApiMockServer.stubGetExternalMovements(prisonCode, prisonerNumbers, start, end, ExternalMovementsResponse(content = emptyList()))

    val response = externalMovementsApiClient.getExternalMovements(prisonCode, prisonerNumbers, start, end)

    assertThat(response.content).isEmpty()
  }

  @Test
  fun `getExternalMovements - multiple movements per prisoner`(): Unit = runBlocking {
    val prisonCode = "NSI"
    val prisonerNumbers = listOf("A1234AA")
    val start = LocalDateTime.of(2026, 5, 1, 12, 0)
    val end = LocalDateTime.of(2026, 6, 30, 12, 0)

    val expectedResponse = ExternalMovementsResponse(
      content = listOf(
        ExternalMovement(
          id = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
          prisonerNumber = "A1234AA",
          description = ExternalMovementDescription(full = "Standard ROTL", short = "Accomodation-related", code = "FB"),
          start = LocalDateTime.of(2026, 5, 10, 9, 0),
          end = LocalDateTime.of(2026, 5, 10, 17, 0),
          status = ExternalMovementStatus(code = "SCHEDULED", description = "Scheduled"),
        ),
        ExternalMovement(
          id = "3fa85f64-5717-7156-b3fc-c2963a63afb5",
          prisonerNumber = "A1234AA",
          description = ExternalMovementDescription(full = "Standard ROTL", short = "Accomodation-related", code = "FB"),
          start = LocalDateTime.of(2026, 5, 11, 9, 0),
          end = LocalDateTime.of(2026, 5, 11, 17, 0),
          status = ExternalMovementStatus(code = "SCHEDULED", description = "Scheduled"),
        ),
      ),
    )

    externalMovementsApiMockServer.stubGetExternalMovements(prisonCode, prisonerNumbers, start, end, expectedResponse)

    val response = externalMovementsApiClient.getExternalMovements(prisonCode, prisonerNumbers, start, end)

    assertThat(response.content)
      .extracting(ExternalMovement::prisonerNumber, { it.description.code }, ExternalMovement::start, ExternalMovement::end, { it.status.code })
      .containsExactly(
        tuple(
          "A1234AA",
          "FB",
          LocalDateTime.of(2026, 5, 10, 9, 0),
          LocalDateTime.of(2026, 5, 10, 17, 0),
          "SCHEDULED",
        ),
        tuple(
          "A1234AA",
          "FB",
          LocalDateTime.of(2026, 5, 11, 9, 0),
          LocalDateTime.of(2026, 5, 11, 17, 0),
          "SCHEDULED",
        ),
      )
  }

  @Test
  fun `getExternalMovements - multiple prisoners`(): Unit = runBlocking {
    val prisonCode = "NSI"
    val prisonerNumbers = listOf("A1234AA", "B1234BB")
    val start = LocalDateTime.of(2026, 5, 1, 12, 0)
    val end = LocalDateTime.of(2026, 6, 30, 12, 0)

    val expectedResponse = ExternalMovementsResponse(
      content = listOf(
        ExternalMovement(
          id = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
          prisonerNumber = "A1234AA",
          description = ExternalMovementDescription(full = "Standard ROTL", short = "Accomodation-related", code = "FB"),
          start = LocalDateTime.of(2026, 5, 10, 9, 0),
          end = LocalDateTime.of(2026, 5, 10, 17, 0),
          status = ExternalMovementStatus(code = "SCHEDULED", description = "Scheduled"),
        ),
        ExternalMovement(
          id = "3fa85f64-5717-7156-b3fc-c2963a63afb5",
          prisonerNumber = "A1234AA",
          description = ExternalMovementDescription(full = "Standard ROTL", short = "Accomodation-related", code = "FB"),
          start = LocalDateTime.of(2026, 5, 11, 9, 0),
          end = LocalDateTime.of(2026, 5, 11, 17, 0),
          status = ExternalMovementStatus(code = "SCHEDULED", description = "Scheduled"),
        ),
        ExternalMovement(
          id = "4af94f64-5717-7166-b3fc-c2963a63afb9",
          prisonerNumber = "B1234BB",
          description = ExternalMovementDescription(full = "Standard ROTL", short = "Other temporary release", code = "YOTR"),
          start = LocalDateTime.of(2026, 5, 5, 9, 0),
          end = LocalDateTime.of(2026, 5, 5, 17, 0),
          status = ExternalMovementStatus(code = "IN_PROGRESS", description = "In Progress"),
        ),
      ),
    )

    externalMovementsApiMockServer.stubGetExternalMovements(prisonCode, prisonerNumbers, start, end, expectedResponse)

    val response = externalMovementsApiClient.getExternalMovements(prisonCode, prisonerNumbers, start, end)

    assertThat(response.content)
      .extracting(ExternalMovement::prisonerNumber, { it.description.code }, ExternalMovement::start, ExternalMovement::end, { it.status.code })
      .containsExactly(
        tuple(
          "A1234AA",
          "FB",
          LocalDateTime.of(2026, 5, 10, 9, 0),
          LocalDateTime.of(2026, 5, 10, 17, 0),
          "SCHEDULED",
        ),
        tuple(
          "A1234AA",
          "FB",
          LocalDateTime.of(2026, 5, 11, 9, 0),
          LocalDateTime.of(2026, 5, 11, 17, 0),
          "SCHEDULED",
        ),
        tuple(
          "B1234BB",
          "YOTR",
          LocalDateTime.of(2026, 5, 5, 9, 0),
          LocalDateTime.of(2026, 5, 5, 17, 0),
          "IN_PROGRESS",
        ),
      )
  }

  @Nested
  @DisplayName("Retrying failed api calls")
  inner class RetryFailedCalls {
    val prisonCode = "NSI"
    val prisonerNumbers = listOf("A1234AA")
    val start = LocalDateTime.of(2026, 5, 1, 12, 0)
    val end = LocalDateTime.of(2026, 6, 30, 12, 0)
    val response = ExternalMovementsResponse(
      content = listOf(
        ExternalMovement(
          id = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
          prisonerNumber = "A1234AA",
          description = ExternalMovementDescription(full = "Standard ROTL", short = "Accomodation-related", code = "FB"),
          start = LocalDateTime.of(2026, 5, 10, 9, 0),
          end = LocalDateTime.of(2026, 5, 10, 17, 0),
          status = ExternalMovementStatus(code = "SCHEDULED", description = "Scheduled"),
        ),
      ),
    )

    @Test
    fun `will succeed if number of fails is less than maximum allowed`(): Unit = runBlocking {
      externalMovementsApiMockServer.stubGetExternalMovementsWithConnectionReset(prisonCode, prisonerNumbers, start, end, response)

      val result = externalMovementsApiClient.getExternalMovements(prisonCode, prisonerNumbers, start, end)
      assertThat(result.content).hasSize(1)
    }

    @Test
    fun `will succeed if number of fails is maximum allowed`(): Unit = runBlocking {
      externalMovementsApiMockServer.stubGetExternalMovementsWithConnectionReset(prisonCode, prisonerNumbers, start, end, response, numFails = 2)

      val result = externalMovementsApiClient.getExternalMovements(prisonCode, prisonerNumbers, start, end)
      assertThat(result.content).hasSize(1)
    }

    @Test
    fun `will fail if number of fails is more than maximum allowed`(): Unit = runBlocking {
      externalMovementsApiMockServer.stubGetExternalMovementsWithConnectionReset(prisonCode, prisonerNumbers, start, end, response, numFails = 3)

      assertThrows<WebClientRequestException> {
        externalMovementsApiClient.getExternalMovements(prisonCode, prisonerNumbers, start, end)
      }
    }
  }
}
