package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentOccurrence
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class AppointmentOccurrenceTest {
  @Test
  fun `entity to model mapping`() {
    val entity = appointmentEntity().occurrences()[0]
    val expectedModel = appointmentOccurrenceModel(entity.updated)
    Assertions.assertThat(entity.toModel()).isEqualTo(expectedModel)
  }

  @Test
  fun `entity list to model list mapping`() {
    val entityList = appointmentEntity().occurrences()
    val expectedModel = listOf(appointmentOccurrenceModel(entityList[0].updated))
    Assertions.assertThat(entityList.toModel()).isEqualTo(expectedModel)
  }
}

internal fun appointmentOccurrenceModel(updated: LocalDateTime?) =
  AppointmentOccurrence(
    1,
    123,
    false,
    LocalDate.now(),
    LocalTime.of(9, 0),
    LocalTime.of(10, 30),
    "Appointment occurrence level comment",
    false,
    updated,
    "UPDATE.USER",
    allocations = listOf(appointmentOccurrenceAllocationModel())
  )
