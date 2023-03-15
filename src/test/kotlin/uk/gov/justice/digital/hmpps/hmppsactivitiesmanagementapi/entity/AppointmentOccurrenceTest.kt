package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentOccurrenceModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.userDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentOccurrenceSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.UserSummary
import java.time.LocalDate
import java.time.LocalTime

class AppointmentOccurrenceTest {
  @Test
  fun `entity to model mapping`() {
    val entity = appointmentEntity().occurrences().first()
    val expectedModel = appointmentOccurrenceModel(entity.updated)
    assertThat(entity.toModel()).isEqualTo(expectedModel)
  }

  @Test
  fun `entity list to model list mapping`() {
    val entityList = appointmentEntity().occurrences()
    val expectedModel = listOf(appointmentOccurrenceModel(entityList.first().updated))
    assertThat(entityList.toModel()).isEqualTo(expectedModel)
  }

  @Test
  fun `prisoner numbers concatenates allocations`() {
    val entity = appointmentEntity(prisonerNumberToBookingIdMap = mapOf("A1234BC" to 456, "B2345CD" to 789)).occurrences().first()
    assertThat(entity.prisonerNumbers()).containsExactly("A1234BC", "B2345CD")
  }

  @Test
  fun `prisoner numbers removes duplicates`() {
    val entity = appointmentEntity(prisonerNumberToBookingIdMap = mapOf("A1234BC" to 456), numberOfOccurrences = 2)
    val occurrence = entity.occurrences().first()
    entity.occurrences().map { it.allocations() }.flatten().forEach { occurrence.addAllocation(it) }
    assertThat(occurrence.allocations().map { it.prisonerNumber }).isEqualTo(listOf("A1234BC", "A1234BC"))
    assertThat(entity.prisonerNumbers()).containsExactly("A1234BC")
  }

  @Test
  fun `prisoner count counts prisoners`() {
    val entity = appointmentEntity(prisonerNumberToBookingIdMap = mapOf("A1234BC" to 456, "B2345CD" to 789)).occurrences().first()
    assertThat(entity.prisonerCount()).isEqualTo(2)
  }

  @Test
  fun `entity to summary mapping`() {
    val entity = appointmentEntity().occurrences().first()
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
    val userMap = mapOf(entity.updatedBy!! to userDetail(1, "UPDATE.USER", "UPDATE", "USER"))
    assertThat(entity.toSummary("TPR", locationMap, userMap, "Appointment level comment")).isEqualTo(
      AppointmentOccurrenceSummary(
        entity.appointmentOccurrenceId,
        1,
        AppointmentLocationSummary(entity.internalLocationId!!, "TPR", "Test Appointment Location"),
        false,
        LocalDate.now(),
        LocalTime.of(9, 0),
        LocalTime.of(10, 30),
        "Appointment occurrence level comment",
        isEdited = false,
        isCancelled = false,
        updated = entity.updated,
        updatedBy = UserSummary(1, "UPDATE.USER", "UPDATE", "USER"),
        prisonerCount = 1,
      ),
    )
  }

  @Test
  fun `entity list to summary list mapping`() {
    val entity = appointmentEntity().occurrences().first()
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
    val userMap = mapOf(entity.updatedBy!! to userDetail(1, "UPDATE.USER", "UPDATE", "USER"))
    assertThat(listOf(entity).toSummary("TPR", locationMap, userMap, "Appointment level comment")).isEqualTo(
      listOf(
        AppointmentOccurrenceSummary(
          entity.appointmentOccurrenceId,
          1,
          AppointmentLocationSummary(entity.internalLocationId!!, "TPR", "Test Appointment Location"),
          entity.inCell,
          entity.startDate,
          entity.startTime,
          entity.endTime,
          "Appointment occurrence level comment",
          isEdited = false,
          isCancelled = false,
          entity.updated,
          updatedBy = UserSummary(1, "UPDATE.USER", "UPDATE", "USER"),
          1,
        ),
      ),
    )
  }

  @Test
  fun `entity to summary mapping in cell nullifies internal location`() {
    val entity = appointmentEntity(inCell = true).occurrences().first()
    entity.internalLocationId = 123
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
    val userMap = mapOf(entity.updatedBy!! to userDetail(1, "UPDATE.USER", "UPDATE", "USER"))
    with(entity.toSummary("TPR", locationMap, userMap, "Appointment level comment")) {
      assertThat(internalLocation).isNull()
      assertThat(inCell).isTrue
    }
  }

  @Test
  fun `entity to summary mapping defaults to appointment level comment`() {
    val entity = appointmentEntity().occurrences().first()
    entity.comment = null
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
    val userMap = mapOf(entity.updatedBy!! to userDetail(1, "UPDATE.USER", "UPDATE", "USER"))
    with(entity.toSummary("TPR", locationMap, userMap, "Appointment level comment")) {
      assertThat(comment).isEqualTo("Appointment level comment")
    }
  }

  @Test
  fun `entity to summary mapping updated by null`() {
    val entity = appointmentEntity().occurrences().first()
    entity.updatedBy = null
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
    val userMap = mapOf("UPDATE.USER" to userDetail(1, "UPDATE.USER", "UPDATE", "USER"))
    with(entity.toSummary("TPR", locationMap, userMap, "Appointment level comment")) {
      assertThat(updatedBy).isNull()
    }
  }
}
