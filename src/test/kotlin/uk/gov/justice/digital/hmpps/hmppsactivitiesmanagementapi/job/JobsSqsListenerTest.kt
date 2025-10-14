package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ExpireAttendancesService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAllocationsDueToEndService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAllocationsDueToExpireService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageNewAllocationsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageNewAttendancesService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageScheduledInstancesService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.SuspendAllocationsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.UnsuspendAllocationsService
import java.time.LocalDate

class JobsSqsListenerTest {
  val scheduledInstancesService: ManageScheduledInstancesService = mock()
  val manageAllocationsDueToEndService: ManageAllocationsDueToEndService = mock()
  val manageAllocationsDueToExpireService: ManageAllocationsDueToExpireService = mock()
  val manageNewAllocationsService: ManageNewAllocationsService = mock()
  val suspendAllocationsService: SuspendAllocationsService = mock()
  val unsuspendAllocationsService: UnsuspendAllocationsService = mock()
  val manageNewAttendancesService: ManageNewAttendancesService = mock()
  val expireAttendancesService: ExpireAttendancesService = mock()
  val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())

  val listener: JobsSqsListener = JobsSqsListener(
    scheduledInstancesService,
    manageAllocationsDueToEndService,
    manageAllocationsDueToExpireService,
    manageNewAllocationsService,
    suspendAllocationsService,
    unsuspendAllocationsService,
    manageNewAttendancesService,
    expireAttendancesService,
    mapper,
  )

  @Test
  fun `should handle SCHEDULES event`() {
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

    verify(scheduledInstancesService).handleEvent(123L, "RSI")
  }

  @Test
  fun `should handle DEALLOCATE_ENDING event`() {
    val rawMessage = """
      {
        "jobId": 123,
        "eventType": "DEALLOCATE_ENDING",
        "messageAttributes": {
          "prisonCode": "RSI"
        }
      }
    """

    listener.onMessage(rawMessage)

    verify(manageAllocationsDueToEndService).handleEvent(123L, "RSI")
  }

  @Test
  fun `should handle DEALLOCATE_EXPIRING event`() {
    val rawMessage = """
      {
        "jobId": 123,
        "eventType": "DEALLOCATE_EXPIRING",
        "messageAttributes": {
          "prisonCode": "RSI"
        }
      }
    """

    listener.onMessage(rawMessage)

    verify(manageAllocationsDueToExpireService).handleEvent(123L, "RSI")
  }

  @Test
  fun `should handle ALLOCATE event`() {
    val rawMessage = """
      {
        "jobId": 123,
        "eventType": "ALLOCATE",
        "messageAttributes": {
          "prisonCode": "RSI"
        }
      }
    """

    listener.onMessage(rawMessage)

    verify(manageNewAllocationsService).handleEvent(123L, "RSI")
  }

  @Test
  fun `should handle START_SUSPENSIONS event`() {
    val rawMessage = """
      {
        "jobId": 123,
        "eventType": "START_SUSPENSIONS",
        "messageAttributes": {
          "prisonCode": "RSI"
        }
      }
    """

    listener.onMessage(rawMessage)

    verify(suspendAllocationsService).handleEvent(123L, "RSI")
  }

  @Test
  fun `should handle END_SUSPENSIONS event`() {
    val rawMessage = """
      {
        "jobId": 123,
        "eventType": "END_SUSPENSIONS",
        "messageAttributes": {
          "prisonCode": "RSI"
        }
      }
    """

    listener.onMessage(rawMessage)

    verify(unsuspendAllocationsService).handleEvent(123L, "RSI")
  }

  @Test
  fun `should handle ATTENDANCE_CREATE event`() {
    val rawMessage = """
      {
        "jobId": 123,
        "eventType": "ATTENDANCE_CREATE",
        "messageAttributes": {
          "prisonCode": "RSI",
          "date": "2023-03-01",
          "expireUnmarkedAttendances": true
        }
      }
    """

    listener.onMessage(rawMessage)

    verify(manageNewAttendancesService).handleEvent(123L, "RSI", LocalDate.parse("2023-03-01"), true)
  }

  @Test
  fun `should handle ATTENDANCE_EXPIRE event`() {
    val rawMessage = """
      {
        "jobId": 123,
        "eventType": "ATTENDANCE_EXPIRE",
        "messageAttributes": {
          "prisonCode": "RSI"
        }
      }
    """

    listener.onMessage(rawMessage)

    verify(expireAttendancesService).handleEvent(123L, "RSI")
  }

  @Test
  fun `should throw an exception if job event cannot be handled`() {
    val rawMessage = """
      {
        "jobId": 123,
        "eventType": "CREATE_APPOINTMENTS",
        "messageAttributes": {
          "prisonCode": "RSI"
        }
      }
    """
    assertThrows<UnsupportedOperationException>("Unsupported job event: CREATE_APPOINTMENTS") {
      listener.onMessage(rawMessage)
    }

    verifyNoInteractions(scheduledInstancesService)
  }
}
