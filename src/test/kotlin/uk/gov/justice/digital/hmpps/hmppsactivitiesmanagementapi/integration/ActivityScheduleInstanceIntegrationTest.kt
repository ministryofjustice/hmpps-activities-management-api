package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isNotEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ScheduledInstanceAttendanceSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ScheduleInstanceCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ScheduleInstancesCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ScheduleInstancesUncancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ScheduledInstancedUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.UncancelScheduledInstanceRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ScheduledAttendee
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.CASELOAD_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_ACTIVITY_ADMIN
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundHMPPSDomainEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.PrisonerAttendanceInformation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.ScheduledInstanceInformation
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

@TestPropertySource(
  properties = [
    "feature.event.activities.scheduled-instance.amended=true",
    "feature.event.activities.prisoner.attendance-amended=true",
    "feature.cancel.instance.priority.change.enabled=true",
  ],
)
class ActivityScheduleInstanceIntegrationTest : ActivitiesIntegrationTestBase() {

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
      with(scheduledInstance.attendances.first()) {
        editable isEqualTo false
      }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-1.sql")
    fun `allocation data should be returned`() {
      webTestClient.getScheduledInstanceById(1)!!.also {
        assertThat(it.nextScheduledInstanceId).isEqualTo(4L)
        assertThat(it.nextScheduledInstanceDate).isEqualTo(LocalDate.of(2022, 10, 11))
        assertThat(it.activitySchedule.activity.allocated).isEqualTo(5)
      }

      webTestClient.getScheduledInstanceById(4)!!.also {
        assertThat(it.previousScheduledInstanceId).isEqualTo(1L)
        assertThat(it.previousScheduledInstanceDate).isEqualTo(LocalDate.of(2022, 10, 10))
        assertThat(it.activitySchedule.activity.allocated).isEqualTo(5)
      }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-1.sql")
    fun `advance attendances should be returned`() {
      val instance = webTestClient.getScheduledInstanceById(1)!!
      assertThat(instance.advanceAttendances).hasSize(1)

      with(instance.advanceAttendances.first()) {
        prisonerNumber isEqualTo "A11111A"
        issuePayment isEqualTo true
        payAmount isEqualTo null
        recordedTime isNotEqualTo null
        recordedBy isEqualTo "John Smith"
        attendanceHistory isEqualTo null
      }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-1.sql")
    fun `403 when fetching a schedule by its id for the wrong caseload`() {
      webTestClient.get()
        .uri("/scheduled-instances/1")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(isClientToken = false, roles = listOf(ROLE_PRISON)))
        .header(CASELOAD_ID, "MDI")
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-1.sql")
    fun `attempting to fetch a schedule by its id without specifying a caseload succeeds if token is a client token`() {
      webTestClient.get()
        .uri("/scheduled-instances/1")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(isClientToken = true, roles = listOf(ROLE_ACTIVITY_ADMIN)))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-1.sql")
    fun `attempting to fetch a schedule by its id without specifying a caseload succeeds if admin role present`() {
      webTestClient.get()
        .uri("/scheduled-instances/1")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(isClientToken = true, roles = listOf(ROLE_ACTIVITY_ADMIN)))
        .exchange()
        .expectStatus().isOk
    }
  }

  @Nested
  @DisplayName("getActivityScheduleInstancesByIds")
  inner class GetActivityScheduleInstancesByIds {
    @Test
    @Sql("classpath:test_data/seed-activity-id-1.sql")
    fun `get schedule by its id`() {
      val scheduledInstances = webTestClient.getScheduledInstancesByIds(1, 2, 3, 4)!!

      assertThat(scheduledInstances).hasSize(4)
      scheduledInstances.forEach {
        assertThat(it.previousScheduledInstanceId).isNull()
        assertThat(it.previousScheduledInstanceDate).isNull()
        assertThat(it.nextScheduledInstanceId).isNull()
        assertThat(it.nextScheduledInstanceId).isNull()
        assertThat(it.activitySchedule.activity.allocated).isZero()
      }
      scheduledInstances.filter { it.id == 4L }.forEach {
        assertThat(it.attendances[0].attendanceReason!!.code).isEqualTo("SICK")
      }
    }
  }

  @Nested
  @DisplayName("getScheduledAttendeesByScheduledInstance")
  inner class GetScheduledAttendeesByScheduledInstance {
    @Test
    @Sql("classpath:test_data/seed-activity-id-1.sql")
    fun `get scheduled attendees by scheduled instance id`() {
      val attendees = webTestClient.getScheduledAttendeesByInstanceId(1)!!
      assertThat(attendees).hasSize(2)
      with(attendees[0]) { assertThat(prisonerNumber).isEqualTo("A11111A") }
      with(attendees[1]) { assertThat(prisonerNumber).isEqualTo("A22222A") }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-with-active-exclusions.sql")
    fun `get scheduled attendees by scheduled instance id - does not contain exclusions today`() {
      val attendees = webTestClient.getScheduledAttendeesByInstanceId(1)!!
      assertThat(attendees).hasSize(2)
      with(attendees[0]) { assertThat(prisonerNumber).isEqualTo("G4793VF") }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-with-historical-exclusions.sql")
    fun `get scheduled attendees by scheduled instance id - ignores historical exclusions today`() {
      val attendees = webTestClient.getScheduledAttendeesByInstanceId(1)!!
      assertThat(attendees).hasSize(2)
      assertThat(attendees.find { it.prisonerNumber == "G4793VF" }).isNotNull()
      assertThat(attendees.find { it.prisonerNumber == "A5193DY" }).isNotNull()
    }

    @Test
    @Sql("classpath:test_data/seed-activity-with-historical-exclusions.sql")
    fun `get scheduled attendees by scheduled instance id - respects historical exclusions`() {
      val attendees = webTestClient.getScheduledAttendeesByInstanceId(3)!!
      assertThat(attendees).hasSize(1)
      with(attendees[0]) { assertThat(prisonerNumber).isEqualTo("G4793VF") }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-with-future-exclusions.sql")
    fun `get scheduled attendees by scheduled instance id - ignores future exclusions today`() {
      val attendees = webTestClient.getScheduledAttendeesByInstanceId(1)!!
      assertThat(attendees).hasSize(2)
      assertThat(attendees.find { it.prisonerNumber == "G4793VF" }).isNotNull()
      assertThat(attendees.find { it.prisonerNumber == "A5193DY" }).isNotNull()
    }

    @Test
    @Sql("classpath:test_data/seed-activity-with-future-exclusions.sql")
    fun `get scheduled attendees by scheduled instance id - respects future exclusions`() {
      val attendees = webTestClient.getScheduledAttendeesByInstanceId(3)!!
      assertThat(attendees).hasSize(1)
      with(attendees[0]) { assertThat(prisonerNumber).isEqualTo("G4793VF") }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-with-session-in-the-past.sql")
    fun `get scheduled attendees by scheduled instance id - with sessions in the past should appear`() {
      val attendees = webTestClient.getScheduledAttendeesByInstanceId(43)!!
      assertThat(attendees).hasSize(2)
      assertThat(attendees.find { it.prisonerNumber == "G6268GL" }).isNotNull()
      assertThat(attendees.find { it.prisonerNumber == "G4206GA" }).isNotNull()
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-1.sql")
    fun `403 when fetching a schedule by its id for the wrong caseload`() {
      webTestClient.get()
        .uri("/scheduled-instances/1/scheduled-attendees")
        .accept(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(isClientToken = false, roles = listOf(ROLE_PRISON)))
        .header(CASELOAD_ID, "MDI")
        .exchange()
        .expectStatus().isForbidden
    }
  }

  @Nested
  @DisplayName("getActivityScheduleInstances")
  inner class GetActivityScheduleInstances {
    @Test
    @Sql("classpath:test_data/seed-activity-id-3.sql")
    fun `returns all 20 rows within the time slot`() {
      val startDate = LocalDate.of(2022, 10, 1)
      val endDate = LocalDate.of(2022, 11, 5)

      val scheduledInstances = webTestClient.getScheduledInstancesBy(MOORLAND_PRISON_CODE, startDate, endDate)

      assertThat(scheduledInstances).hasSize(20)
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-3.sql")
    fun `returns 18 rows within the time slot ignoring cancelled instances`() {
      val startDate = LocalDate.of(2022, 10, 1)
      val endDate = LocalDate.of(2022, 11, 5)

      val scheduledInstances = webTestClient.getScheduledInstancesBy(MOORLAND_PRISON_CODE, startDate, endDate, null, false)

      assertThat(scheduledInstances).hasSize(18)
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-3.sql")
    fun `returns 2 rows within the time slot for only cancelled instances`() {
      val startDate = LocalDate.of(2022, 10, 1)
      val endDate = LocalDate.of(2022, 11, 5)

      val scheduledInstances = webTestClient.getScheduledInstancesBy(MOORLAND_PRISON_CODE, startDate, endDate, null, true)

      assertThat(scheduledInstances).hasSize(2)
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-3.sql")
    fun `returns 10 rows with the time slot filter`() {
      val startDate = LocalDate.of(2022, 10, 1)
      val endDate = LocalDate.of(2022, 11, 5)

      val scheduledInstances =
        webTestClient.getScheduledInstancesBy(MOORLAND_PRISON_CODE, startDate, endDate, TimeSlot.AM)

      assertThat(scheduledInstances).hasSize(10)
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-3.sql")
    fun `date range precludes 4 rows from the sample of 20`() {
      val startDate = LocalDate.of(2022, 10, 2)
      val endDate = LocalDate.of(2022, 11, 4)

      val scheduledInstances = webTestClient.getScheduledInstancesBy(MOORLAND_PRISON_CODE, startDate, endDate)

      assertThat(scheduledInstances).hasSize(16)
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-10.sql")
    fun `returns instance when no allocations`() {
      val startDate = LocalDate.of(2022, 10, 1)
      val endDate = LocalDate.of(2022, 11, 5)

      val scheduledInstances =
        webTestClient.getScheduledInstancesBy(MOORLAND_PRISON_CODE, startDate, endDate, TimeSlot.AM)

      assertThat(scheduledInstances).hasSize(1)
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-10.sql")
    fun `returns no instance when match on time slot`() {
      val startDate = LocalDate.of(2022, 10, 1)
      val endDate = LocalDate.of(2022, 11, 5)

      val scheduledInstances =
        webTestClient.getScheduledInstancesBy(MOORLAND_PRISON_CODE, startDate, endDate, TimeSlot.PM)

      assertThat(scheduledInstances).isEmpty()
    }
  }

  @Nested
  @DisplayName("uncancelScheduledInstance")
  inner class UncancelScheduledInstance {

    @Test
    @Sql("classpath:test_data/seed-activity-id-13.sql")
    fun `scheduled instance is uncancelled`() {
      val response = webTestClient.uncancelScheduledInstance(1, "CAN1234", "Mr Cancel")
      response.expectStatus().isNoContent

      with(webTestClient.getScheduledInstanceById(1)!!) {
        assertThat(cancelled).isFalse
        assertThat(cancelledBy).isNull()
        assertThat(cancelledIssuePayment).isNull()

        with(attendances.first()) {
          assertThat(attendanceReason).isNull()
          assertThat(status).isEqualTo("WAITING")
          assertThat(comment).isNull()
          assertThat(recordedBy).isNull()
          assertThat(editable).isTrue
          assertThat(issuePayment).isNull()
        }
      }

      verify(eventsPublisher, times(2)).send(eventCaptor.capture())

      with(eventCaptor.firstValue) {
        assertThat(eventType).isEqualTo("activities.scheduled-instance.amended")
        assertThat(additionalInformation).isEqualTo(ScheduledInstanceInformation(1))
        assertThat(occurredAt).isCloseTo(TimeSource.now(), within(60, ChronoUnit.SECONDS))
        assertThat(description).isEqualTo("A scheduled instance has been amended in the activities management service")
      }

      with(eventCaptor.secondValue) {
        eventType isEqualTo "activities.prisoner.attendance-amended"
        additionalInformation isEqualTo PrisonerAttendanceInformation(1)
        occurredAt isCloseTo TimeSource.now()
      }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-29.sql")
    fun `scheduled instance is uncancelled when session is cancelled in the past`() {
      val response = webTestClient.uncancelScheduledInstance(122405, "CAN1234", "Mr Cancel")
      response.expectStatus().isNoContent

      with(webTestClient.getScheduledInstanceById(122405)!!) {
        assertThat(cancelled).isFalse
        assertThat(cancelledBy).isNull()
        assertThat(cancelledIssuePayment).isNull()

        with(attendances.first()) {
          assertThat(attendanceReason).isNull()
          assertThat(status).isEqualTo("WAITING")
          assertThat(comment).isNull()
          assertThat(recordedBy).isNull()
          assertThat(editable).isTrue
          assertThat(issuePayment).isNull()
        }
      }

      verify(eventsPublisher, times(2)).send(eventCaptor.capture())

      with(eventCaptor.firstValue) {
        assertThat(eventType).isEqualTo("activities.scheduled-instance.amended")
        assertThat(additionalInformation).isEqualTo(ScheduledInstanceInformation(122405))
        assertThat(occurredAt).isCloseTo(TimeSource.now(), within(60, ChronoUnit.SECONDS))
        assertThat(description).isEqualTo("A scheduled instance has been amended in the activities management service")
      }

      with(eventCaptor.secondValue) {
        eventType isEqualTo "activities.prisoner.attendance-amended"
        additionalInformation isEqualTo PrisonerAttendanceInformation(680879)
        occurredAt isCloseTo TimeSource.now()
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
  @DisplayName("updateScheduledInstance")
  inner class UpdateScheduledInstance {

    @Test
    @Sql("classpath:test_data/seed-activity-id-38.sql")
    fun `scheduled instance is updated with reason and comment`() {
      webTestClient.updateScheduledInstance(1, "New reason", "New comment", null)
        .expectStatus().isNoContent

      with(webTestClient.getScheduledInstanceById(1)!!) {
        assertThat(cancelled).isTrue
        assertThat(cancelledBy).isEqualTo("test-client")
        assertThat(cancelledReason).isEqualTo("New reason")
        assertThat(comment).isEqualTo("New comment")
        assertThat(cancelledTime).isCloseTo(TimeSource.now(), within(60, ChronoUnit.SECONDS))
        assertThat(cancelledIssuePayment).isFalse

        with(attendances.first()) {
          assertThat(attendanceReason?.id).isEqualTo(8)
          assertThat(status).isEqualTo("COMPLETED")
          assertThat(comment).isEqualTo("Old comment")
          assertThat(recordedBy).isEqualTo("Old user")
          assertThat(recordedTime).isCloseTo(TimeSource.now().minusDays(1), within(60, ChronoUnit.SECONDS))
          assertThat(editable).isTrue
          assertThat(issuePayment).isFalse
        }
      }

      verify(eventsPublisher, times(1)).send(eventCaptor.capture())

      with(eventCaptor.firstValue) {
        assertThat(eventType).isEqualTo("activities.scheduled-instance.amended")
        assertThat(additionalInformation).isEqualTo(ScheduledInstanceInformation(1))
        assertThat(occurredAt).isCloseTo(TimeSource.now(), within(60, ChronoUnit.SECONDS))
        assertThat(description).isEqualTo("A scheduled instance has been amended in the activities management service")
      }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-38.sql")
    fun `scheduled instance is updated with reason and issue payment`() {
      webTestClient.updateScheduledInstance(1, "New reason", "New comment", true)
        .expectStatus().isNoContent

      with(webTestClient.getScheduledInstanceById(1)!!) {
        assertThat(cancelled).isTrue
        assertThat(cancelledBy).isEqualTo("test-client")
        assertThat(cancelledReason).isEqualTo("New reason")
        assertThat(comment).isEqualTo("New comment")
        assertThat(cancelledTime).isCloseTo(TimeSource.now(), within(60, ChronoUnit.SECONDS))
        assertThat(cancelledIssuePayment).isTrue

        with(attendances.first()) {
          assertThat(attendanceReason?.id).isEqualTo(8)
          assertThat(status).isEqualTo("COMPLETED")
          assertThat(comment).isEqualTo("New reason")
          assertThat(recordedBy).isEqualTo("test-client")
          assertThat(recordedTime).isCloseTo(TimeSource.now(), within(60, ChronoUnit.SECONDS))
          assertThat(editable).isTrue
          assertThat(issuePayment).isTrue
        }
      }

      verify(eventsPublisher, times(2)).send(eventCaptor.capture())

      with(eventCaptor.firstValue) {
        assertThat(eventType).isEqualTo("activities.scheduled-instance.amended")
        assertThat(additionalInformation).isEqualTo(ScheduledInstanceInformation(1))
        assertThat(occurredAt).isCloseTo(TimeSource.now(), within(60, ChronoUnit.SECONDS))
        assertThat(description).isEqualTo("A scheduled instance has been amended in the activities management service")
      }

      with(eventCaptor.secondValue) {
        eventType isEqualTo "activities.prisoner.attendance-amended"
        additionalInformation isEqualTo PrisonerAttendanceInformation(1)
        occurredAt isCloseTo TimeSource.now()
      }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-38.sql")
    fun `only issue payment is updated`() {
      webTestClient.updateScheduledInstance(1, null, null, true)
        .expectStatus().isNoContent

      with(webTestClient.getScheduledInstanceById(1)!!) {
        assertThat(cancelled).isTrue
        assertThat(cancelledBy).isEqualTo("Old user")
        assertThat(cancelledReason).isEqualTo("Old reason")
        assertThat(comment).isNull()
        assertThat(cancelledTime).isCloseTo(TimeSource.now().minusDays(1), within(60, ChronoUnit.SECONDS))
        assertThat(cancelledIssuePayment).isTrue

        with(attendances.first()) {
          assertThat(attendanceReason?.id).isEqualTo(8)
          assertThat(status).isEqualTo("COMPLETED")
          assertThat(comment).isEqualTo("Old comment")
          assertThat(recordedBy).isEqualTo("test-client")
          assertThat(recordedTime).isCloseTo(TimeSource.now(), within(60, ChronoUnit.SECONDS))
          assertThat(editable).isTrue
          assertThat(issuePayment).isTrue
        }
      }

      verify(eventsPublisher, times(2)).send(eventCaptor.capture())

      with(eventCaptor.firstValue) {
        assertThat(eventType).isEqualTo("activities.scheduled-instance.amended")
        assertThat(additionalInformation).isEqualTo(ScheduledInstanceInformation(1))
        assertThat(occurredAt).isCloseTo(TimeSource.now(), within(60, ChronoUnit.SECONDS))
        assertThat(description).isEqualTo("A scheduled instance has been amended in the activities management service")
      }

      with(eventCaptor.secondValue) {
        eventType isEqualTo "activities.prisoner.attendance-amended"
        additionalInformation isEqualTo PrisonerAttendanceInformation(1)
        occurredAt isCloseTo TimeSource.now()
      }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-38.sql")
    fun `non-cancelled attendances are ignored`() {
      webTestClient.updateScheduledInstance(2, "New reason", "New comment", true)
        .expectStatus().isNoContent

      with(webTestClient.getScheduledInstanceById(2)!!) {
        assertThat(cancelled).isTrue
        assertThat(cancelledBy).isEqualTo("test-client")
        assertThat(cancelledReason).isEqualTo("New reason")
        assertThat(comment).isEqualTo("New comment")
        assertThat(cancelledTime).isCloseTo(TimeSource.now(), within(60, ChronoUnit.SECONDS))
        assertThat(cancelledIssuePayment).isTrue
        assertThat(attendances).hasSize(10)

        val cancelledAttendances = attendances.filter { it.id == 9L }
        assertThat(cancelledAttendances).hasSize(1)

        with(cancelledAttendances.first()) {
          assertThat(attendanceReason?.id).isEqualTo(8)
          assertThat(status).isEqualTo("COMPLETED")
          assertThat(comment).isEqualTo("New reason")
          assertThat(recordedBy).isEqualTo("test-client")
          assertThat(recordedTime).isCloseTo(TimeSource.now(), within(60, ChronoUnit.SECONDS))
          assertThat(editable).isTrue
          assertThat(issuePayment).isTrue
        }

        val nonCancelledAttendances = attendances.filterNot { it.id == 9L }
        assertThat(nonCancelledAttendances).hasSize(9)
        nonCancelledAttendances.forEach {
          assertThat(it.attendanceReason?.id).isNotEqualTo(8)
          assertThat(it.status).isEqualTo("COMPLETED")
          assertThat(it.comment).isEqualTo("Old comment")
          assertThat(it.recordedBy).isEqualTo("Old user")
          assertThat(it.recordedTime).isCloseTo(TimeSource.now().minusDays(1), within(60, ChronoUnit.SECONDS))
          assertThat(it.issuePayment).isFalse
        }
      }

      verify(eventsPublisher, times(2)).send(eventCaptor.capture())

      with(eventCaptor.firstValue) {
        assertThat(eventType).isEqualTo("activities.scheduled-instance.amended")
        assertThat(additionalInformation).isEqualTo(ScheduledInstanceInformation(2))
        assertThat(occurredAt).isCloseTo(TimeSource.now(), within(60, ChronoUnit.SECONDS))
        assertThat(description).isEqualTo("A scheduled instance has been amended in the activities management service")
      }

      with(eventCaptor.secondValue) {
        eventType isEqualTo "activities.prisoner.attendance-amended"
        additionalInformation isEqualTo PrisonerAttendanceInformation(9)
        occurredAt isCloseTo TimeSource.now()
      }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-37.sql")
    fun `400 - fails - as session is not cancelled`() {
      webTestClient.updateScheduledInstance(3, null, null, true)
        .expectStatus().isBadRequest
        .expectBody().jsonPath("developerMessage").isEqualTo("Cannot update Maths PM (PM) because it is not cancelled")

      with(webTestClient.getScheduledInstanceById(3)!!) {
        assertThat(cancelled).isFalse
        assertThat(cancelledBy).isNull()
        assertThat(cancelledReason).isNull()
        assertThat(cancelledTime).isNull()
        assertThat(cancelledIssuePayment).isNull()
      }

      verifyNoInteractions(eventsPublisher)
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-37.sql")
    fun `400 - fails - as session is not payable when trying to issue payment`() {
      webTestClient.updateScheduledInstance(1, null, null, true)
        .expectStatus().isBadRequest
        .expectBody().jsonPath("developerMessage").isEqualTo("Cannot issue payment for Maths AM (AM) because it is not payable")

      with(webTestClient.getScheduledInstanceById(1)!!) {
        assertThat(cancelled).isTrue
        assertThat(cancelledBy).isEqualTo("USER1")
        assertThat(cancelledReason).isEqualTo("Location unavailable")
        assertThat(cancelledTime).isNotNull()
        assertThat(cancelledTime).isNotNull()
        assertThat(cancelledIssuePayment).isFalse

        assertThat(attendances).hasSize(2)
        attendances.forEach { attendance ->
          assertThat(attendance.status).isEqualTo(AttendanceStatus.COMPLETED.toString())
          assertThat(attendance.recordedBy).isEqualTo("USER1")
          assertThat(attendance.recordedTime).isNotNull
          assertThat(attendance.editable).isTrue
        }
      }

      verifyNoInteractions(eventsPublisher)
    }
  }

  @Nested
  @DisplayName("cancelScheduledInstance")
  inner class CancelScheduledInstance {
    @Test
    @Sql("classpath:test_data/seed-activity-id-16.sql")
    fun success() {
      webTestClient.cancelScheduledInstance(1, "Location unavailable", "USER1")

      with(webTestClient.getScheduledInstanceById(1)!!) {
        assertThat(cancelled).isTrue
        assertThat(cancelledBy).isEqualTo("USER1")
        assertThat(cancelledIssuePayment).isNull()

        with(attendances.first()) {
          assertThat(attendanceReason!!.code).isEqualTo("CANCELLED")
          assertThat(status).isEqualTo("COMPLETED")
          assertThat(comment).isEqualTo("Location unavailable")
          assertThat(recordedBy).isEqualTo("USER1")
          assertThat(recordedTime).isNotNull
          assertThat(editable).isTrue
          assertThat(issuePayment).isTrue
        }
      }

      verify(eventsPublisher, times(3)).send(eventCaptor.capture())

      val allEvents = eventCaptor.allValues
      assertThat(allEvents.size).isEqualTo(3)
      val scheduledInstanceAmendedEvent = allEvents.first { e -> e.additionalInformation == ScheduledInstanceInformation(1) }
      val attendance1AmendedEvent = allEvents.first { e -> e.additionalInformation == PrisonerAttendanceInformation(1) }
      val attendance2AmendedEvent = allEvents.first { e -> e.additionalInformation == PrisonerAttendanceInformation(2) }

      with(scheduledInstanceAmendedEvent) {
        assertThat(eventType).isEqualTo("activities.scheduled-instance.amended")
        assertThat(occurredAt).isCloseTo(java.time.LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
        assertThat(description).isEqualTo("A scheduled instance has been amended in the activities management service")
      }

      with(attendance1AmendedEvent) {
        eventType isEqualTo "activities.prisoner.attendance-amended"
        occurredAt isCloseTo TimeSource.now()
      }

      with(attendance2AmendedEvent) {
        eventType isEqualTo "activities.prisoner.attendance-amended"
        occurredAt isCloseTo TimeSource.now()
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
        .expectBody().jsonPath("developerMessage").isEqualTo("Maths PM (PM) has ended")
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-16.sql")
    fun `400 - scheduled instance has been canceled`() {
      val response = webTestClient.cancelScheduledInstance(3, "Location unavailable", "USER1")
      response
        .expectStatus().isBadRequest
        .expectBody().jsonPath("developerMessage").isEqualTo("Maths PM (PM) has already been cancelled")
    }
  }

  @Nested
  @DisplayName("cancelScheduledInstances")
  inner class CancelScheduledInstances {
    @Test
    @Sql("classpath:test_data/seed-activity-id-35.sql")
    fun success() {
      webTestClient.cancelScheduledInstances(listOf(1, 4), "Location unavailable", "USER1", "comment 1", true)
        .expectStatus().isNoContent

      with(webTestClient.getScheduledInstanceById(1)!!) {
        assertThat(cancelled).isTrue
        assertThat(cancelledBy).isEqualTo("USER1")
        assertThat(cancelledIssuePayment).isTrue

        with(attendances.first()) {
          assertThat(attendanceReason!!.code).isEqualTo("CANCELLED")
          assertThat(status).isEqualTo("COMPLETED")
          assertThat(comment).isEqualTo("Location unavailable")
          assertThat(recordedBy).isEqualTo("USER1")
          assertThat(recordedTime).isNotNull
          assertThat(editable).isTrue
          assertThat(issuePayment).isTrue
        }
      }

      with(webTestClient.getScheduledInstanceById(4)!!) {
        assertThat(cancelled).isTrue
        assertThat(cancelledBy).isEqualTo("USER1")

        with(attendances.first()) {
          assertThat(attendanceReason!!.code).isEqualTo("CANCELLED")
          assertThat(status).isEqualTo("COMPLETED")
          assertThat(comment).isEqualTo("Location unavailable")
          assertThat(recordedBy).isEqualTo("USER1")
          assertThat(recordedTime).isNotNull
          assertThat(editable).isTrue
          assertThat(issuePayment).isFalse
        }
      }

      verify(eventsPublisher, times(5)).send(eventCaptor.capture())

      val allEvents = eventCaptor.allValues
      assertThat(allEvents.size).isEqualTo(5)
      val scheduledInstance1AmendedEvent =
        allEvents.first { e -> e.additionalInformation == ScheduledInstanceInformation(1) }
      val attendance1AmendedEvent = allEvents.first { e -> e.additionalInformation == PrisonerAttendanceInformation(1) }
      val attendance2AmendedEvent = allEvents.first { e -> e.additionalInformation == PrisonerAttendanceInformation(2) }

      with(scheduledInstance1AmendedEvent) {
        assertThat(eventType).isEqualTo("activities.scheduled-instance.amended")
        assertThat(occurredAt).isCloseTo(java.time.LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
        assertThat(description).isEqualTo("A scheduled instance has been amended in the activities management service")
      }

      with(attendance1AmendedEvent) {
        eventType isEqualTo "activities.prisoner.attendance-amended"
        occurredAt isCloseTo TimeSource.now()
      }

      with(attendance2AmendedEvent) {
        eventType isEqualTo "activities.prisoner.attendance-amended"
        occurredAt isCloseTo TimeSource.now()
      }

      val scheduledInstance2AmendedEvent =
        allEvents.first { e -> e.additionalInformation == ScheduledInstanceInformation(4) }
      val attendance3AmendedEvent = allEvents.first { e -> e.additionalInformation == PrisonerAttendanceInformation(3) }

      with(scheduledInstance2AmendedEvent) {
        assertThat(eventType).isEqualTo("activities.scheduled-instance.amended")
        assertThat(occurredAt).isCloseTo(java.time.LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
        assertThat(description).isEqualTo("A scheduled instance has been amended in the activities management service")
      }

      with(attendance3AmendedEvent) {
        eventType isEqualTo "activities.prisoner.attendance-amended"
        occurredAt isCloseTo TimeSource.now()
      }
    }
  }

  @Nested
  @DisplayName("uncancelScheduledInstances")
  inner class UncancelScheduledInstances {
    @Test
    @Sql("classpath:test_data/seed-activity-id-37.sql")
    fun `200 - success`() {
      webTestClient.uncancelScheduledInstances(listOf(1, 4))
        .expectStatus().isNoContent

      with(webTestClient.getScheduledInstanceById(1)!!) {
        assertThatInstanceIsUncancelled(this)
        assertThat(attendances).hasSize(2)
        assertThatAttendanceIsReset(attendances[0])
        assertThatAttendanceIsReset(attendances[1])
      }

      with(webTestClient.getScheduledInstanceById(4)!!) {
        assertThatInstanceIsUncancelled(this)
        assertThat(attendances).hasSize(1)
        assertThatAttendanceIsReset(attendances[0])
      }

      verify(eventsPublisher, times(5)).send(eventCaptor.capture())

      val allEvents = eventCaptor.allValues
      assertThat(allEvents.size).isEqualTo(5)
      val scheduledInstance1AmendedEvent =
        allEvents.first { e -> e.additionalInformation == ScheduledInstanceInformation(1) }
      val attendance1AmendedEvent = allEvents.first { e -> e.additionalInformation == PrisonerAttendanceInformation(1) }
      val attendance2AmendedEvent = allEvents.first { e -> e.additionalInformation == PrisonerAttendanceInformation(2) }

      with(scheduledInstance1AmendedEvent) {
        assertThat(eventType).isEqualTo("activities.scheduled-instance.amended")
        assertThat(occurredAt).isCloseTo(java.time.LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
        assertThat(description).isEqualTo("A scheduled instance has been amended in the activities management service")
      }

      with(allEvents.first { e -> e.additionalInformation == ScheduledInstanceInformation(1) }) {
        assertThat(eventType).isEqualTo("activities.scheduled-instance.amended")
        assertThat(occurredAt).isCloseTo(java.time.LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
        assertThat(description).isEqualTo("A scheduled instance has been amended in the activities management service")
      }

      with(allEvents.first { e -> e.additionalInformation == ScheduledInstanceInformation(4) }) {
        assertThat(eventType).isEqualTo("activities.scheduled-instance.amended")
        assertThat(occurredAt).isCloseTo(java.time.LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
        assertThat(description).isEqualTo("A scheduled instance has been amended in the activities management service")
      }

      with(attendance1AmendedEvent) {
        eventType isEqualTo "activities.prisoner.attendance-amended"
        occurredAt isCloseTo TimeSource.now()
      }

      with(attendance2AmendedEvent) {
        eventType isEqualTo "activities.prisoner.attendance-amended"
        occurredAt isCloseTo TimeSource.now()
      }

      val scheduledInstance2AmendedEvent =
        allEvents.first { e -> e.additionalInformation == ScheduledInstanceInformation(4) }
      val attendance3AmendedEvent = allEvents.first { e -> e.additionalInformation == PrisonerAttendanceInformation(3) }

      with(scheduledInstance2AmendedEvent) {
        assertThat(eventType).isEqualTo("activities.scheduled-instance.amended")
        assertThat(occurredAt).isCloseTo(java.time.LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
        assertThat(description).isEqualTo("A scheduled instance has been amended in the activities management service")
      }

      with(attendance3AmendedEvent) {
        eventType isEqualTo "activities.prisoner.attendance-amended"
        occurredAt isCloseTo TimeSource.now()
      }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-37.sql")
    fun `200 - success - ignores attendances which are not cancelled`() {
      webTestClient.uncancelScheduledInstances(listOf(7))
        .expectStatus().isNoContent

      with(webTestClient.getScheduledInstanceById(7)!!) {
        assertThatInstanceIsUncancelled(this)
        assertThat(attendances).hasSize(9)
        attendances.forEach { attendance ->
          assertThat(attendance.status).isEqualTo(AttendanceStatus.COMPLETED.toString())
        }
      }

      verify(eventsPublisher).send(eventCaptor.capture())

      val allEvents = eventCaptor.allValues
      assertThat(allEvents.size).isEqualTo(1)
      val scheduledInstanceEvent = eventCaptor.firstValue

      with(scheduledInstanceEvent) {
        assertThat(eventType).isEqualTo("activities.scheduled-instance.amended")
        assertThat(occurredAt).isCloseTo(java.time.LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
        assertThat(description).isEqualTo("A scheduled instance has been amended in the activities management service")
      }
    }

    @Test
    @Sql("classpath:test_data/seed-activity-id-37.sql")
    fun `400 - fails - a session is not cancelled`() {
      webTestClient.uncancelScheduledInstances(listOf(1, 5))
        .expectStatus().isBadRequest
        .expectBody().jsonPath("developerMessage").isEqualTo("Cannot uncancel scheduled instance [5] because it is not cancelled")

      with(webTestClient.getScheduledInstanceById(1)!!) {
        assertThat(cancelled).isTrue
        assertThat(cancelledBy).isEqualTo("USER1")
        assertThat(cancelledReason).isEqualTo("Location unavailable")
        assertThat(cancelledTime).isNotNull()

        assertThat(attendances).hasSize(2)
        attendances.forEach { attendance ->
          assertThat(attendance.status).isEqualTo(AttendanceStatus.COMPLETED.toString())
          assertThat(attendance.recordedBy).isEqualTo("USER1")
          assertThat(attendance.recordedTime).isNotNull
          assertThat(attendance.editable).isTrue
        }
      }

      with(webTestClient.getScheduledInstanceById(5)!!) {
        assertThatInstanceIsUncancelled(this)

        assertThat(attendances).hasSize(1)
        attendances.forEach { attendance ->
          assertThat(attendance.status).isEqualTo(AttendanceStatus.WAITING.toString())
          assertThat(attendance.recordedBy).isNull()
          assertThat(attendance.recordedTime).isNull()
          assertThat(attendance.editable).isTrue
        }
      }

      verifyNoInteractions(eventsPublisher)
    }

    private fun assertThatInstanceIsUncancelled(instances: ActivityScheduleInstance) = with(instances) {
      assertThat(cancelled).isFalse
      assertThat(cancelledBy).isNull()
      assertThat(cancelledReason).isNull()
      assertThat(cancelledTime).isNull()
      assertThat(cancelledIssuePayment).isNull()
    }

    private fun assertThatAttendanceIsReset(attendances: Attendance) = with(attendances) {
      assertThat(attendanceReason).isNull()
      assertThat(status).isEqualTo("WAITING")
      assertThat(comment).isNull()
      assertThat(recordedBy).isNull()
      assertThat(recordedTime).isNotNull
      assertThat(editable).isTrue
      assertThat(issuePayment).isNull()
      assertThat(bonusAmount).isNull()
      assertThat(pieces).isNull()
      assertThat(caseNoteText).isNull()
      assertThat(otherAbsenceReason).isNull()
    }
  }

  @Test
  @Sql("classpath:test_data/seed-activity-id-35.sql")
  fun `400 - a scheduled instance has already been cancelled`() {
    val response = webTestClient.cancelScheduledInstances(listOf(4, 3), "Location unavailable", "USER2", "comment 1", false)

    response
      .expectStatus().isBadRequest
      .expectBody().jsonPath("developerMessage").isEqualTo("Maths PM (PM) has already been cancelled")

    with(webTestClient.getScheduledInstanceById(3)!!) {
      assertThat(cancelled).isTrue
      assertThat(cancelledBy).isEqualTo("USER1")
      assertThat(cancelledIssuePayment).isTrue
    }

    with(webTestClient.getScheduledInstanceById(4)!!) {
      assertThat(cancelled).isFalse
      assertThat(cancelledBy).isNull()
      assertThat(cancelledIssuePayment).isNull()

      with(attendances.first()) {
        assertThat(attendanceReason).isNull()
        assertThat(status).isEqualTo("WAITING")
        assertThat(comment).isNull()
        assertThat(recordedBy).isNull()
        assertThat(recordedTime).isNull()
        assertThat(issuePayment).isNull()
      }
    }
    verifyNoInteractions(eventsPublisher)
  }

  @Test
  @Sql("classpath:test_data/seed-activity-id-35.sql")
  fun `400 - a scheduled instance has already ended`() {
    val response = webTestClient.cancelScheduledInstances(listOf(4, 2), "Location unavailable", "USER2", "comment 1", true)

    response
      .expectStatus().isBadRequest
      .expectBody().jsonPath("developerMessage").isEqualTo("Maths PM (PM) has ended")

    with(webTestClient.getScheduledInstanceById(2)!!) {
      assertThat(cancelled).isFalse
      assertThat(cancelledBy).isNull()
      assertThat(cancelledIssuePayment).isNull()
    }

    with(webTestClient.getScheduledInstanceById(4)!!) {
      assertThat(cancelled).isFalse
      assertThat(cancelledBy).isNull()
      assertThat(cancelledIssuePayment).isNull()

      with(attendances.first()) {
        assertThat(attendanceReason).isNull()
        assertThat(status).isEqualTo("WAITING")
        assertThat(comment).isNull()
        assertThat(recordedBy).isNull()
        assertThat(recordedTime).isNull()
        assertThat(issuePayment).isNull()
      }
    }
    verifyNoInteractions(eventsPublisher)
  }

  @Test
  @Sql("classpath:test_data/seed-activity-id-16.sql")
  fun `return attendance summary`() {
    val response = webTestClient.getAttendanceSummary("PVI", LocalDate.now())
    assertThat(response).isEqualTo(
      listOf(
        ScheduledInstanceAttendanceSummary(
          scheduledInstanceId = 1,
          activityId = 1,
          activityScheduleId = 1,
          summary = "Maths",
          categoryId = 1,
          sessionDate = LocalDate.now(),
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(11, 0),
          inCell = false,
          onWing = false,
          offWing = false,
          attendanceRequired = true,
          internalLocation = InternalLocation(
            id = 1,
            code = "L1",
            description = "Location 1",
          ),
          cancelled = false,
          timeSlot = TimeSlot.AM,
          attendanceSummary = ScheduledInstanceAttendanceSummary.AttendanceSummaryDetails(
            allocations = 2,
            attendees = 2,
            notRecorded = 2,
            attended = 0,
            absences = 0,
            paid = 0,
          ),
        ),
        ScheduledInstanceAttendanceSummary(
          scheduledInstanceId = 3,
          activityId = 1,
          activityScheduleId = 2,
          summary = "Maths",
          categoryId = 1,
          sessionDate = LocalDate.now(),
          startTime = LocalTime.of(14, 0),
          endTime = LocalTime.of(15, 0),
          inCell = false,
          onWing = false,
          offWing = false,
          attendanceRequired = true,
          internalLocation = InternalLocation(
            id = 2,
            code = "L2",
            description = "Location 2",
          ),
          cancelled = true,
          timeSlot = TimeSlot.PM,
          attendanceSummary = ScheduledInstanceAttendanceSummary.AttendanceSummaryDetails(
            allocations = 2,
          ),
        ),
      ),
    )
  }

  private fun WebTestClient.getScheduledInstanceById(id: Long) = get()
    .uri("/scheduled-instances/$id")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
    .header(CASELOAD_ID, "PVI")
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(ActivityScheduleInstance::class.java)
    .returnResult().responseBody

  private fun WebTestClient.getScheduledAttendeesByInstanceId(id: Long) = get()
    .uri("/scheduled-instances/$id/scheduled-attendees")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
    .header(CASELOAD_ID, "PVI")
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList(ScheduledAttendee::class.java)
    .returnResult().responseBody

  private fun WebTestClient.uncancelScheduledInstance(id: Long, username: String, displayName: String) = put()
    .uri("/scheduled-instances/$id/uncancel")
    .bodyValue(UncancelScheduledInstanceRequest(username, displayName))
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
    .exchange()

  private fun WebTestClient.updateScheduledInstance(id: Long, cancelledReason: String?, comment: String?, issuePayment: Boolean?) = put()
    .uri("/scheduled-instances/$id")
    .bodyValue(ScheduledInstancedUpdateRequest(cancelledReason, comment, issuePayment))
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
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
    .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
    .exchange()

  private fun WebTestClient.cancelScheduledInstances(
    ids: List<Long>,
    reason: String,
    username: String,
    comment: String? = null,
    issuePayment: Boolean? = true,
  ) = put()
    .uri("/scheduled-instances/cancel")
    .bodyValue(ScheduleInstancesCancelRequest(ids, reason, username, comment, issuePayment))
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
    .exchange()

  private fun WebTestClient.uncancelScheduledInstances(
    ids: List<Long>,
  ) = put()
    .uri("/scheduled-instances/uncancel")
    .bodyValue(ScheduleInstancesUncancelRequest(ids))
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
    .exchange()

  private fun WebTestClient.getScheduledInstancesBy(
    prisonCode: String,
    startDate: LocalDate,
    endDate: LocalDate,
    timeSlot: TimeSlot? = null,
    cancelled: Boolean? = null,
  ) = get()
    .uri { builder ->
      builder
        .path("/prisons/$prisonCode/scheduled-instances")
        .queryParam("startDate", startDate)
        .queryParam("endDate", endDate)
        .maybeQueryParam("slot", timeSlot)
        .maybeQueryParam("cancelled", cancelled)
        .build()
    }
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList(ActivityScheduleInstance::class.java)
    .returnResult().responseBody

  private fun WebTestClient.getAttendanceSummary(prisonCode: String, date: LocalDate) = get()
    .uri { builder ->
      builder.path("/scheduled-instances/attendance-summary")
        .queryParam("prisonCode", prisonCode)
        .queryParam("date", date)
        .build()
    }
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
    .header(CASELOAD_ID, PENTONVILLE_PRISON_CODE)
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList(ScheduledInstanceAttendanceSummary::class.java)
    .returnResult().responseBody
}
