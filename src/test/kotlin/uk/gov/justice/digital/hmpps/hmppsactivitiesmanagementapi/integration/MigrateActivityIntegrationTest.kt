package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Slot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityMigrateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AllocationMigrateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.NomisPayRate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.NomisScheduleRule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityMigrateResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AllocationMigrateResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.CASELOAD_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundHMPPSDomainEvent
import java.time.LocalDate
import java.time.LocalTime

@TestPropertySource(
  properties = [
    "feature.events.sns.enabled=true",
    "feature.event.activities.activity-schedule.created=true",
    "feature.event.activities.activity-schedule.amended=true",
    "feature.event.activities.prisoner.allocated=true",
    "feature.migrate.split.regime.enabled=true",
  ],
)
class MigrateActivityIntegrationTest : IntegrationTestBase() {

  @Autowired
  private lateinit var activityRepository: ActivityRepository

  private val eventCaptor = argumentCaptor<OutboundHMPPSDomainEvent>()
  private val startTime = LocalTime.of(10, 0)
  private val endTime = LocalTime.of(11, 0)

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Test
  fun `migrate activity - success`() {
    val nomisPayRates = listOf(
      NomisPayRate(incentiveLevel = "BAS", nomisPayBand = "1", rate = 110),
    )

    val nomisScheduleRules = listOf(
      NomisScheduleRule(startTime = startTime, endTime = endTime, monday = true),
    )

    val requestBody = buildActivityMigrateRequest(nomisPayRates, nomisScheduleRules)
    incentivesApiMockServer.stubGetIncentiveLevels(requestBody.prisonCode)
    prisonApiMockServer.stubGetLocation(1L, "prisonapi/location-id-1.json")

    val response = webTestClient.migrateActivity(requestBody, listOf("ROLE_NOMIS_ACTIVITIES"))
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ActivityMigrateResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(activityId).isNotNull
      assertThat(splitRegimeActivityId).isNull()
    }

    verify(eventsPublisher).send(eventCaptor.capture())

    eventCaptor.allValues.forEach { event ->
      log.info("Event captured on successful activity migration: ${event.eventType}")
    }

    with(eventCaptor.firstValue) {
      assertThat(eventType).isEqualTo("activities.activity-schedule.created")
    }
  }

  @Test
  fun `migrate activity - incorrect roles is forbidden`() {
    val nomisPayRates = listOf(
      NomisPayRate(incentiveLevel = "BAS", nomisPayBand = "1", rate = 110),
    )

    val nomisScheduleRules = listOf(
      NomisScheduleRule(startTime = startTime, endTime = endTime, monday = true),
    )

    val error = webTestClient.migrateActivity(
      buildActivityMigrateRequest(nomisPayRates, nomisScheduleRules),
      emptyList(),
    )
      .expectStatus().isForbidden
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(error!!) {
      assertThat(status).isEqualTo(403)
      assertThat(userMessage).isEqualTo("Access denied: Access Denied")
      assertThat(developerMessage).isEqualTo("Access Denied")
    }

    verifyNoInteractions(eventsPublisher)
  }

  @Test
  fun `migrate activity - invalid pay band in rates`() {
    prisonApiMockServer.stubGetLocation(1L, "prisonapi/location-id-1.json")

    val nomisPayRates = listOf(
      NomisPayRate(incentiveLevel = "BAS", nomisPayBand = "12", rate = 110),
    )

    val nomisScheduleRules = listOf(
      NomisScheduleRule(startTime = startTime, endTime = endTime, monday = true),
    )

    val error = webTestClient.migrateActivity(
      buildActivityMigrateRequest(nomisPayRates, nomisScheduleRules),
      listOf("ROLE_NOMIS_ACTIVITIES"),
    )
      .expectStatus().is4xxClientError
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(error!!) {
      assertThat(status).isEqualTo(400)
      assertThat(userMessage).contains("No prison pay band for Nomis pay band 12")
    }

    verifyNoInteractions(eventsPublisher)
  }

  private fun getActivity(activityId: Long, agencyId: String = "IWI"): Activity =
    webTestClient.get()
      .uri("/activities/$activityId/filtered")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(isClientToken = false, roles = listOf(ROLE_PRISON)))
      .header(CASELOAD_ID, agencyId)
      .exchange()
      .expectBody(Activity::class.java)
      .returnResult().responseBody!!

  @Test
  @Sql("classpath:test_data/seed-activity-id-23-1.sql")
  fun `migrate allocation with multiple exclusions - success`() {
    val request = buildAllocationMigrateRequestWithMultipleExclusions()

    stubPrisonerSearch(request.prisonCode, request.prisonerNumber, true)

    val response = webTestClient.migrateAllocation(request, listOf("ROLE_NOMIS_ACTIVITIES"))
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AllocationMigrateResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(allocationId).isNotNull

      val activity = getActivity(activityId, "MDI")
      val slots = activity.schedules.first().slots

      assertThat(slots).hasSize(3)
      assertTrue(slots.any { slot -> slot.weekNumber == 1 && slot.timeSlot == TimeSlot.AM && slot.mondayFlag && slot.tuesdayFlag && slot.wednesdayFlag && slot.thursdayFlag && !slot.fridayFlag  })
      assertTrue(slots.any { slot -> slot.weekNumber == 1 && slot.timeSlot == TimeSlot.PM && slot.mondayFlag && slot.tuesdayFlag && slot.wednesdayFlag && slot.thursdayFlag && !slot.fridayFlag  })
      assertTrue(slots.any { slot -> slot.weekNumber == 1 && slot.timeSlot == TimeSlot.AM && !slot.mondayFlag && !slot.tuesdayFlag && !slot.wednesdayFlag && !slot.thursdayFlag && slot.fridayFlag  })


      val allocation = activity.schedules.first().allocations.first()
      assertThat(allocation.exclusions).hasSize(1)
      val exclusion = allocation.exclusions.first()
      assertThat(exclusion.monday).isTrue()
      assertThat(exclusion.tuesday).isTrue()
      assertThat(exclusion.wednesday).isTrue()
      assertThat(exclusion.thursday).isTrue()
      assertThat(exclusion.friday).isTrue()
      assertThat(exclusion.saturday).isFalse()
      assertThat(exclusion.sunday).isFalse()
    }

    verify(eventsPublisher).send(eventCaptor.capture())

    eventCaptor.allValues.forEach { event ->
      log.info("Event captured on successful allocation: ${event.eventType}")
    }

    with(eventCaptor.firstValue) {
      assertThat(eventType).isEqualTo("activities.prisoner.allocated")
    }
  }

  @Test
  @Sql("classpath:test_data/seed-activity-id-23.sql")
  fun `migrate allocation - success with no pay band - defaults to lowest pay`() {
    val request = buildAllocationMigrateRequest().copy(
      nomisPayBand = null,
    )

    stubPrisonerSearch(request.prisonCode, request.prisonerNumber, true)

    val response = webTestClient.migrateAllocation(request, listOf("ROLE_NOMIS_ACTIVITIES"))
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AllocationMigrateResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(allocationId).isNotNull
    }

    verify(eventsPublisher).send(eventCaptor.capture())

    eventCaptor.allValues.forEach { event ->
      log.info("Event captured on successful allocation: ${event.eventType}")
    }

    with(eventCaptor.firstValue) {
      assertThat(eventType).isEqualTo("activities.prisoner.allocated")
    }
  }

  @Test
  @Sql("classpath:test_data/seed-activity-id-23.sql")
  fun `migrate allocation - already allocated error`() {
    val request = buildAllocationMigrateRequest().copy(
      prisonerNumber = "A1234AA",
    )

    val error = webTestClient.migrateAllocation(request, listOf("ROLE_NOMIS_ACTIVITIES"))
      .expectStatus().is4xxClientError
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(error!!) {
      assertThat(userMessage).contains("Allocation failed A1234AA. Already allocated to 1 Maths")
    }

    verifyNoInteractions(eventsPublisher)
  }

  @Test
  fun `migrate allocation - incorrect roles is forbidden`() {
    val error = webTestClient.migrateAllocation(
      buildAllocationMigrateRequest(),
      emptyList(),
    )
      .expectStatus().isForbidden
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(error!!) {
      assertThat(status).isEqualTo(403)
      assertThat(userMessage).isEqualTo("Access denied: Access Denied")
      assertThat(developerMessage).isEqualTo("Access Denied")
    }

    verifyNoInteractions(eventsPublisher)
  }

  @Test
  @Sql("classpath:test_data/seed-activity-id-23.sql")
  fun `migrate allocation - invalid pay band specified`() {
    val request = buildAllocationMigrateRequest().copy(
      nomisPayBand = "11",
    )

    stubPrisonerSearch(request.prisonCode, request.prisonerNumber, true)

    val response = webTestClient.migrateAllocation(request, listOf("ROLE_NOMIS_ACTIVITIES"))
      .expectStatus().is4xxClientError
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(userMessage).contains("Allocation failed A1234BB. Nomis pay band 11 is not configured for MDI")
    }

    verifyNoInteractions(eventsPublisher)
  }

  @Test
  @Sql("classpath:test_data/seed-activity-id-23.sql")
  fun `migrate allocation - prisoner is not active in the prison`() {
    val request = buildAllocationMigrateRequest()

    stubPrisonerSearch(request.prisonCode, request.prisonerNumber, false)

    val response = webTestClient.migrateAllocation(request, listOf("ROLE_NOMIS_ACTIVITIES"))
      .expectStatus().is4xxClientError
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(userMessage).contains("Allocation failed A1234BB. Prisoner not in MDI or INACTIVE")
    }

    verifyNoInteractions(eventsPublisher)
  }

  @Test
  @Sql(
    "classpath:test_data/clean-all-data.sql",
    "classpath:test_data/seed-reference-data.sql",
    "classpath:test_data/seed-activity-id-13.sql",
  )
  fun `delete cascade - removes an activity and all its child entities`() {
    val prisonCode = "PVI"
    val activityId = 1L

    webTestClient.deleteCascade(prisonCode, activityId, listOf("ROLE_NOMIS_ACTIVITIES"))
      .expectStatus().isOk

    val activity = activityRepository.findByActivityIdAndPrisonCode(activityId, prisonCode)
    assertThat(activity).isNull()

    assertThat(activityRepository.count()).isEqualTo(0)

    verifyNoInteractions(eventsPublisher)
  }

  private fun buildActivityMigrateRequest(
    payRates: List<NomisPayRate> = emptyList(),
    scheduleRules: List<NomisScheduleRule> = emptyList(),
  ) =
    ActivityMigrateRequest(
      programServiceCode = "CLNR",
      prisonCode = "MDI",
      startDate = LocalDate.now().minusDays(1),
      endDate = null,
      internalLocationId = 1,
      internalLocationCode = "011",
      internalLocationDescription = "MDI-1-1-011",
      capacity = 10,
      description = "An activity",
      payPerSession = "H",
      runsOnBankHoliday = false,
      outsideWork = false,
      scheduleRules,
      payRates,
    )

  private fun buildAllocationMigrateRequest() =
    AllocationMigrateRequest(
      prisonCode = "MDI",
      activityId = 1,
      splitRegimeActivityId = null,
      prisonerNumber = "A1234BB",
      bookingId = 1,
      cellLocation = "MDI-1-1-001",
      nomisPayBand = "1",
      startDate = LocalDate.now().minusDays(1),
      endDate = null,
      endComment = null,
      suspendedFlag = false,
      exclusions = listOf(
        Slot(weekNumber = 1, timeSlot = TimeSlot.AM, monday = true),
      ),
    )

  private fun buildAllocationMigrateRequestWithMultipleExclusions() =
    AllocationMigrateRequest(
      prisonCode = "MDI",
      activityId = 1,
      splitRegimeActivityId = null,
      prisonerNumber = "A1234BB",
      bookingId = 1,
      cellLocation = "MDI-1-1-001",
      nomisPayBand = "1",
      startDate = LocalDate.now().minusDays(1),
      endDate = null,
      endComment = null,
      suspendedFlag = false,
      exclusions = listOf(
        Slot(weekNumber = 1, timeSlot = TimeSlot.AM, monday = true, tuesday = true, wednesday = true, thursday = true, friday = true),
      ),
    )

  private fun WebTestClient.migrateActivity(request: ActivityMigrateRequest, roles: List<String>) =
    post()
      .uri("/migrate/activity")
      .bodyValue(request)
      .headers(setAuthorisation(roles = roles))
      .exchange()

  private fun WebTestClient.migrateAllocation(request: AllocationMigrateRequest, roles: List<String>) =
    post()
      .uri("/migrate/allocation")
      .bodyValue(request)
      .headers(setAuthorisation(roles = roles))
      .exchange()

  private fun WebTestClient.deleteCascade(prisonCode: String, activityId: Long, roles: List<String>) =
    delete()
      .uri("/migrate/delete-activity/prison/$prisonCode/id/$activityId")
      .headers(setAuthorisation(roles = roles))
      .exchange()

  private fun stubPrisonerSearch(prisonCode: String, prisonerNumber: String, active: Boolean = true) =
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      listOf(prisonerNumber),
      listOf(
        PrisonerSearchPrisonerFixture.instance(
          prisonerNumber = prisonerNumber,
          bookingId = 1,
          prisonId = prisonCode,
          status = if (active) "ACTIVE IN" else "INACTIVE OUT",
        ),
      ),
    )
}
