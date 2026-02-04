package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityPayHistory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AdvanceAttendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AdvanceAttendanceCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivitySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.CASELOAD_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_ACTIVITY_ADMIN
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON

abstract class ActivitiesIntegrationTestBase : IntegrationTestBase() {

  fun WebTestClient.getScheduledInstancesByIds(vararg ids: Long) = post()
    .uri("/scheduled-instances")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationAsClient(roles = listOf(ROLE_PRISON)))
    .header(CASELOAD_ID, "PVI")
    .bodyValue(ids)
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList(ActivityScheduleInstance::class.java)
    .returnResult().responseBody

  fun WebTestClient.getActivities(prisonCode: String, nameSearch: String? = null) = get()
    .uri("/prison/$prisonCode/activities" + (nameSearch?.let { "?nameSearch=$nameSearch" } ?: ""))
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationAsClient(roles = listOf(ROLE_PRISON)))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList(ActivitySummary::class.java)
    .returnResult().responseBody

  fun WebTestClient.getActivityById(id: Long, caseLoadId: String = "PVI") = get()
    .uri("/activities/$id/filtered")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationAsClient(roles = listOf(ROLE_PRISON)))
    .header(CASELOAD_ID, caseLoadId)
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(Activity::class.java)
    .returnResult().responseBody!!

  fun WebTestClient.getActivityPayHistory(id: Long) = get()
    .uri("/activities/$id/pay-history")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationAsClient(roles = listOf(ROLE_PRISON)))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList(ActivityPayHistory::class.java)
    .returnResult().responseBody!!

  fun WebTestClient.createAdvanceAttendance(request: AdvanceAttendanceCreateRequest, caseLoad: String? = "PVI") = post()
    .uri("/advance-attendances")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationAsUser(roles = listOf(ROLE_ACTIVITY_ADMIN)))
    .header(CASELOAD_ID, "$caseLoad")
    .exchange()
    .expectStatus().isCreated
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(AdvanceAttendance::class.java)
    .returnResult().responseBody

  fun WebTestClient.retrieveAdvanceAttendance(id: Long) = get()
    .uri("/advance-attendances/$id")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationAsUser(roles = listOf(ROLE_ACTIVITY_ADMIN)))
    .header(CASELOAD_ID, "PVI")
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(AdvanceAttendance::class.java)
    .returnResult().responseBody

  fun WebTestClient.checkAdvanceAttendanceDoesNotExist(id: Long) = get()
    .uri("/advance-attendances/$id")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationAsUser(roles = listOf(ROLE_ACTIVITY_ADMIN)))
    .header(CASELOAD_ID, "PVI")
    .exchange()
    .expectStatus().isNotFound

  fun WebTestClient.getAllocation(id: Long, caseLoadId: String = "PVI") = webTestClient.get()
    .uri("/allocations/id/$id")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationAsUser(roles = listOf(ROLE_ACTIVITY_ADMIN)))
    .header(CASELOAD_ID, caseLoadId)
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(Allocation::class.java)
    .returnResult().responseBody

  fun WebTestClient.getAttendanceById(id: Long) = get()
    .uri("/attendances/$id")
    .headers(setAuthorisationAsClient(roles = listOf(ROLE_PRISON)))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(Attendance::class.java)
    .returnResult().responseBody
}
