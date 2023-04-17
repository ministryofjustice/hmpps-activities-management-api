package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentOccurrenceModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.userDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentOccurrenceDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentOccurrenceSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentRepeat
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentRepeatPeriod
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.UserSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import java.time.LocalDate
import java.time.LocalTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentRepeatPeriod as AppointmentRepeatPeriodEnity

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
    val entity = appointmentEntity(
      appointmentType = AppointmentType.GROUP,
      prisonerNumberToBookingIdMap = mapOf("A1234BC" to 456),
      numberOfOccurrences = 2,
    )
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

  @Test
  fun `entity to details mapping`() {
    val appointment = appointmentEntity()
    val entity = appointment.occurrences().first()
    val referenceCodeMap = mapOf(appointment.categoryCode to appointmentCategoryReferenceCode(appointment.categoryCode))
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
    val userMap = mapOf(
      appointment.createdBy to userDetail(1, "CREATE.USER", "CREATE", "USER"),
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
    assertThat(entity.toDetails(referenceCodeMap, "TPR", locationMap, userMap, prisoners)).isEqualTo(
      AppointmentOccurrenceDetails(
        entity.appointmentOccurrenceId,
        appointment.appointmentId,
        AppointmentType.INDIVIDUAL,
        1,
        AppointmentCategorySummary(appointment.categoryCode, "Test Category"),
        "TPR",
        AppointmentLocationSummary(entity.internalLocationId!!, "TPR", "Test Appointment Location"),
        false,
        LocalDate.now(),
        LocalTime.of(9, 0),
        LocalTime.of(10, 30),
        null,
        "Appointment occurrence level comment",
        isRepeat = false,
        isEdited = false,
        isCancelled = false,
        created = appointment.created,
        UserSummary(1, "CREATE.USER", "CREATE", "USER"),
        updated = entity.updated,
        updatedBy = UserSummary(2, "UPDATE.USER", "UPDATE", "USER"),
        prisoners = listOf(
          PrisonerSummary("A1234BC", 456, "TEST", "PRISONER", "TPR", "1-2-3"),
        ),
      ),
    )
  }

  @Test
  fun `entity to details mapping in cell nullifies internal location`() {
    val appointment = appointmentEntity(inCell = true)
    appointment.internalLocationId = 123
    val entity = appointment.occurrences().first()
    entity.internalLocationId = 123
    val referenceCodeMap = mapOf(appointment.categoryCode to appointmentCategoryReferenceCode(appointment.categoryCode))
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
    val userMap = mapOf(
      appointment.createdBy to userDetail(1, "CREATE.USER", "CREATE", "USER"),
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
    with(entity.toDetails(referenceCodeMap, "TPR", locationMap, userMap, prisoners)) {
      assertThat(internalLocation).isNull()
      assertThat(inCell).isTrue
    }
  }

  @Test
  fun `entity to details mapping updated by null`() {
    val appointment = appointmentEntity(updatedBy = null)
    val entity = appointment.occurrences().first()
    entity.updatedBy = null
    val referenceCodeMap = mapOf(appointment.categoryCode to appointmentCategoryReferenceCode(appointment.categoryCode))
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
    val userMap = mapOf(
      appointment.createdBy to userDetail(1, "CREATE.USER", "CREATE", "USER"),
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
    with(entity.toDetails(referenceCodeMap, "TPR", locationMap, userMap, prisoners)) {
      assertThat(updatedBy).isNull()
    }
  }

  @Test
  fun `entity to details mapping repeat appointment`() {
    val appointment = appointmentEntity(repeatPeriod = AppointmentRepeatPeriodEnity.WEEKLY, numberOfOccurrences = 4)
    val entity = appointment.occurrences().first()
    val referenceCodeMap = mapOf(appointment.categoryCode to appointmentCategoryReferenceCode(appointment.categoryCode))
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
    val userMap = mapOf(
      appointment.createdBy to userDetail(1, "CREATE.USER", "CREATE", "USER"),
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
    with(entity.toDetails(referenceCodeMap, "TPR", locationMap, userMap, prisoners)) {
      assertThat(repeat).isEqualTo(AppointmentRepeat(AppointmentRepeatPeriod.WEEKLY, 4))
    }
  }

  @Test
  fun `cannot allocate multiple prisoners to individual appointment`() {
    assertThrows<IllegalArgumentException>(
      "Cannot allocate multiple prisoners to an individual appointment",
    ) {
      appointmentEntity(
        appointmentType = AppointmentType.INDIVIDUAL,
        prisonerNumberToBookingIdMap = mapOf("A1234BC" to 456, "B2345CD" to 789),
      )
    }
  }
}
