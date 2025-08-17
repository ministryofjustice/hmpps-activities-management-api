package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageScheduledInstancesService

class JobsSqsListenerTest {
  val scheduledInstancesService: ManageScheduledInstancesService = mock()
  val mapper = jacksonObjectMapper()

  val listener: JobsSqsListener = JobsSqsListener(scheduledInstancesService, mapper)

  @Test
  fun `should handle create schedules event`() {
    val rawMessage = """
      {
        "jobId": 123,
        "eventType": "SCHEDULES",
        "messageAttributes": {
          "prisonCode": "RSI"
        }
      }
    """

    listener.onMessage(rawMessage)

    verify(scheduledInstancesService).handleCreateSchedulesEvent(123L, "RSI")
  }

  @Test
  fun `should throw an exception if job event cannot be handled`() {
    val rawMessage = """
      {
        "jobId": 123,
        "eventType": "ATTENDANCE_CREATE",
        "messageAttributes": {
          "prisonCode": "RSI"
        }
      }
    """
    assertThrows<UnsupportedOperationException>("Unsupported job event: ATTENDANCE_CREATE") {
      listener.onMessage(rawMessage)
    }

    verifyNoInteractions(scheduledInstancesService)
  }
}
