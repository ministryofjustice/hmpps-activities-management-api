package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.userDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentOccurrenceSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentRepeat
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.UserSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentRepeatPeriod as AppointmentRepeatPeriodModel

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

  @Test
  fun `entity to details mapping`() {
    val entity = appointmentEntity()
    val occurrenceEntity = entity.occurrences().first()
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
    val userMap = mapOf(
      entity.createdBy to userDetail(1, "CREATE.USER", "CREATE", "USER"),
      entity.updatedBy!! to userDetail(2, "UPDATE.USER", "UPDATE", "USER"),
    )
    val prisoners = listOf(
      PrisonerSearchPrisonerFixture.instance(
        prisonerNumber = "A1234BC",
        bookingId = 456,
        firstName = "TEST",
        lastName = "PRISONER",
        prisonId = "TPR",
        cellLocation = "1-2-3",
      ),
    )
    assertThat(entity.toDetails(locationMap, userMap, prisoners)).isEqualTo(
      AppointmentDetails(
        entity.appointmentId,
        AppointmentCategorySummary(entity.category.appointmentCategoryId, entity.category.code, entity.category.description),
        entity.prisonCode,
        AppointmentLocationSummary(entity.internalLocationId!!, "TPR", "Test Appointment Location"),
        entity.inCell,
        entity.startDate,
        entity.startTime,
        entity.endTime,
        null,
        entity.comment,
        entity.created,
        UserSummary(1, "CREATE.USER", "CREATE", "USER"),
        entity.updated,
        UserSummary(2, "UPDATE.USER", "UPDATE", "USER"),
        occurrences = listOf(
          AppointmentOccurrenceSummary(
            occurrenceEntity.appointmentOccurrenceId,
            occurrenceEntity.sequenceNumber,
            AppointmentLocationSummary(occurrenceEntity.internalLocationId!!, "TPR", "Test Appointment Location"),
            occurrenceEntity.inCell,
            occurrenceEntity.startDate,
            occurrenceEntity.startTime,
            occurrenceEntity.endTime,
            "Appointment occurrence level comment",
            isEdited = false,
            isCancelled = false,
            occurrenceEntity.updated,
            UserSummary(2, "UPDATE.USER", "UPDATE", "USER"),
            1,
          ),
        ),
        prisoners = listOf(
          PrisonerSummary("A1234BC", 456, "TEST", "PRISONER", "TPR", "1-2-3"),
        ),
      ),
    )
  }

  @Test
  fun `entity to details mapping in cell nullifies internal location`() {
    val entity = appointmentEntity(inCell = true)
    entity.internalLocationId = 123
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
    val userMap = mapOf(
      entity.createdBy to userDetail(1, "CREATE.USER", "CREATE", "USER"),
      entity.updatedBy!! to userDetail(2, "UPDATE.USER", "UPDATE", "USER"),
    )
    val prisoners = listOf(
      PrisonerSearchPrisonerFixture.instance(
        prisonerNumber = "A1234BC",
        bookingId = 456,
        firstName = "TEST",
        lastName = "PRISONER",
        prisonId = "TPR",
        cellLocation = "1-2-3",
      ),
    )
    with(entity.toDetails(locationMap, userMap, prisoners)) {
      assertThat(internalLocation).isNull()
      assertThat(inCell).isTrue
    }
  }

  @Test
  fun `entity to details mapping updated by null`() {
    val entity = appointmentEntity(updatedBy = null)
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
    val userMap = mapOf(
      entity.createdBy to userDetail(1, "CREATE.USER", "CREATE", "USER"),
    )
    val prisoners = listOf(
      PrisonerSearchPrisonerFixture.instance(
        prisonerNumber = "A1234BC",
        bookingId = 456,
        firstName = "TEST",
        lastName = "PRISONER",
        prisonId = "TPR",
        cellLocation = "1-2-3",
      ),
    )
    with(entity.toDetails(locationMap, userMap, prisoners)) {
      assertThat(updatedBy).isNull()
    }
  }

  @Test
  fun `entity to details mapping schedule to repeat`() {
    val entity = appointmentEntity(updatedBy = null, repeatPeriod = AppointmentRepeatPeriod.FORTNIGHTLY, numberOfOccurrences = 2)
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
    val userMap = mapOf(
      entity.createdBy to userDetail(1, "CREATE.USER", "CREATE", "USER"),
    )
    val prisoners = listOf(
      PrisonerSearchPrisonerFixture.instance(
        prisonerNumber = "A1234BC",
        bookingId = 456,
        firstName = "TEST",
        lastName = "PRISONER",
        prisonId = "TPR",
        cellLocation = "1-2-3",
      ),
    )
    with(entity.toDetails(locationMap, userMap, prisoners)) {
      assertThat(repeat).isEqualTo(AppointmentRepeat(AppointmentRepeatPeriodModel.FORTNIGHTLY, 2))
    }
  }

  @Test
  fun `null schedule returns default iterator`() {
    val today = LocalDate.now()
    val entity = appointmentEntity(startDate = today).apply { schedule = null }
    assertThat(entity.scheduleIterator().asSequence().toList()).containsExactly(
      today,
    )
  }

  @Test
  fun `schedule values populates iterator`() {
    val today = LocalDate.now()
    val entity = appointmentEntity(startDate = today, repeatPeriod = AppointmentRepeatPeriod.WEEKLY, numberOfOccurrences = 3)
    assertThat(entity.scheduleIterator().asSequence().toList()).containsExactly(
      today,
      today.plusWeeks(1),
      today.plusWeeks(2),
    )
  }
}
