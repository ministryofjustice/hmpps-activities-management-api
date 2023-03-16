package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.attendanceReasons
import java.time.LocalDateTime

class AttendanceTest {
  private val activity = activityEntity()
  private val activitySchedule = activity.schedules().first()
  private val instance = activitySchedule.instances().first()
  private val attendance = instance.attendances.first()
  private val today = LocalDateTime.now()

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
      assertThat(attendanceReason).isNull()
      assertThat(status).isEqualTo(AttendanceStatus.WAIT)
      assertThat(comment).isNull()
      assertThat(recordedBy).isNull()
      assertThat(recordedTime).isNull()
    }
  }

  @Test
  fun `can cancel attendance`() {
    val attendanceReason = attendanceReasons()["CANC"]!!
    val canceledInstance = instance.copy(cancelledBy = "USER1", cancelledTime = today, cancelledReason = "Staff unavailable")
    val attendanceWithCanceledInstance = attendance.copy(scheduledInstance = canceledInstance)

    attendanceWithCanceledInstance.cancel(attendanceReason, "BAS")
    assertThat(attendanceWithCanceledInstance.status).isEqualTo(AttendanceStatus.COMPLETED)
    assertThat(attendanceWithCanceledInstance.payAmount).isEqualTo(30)
    assertThat(attendanceWithCanceledInstance.attendanceReason).isEqualTo(attendanceReason)
    assertThat(attendanceWithCanceledInstance.comment).isEqualTo("Staff unavailable")
    assertThat(attendanceWithCanceledInstance.recordedTime).isEqualTo(today)
    assertThat(attendanceWithCanceledInstance.recordedBy).isEqualTo("USER1")
  }
}
