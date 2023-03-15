package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.time.LocalDateTime

class AttendanceTest {

  @Test
  fun `waiting method sets the state correctly`() {
    val attendance = Attendance(
      scheduledInstance = mock(),
      prisonerNumber = "P000111",
      attendanceReason = AttendanceReason(1, "Some Reason", "Some Desc"),
      status = AttendanceStatus.CANCELLED,
      comment = "Some Comment",
      recordedBy = "Old User",
      recordedTime = LocalDateTime.now(),
    )

    attendance.waiting()

    with(attendance) {
      Assertions.assertThat(attendanceReason).isNull()
      Assertions.assertThat(status).isEqualTo(AttendanceStatus.WAIT)
      Assertions.assertThat(comment).isNull()
      Assertions.assertThat(recordedBy).isNull()
      Assertions.assertThat(recordedTime).isNull()
    }
  }
}
