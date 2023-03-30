package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AuditRecordSearchFilters
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.LocalAuditSearchResults
import java.time.LocalDateTime

class AuditIntegrationTest : IntegrationTestBase() {

  @Sql(
    "classpath:test_data/seed-audit-id-1.sql",
  )
  @Test
  fun `should fetch all audit events when filters not set`() {
    val auditRecords = webTestClient.getAuditRecords()
    assertThat(auditRecords!!.content.size).isEqualTo(10)
  }

  @Sql(
    "classpath:test_data/seed-audit-id-1.sql",
  )
  @Test
  fun `should paginate`() {
    var auditRecords = webTestClient.getAuditRecords(page = 0, size = 6)
    assertThat(auditRecords!!.content.size).isEqualTo(6)

    auditRecords = webTestClient.getAuditRecords(page = 1, size = 6)
    assertThat(auditRecords!!.content.size).isEqualTo(4)

    auditRecords = webTestClient.getAuditRecords(page = 2, size = 6)
    assertThat(auditRecords!!.content.size).isEqualTo(0)
  }

  @Sql(
    "classpath:test_data/seed-audit-id-1.sql",
  )
  @Test
  fun `should fetch audit events by prison code`() {
    val auditRecords = webTestClient.getAuditRecords(prisonCode = "MDI")
    assertThat(auditRecords!!.content.size).isEqualTo(1)
    with(auditRecords.content.first()) {
      assertThat(localAuditId).isEqualTo(2)
    }
  }

  @Sql(
    "classpath:test_data/seed-audit-id-1.sql",
  )
  @Test
  fun `should fetch audit events by username`() {
    val auditRecords = webTestClient.getAuditRecords(username = "Terry")
    assertThat(auditRecords!!.content.size).isEqualTo(1)
    with(auditRecords.content.first()) {
      assertThat(localAuditId).isEqualTo(3)
    }
  }

  @Sql(
    "classpath:test_data/seed-audit-id-1.sql",
  )
  @Test
  fun `should fetch audit events by audit type`() {
    val auditRecords = webTestClient.getAuditRecords(auditType = AuditType.PRISONER)
    assertThat(auditRecords!!.content.size).isEqualTo(1)
    with(auditRecords.content.first()) {
      assertThat(localAuditId).isEqualTo(4)
    }
  }

  @Sql(
    "classpath:test_data/seed-audit-id-1.sql",
  )
  @Test
  fun `should fetch audit events by audit event type`() {
    val auditRecords = webTestClient.getAuditRecords(auditEventType = AuditEventType.ACTIVITY_UPDATED)
    assertThat(auditRecords!!.content.size).isEqualTo(1)
    with(auditRecords.content.first()) {
      assertThat(localAuditId).isEqualTo(5)
    }
  }

  @Sql(
    "classpath:test_data/seed-audit-id-1.sql",
  )
  @Test
  fun `should fetch audit events by start time`() {
    val auditRecords = webTestClient.getAuditRecords(startTime = LocalDateTime.of(2022, 2, 3, 1, 2, 0))
    assertThat(auditRecords!!.content.size).isEqualTo(1)
    with(auditRecords.content.first()) {
      assertThat(localAuditId).isEqualTo(6)
    }
  }

  @Sql(
    "classpath:test_data/seed-audit-id-1.sql",
  )
  @Test
  fun `should fetch audit events by end time`() {
    val auditRecords = webTestClient.getAuditRecords(endTime = LocalDateTime.of(1996, 1, 1, 1, 2, 0))
    assertThat(auditRecords!!.content.size).isEqualTo(1)
    with(auditRecords.content.first()) {
      assertThat(localAuditId).isEqualTo(7)
    }
  }

  @Sql(
    "classpath:test_data/seed-audit-id-1.sql",
  )
  @Test
  fun `should fetch audit events by prisoner number`() {
    val auditRecords = webTestClient.getAuditRecords(prisonerNumber = "B987654")
    assertThat(auditRecords!!.content.size).isEqualTo(1)
    with(auditRecords.content.first()) {
      assertThat(localAuditId).isEqualTo(8)
    }
  }

  @Sql(
    "classpath:test_data/seed-audit-id-1.sql",
  )
  @Test
  fun `should fetch audit events by activity ID`() {
    val auditRecords = webTestClient.getAuditRecords(activityId = 42)
    assertThat(auditRecords!!.content.size).isEqualTo(1)
    with(auditRecords.content.first()) {
      assertThat(localAuditId).isEqualTo(9)
    }
  }

  @Sql(
    "classpath:test_data/seed-audit-id-1.sql",
  )
  @Test
  fun `should fetch audit events by schedule ID`() {
    val auditRecords = webTestClient.getAuditRecords(scheduleId = 99)
    assertThat(auditRecords!!.content.size).isEqualTo(1)
    with(auditRecords.content.first()) {
      assertThat(localAuditId).isEqualTo(10)
    }
  }

  private fun WebTestClient.getAuditRecords(
    prisonCode: String? = null,
    prisonerNumber: String? = null,
    username: String? = null,
    auditType: AuditType? = null,
    auditEventType: AuditEventType? = null,
    startTime: LocalDateTime? = null,
    endTime: LocalDateTime? = null,
    activityId: Long? = null,
    scheduleId: Long? = null,
    page: Long? = 0,
    size: Long? = 10,
  ) =
    post()
      .uri("/audit/search?page=$page&size=$size")
      .bodyValue(
        AuditRecordSearchFilters(
          prisonCode = prisonCode,
          prisonerNumber = prisonerNumber,
          username = username,
          auditType = auditType,
          auditEventType = auditEventType,
          startTime = startTime,
          endTime = endTime,
          activityId = activityId,
          scheduleId = scheduleId,
        ),
      )
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_ACTIVITY_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(LocalAuditSearchResults::class.java)
      .returnResult().responseBody
}
