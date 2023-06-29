package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.CurrentIncentive
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.IncentiveLevel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ScheduleInstanceCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.UncancelScheduledInstanceRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsPublisher
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundHMPPSDomainEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.ScheduledInstanceInformation
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@TestPropertySource(
  properties = [
    "feature.event.activities.scheduled-instance.amended=true",
  ],
)
class ActivityScheduleInstanceIntegrationTest : IntegrationTestBase() {

  @MockBean
  private lateinit var eventsPublisher: OutboundEventsPublisher

  private val eventCaptor = argumentCaptor<OutboundHMPPSDomainEvent>()

  @Nested
  @DisplayName("getScheduledInstancesById")
  inner class GetScheduledInstancesById {
    @Test
    @Sql("classpath:test_data/seed-activity-id-1.sql")
    fun `get schedule by its id`() {
      val scheduledInstance = webTestClient.getScheduledInstanceById(1)!!

      assertThat(scheduledInstance.id).isEqualTo(1L)
      assertThat(scheduledInstance.startTime.toString()).isEqualTo("10:00")
      assertThat(scheduledInstance.endTime.toString()).isEqualTo("11:00")
    }
  }

  @Nested
  @DisplayName("getActivityScheduleInstances")
  inner class GetActivityScheduleInstances {
    @Test
    @Sql("classpath:test_data/seed-activity-id-3.sql")
    fun `returns all 20 rows without the time slot`() {
      val startDate = LocalDate.of(2022, 10, 1)
      val endDate = LocalDate.of(2022, 11, 5)

      val scheduledInstances = webTestClient.getScheduledInstancesBy(moorlandPrisonCode, startDate, endDate)

      assertThat(scheduledInstances).hasSize(20)
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-3.sql")
    fun `returns 10 rows with the time slot filter`() {
      val startDate = LocalDate.of(2022, 10, 1)
      val endDate = LocalDate.of(2022, 11, 5)

      val scheduledInstances =
        webTestClient.getScheduledInstancesBy(moorlandPrisonCode, startDate, endDate, TimeSlot.AM)

      assertThat(scheduledInstances).hasSize(10)
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-3.sql")
    fun `date range precludes 4 rows from the sample of 20`() {
      val startDate = LocalDate.of(2022, 10, 2)
      val endDate = LocalDate.of(2022, 11, 4)

      val scheduledInstances = webTestClient.getScheduledInstancesBy(moorlandPrisonCode, startDate, endDate)

      assertThat(scheduledInstances).hasSize(16)
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-10.sql")
    fun `returns instance when no allocations`() {
      val startDate = LocalDate.of(2022, 10, 1)
      val endDate = LocalDate.of(2022, 11, 5)

      val scheduledInstances =
        webTestClient.getScheduledInstancesBy(moorlandPrisonCode, startDate, endDate, TimeSlot.AM)

      assertThat(scheduledInstances).hasSize(1)
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-10.sql")
    fun `returns no instance when match on time slot`() {
      val startDate = LocalDate.of(2022, 10, 1)
      val endDate = LocalDate.of(2022, 11, 5)

      val scheduledInstances =
        webTestClient.getScheduledInstancesBy(moorlandPrisonCode, startDate, endDate, TimeSlot.PM)

      assertThat(scheduledInstances).isEmpty()
    }
  }

  @Nested
  @DisplayName("uncancelScheduledInstance")
  inner class UncancelScheduledInstance {

    @Test
    @Sql("classpath:test_data/seed-activity-id-13.sql")
    fun `scheduled instance is cancelled`() {
      val response = webTestClient.uncancelScheduledInstance(1, "CAN1234", "Mr Cancel")
      response.expectStatus().isNoContent

      with(webTestClient.getScheduledInstanceById(1)!!) {
        assertThat(cancelled).isFalse
        assertThat(cancelledBy).isNull()

        with(attendances.first()) {
          assertThat(attendanceReason).isNull()
          assertThat(status).isEqualTo("WAITING")
          assertThat(comment).isNull()
          assertThat(recordedBy).isNull()

        }
      }

      verify(eventsPublisher).send(eventCaptor.capture())

      with(eventCaptor.firstValue) {
        assertThat(eventType).isEqualTo("activities.scheduled-instance.amended")
        assertThat(additionalInformation).isEqualTo(ScheduledInstanceInformation(1))
        assertThat(occurredAt).isCloseTo(java.time.LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
        assertThat(description).isEqualTo("A scheduled instance has been amended in the activities management service")
      }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-14.sql")
    fun `scheduled instance is not cancelled`() {
      val response = webTestClient.uncancelScheduledInstance(1, "CAN1234", "Mr Cancel")
      response.expectStatus().isBadRequest
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-15.sql")
    fun `scheduled instance is in the past`() {
      val response = webTestClient.uncancelScheduledInstance(1, "CAN1234", "Mr Cancel")
      response.expectStatus().isBadRequest
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-15.sql")
    fun `scheduled instance does not exist`() {
      val response = webTestClient.uncancelScheduledInstance(2, "CAN1234", "Mr Cancel")
      response.expectStatus().isNotFound
    }
  }

  @Nested
  @DisplayName("cancelScheduledInstance")
  inner class CancelScheduledInstance {
    @Test
    @Sql("classpath:test_data/seed-activity-id-16.sql")
    fun success() {
      prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
        listOf("A11111A", "A22222A"),
        listOf(
          PrisonerSearchPrisonerFixture.instance(
            prisonerNumber = "A11111A",
            bookingId = 456,
            prisonId = "TPR",
            currentIncentive = CurrentIncentive(
              level = IncentiveLevel(
                description = "Basic",
                code = "BAS",
              ),
              dateTime = LocalDate.now().toString(),
              nextReviewDate = LocalDate.now(),
            ),
          ),
          PrisonerSearchPrisonerFixture.instance(
            prisonerNumber = "A22222A",
            bookingId = 456,
            prisonId = "TPR",
            currentIncentive = CurrentIncentive(
              level = IncentiveLevel(
                description = "Basic",
                code = "BAS",
              ),
              dateTime = LocalDate.now().toString(),
              nextReviewDate = LocalDate.now(),
            ),
          ),
        ),
      )

      webTestClient.cancelScheduledInstance(1, "Location unavailable", "USER1")

      with(webTestClient.getScheduledInstanceById(1)!!) {
        assertThat(cancelled).isTrue
        assertThat(cancelledBy).isEqualTo("USER1")

        with(attendances.first()) {
          assertThat(attendanceReason!!.code).isEqualTo("CANCELLED")
          assertThat(status).isEqualTo("COMPLETED")
          assertThat(comment).isEqualTo("Location unavailable")
          assertThat(recordedBy).isEqualTo("USER1")
          assertThat(recordedTime).isNotNull
        }
      }

      verify(eventsPublisher).send(eventCaptor.capture())

      with(eventCaptor.firstValue) {
        assertThat(eventType).isEqualTo("activities.scheduled-instance.amended")
        assertThat(additionalInformation).isEqualTo(ScheduledInstanceInformation(1))
        assertThat(occurredAt).isCloseTo(java.time.LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
        assertThat(description).isEqualTo("A scheduled instance has been amended in the activities management service")
      }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-16.sql")
    fun `404 - scheduled instance not found`() {
      val response = webTestClient.cancelScheduledInstance(4, "Location unavailable", "USER1")
      response.expectStatus().isNotFound
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-16.sql")
    fun `400 - scheduled instance in past`() {
      val response = webTestClient.cancelScheduledInstance(2, "Location unavailable", "USER1")
      response
        .expectStatus().isBadRequest
        .expectBody().jsonPath("developerMessage").isEqualTo("The schedule instance has ended")
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-16.sql")
    fun `400 - scheduled instance has been canceled`() {
      val response = webTestClient.cancelScheduledInstance(3, "Location unavailable", "USER1")
      response
        .expectStatus().isBadRequest
        .expectBody().jsonPath("developerMessage").isEqualTo("The schedule instance has already been cancelled")
    }
  }

  private fun WebTestClient.getScheduledInstanceById(id: Long) = get()
    .uri("/scheduled-instances/$id")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf()))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(ActivityScheduleInstance::class.java)
    .returnResult().responseBody

  private fun WebTestClient.uncancelScheduledInstance(id: Long, username: String, displayName: String) = put()
    .uri("/scheduled-instances/$id/uncancel")
    .bodyValue(UncancelScheduledInstanceRequest(username, displayName))
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf()))
    .exchange()

  private fun WebTestClient.cancelScheduledInstance(
    id: Long,
    reason: String,
    username: String,
    comment: String? = null,
  ) = put()
    .uri("/scheduled-instances/$id/cancel")
    .bodyValue(ScheduleInstanceCancelRequest(reason, username, comment))
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf()))
    .exchange()

  private fun WebTestClient.getScheduledInstancesBy(
    prisonCode: String,
    startDate: LocalDate,
    endDate: LocalDate,
    timeSlot: TimeSlot? = null,
  ) =
    get()
      .uri { builder ->
        builder
          .path("/prisons/$prisonCode/scheduled-instances")
          .queryParam("startDate", startDate)
          .queryParam("endDate", endDate)
          .maybeQueryParam("slot", timeSlot)
          .build()
      }
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(ActivityScheduleInstance::class.java)
      .returnResult().responseBody
}
