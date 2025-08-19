package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.updatesfromexternalsystems

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AttendanceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonerAllocationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonerDeallocationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ActivityScheduleService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AttendancesService
import java.time.LocalDate
import java.util.UUID

class UpdateFromExternalSystemEventsListenerTest {
  private val objectMapper = jacksonObjectMapper()
  private val attendancesService: AttendancesService = mock()
  private val activityScheduleService: ActivityScheduleService = mock()
  private val updateFromExternalSystemListener = UpdateFromExternalSystemsEventsListener(
    objectMapper,
    attendancesService,
    activityScheduleService,
  )

  @Test
  fun `will handle a test event passed in`() {
    val messageId = UUID.randomUUID().toString()
    val message = """
    {
      "messageId": "$messageId",
      "eventType": "TestEvent",
      "description": null,
      "messageAttributes": {},
      "who": "automated-test-client"
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
      "messageId": "$messageId",
      "eventType": "InvalidEventType",
      "description": null,
      "messageAttributes": {},
      "who": "automated-test-client"
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
    fun `will handle a valid event passed in`() {
      val messageId = UUID.randomUUID().toString()
      val message = """
      {
        "messageId": "$messageId",
        "eventType": "MarkPrisonerAttendance",
        "description": null,
        "messageAttributes": {
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
        "who": "automated-test-client"
      }
      """

      assertDoesNotThrow {
        updateFromExternalSystemListener.onMessage(message)
      }
      verify(attendancesService).mark(eq("automated-test-client"), any<List<AttendanceUpdateRequest>>())
    }

    @Test
    fun `will throw an error if message attributes are invalid`() {
      val messageId = UUID.randomUUID().toString()
      val message = """
      {
        "messageId": "$messageId",
        "eventType": "MarkPrisonerAttendance",
        "description": null,
        "messageAttributes": {
          "invalidField": "invalid value"
        },
        "who": "automated-test-client"
      }
      """

      assertThrows<Exception> {
        updateFromExternalSystemListener.onMessage(message)
      }
      verify(attendancesService, times(0)).mark(any(), any<List<AttendanceUpdateRequest>>())
    }
  }

  @Nested
  @DisplayName("DeallocatePrisonerFromActivitySchedule")
  inner class DeallocatePrisonerFromActivitySchedule {
    val scheduleId = 1234L
    val who = "automated-test-client"

    @Test
    fun `will handle a valid event passed in`() {
      val messageId = UUID.randomUUID().toString()
      val message = """
      {
        "messageId": "$messageId",
        "eventType": "DeallocatePrisonerFromActivitySchedule",
        "description": null,
        "messageAttributes": {
          "scheduleId": $scheduleId,
          "prisonerNumbers": [
            "A1234BC"
          ],
          "reasonCode": "RELEASED",
          "endDate": "${LocalDate.now()}",
          "caseNote": {
            "type": "GEN",
            "text": "string"
          },
          "scheduleInstanceId": 0
        },
        "who": "$who"
      }
      """

      assertDoesNotThrow {
        updateFromExternalSystemListener.onMessage(message)
      }
      verify(activityScheduleService).deallocatePrisoners(
        eq(scheduleId),
        any<PrisonerDeallocationRequest>(),
        eq(who),
      )
    }

    @Test
    fun `will throw an error if message attributes are not supplied`() {
      val messageId = UUID.randomUUID().toString()
      val message = """
      {
        "messageId": "$messageId",
        "eventType": "DeallocatePrisonerFromActivitySchedule",
        "description": null,
        "messageAttributes": {
          "invalidField": "invalid value"
        },
        "who": "automated-test-client"
      }
      """

      assertThrows<Exception> {
        updateFromExternalSystemListener.onMessage(message)
      }
      verify(activityScheduleService, never()).deallocatePrisoners(any(), any(), any())
    }

    @Test
    fun `will throw an error if message attributes fail validation`() {
      val messageId = UUID.randomUUID().toString()
      val message = """
      {
        "messageId": "$messageId",
        "eventType": "DeallocatePrisonerFromActivitySchedule",
        "description": null,
        "messageAttributes": {
          "scheduleId": $scheduleId,
          "prisonerNumbers": [],
          "reasonCode": "",
          "endDate": "2021-01-01",
          "caseNote": {
            "type": "GEN",
            "text": "string"
          },
          "scheduleInstanceId": 0
        },
        "who": "automated-test-client"
      }
      """

      assertThrows<ValidationException> {
        updateFromExternalSystemListener.onMessage(message)
      }
      verify(activityScheduleService, never()).deallocatePrisoners(any(), any(), any())
    }
  }

  @Nested
  @DisplayName("AllocatePrisonerToActivitySchedule")
  inner class AllocatePrisonerToActivitySchedule {
    val scheduleId = 1234L
    val who = "automated-test-client"

    @Test
    fun `will handle a valid event passed in`() {
      val messageId = UUID.randomUUID().toString()
      val message = """
      {
        "messageId": "$messageId",
        "eventType": "AllocatePrisonerToActivitySchedule",
        "description": null,
        "messageAttributes": {
          "scheduleId": $scheduleId,
          "prisonerNumber": "A1234BC",
          "payBandId": 123,
          "startDate": "${LocalDate.now()}",
          "endDate": "${LocalDate.now().plusMonths(1)}",
          "exclusions": [],
          "scheduleInstanceId": 0
        },
        "who": "$who"
      }
      """

      assertDoesNotThrow {
        updateFromExternalSystemListener.onMessage(message)
      }
      verify(activityScheduleService).allocatePrisoner(
        eq(scheduleId),
        any<PrisonerAllocationRequest>(),
        eq(who),
        any(),
      )
    }

    @Test
    fun `will throw an error if message attributes are not supplied`() {
      val messageId = UUID.randomUUID().toString()
      val message = """
      {
        "messageId": "$messageId",
        "eventType": "AllocatePrisonerToActivitySchedule",
        "description": null,
        "messageAttributes": {
          "invalidField": "invalid value"
        },
        "who": "automated-test-client"
      }
      """

      assertThrows<Exception> {
        updateFromExternalSystemListener.onMessage(message)
      }
      verify(activityScheduleService, never()).allocatePrisoner(any(), any(), any(), any())
    }

    @Test
    fun `will throw an error if message attributes fail validation`() {
      val messageId = UUID.randomUUID().toString()
      val message = """
      {
        "messageId": "$messageId",
        "eventType": "AllocatePrisonerToActivitySchedule",
        "description": null,
        "messageAttributes": {
          "scheduleId": $scheduleId,
          "prisonerNumber": "",
          "payBandId": 123,
          "startDate": "${LocalDate.now()}",
          "endDate": "${LocalDate.now().plusMonths(1)}",
          "exclusions": [],
          "scheduleInstanceId": 0
        },
        "who": "automated-test-client"
      }
      """

      assertThrows<ValidationException> {
        updateFromExternalSystemListener.onMessage(message)
      }
      verify(activityScheduleService, never()).allocatePrisoner(any(), any(), any(), any())
    }
  }
}
