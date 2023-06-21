package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.UserDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.bulkAppointmentDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.bulkAppointmentEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.userDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.BulkAppointmentSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.BulkAppointment as BulkAppointmentModel

class BulkAppointmentTest {
  @Test
  fun `entity to model mapping`() {
    val entity = bulkAppointmentEntity()
    val expectedModel = BulkAppointmentModel(
      entity.bulkAppointmentId,
      entity.prisonCode,
      entity.categoryCode,
      entity.appointmentDescription,
      entity.internalLocationId,
      entity.inCell,
      entity.startDate,
      entity.appointments().toModel(),
      entity.created,
      entity.createdBy,
    )
    assertThat(entity.toModel()).isEqualTo(expectedModel)
  }

  @Test
  fun `entity to summary mapping`() {
    val entity = bulkAppointmentEntity()
    assertThat(entity.toSummary()).isEqualTo(BulkAppointmentSummary(1, 3))
  }

  @Test
  fun `prisoner numbers concatenates all appointment occurrence allocations`() {
    val entity = bulkAppointmentEntity()
    assertThat(entity.prisonerNumbers()).containsExactly("A1234BC", "B2345CD", "C3456DE")
  }

  @Test
  fun `prisoner numbers removes duplicates`() {
    val entity = bulkAppointmentEntity(prisonerNumberToBookingIdMap = mapOf("A1234BC" to 456, "A1234BC" to 456, "A1234BC" to 456))
    assertThat(entity.prisonerNumbers()).containsExactly("A1234BC")
  }

  @Test
  fun `usernames includes created by, appointment updated by and occurrence updated by`() {
    val entity = bulkAppointmentEntity().apply {
      appointments().first().apply {
        updatedBy = "APPOINTMENT.UPDATE.USER"
        occurrences().first().updatedBy = "OCCURRENCE.UPDATE.USER"
      }
    }
    assertThat(entity.usernames()).containsExactly("CREATE.USER", "APPOINTMENT.UPDATE.USER", "OCCURRENCE.UPDATE.USER")
  }

  @Test
  fun `usernames removes duplicates`() {
    val entity = bulkAppointmentEntity().apply {
      appointments().first().apply {
        updatedBy = "CREATE.USER"
        occurrences().first().updatedBy = "CREATE.USER"
      }
    }
    assertThat(entity.usernames()).containsExactly("CREATE.USER")
  }

  @Test
  fun `entity to details mapping`() {
    val entity = bulkAppointmentEntity()
    val referenceCodeMap = mapOf(entity.categoryCode to appointmentCategoryReferenceCode(entity.categoryCode))
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
    val userMap = mapOf(
      "CREATE.USER" to userDetail(1, "CREATE.USER", "CREATE", "USER"),
      "UPDATE.USER" to userDetail(2, "UPDATE.USER", "UPDATE", "USER"),
    )
    val prisonerMap = getPrisonerMap()
    val bulkAppointmentSummary = BulkAppointmentSummary(1, 3)

    assertThat(entity.toDetails(prisonerMap, referenceCodeMap, locationMap, userMap)).isEqualTo(
      bulkAppointmentDetails(entity.created),
    )
  }

  @Test
  fun `entity to details mapping reference code not found`() {
    val entity = bulkAppointmentEntity()
    val referenceCodeMap = emptyMap<String, ReferenceCode>()
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
    val userMap = mapOf(
      entity.createdBy to userDetail(1, "CREATE.USER", "CREATE", "USER"),
    )
    val prisonerMap = getPrisonerMap()
    with(entity.toDetails(prisonerMap, referenceCodeMap, locationMap, userMap)) {
      assertThat(category.code).isEqualTo(entity.categoryCode)
      assertThat(category.description).isEqualTo("UNKNOWN")
    }
  }

  @Test
  fun `entity to details mapping location not found`() {
    val entity = bulkAppointmentEntity()
    val referenceCodeMap = mapOf(entity.categoryCode to appointmentCategoryReferenceCode(entity.categoryCode))
    val locationMap = emptyMap<Long, Location>()
    val userMap = mapOf(
      entity.createdBy to userDetail(1, "CREATE.USER", "CREATE", "USER"),
    )
    val prisonerMap = getPrisonerMap()
    with(entity.toDetails(prisonerMap, referenceCodeMap, locationMap, userMap)) {
      assertThat(internalLocation).isNotNull
      assertThat(internalLocation!!.id).isEqualTo(entity.internalLocationId)
      assertThat(internalLocation!!.prisonCode).isEqualTo("TPR")
      assertThat(internalLocation!!.description).isEqualTo("UNKNOWN")
    }
  }

  @Test
  fun `entity to details mapping users not found`() {
    val entity = bulkAppointmentEntity()
    val referenceCodeMap = mapOf(entity.categoryCode to appointmentCategoryReferenceCode(entity.categoryCode))
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
    val userMap = emptyMap<String, UserDetail>()
    val prisonerMap = getPrisonerMap()
    with(entity.toDetails(prisonerMap, referenceCodeMap, locationMap, userMap)) {
      assertThat(createdBy.id).isEqualTo(-1)
      assertThat(createdBy.username).isEqualTo("CREATE.USER")
      assertThat(createdBy.firstName).isEqualTo("UNKNOWN")
      assertThat(createdBy.lastName).isEqualTo("UNKNOWN")
    }
  }

  @Test
  fun `entity to details mapping in cell nullifies internal location`() {
    val entity = bulkAppointmentEntity(inCell = true)
    entity.internalLocationId = 123
    val referenceCodeMap = mapOf(entity.categoryCode to appointmentCategoryReferenceCode(entity.categoryCode))
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
    val userMap = mapOf(
      entity.createdBy to userDetail(1, "CREATE.USER", "CREATE", "USER"),
    )
    val prisonerMap = getPrisonerMap()
    with(entity.toDetails(prisonerMap, referenceCodeMap, locationMap, userMap)) {
      assertThat(internalLocation).isNull()
      assertThat(inCell).isTrue
    }
  }

  private fun getPrisonerMap() = mapOf(
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
      bookingId = 457,
      firstName = "TEST02",
      lastName = "PRISONER02",
      prisonId = "TPR",
      cellLocation = "1-2-4",
    ),
    "C3456DE" to PrisonerSearchPrisonerFixture.instance(
      prisonerNumber = "C3456DE",
      bookingId = 458,
      firstName = "TEST03",
      lastName = "PRISONER03",
      prisonId = "TPR",
      cellLocation = "1-2-5",
    ),
  )
}
