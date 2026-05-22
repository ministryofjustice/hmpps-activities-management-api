package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.externalmovementsapi.model.ExternalMovement
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.externalmovementsapi.model.ExternalMovementDescription
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.externalmovementsapi.model.ExternalMovementDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.externalmovementsapi.model.ExternalMovementStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.EventPriorities
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.Priority
import java.time.LocalDateTime
import java.util.UUID

class ExternalMovementsTransformationsTest {

  private val prisonCode = "MDI"
  private val priorities = EventPriorities(EventType.entries.associateWith { listOf(Priority(it.defaultPriority)) })

  private fun externalMovement(
    code: String = "FB",
    shortDesc: String = "Accommodation-related",
    fullDesc: String = "Standard ROTL",
    isSensitive: Boolean = false,
  ) = ExternalMovement(
    id = UUID.randomUUID(),
    prisonerNumber = "A1234AA",
    description = ExternalMovementDescription(full = fullDesc, short = shortDesc, code = code),
    start = LocalDateTime.of(2026, 5, 10, 9, 0),
    end = LocalDateTime.of(2026, 5, 10, 17, 0),
    status = ExternalMovementStatus(code = "SCHEDULED", description = "Scheduled"),
    detail = ExternalMovementDetail(uiUrl = "TestUrl", requiredRoles = setOf("TEST_ROLE")),
    isSensitive = isSensitive,
  )

  @Test
  fun `summary is 'Accommodation-related ROTL' when code is FB`() {
    val result = externalMovement(code = "FB").toScheduledEvent(prisonCode, priorities)
    assertThat(result.summary).isEqualTo("Accommodation-related ROTL")
  }

  @ParameterizedTest
  @ValueSource(strings = ["YOTR", "20"])
  fun `summary is 'Sentence or resettlement plan ROTL' when code is YOTR or 20`(code: String) {
    val result = externalMovement(code = code).toScheduledEvent(prisonCode, priorities)
    assertThat(result.summary).isEqualTo("Sentence or resettlement plan ROTL")
  }

  @Test
  fun `summary falls back to short description for unknown codes`() {
    val result = externalMovement(code = "UNKNOWN", shortDesc = "Testing short desc").toScheduledEvent(prisonCode, priorities)
    assertThat(result.summary).isEqualTo("Testing short desc")
  }

  @ParameterizedTest
  @ValueSource(strings = ["FB", "YOTR", "20", "UNKNOWN"])
  fun `summary is 'ROTL or other temporary absence' when isSensitive is true regardless of code`(code: String) {
    val result = externalMovement(code = code, isSensitive = true).toScheduledEvent(prisonCode, priorities)
    assertThat(result.summary).isEqualTo("ROTL or other temporary absence")
  }

  @Test
  fun `toScheduledEvent maps fields correctly`() {
    val movement = externalMovement(code = "FB")
    val result = movement.toScheduledEvent(prisonCode, priorities)

    assertThat(result.prisonCode).isEqualTo(prisonCode)
    assertThat(result.eventSource).isEqualTo("EXTERNAL_MOVEMENTS_API")
    assertThat(result.eventType).isEqualTo(EventType.ACTIVITY.name)
    assertThat(result.categoryCode).isEqualTo(movement.description.code)
    assertThat(result.categoryDescription).isNull()
    assertThat(result.summary).isEqualTo("Accommodation-related ROTL")
    assertThat(result.outsidePrison).isTrue()
    assertThat(result.prisonerNumber).isEqualTo(movement.prisonerNumber)
    assertThat(result.date).isEqualTo(movement.start.toLocalDate())
    assertThat(result.startTime).isEqualTo(movement.start.toLocalTime())
    assertThat(result.endTime).isEqualTo(movement.end.toLocalTime())
    assertThat(result.priority).isEqualTo(EventType.ACTIVITY.defaultPriority)
    assertThat(result.status).isEqualTo(movement.status.description)
  }

  @Test
  fun `toScheduledEvents maps list of movements`() {
    val movements = listOf(
      externalMovement(code = "FB"),
      externalMovement(code = "YOTR"),
      externalMovement(code = "20"),
    )
    val results = movements.toScheduledEvents(prisonCode, priorities)

    assertThat(results.map { it.summary }).containsExactly(
      "Accommodation-related ROTL",
      "Sentence or resettlement plan ROTL",
      "Sentence or resettlement plan ROTL",
    )
  }

  @Test
  fun `toScheduledEvents returns empty list when source list is empty`() {
    val result = emptyList<ExternalMovement>()
      .toScheduledEvents(prisonCode, priorities)

    assertThat(result).isEmpty()
  }
}
