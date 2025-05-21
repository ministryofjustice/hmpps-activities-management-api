package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.schedule
import java.time.LocalDateTime

class AdvanceAttendanceTest {
  @Test
  fun `should return history`() {
    val activity = activityEntity()

    val attendance = AdvanceAttendance(
      advanceAttendanceId = 1,
      scheduledInstance = activity.schedule().instances().first(),
      prisonerNumber = "AB1111A",
      issuePayment = true,
      recordedTime = LocalDateTime.now().minusMinutes(10),
      recordedBy = "USER1",
    )

    attendance.updatePayment(false, "USER2")
    attendance.updatePayment(true, "USER3")

    assertThat(attendance.history())
      .extracting("issuePayment", "recordedBy")
      .containsExactly(tuple(true, "USER1"), tuple(false, "USER2"))
  }
}
