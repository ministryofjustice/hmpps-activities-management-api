package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentModel

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

  @Test
  fun `internal location ids includes occurrence ids`() {
    val entity = appointmentEntity(123).apply { occurrences().first().internalLocationId = 124 }
    assertThat(entity.internalLocationIds()).containsExactly(123, 124)
  }

  @Test
  fun `internal location ids removes duplicates`() {
    val entity = appointmentEntity(123).apply { occurrences().first().internalLocationId = 123 }
    assertThat(entity.internalLocationIds()).containsExactly(123)
  }

  @Test
  fun `internal location ids removes null`() {
    val entity = appointmentEntity(123).apply { occurrences().first().internalLocationId = null }
    assertThat(entity.internalLocationIds()).containsExactly(123)
  }

  @Test
  fun `prisoner numbers concatenates all occurrence allocations`() {
    val entity = appointmentEntity(prisonerNumberToBookingIdMap = mapOf("A1234BC" to 456, "B2345CD" to 789))
    assertThat(entity.prisonerNumbers()).containsExactly("A1234BC", "B2345CD")
  }

  @Test
  fun `prisoner numbers removes duplicates`() {
    val entity = appointmentEntity(prisonerNumberToBookingIdMap = mapOf("A1234BC" to 456), numberOfOccurrences = 2)
    assertThat(entity.prisonerNumbers()).containsExactly("A1234BC")
  }

  @Test
  fun `usernames includes created by, updated by and occurrence updated by`() {
    val entity = appointmentEntity(createdBy = "CREATE.USER", updatedBy = "UPDATE.USER").apply { occurrences().first().updatedBy = "OCCURRENCE.UPDATE.USER" }
    assertThat(entity.usernames()).containsExactly("CREATE.USER", "UPDATE.USER", "OCCURRENCE.UPDATE.USER")
  }

  @Test
  fun `usernames removes null`() {
    val entity = appointmentEntity(createdBy = "CREATE.USER", updatedBy = null).apply { occurrences().first().updatedBy = "OCCURRENCE.UPDATE.USER" }
    assertThat(entity.usernames()).containsExactly("CREATE.USER", "OCCURRENCE.UPDATE.USER")
  }

  @Test
  fun `usernames removes duplicates`() {
    val entity = appointmentEntity(createdBy = "CREATE.USER", updatedBy = "CREATE.USER").apply { occurrences().first().updatedBy = "CREATE.USER" }
    assertThat(entity.usernames()).containsExactly("CREATE.USER")
  }
}
