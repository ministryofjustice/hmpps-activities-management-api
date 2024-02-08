package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.containsExactly
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.containsExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.SarAllocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.SubjectAccessRequestContent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.Role
import java.time.LocalDate

class SubjectAccessRequestIntegrationTest : IntegrationTestBase() {

  @Sql("classpath:test_data/seed-subject-access-request.sql")
  @Test
  fun `should return single allocation for subject access request`() {
    val response = webTestClient.getSarContent("111111", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 1))

    response.allocations containsExactly listOf(
      SarAllocation(
        allocationId = 1,
        prisonCode = PENTONVILLE_PRISON_CODE,
        prisonerStatus = "ENDED",
        startDate = LocalDate.of(2020, 1, 1),
        endDate = LocalDate.of(2020, 12, 1),
        activityId = 1,
        activitySummary = "Maths Level 1",
        payBand = "Pay band 1 (lowest)",
      ),
    )
  }

  @Sql("classpath:test_data/seed-subject-access-request.sql")
  @Test
  fun `should return two allocations for subject access request`() {
    val response = webTestClient.getSarContent("111111", LocalDate.of(2020, 1, 1), TimeSource.today())

    response.allocations containsExactlyInAnyOrder listOf(
      SarAllocation(
        allocationId = 1,
        prisonCode = PENTONVILLE_PRISON_CODE,
        prisonerStatus = "ENDED",
        startDate = LocalDate.of(2020, 1, 1),
        endDate = LocalDate.of(2020, 12, 1),
        activityId = 1,
        activitySummary = "Maths Level 1",
        payBand = "Pay band 1 (lowest)",
      ),
      SarAllocation(
        allocationId = 2,
        prisonCode = PENTONVILLE_PRISON_CODE,
        prisonerStatus = "ACTIVE",
        startDate = TimeSource.today(),
        endDate = null,
        activityId = 1,
        activitySummary = "Maths Level 1",
        payBand = "Pay band 1 (lowest)",
      ),
    )
  }

  private fun WebTestClient.getSarContent(prisonerNumber: String, fromDate: LocalDate, toDate: LocalDate) =
    get()
      .uri("/subject-access-request?prn=$prisonerNumber&fromDate=$fromDate&toDate=$toDate")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(Role.SUBJECT_ACCESS_REQUEST)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SubjectAccessRequestContent::class.java)
      .returnResult().responseBody!!
}
