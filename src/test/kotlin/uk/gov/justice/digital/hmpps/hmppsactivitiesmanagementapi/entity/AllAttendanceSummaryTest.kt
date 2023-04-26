package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.attendanceSummary
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AllAttendanceSummary as ModelAllAttendanceSummary

class AllAttendanceSummaryTest {
  @Test
  fun `converted to model`() {
    val expectedModel = ModelAllAttendanceSummary(1, LocalDate.now(), "AM", "WAITING", null, null, 2)
    assertThat(attendanceSummary().toModel().first()).isEqualTo(expectedModel)
  }

  @Test
  fun `List converted to model`() {
    val expectedModel = listOf(ModelAllAttendanceSummary(1, LocalDate.now(), "AM", "WAITING", null, null, 2))
    assertThat(attendanceSummary().toModel()).isEqualTo(expectedModel)
  }
}
