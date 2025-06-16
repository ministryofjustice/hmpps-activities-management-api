package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.updatesfromexternalsystems

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AttendanceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AttendancesService
import java.util.UUID

class UpdateFromExternalSystemEventsListenerTest {
  private val objectMapper = jacksonObjectMapper()
  private val attendancesService: AttendancesService = mock()
  private val updateFromExternalSystemListener = UpdateFromExternalSystemsEventsListener(
    objectMapper,
    attendancesService,
  )

  @Test
  fun `will handle a test event passed in`() {
    val messageId = UUID.randomUUID().toString()
    val message = """
    {
      "messageId" : "$messageId",
      "eventType" : "TestEvent",
      "description" : null,
      "messageAttributes" : {},
      "who" : "automated-test-client"
    }
    """

    assertDoesNotThrow {
      updateFromExternalSystemListener.onMessage(message)
    }
  }

  @Test
  fun `will throw an an exception when an invalid event passed in`() {
    val messageId = UUID.randomUUID().toString()
    val message = """
    {
      "messageId" : "$messageId",
      "eventType" : "InvalidEventType",
      "description" : null,
      "messageAttributes" : {},
      "who" : "automated-test-client"
    }
    """

    val exception = assertThrows<Exception> {
      updateFromExternalSystemListener.onMessage(message)
    }
    assertThat(exception.message).contains("Unrecognised message type on external system event: InvalidEventType")
  }

  @Nested
  @DisplayName("MarkPrisonerAttendance")
  inner class MarkPrisonerAttendance {
    @Test
    fun `will handle a mark prisoner attendance event passed in`() {
      val messageId = UUID.randomUUID().toString()
      val message = """
      {
        "messageId" : "$messageId",
        "eventType" : "MarkPrisonerAttendance",
        "description" : null,
        "messageAttributes" : {
          "attendanceUpdateRequests": [
            {
              "id": 123456, 
              "prisonCode": "MDI", 
              "status": "WAITING", 
              "attendanceReason": "ATTENDED", 
              "comment": "Prisoner has COVID-19", 
              "issuePayment": true, 
              "caseNote": "Prisoner refused to attend the scheduled activity without reasonable excuse", 
              "incentiveLevelWarningIssued": true, 
              "otherAbsenceReason": "Prisoner has another reason for missing the activity" 
            }
          ]
        },
        "who" : "automated-test-client"
      }
      """

      assertDoesNotThrow {
        updateFromExternalSystemListener.onMessage(message)
      }
      verify(attendancesService, times(1)).mark(eq("automated-test-client"), any<List<AttendanceUpdateRequest>>())
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

      assertThrows<Exception> {
        updateFromExternalSystemListener.onMessage(message)
      }
      verify(attendancesService, times(0)).mark(any(), any<List<AttendanceUpdateRequest>>())
    }
  }
}
