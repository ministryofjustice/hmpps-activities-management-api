package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.UserDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentOccurrenceDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentOccurrenceModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.bulkAppointmentEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.userDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentOccurrenceSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentRepeat
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentRepeatPeriod
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.BulkAppointmentSummary
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
  fun `entity to summary mapping`() {
    val entity = appointmentEntity().occurrences().first()
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
    val userMap = mapOf(entity.updatedBy!! to userDetail(1, "UPDATE.USER", "UPDATE", "USER"))
    assertThat(entity.toSummary("TPR", locationMap, userMap, "Appointment level comment")).isEqualTo(
      AppointmentOccurrenceSummary(
        entity.appointmentOccurrenceId,
        1,
        AppointmentLocationSummary(entity.internalLocationId!!, "TPR", "Test Appointment Location User Description"),
        false,
        LocalDate.now().plusDays(1),
        LocalTime.of(9, 0),
        LocalTime.of(10, 30),
        "Appointment occurrence level comment",
        isEdited = true,
        isCancelled = false,
        updated = entity.updated,
        updatedBy = UserSummary(1, "UPDATE.USER", "UPDATE", "USER"),
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
          AppointmentLocationSummary(entity.internalLocationId!!, "TPR", "Test Appointment Location User Description"),
          entity.inCell,
          entity.startDate,
          entity.startTime,
          entity.endTime,
          "Appointment occurrence level comment",
          isEdited = true,
          isCancelled = false,
          entity.updated,
          updatedBy = UserSummary(1, "UPDATE.USER", "UPDATE", "USER"),
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
    val entity = appointmentEntity(updatedBy = null).occurrences().first()
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
    val userMap = mapOf("UPDATE.USER" to userDetail(1, "UPDATE.USER", "UPDATE", "USER"))
    with(entity.toSummary("TPR", locationMap, userMap, "Appointment level comment")) {
      assertThat(updatedBy).isNull()
      assertThat(isEdited).isFalse
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
    val prisonerMap = mapOf(
      "A1234BC" to PrisonerSearchPrisonerFixture.instance(
        prisonerNumber = "A1234BC",
        bookingId = 456,
        firstName = "TEST",
        lastName = "PRISONER",
        prisonId = "TPR",
        cellLocation = "1-2-3",
      ),
    )
    assertThat(entity.toDetails("TPR", prisonerMap, referenceCodeMap, locationMap, userMap)).isEqualTo(
      appointmentOccurrenceDetails(
        entity.appointmentOccurrenceId,
        appointment.appointmentId,
        sequenceNumber = 1,
        appointmentDescription = appointment.appointmentDescription,
        created = appointment.created,
        updated = entity.updated,
      ),
    )
  }

  @Test
  fun `entity list to details list mapping`() {
    val appointment = appointmentEntity()
    val entity = appointment.occurrences().first()
    val referenceCodeMap = mapOf(appointment.categoryCode to appointmentCategoryReferenceCode(appointment.categoryCode))
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
    val userMap = mapOf(
      appointment.createdBy to userDetail(1, "CREATE.USER", "CREATE", "USER"),
      entity.updatedBy!! to userDetail(2, "UPDATE.USER", "UPDATE", "USER"),
    )
    val prisonerMap = mapOf(
      "A1234BC" to PrisonerSearchPrisonerFixture.instance(
        prisonerNumber = "A1234BC",
        bookingId = 456,
        firstName = "TEST",
        lastName = "PRISONER",
        prisonId = "TPR",
        cellLocation = "1-2-3",
      ),
    )
    assertThat(listOf(entity).toDetails("TPR", prisonerMap, referenceCodeMap, locationMap, userMap)).isEqualTo(
      listOf(
        appointmentOccurrenceDetails(
          entity.appointmentOccurrenceId,
          appointment.appointmentId,
          sequenceNumber = 1,
          appointmentDescription = appointment.appointmentDescription,
          created = appointment.created,
          updated = entity.updated,
        ),
      ),
    )
  }

  @Test
  fun `entity to details mapping bulk appointment`() {
    val appointment = bulkAppointmentEntity().appointments().first()
    val entity = appointment.occurrences().first()
    val referenceCodeMap = mapOf(appointment.categoryCode to appointmentCategoryReferenceCode(appointment.categoryCode))
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
    val userMap = mapOf(
      appointment.createdBy to userDetail(1, "CREATE.USER", "CREATE", "USER"),
    )
    val prisonerMap = mapOf(
      "A1234BC" to PrisonerSearchPrisonerFixture.instance(
        prisonerNumber = "A1234BC",
        bookingId = 456,
        firstName = "TEST",
        lastName = "PRISONER",
        prisonId = "TPR",
        cellLocation = "1-2-3",
      ),
    )
    assertThat(entity.toDetails("TPR", prisonerMap, referenceCodeMap, locationMap, userMap).bulkAppointment).isEqualTo(BulkAppointmentSummary(1, 3))
  }

  @Test
  fun `entity to details mapping prisoner not found`() {
    val appointment = appointmentEntity()
    val entity = appointment.occurrences().first()
    val referenceCodeMap = mapOf(appointment.categoryCode to appointmentCategoryReferenceCode(appointment.categoryCode))
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
    val userMap = mapOf(
      appointment.createdBy to userDetail(1, "CREATE.USER", "CREATE", "USER"),
      entity.updatedBy!! to userDetail(2, "UPDATE.USER", "UPDATE", "USER"),
    )
    val prisonerMap = emptyMap<String, Prisoner>()
    with(entity.toDetails("TPR", prisonerMap, referenceCodeMap, locationMap, userMap).prisoners) {
      assertThat(size).isEqualTo(1)
      with(first()) {
        assertThat(prisonerNumber).isEqualTo("A1234BC")
        assertThat(bookingId).isEqualTo(456)
        assertThat(firstName).isEqualTo("UNKNOWN")
        assertThat(lastName).isEqualTo("UNKNOWN")
        assertThat(prisonCode).isEqualTo("TPR")
        assertThat(cellLocation).isEqualTo("UNKNOWN")
      }
    }
  }

  @Test
  fun `entity to details mapping map single prisoner`() {
    val appointment = appointmentEntity()
    val entity = appointment.occurrences().first()
    val referenceCodeMap = mapOf(appointment.categoryCode to appointmentCategoryReferenceCode(appointment.categoryCode))
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
    val userMap = mapOf(
      appointment.createdBy to userDetail(1, "CREATE.USER", "CREATE", "USER"),
      entity.updatedBy!! to userDetail(2, "UPDATE.USER", "UPDATE", "USER"),
    )
    val prisonersMap = mapOf(
      "A1234BC" to PrisonerSearchPrisonerFixture.instance(
        prisonerNumber = "A1234BC",
        bookingId = 456,
        firstName = "TEST01",
        lastName = "PRISONER01",
        prisonId = "TPR",
        cellLocation = "1-2-3",
      ),
      "B2345CD" to PrisonerSearchPrisonerFixture.instance(
        prisonerNumber = "B2345CD",
        bookingId = 789,
        firstName = "TEST02",
        lastName = "PRISONER02",
        prisonId = "TPR",
        cellLocation = "4-5-6",
      ),
    )
    with(entity.toDetails("TPR", prisonersMap, referenceCodeMap, locationMap, userMap).prisoners) {
      assertThat(size).isEqualTo(1)
      with(first()) {
        assertThat(prisonerNumber).isEqualTo("A1234BC")
        assertThat(bookingId).isEqualTo(456)
        assertThat(firstName).isEqualTo("TEST01")
        assertThat(lastName).isEqualTo("PRISONER01")
        assertThat(prisonCode).isEqualTo("TPR")
        assertThat(cellLocation).isEqualTo("1-2-3")
      }
    }
  }

  @Test
  fun `entity to details mapping reference code not found`() {
    val appointment = appointmentEntity()
    val entity = appointment.occurrences().first()
    val referenceCodeMap = emptyMap<String, ReferenceCode>()
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
    val userMap = mapOf(
      appointment.createdBy to userDetail(1, "CREATE.USER", "CREATE", "USER"),
      entity.updatedBy!! to userDetail(2, "UPDATE.USER", "UPDATE", "USER"),
    )
    val prisonerMap = mapOf(
      "A1234BC" to PrisonerSearchPrisonerFixture.instance(
        prisonerNumber = "A1234BC",
        bookingId = 456,
        firstName = "TEST",
        lastName = "PRISONER",
        prisonId = "TPR",
        cellLocation = "1-2-3",
      ),
    )
    with(entity.toDetails("TPR", prisonerMap, referenceCodeMap, locationMap, userMap)) {
      assertThat(category.code).isEqualTo(appointment.categoryCode)
      assertThat(category.description).isEqualTo("UNKNOWN")
    }
  }

  @Test
  fun `entity to details mapping location not found`() {
    val appointment = appointmentEntity()
    val entity = appointment.occurrences().first()
    val referenceCodeMap = mapOf(appointment.categoryCode to appointmentCategoryReferenceCode(appointment.categoryCode))
    val locationMap = emptyMap<Long, Location>()
    val userMap = mapOf(
      appointment.createdBy to userDetail(1, "CREATE.USER", "CREATE", "USER"),
      entity.updatedBy!! to userDetail(2, "UPDATE.USER", "UPDATE", "USER"),
    )
    val prisonerMap = mapOf(
      "A1234BC" to PrisonerSearchPrisonerFixture.instance(
        prisonerNumber = "A1234BC",
        bookingId = 456,
        firstName = "TEST",
        lastName = "PRISONER",
        prisonId = "TPR",
        cellLocation = "1-2-3",
      ),
    )
    with(entity.toDetails("TPR", prisonerMap, referenceCodeMap, locationMap, userMap)) {
      assertThat(internalLocation).isNotNull
      assertThat(internalLocation!!.id).isEqualTo(entity.internalLocationId)
      assertThat(internalLocation!!.prisonCode).isEqualTo("TPR")
      assertThat(internalLocation!!.description).isEqualTo("UNKNOWN")
    }
  }

  @Test
  fun `entity to details mapping users not found`() {
    val appointment = appointmentEntity()
    val entity = appointment.occurrences().first()
    val referenceCodeMap = mapOf(appointment.categoryCode to appointmentCategoryReferenceCode(appointment.categoryCode))
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
    val userMap = emptyMap<String, UserDetail>()
    val prisonerMap = mapOf(
      "A1234BC" to PrisonerSearchPrisonerFixture.instance(
        prisonerNumber = "A1234BC",
        bookingId = 456,
        firstName = "TEST",
        lastName = "PRISONER",
        prisonId = "TPR",
        cellLocation = "1-2-3",
      ),
    )
    with(entity.toDetails("TPR", prisonerMap, referenceCodeMap, locationMap, userMap)) {
      assertThat(createdBy.id).isEqualTo(-1)
      assertThat(createdBy.username).isEqualTo("CREATE.USER")
      assertThat(createdBy.firstName).isEqualTo("UNKNOWN")
      assertThat(createdBy.lastName).isEqualTo("UNKNOWN")
      assertThat(updatedBy).isNotNull
      assertThat(updatedBy!!.id).isEqualTo(-1)
      assertThat(updatedBy!!.username).isEqualTo("UPDATE.USER")
      assertThat(updatedBy!!.firstName).isEqualTo("UNKNOWN")
      assertThat(updatedBy!!.lastName).isEqualTo("UNKNOWN")
    }
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
    val prisonerMap = mapOf(
      "A1234BC" to PrisonerSearchPrisonerFixture.instance(
        prisonerNumber = "A1234BC",
        bookingId = 456,
        firstName = "TEST",
        lastName = "PRISONER",
        prisonId = "TPR",
        cellLocation = "1-2-3",
      ),
    )
    with(entity.toDetails("TPR", prisonerMap, referenceCodeMap, locationMap, userMap)) {
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
    val prisonerMap = mapOf(
      "A1234BC" to PrisonerSearchPrisonerFixture.instance(
        prisonerNumber = "A1234BC",
        bookingId = 456,
        firstName = "TEST",
        lastName = "PRISONER",
        prisonId = "TPR",
        cellLocation = "1-2-3",
      ),
    )
    with(entity.toDetails("TPR", prisonerMap, referenceCodeMap, locationMap, userMap)) {
      assertThat(updatedBy).isNull()
      assertThat(isEdited).isFalse
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
    val prisonerMap = mapOf(
      "A1234BC" to PrisonerSearchPrisonerFixture.instance(
        prisonerNumber = "A1234BC",
        bookingId = 456,
        firstName = "TEST",
        lastName = "PRISONER",
        prisonId = "TPR",
        cellLocation = "1-2-3",
      ),
    )
    with(entity.toDetails("TPR", prisonerMap, referenceCodeMap, locationMap, userMap)) {
      assertThat(repeat).isEqualTo(AppointmentRepeat(AppointmentRepeatPeriod.WEEKLY, 4))
    }
  }

  @Test
  fun `entity to details mapping includes appointment description in name`() {
    val appointment = appointmentEntity(appointmentDescription = "appointment name")
    val entity = appointment.occurrences().first()
    val referenceCodeMap = mapOf(
      appointment.categoryCode to appointmentCategoryReferenceCode(
        appointment.categoryCode,
        "test category",
      ),
    )
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
    val userMap = mapOf(
      appointment.createdBy to userDetail(1, "CREATE.USER", "CREATE", "USER"),
    )
    val prisonerMap = mapOf(
      "A1234BC" to PrisonerSearchPrisonerFixture.instance(
        prisonerNumber = "A1234BC",
        bookingId = 456,
        firstName = "TEST",
        lastName = "PRISONER",
        prisonId = "TPR",
        cellLocation = "1-2-3",
      ),
    )
    with(entity.toDetails("TPR", prisonerMap, referenceCodeMap, locationMap, userMap)) {
      assertThat(appointmentName).isEqualTo("appointment name (test category)")
    }
  }

  @Test
  fun `entity to details mapping does not include appointment description in name`() {
    val appointment = appointmentEntity(appointmentDescription = null)
    val entity = appointment.occurrences().first()
    val referenceCodeMap = mapOf(
      appointment.categoryCode to appointmentCategoryReferenceCode(
        appointment.categoryCode,
        "test category",
      ),
    )
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
    val userMap = mapOf(
      appointment.createdBy to userDetail(1, "CREATE.USER", "CREATE", "USER"),
    )
    val prisonerMap = mapOf(
      "A1234BC" to PrisonerSearchPrisonerFixture.instance(
        prisonerNumber = "A1234BC",
        bookingId = 456,
        firstName = "TEST",
        lastName = "PRISONER",
        prisonId = "TPR",
        cellLocation = "1-2-3",
      ),
    )
    with(entity.toDetails("TPR", prisonerMap, referenceCodeMap, locationMap, userMap)) {
      assertThat(appointmentName).isEqualTo("test category")
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

  @Test
  fun `isCancelled is false when cancellation reason is false`() {
    val entity = appointmentEntity().occurrences().first()
    assertThat(entity.isCancelled()).isFalse
  }

  @Test
  fun `isCancelled is false when cancellation reason deleted is true`() {
    val entity = appointmentEntity().occurrences().first().apply { cancellationReason = AppointmentCancellationReason(1, "", true) }
    assertThat(entity.isCancelled()).isFalse
  }

  @Test
  fun `isCancelled is true when cancellation reason deleted is false`() {
    val entity = appointmentEntity().occurrences().first().apply { cancellationReason = AppointmentCancellationReason(1, "", false) }
    assertThat(entity.isCancelled()).isTrue
  }
}
