package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivitySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.CASELOAD_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON

abstract class ActivitiesIntegrationTestBase : IntegrationTestBase() {

  fun WebTestClient.getScheduledInstancesByIds(vararg ids: Long) = post()
    .uri("/scheduled-instances")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
    .header(CASELOAD_ID, "PVI")
    .bodyValue(ids)
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList(ActivityScheduleInstance::class.java)
    .returnResult().responseBody

  fun WebTestClient.getActivities(prisonCode: String) = get()
    .uri("/prison/$prisonCode/activities")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList(ActivitySummary::class.java)
    .returnResult().responseBody
}
