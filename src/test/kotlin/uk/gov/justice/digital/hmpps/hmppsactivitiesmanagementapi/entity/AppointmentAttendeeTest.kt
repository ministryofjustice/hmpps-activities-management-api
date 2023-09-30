package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentAttendeeDeletedReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentAttendeeModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentAttendeeRemovedReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesEntity
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
      remove(removalReason = appointmentAttendeeDeletedReason(), removedBy = "TEST.USER")
    }
    assertThat(entity.removedTime).isCloseTo(LocalDateTime.now(), Assertions.within(1, ChronoUnit.SECONDS))
    assertThat(entity.removalReason).isEqualTo(appointmentAttendeeDeletedReason())
    assertThat(entity.removedBy).isEqualTo("TEST.USER")
    assertThat(entity.isRemoved()).isFalse()
    assertThat(entity.isDeleted).isTrue()
  }

  @Test
  fun `removal with non soft delete reason`() {
    val entity = appointmentSeriesEntity().appointments().first().attendees().first().apply {
      remove(removalReason = appointmentAttendeeRemovedReason(), removedBy = "TEST.USER")
    }
    assertThat(entity.removedTime).isCloseTo(LocalDateTime.now(), Assertions.within(1, ChronoUnit.SECONDS))
    assertThat(entity.removalReason).isEqualTo(appointmentAttendeeRemovedReason())
    assertThat(entity.removedBy).isEqualTo("TEST.USER")
    assertThat(entity.isRemoved()).isTrue()
    assertThat(entity.isDeleted).isFalse()
  }
}
