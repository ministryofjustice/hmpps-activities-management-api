package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class AppointmentTest {
  @Test
  fun `entity to model mapping`() {
    val entity = appointmentEntity()
    val expectedModel = appointmentModel(entity.created, entity.updated, entity.occurrences().first().updated)
    assertThat(entity.toModel()).isEqualTo(expectedModel)
  }

  @Test
  fun `entity list to model list mapping`() {
    val entity = appointmentEntity()
    val expectedModel = listOf(appointmentModel(entity.created, entity.updated, entity.occurrences().first().updated))
    assertThat(listOf(entity).toModel()).isEqualTo(expectedModel)
  }
}

internal fun appointmentModel(created: LocalDateTime, updated: LocalDateTime?, occurrenceUpdated: LocalDateTime?) =
  Appointment(
    1,
    appointmentCategoryModel(),
    "TPR",
    123,
    false,
    LocalDate.now(),
    LocalTime.of(9, 0),
    LocalTime.of(10, 30),
    "Appointment level comment",
    created,
    "CREATE.USER",
    updated,
    "UPDATE.USER",
    occurrences = listOf(appointmentOccurrenceModel(occurrenceUpdated))
  )
