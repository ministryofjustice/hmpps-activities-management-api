package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentAttendeeModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.prisonerReleasedAppointmentAttendeeRemovalReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.tempRemovalByUserAppointmentAttendeeRemovalReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentAttendeeSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class AppointmentAttendeeTest {
  @Test
  fun `entity to model mapping`() {
    val expectedModel = appointmentAttendeeModel()
    assertThat(appointmentSeriesEntity().appointments().first().attendees().first().toModel()).isEqualTo(expectedModel)
  }

  @Test
  fun `entity list to model list mapping`() {
    val expectedModel = listOf(appointmentAttendeeModel())
    assertThat(appointmentSeriesEntity().appointments().first().attendees().toModel()).isEqualTo(expectedModel)
  }

  @Test
  fun `removal with soft delete reason`() {
    val entity = appointmentSeriesEntity().appointments().first().attendees().first().apply {
      remove(removalReason = prisonerReleasedAppointmentAttendeeRemovalReason(), removedBy = "TEST.USER")
    }
    assertThat(entity.removedTime).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.SECONDS))
    assertThat(entity.removalReason).isEqualTo(prisonerReleasedAppointmentAttendeeRemovalReason())
    assertThat(entity.removedBy).isEqualTo("TEST.USER")
    assertThat(entity.isRemoved()).isFalse()
    assertThat(entity.isDeleted).isTrue()
  }

  @Test
  fun `removal with non soft delete reason`() {
    val entity = appointmentSeriesEntity().appointments().first().attendees().first().apply {
      remove(removalReason = tempRemovalByUserAppointmentAttendeeRemovalReason(), removedBy = "TEST.USER")
    }
    assertThat(entity.removedTime).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.SECONDS))
    assertThat(entity.removalReason).isEqualTo(tempRemovalByUserAppointmentAttendeeRemovalReason())
    assertThat(entity.removedBy).isEqualTo("TEST.USER")
    assertThat(entity.isRemoved()).isTrue()
    assertThat(entity.isDeleted).isFalse()
  }

  @Test
  fun `entity to summary mapping`() {
    val entity = appointmentSeriesEntity().appointments().first().attendees().first()
    val prisonerMap = mapOf(
      "A1234BC" to PrisonerSearchPrisonerFixture.instance(
        prisonerNumber = "A1234BC",
        bookingId = 456,
        firstName = "TEST",
        lastName = "PRISONER",
        prisonId = "TPR",
        cellLocation = "1-2-3",
        category = null,
      ),
    )
    assertThat(entity.toSummary(prisonerMap)).isEqualTo(
      AppointmentAttendeeSummary(
        entity.appointmentAttendeeId,
        PrisonerSummary("A1234BC", 456, "TEST", "PRISONER", "ACTIVE IN", "TPR", "1-2-3", "UNKNOWN"),
        null,
        null,
        null,
      ),
    )
  }

  @Test
  fun `entity to summary attendance mapping`() {
    val entity = appointmentSeriesEntity().appointments().first().attendees().first().apply {
      attended = true
      attendanceRecordedTime = LocalDateTime.now()
      attendanceRecordedBy = "ATTENDANCE.RECORDED.BY"
    }
    val prisonerMap = mapOf(
      "A1234BC" to PrisonerSearchPrisonerFixture.instance(
        prisonerNumber = "A1234BC",
        bookingId = 456,
        firstName = "TEST",
        lastName = "PRISONER",
        prisonId = "TPR",
        cellLocation = "1-2-3",
        category = "P",
      ),
    )
    assertThat(entity.toSummary(prisonerMap)).isEqualTo(
      AppointmentAttendeeSummary(
        entity.appointmentAttendeeId,
        PrisonerSummary("A1234BC", 456, "TEST", "PRISONER", "ACTIVE IN", "TPR", "1-2-3", "P"),
        true,
        entity.attendanceRecordedTime,
        "ATTENDANCE.RECORDED.BY",
      ),
    )
  }
}
