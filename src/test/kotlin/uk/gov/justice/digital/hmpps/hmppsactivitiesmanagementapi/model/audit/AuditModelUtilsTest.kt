package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class AuditModelUtilsTest {

  @Test
  fun `creates empty json object if no fields supplied`() {
    assertThat(AuditModelUtils.generateHmppsAuditJson()).isEqualTo("{}")
  }

  @Test
  fun `creates correct json object if all fields supplied`() {
    val activityId = 1L
    val activityName = "Some Activity"
    val prisonCode = "PVI"
    val prisonerNumber = "A12346"
    val scheduleId = 2L
    val date = LocalDate.of(2023, 1, 2)
    val startTime = LocalTime.of(10, 1, 2)
    val endTime = LocalTime.of(11, 3, 4)
    val createdAt = LocalDateTime.of(2022, 12, 11, 9, 5, 6)
    val createdBy = "Bob"

    assertThat(
      AuditModelUtils.generateHmppsAuditJson(
        activityId = activityId,
        activityName = activityName,
        prisonCode = prisonCode,
        prisonerNumber = prisonerNumber,
        scheduleId = scheduleId,
        date = date,
        startTime = startTime,
        endTime = endTime,
        createdAt = createdAt,
        createdBy = createdBy,
      ),
    ).isEqualTo(
      """{"activityId":1,"activityName":"Some Activity","prisonCode":"PVI","prisonerNumber":"A12346","scheduleId":2,"date":"2023-01-02","startTime":"10:01:02","endTime":"11:03:04","createdAt":"2022-12-11T09:05:06","createdBy":"Bob"}""",
    )
  }
}
