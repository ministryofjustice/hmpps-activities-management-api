package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class AttendanceEntityListenerTest(@Autowired private val listener: AttendanceEntityListener) {

  @MockBean
  private lateinit var outboundEventsService: OutboundEventsService
  private val attendance = attendance()

  @Test
  fun `prisoner attendance created event raised on creation`() {
    listener.onCreate(attendance)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ATTENDANCE_CREATED, attendance.attendanceId)
    verifyNoMoreInteractions(outboundEventsService)
  }
}
