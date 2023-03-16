package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.time.LocalDateTime

class AttendanceTest {

  @Test
  fun `waiting method sets the state correctly`() {
    val attendance = Attendance(
      scheduledInstance = mock(),
      prisonerNumber = "P000111",
      attendanceReason = AttendanceReason(
        1,
        "Some Reason",
        "Some Desc",
        false,
        true,
        true,
        false,
        false,
        false,
        true,
        1,
        "some note",
      ),
      status = AttendanceStatus.CANCELLED,
      comment = "Some Comment",
      recordedBy = "Old User",
      recordedTime = LocalDateTime.now(),
    )

    attendance.waiting()

    with(attendance) {
      assertThat(attendanceReason).isNull()
      assertThat(status).isEqualTo(AttendanceStatus.WAITING)
      assertThat(comment).isNull()
      assertThat(recordedBy).isNull()
      assertThat(recordedTime).isNull()
    }
  }
}
