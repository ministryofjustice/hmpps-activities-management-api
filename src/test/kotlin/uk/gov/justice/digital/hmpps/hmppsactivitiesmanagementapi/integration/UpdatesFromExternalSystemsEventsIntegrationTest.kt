package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.context.jdbc.Sql
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AttendanceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AttendancesService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundHMPPSDomainEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.PrisonerAttendanceInformation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.updatesfromexternalsystems.UPDATE_FROM_EXTERNAL_SYSTEM_QUEUE_NAME
import uk.gov.justice.hmpps.sqs.MissingQueueException
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class UpdatesFromExternalSystemsEventsIntegrationTest : LocalStackTestBase() {
  @Autowired
  private lateinit var attendanceRepository: AttendanceRepository

  @MockitoSpyBean
  lateinit var attendancesService: AttendancesService

  @Autowired
  private lateinit var attendanceHistoryRepository: AttendanceHistoryRepository

  private val eventCaptor = argumentCaptor<OutboundHMPPSDomainEvent>()

  private val updateFromExternalSystemEventsQueue by lazy {
    hmppsQueueService.findByQueueId(UPDATE_FROM_EXTERNAL_SYSTEM_QUEUE_NAME) ?: throw MissingQueueException("HmppsQueue $UPDATE_FROM_EXTERNAL_SYSTEM_QUEUE_NAME not found")
  }
  internal val queueSqsClient by lazy { updateFromExternalSystemEventsQueue.sqsClient }
  internal val queueUrl by lazy { updateFromExternalSystemEventsQueue.queueUrl }
  internal val queueSqsDlqClient by lazy { updateFromExternalSystemEventsQueue.sqsDlqClient as SqsAsyncClient }
  internal val dlqUrl by lazy { updateFromExternalSystemEventsQueue.dlqUrl as String }

  fun getNumberOfMessagesCurrentlyOnQueue(): Int = queueSqsClient.countAllMessagesOnQueue(queueUrl).get()
  fun getNumberOfMessagesCurrentlyOnDlq(): Int = queueSqsDlqClient.countAllMessagesOnQueue(dlqUrl).get()

  @BeforeEach
  fun setup() {
    queueSqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(queueUrl).build())
    queueSqsDlqClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(dlqUrl).build())
    await untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }
    await untilCallTo { getNumberOfMessagesCurrentlyOnDlq() } matches { it == 0 }
  }

  @Test
  fun `will handle a test event`() {
    val messageId = UUID.randomUUID().toString()
    val message = """
    {
      "messageId" : "$messageId",
      "eventType" : "TestEvent",
      "description" : null,
      "messageAttributes" : {}
    }
    """

    queueSqsClient.sendMessage(
      SendMessageRequest.builder().queueUrl(queueUrl).messageBody(message).build(),
    )

    await untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 1 }
    await untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }
    await untilCallTo { getNumberOfMessagesCurrentlyOnDlq() } matches { it == 0 }
  }

  @Test
  fun `will write an invalid event to the dlq`() {
    val messageId = UUID.randomUUID().toString()
    val message = """
    {
      "messageId" : "$messageId",
      "eventType" : "InvalidEventType",
      "description" : null,
      "messageAttributes" : {}
    }
    """

    queueSqsClient.sendMessage(
      SendMessageRequest.builder().queueUrl(queueUrl).messageBody(message).build(),
    )

    await untilCallTo { getNumberOfMessagesCurrentlyOnDlq() } matches { it == 1 }
  }

  @Nested
  @DisplayName("MarkPrisonerAttendance")
  inner class MarkPrisonerAttendance {
    @Sql(
      "classpath:test_data/seed-activity-id-18.sql",
    )
    @Test
    fun `will handle a mark prisoner attendance event passed in`() {
      attendanceRepository.findAll().also { assertThat(it).hasSize(1) }

      val messageId = UUID.randomUUID().toString()
      val message = """
      {
        "messageId" : "$messageId",
        "eventType" : "MarkPrisonerAttendance",
        "description" : null,
        "messageAttributes" : {
          "attendanceUpdateRequests": [
            {
              "id": 1, 
              "prisonCode": "MDI", 
              "status": "COMPLETED", 
              "attendanceReason": "SICK", 
              "comment": null, 
              "issuePayment": true, 
              "caseNote": null, 
              "incentiveLevelWarningIssued": null, 
              "otherAbsenceReason": null
            }
          ]
        },
        "who" : "automated-test-client"
      }
      """

      queueSqsClient.sendMessage(
        SendMessageRequest.builder().queueUrl(queueUrl).messageBody(message).build(),
      )

      await untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 1 }
      await untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }
      await untilCallTo { getNumberOfMessagesCurrentlyOnDlq() } matches { it == 0 }

      await untilAsserted {
        verify(attendancesService, times(1)).mark(
          "automated-test-client",
          listOf(
            AttendanceUpdateRequest(
              1,
              MOORLAND_PRISON_CODE,
              AttendanceStatus.COMPLETED,
              "SICK",
              null,
              true,
              null,
              null,
              null,
            ),
          ),
        )
      }

      val updatedAttendances = attendanceRepository.findAll().toList().also { assertThat(it).hasSize(1) }
      assertThat(updatedAttendances.prisonerAttendanceReason("A11111A").code).isEqualTo(AttendanceReasonEnum.SICK)

      val history = attendanceHistoryRepository.findAll()
      assertThat(history.filter { it.attendance.attendanceId == updatedAttendances[0].attendanceId }).hasSize(1)

      verify(eventsPublisher).send(eventCaptor.capture())

      // Should detect the attendance updated event
      with(eventCaptor.firstValue) {
        assertThat(eventType).isEqualTo("activities.prisoner.attendance-amended")
        assertThat(additionalInformation).isEqualTo(PrisonerAttendanceInformation(1))
        assertThat(occurredAt).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
        assertThat(description).isEqualTo("A prisoner attendance has been amended in the activities management service")
      }
    }

    @Test
    fun `will throw an error is message attributes invalid`() {
      val messageId = UUID.randomUUID().toString()
      val message = """
      {
        "messageId" : "$messageId",
        "eventType" : "MarkPrisonerAttendance",
        "description" : null,
        "messageAttributes" : {
          "invalidField": "invalid value"
        },
        "who" : "automated-test-client"
      }
      """

      queueSqsClient.sendMessage(
        SendMessageRequest.builder().queueUrl(queueUrl).messageBody(message).build(),
      )

      await untilCallTo { getNumberOfMessagesCurrentlyOnDlq() } matches { it == 1 }
      verify(attendancesService, times(0)).mark(any(), any<List<AttendanceUpdateRequest>>())
    }
  }

  private fun List<Attendance>.prisonerAttendanceReason(prisonNumber: String) = firstOrNull { it.prisonerNumber.uppercase() == prisonNumber.uppercase() }.let { it?.attendanceReason }
    ?: throw AssertionError("Prison attendance $prisonNumber not found.")
}
