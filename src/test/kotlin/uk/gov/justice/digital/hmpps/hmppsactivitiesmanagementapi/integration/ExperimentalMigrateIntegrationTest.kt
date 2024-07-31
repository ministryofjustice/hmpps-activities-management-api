package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityMigrateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.NomisScheduleRule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityMigrateResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.CASELOAD_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON
import java.time.LocalDate
import java.time.LocalTime

@ActiveProfiles("experimental")
class ExperimentalMigrateIntegrationTest : IntegrationTestBase() {

  private val exceptionRequest =
    ActivityMigrateRequest(
      programServiceCode = "INT_NOM",
      prisonCode = "IWI",
      startDate = LocalDate.of(2024, 7, 9),
      endDate = null,
      internalLocationId = 468492,
      internalLocationCode = "SITE 3",
      internalLocationDescription = "IWI-ESTAB-SITE 3",
      capacity = 1,
      description = "BNM + 27 PK",
      payPerSession = "H",
      runsOnBankHoliday = true,
      outsideWork = false,
      scheduleRules = listOf(
        NomisScheduleRule(
          startTime = LocalTime.of(9, 25, 0),
          endTime = LocalTime.of(11, 35, 0),
          monday = true,
          tuesday = true,
          wednesday = true,
          thursday = true,
        ),
        NomisScheduleRule(
          startTime = LocalTime.of(13, 40, 0),
          endTime = LocalTime.of(16, 50, 0),
          monday = true,
          tuesday = true,
          wednesday = true,
          thursday = true,
        ),
        NomisScheduleRule(
          startTime = LocalTime.of(8, 25, 0),
          endTime = LocalTime.of(11, 35, 0),
          friday = true,
        ),
        NomisScheduleRule(
          startTime = LocalTime.of(13, 40, 0),
          endTime = LocalTime.of(16, 0, 0),
          friday = true,
        ),
      ),
      payRates = emptyList(),
    )

  @Sql(
    "classpath:test_data/seed-migrate-experiment.sql",
  )
  @Test
  fun `import IWI record that bypasses rule `() {
    incentivesApiMockServer.stubGetIncentiveLevels("IWI")
    prisonApiMockServer.stubGetLocation(468492L, "prisonapi/location-id-1.json")

    val response = webTestClient.post()
      .uri("/migrate/activity")
      .bodyValue(exceptionRequest)
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ActivityMigrateResponse::class.java)
      .returnResult().responseBody

    // grab activity and print it out.
    webTestClient.get()
      .uri("/activities/${response!!.activityId}")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(isClientToken = false, roles = listOf(ROLE_PRISON)))
      .header(CASELOAD_ID, "IWI")
      .exchange()
      .expectBody()
      .consumeWith(System.out::println)
  }
}
