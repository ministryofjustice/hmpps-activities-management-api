package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import com.fasterxml.jackson.module.kotlin.readValue
import com.jayway.jsonpath.JsonPath
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.fail
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.config.LocalStackContainer
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.config.LocalStackContainer.setLocalStackProperties
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.InboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.ACTIVITY_SCHEDULED_INSTANCE_AMENDED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.ACTIVITY_SCHEDULE_CREATED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.ACTIVITY_SCHEDULE_UPDATED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.APPOINTMENT_INSTANCE_CANCELLED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.APPOINTMENT_INSTANCE_CREATED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.APPOINTMENT_INSTANCE_DELETED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.APPOINTMENT_INSTANCE_UNCANCELLED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.APPOINTMENT_INSTANCE_UPDATED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.PRISONER_ALLOCATED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.PRISONER_ALLOCATION_AMENDED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.PRISONER_ATTENDANCE_AMENDED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.PRISONER_ATTENDANCE_CREATED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.PRISONER_ATTENDANCE_DELETED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.PRISONER_ATTENDANCE_EXPIRED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.SQSMessage
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.LocalStackTestHelper.Companion.clearQueues
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue

@ActiveProfiles("test-localstack", inheritProfiles = false)
@TestPropertySource(
  properties = [
    "feature.audit.service.hmpps.enabled=true",
    "feature.audit.service.local.enabled=true",
    "feature.cancel.instance.priority.change.enabled=true",
    "feature.event.activities.activity-schedule.amended=true",
    "feature.event.activities.activity-schedule.created=true",
    "feature.event.activities.prisoner.allocated=true",
    "feature.event.activities.prisoner.allocation-amended=true",
    "feature.event.activities.prisoner.attendance-amended=true",
    "feature.event.activities.prisoner.attendance-created=true",
    "feature.event.activities.prisoner.attendance-deleted=true",
    "feature.event.activities.prisoner.attendance-expired=true",
    "feature.event.activities.scheduled-instance.amended=true",
    "feature.event.appointments.appointment-instance.cancelled=true",
    "feature.event.appointments.appointment-instance.created=true",
    "feature.event.appointments.appointment-instance.deleted=true",
    "feature.event.appointments.appointment-instance.uncancelled=true",
    "feature.event.appointments.appointment-instance.updated=true",
    "feature.event.prison-offender-events.prisoner.activities-changed=true",
    "feature.event.prison-offender-events.prisoner.appointments-changed=true",
    "feature.event.prison-offender-events.prisoner.merged=true",
    "feature.event.prisoner-offender-search.prisoner.alerts-updated=true",
    "feature.event.prisoner-offender-search.prisoner.received=true",
    "feature.event.prisoner-offender-search.prisoner.released=true",
    "feature.event.prisoner-offender-search.prisoner.updated=true",
    "feature.events.sns.enabled=true",
    "feature.jobs.sqs.activate.allocations.enabled=true",
    "feature.jobs.sqs.deallocate.ending.enabled=true",
    "feature.jobs.sqs.deallocate.expiring.enabled=true",
    "feature.jobs.sqs.manage.attendances.enabled=true",
    "feature.jobs.sqs.manage.appointment.attendees.enabled=true",
    "feature.offender.merge.enabled=true",
    "jobs.deallocate-allocations-ending.days-start=22",
  ],
)
abstract class LocalStackTestBase : ActivitiesIntegrationTestBase() {

  @Autowired
  lateinit var hmppsQueueService: HmppsQueueService

  private val activitiesQueue by lazy { hmppsQueueService.findByQueueId("activities") as HmppsQueue }
  private val activitiesQueueUrl by lazy { activitiesQueue.queueUrl }
  protected val activitiesClient by lazy { activitiesQueue.sqsClient }

  private val outboundTestQueue by lazy { hmppsQueueService.findByQueueId("outboundtestqueue") ?: throw MissingQueueException("HmppsQueue outboundtestqueue not found") }
  protected val outboundTestQueueUrl by lazy { outboundTestQueue.queueUrl }
  protected val outboundTestSqsClient by lazy { outboundTestQueue.sqsClient }

  companion object {
    internal val localStackContainer = LocalStackContainer.instance

    @JvmStatic
    @DynamicPropertySource
    fun properties(registry: DynamicPropertyRegistry) {
      localStackContainer?.also { setLocalStackProperties(it, registry) }
    }
  }

  @BeforeEach
  fun `clear queues`() {
    clearQueues(activitiesClient, activitiesQueue)
    clearQueues(outboundTestSqsClient, outboundTestQueue)
  }

  protected fun sendInboundEvent(event: InboundEvent) {
    val sqsMessage = SQSMessage("Notification", mapper.writeValueAsString(event))

    activitiesQueue.sqsClient.sendMessage(
      SendMessageRequest.builder()
        .queueUrl(activitiesQueueUrl)
        .messageBody(mapper.writeValueAsString(sqsMessage))
        .build(),
    )
  }

  protected fun validateNoMessagesSent() {
    await untilCallTo { outboundTestSqsClient.countMessagesOnQueue(outboundTestQueueUrl).get() } matches { it == 0 }
  }

  protected fun validateOutboundEvents(vararg expectedOutboundEvents: ExpectedOutboundEvent) {
    val totalCallsExpected = expectedOutboundEvents.sumOf { it.times }
    await untilCallTo {
      val i = outboundTestSqsClient.countMessagesOnQueue(outboundTestQueueUrl).get()
      println("Outbound queue count: $i out of expected $totalCallsExpected")
      i
    } matches { it == totalCallsExpected }

    val originalActualMessages = (1..totalCallsExpected).map {
      outboundTestSqsClient.receiveMessage(
        ReceiveMessageRequest.builder().queueUrl(outboundTestQueueUrl).maxNumberOfMessages(1).build(),
      ).get().messages()[0]
        .let {
          mapper.readValue<SQSMessage>(it.body()).Message
        }
    }

    val actualMessages = originalActualMessages.toMutableList()

    expectedOutboundEvents.forEach { expectedMessage ->
      val matched = actualMessages.filter { actualMessage ->
        compare(actualMessage, expectedMessage)
      }
      if (matched.size != expectedMessage.times) {
        fail("Not matched $expectedMessage expected messages when compared to the actual messages: $originalActualMessages")
      }
      actualMessages.removeAll(matched)
    }
  }

  private fun compare(actualMessage: String, expectedOutboundEvent: ExpectedOutboundEvent): Boolean {
    val (firstIdKey, secondIdKey) = when (expectedOutboundEvent.event) {
      ACTIVITY_SCHEDULE_CREATED, ACTIVITY_SCHEDULE_UPDATED -> "activityScheduleId" to null
      ACTIVITY_SCHEDULED_INSTANCE_AMENDED -> "scheduledInstanceId" to null
      APPOINTMENT_INSTANCE_CREATED, APPOINTMENT_INSTANCE_UPDATED, APPOINTMENT_INSTANCE_DELETED, APPOINTMENT_INSTANCE_CANCELLED, APPOINTMENT_INSTANCE_UNCANCELLED -> "appointmentInstanceId" to "categoryCode"
      PRISONER_ALLOCATED, PRISONER_ALLOCATION_AMENDED -> "allocationId" to null
      PRISONER_ATTENDANCE_CREATED, PRISONER_ATTENDANCE_AMENDED, PRISONER_ATTENDANCE_EXPIRED -> "attendanceId" to null
      PRISONER_ATTENDANCE_DELETED -> "bookingId" to "scheduledInstanceId"
    }

    val actualEventType = JsonPath.parse(actualMessage).read<String>("$.eventType")
    if (expectedOutboundEvent.event.eventType != actualEventType) return false
    val actualFirstId = JsonPath.parse(actualMessage).read<Int>("$.additionalInformation.$firstIdKey").toLong()
    if (actualFirstId != expectedOutboundEvent.firstId) return false

    if (secondIdKey == null) return true
    val actualSecondId = JsonPath.parse(actualMessage).read<Any>("$.additionalInformation.$secondIdKey")

    return when (expectedOutboundEvent.secondId) {
      is Long -> when (actualSecondId) {
        is Long -> actualSecondId == expectedOutboundEvent.secondId
        is Int -> actualSecondId.toLong() == expectedOutboundEvent.secondId
        else -> false
      }
      is String -> actualSecondId.toString() == expectedOutboundEvent.secondId
      else -> actualSecondId?.toString() == expectedOutboundEvent.secondId?.toString()
    }
  }

  data class ExpectedOutboundEvent(val event: OutboundEvent, val firstId: Long, val secondId: Any? = null, val times: Int = 1)
}
