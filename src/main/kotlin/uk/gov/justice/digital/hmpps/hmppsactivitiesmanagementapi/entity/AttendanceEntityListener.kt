package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.PostPersist
import jakarta.persistence.PostUpdate
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService

@Component
class AttendanceEntityListener {

  @Autowired
  private lateinit var outboundEventsService: OutboundEventsService

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @PostPersist
  fun onCreate(attendance: Attendance) {
    send(
      OutboundEvent.PRISONER_ATTENDANCE_CREATED,
      attendance.attendanceId,
      "Failed to send prisoner attendance created event for attendance ID ${attendance.attendanceId}",
    )
  }

  @PostUpdate
  fun onUpdate(attendance: Attendance) {
    send(
      OutboundEvent.PRISONER_ATTENDANCE_AMENDED,
      attendance.attendanceId,
      "Failed to send prisoner attendance amended event for attendance ID ${attendance.attendanceId}",
    )
  }

  private fun send(outboundEvent: OutboundEvent, attendanceId: Long, failureMessage: String) {
    runCatching {
      outboundEventsService.send(outboundEvent, attendanceId)
    }.onFailure {
      log.error(failureMessage, it)
    }
  }
}
