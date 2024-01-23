package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.UserDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSetDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSetEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.prisonerReleasedAppointmentAttendeeRemovalReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.userDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSetSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSet as AppointmentSetModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.EventOrganiser as EventOrganiserModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.EventTier as EventTierModel

class AppointmentSetTest {
  @Test
  fun `entity to model mapping`() {
    val entity = appointmentSetEntity()
    val expectedModel = AppointmentSetModel(
      entity.appointmentSetId,
      entity.prisonCode,
      entity.categoryCode,
      EventTierModel(
        entity.appointmentTier!!.eventTierId,
        entity.appointmentTier!!.code,
        entity.appointmentTier!!.description,
      ),
      EventOrganiserModel(
        entity.appointmentOrganiser!!.eventOrganiserId,
        entity.appointmentOrganiser!!.code,
        entity.appointmentOrganiser!!.description,
      ),
      entity.customName,
      entity.internalLocationId,
      entity.inCell,
      entity.startDate,
      entity.appointments().toModel(),
      entity.createdTime,
      entity.createdBy,
    )
    assertThat(entity.toModel()).isEqualTo(expectedModel)
  }

  @Test
  fun `entity to summary mapping`() {
    val entity = appointmentSetEntity()
    assertThat(entity.toSummary()).isEqualTo(AppointmentSetSummary(1, 3, 3))
  }

  @Test
  fun `prisoner numbers concatenates all appointment attendees`() {
    val entity = appointmentSetEntity()
    assertThat(entity.prisonerNumbers()).containsExactly("A1234BC", "B2345CD", "C3456DE")
  }

  @Test
  fun `prisoner numbers removes duplicates`() {
    val entity = appointmentSetEntity(prisonerNumberToBookingIdMap = mapOf("A1234BC" to 456, "A1234BC" to 456, "A1234BC" to 456))
    assertThat(entity.prisonerNumbers()).containsExactly("A1234BC")
  }

  @Test
  fun `usernames includes created by, updated by and appointment updated by`() {
    val entity = appointmentSetEntity().apply {
      updatedBy = "UPDATE.USER"
      appointmentSeries().first().apply {
        updatedBy = "APPOINTMENT_SERIES.UPDATE.USER"
        appointments().first().updatedBy = "APPOINTMENT.UPDATE.USER"
      }
    }
    assertThat(entity.usernames()).containsExactly("CREATE.USER", "UPDATE.USER", "APPOINTMENT.UPDATE.USER")
  }

  @Test
  fun `usernames removes duplicates`() {
    val entity = appointmentSetEntity().apply {
      updatedBy = "CREATE.USER"
      appointmentSeries().first().apply {
        updatedBy = "CREATE.USER"
        appointments().first().updatedBy = "CREATE.USER"
      }
    }
    assertThat(entity.usernames()).containsExactly("CREATE.USER")
  }

  @Test
  fun `entity to details mapping`() {
    val entity = appointmentSetEntity()
    val referenceCodeMap = mapOf(entity.categoryCode to appointmentCategoryReferenceCode(entity.categoryCode))
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
    val userMap = mapOf(
      "CREATE.USER" to userDetail(1, "CREATE.USER", "CREATE", "USER"),
      "UPDATE.USER" to userDetail(2, "UPDATE.USER", "UPDATE", "USER"),
    )
    val prisonerMap = getPrisonerMap()

    assertThat(entity.toDetails(prisonerMap, referenceCodeMap, locationMap, userMap)).isEqualTo(
      appointmentSetDetails(createdTime = entity.createdTime),
    )
  }

  @Test
  fun `entity to details mapping includes appointment description in name`() {
    val entity = appointmentSetEntity(customName = "appointment name")
    val referenceCodeMap = mapOf(
      entity.categoryCode to appointmentCategoryReferenceCode(
        entity.categoryCode,
        "test category",
      ),
    )
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
    val userMap = mapOf(
      "CREATE.USER" to userDetail(1, "CREATE.USER", "CREATE", "USER"),
      "UPDATE.USER" to userDetail(2, "UPDATE.USER", "UPDATE", "USER"),
    )
    val prisonerMap = getPrisonerMap()

    with(entity.toDetails(prisonerMap, referenceCodeMap, locationMap, userMap)) {
      assertThat(appointmentName).isEqualTo("appointment name (test category)")
    }
  }

  @Test
  fun `entity to details mapping does not include appointment description in name`() {
    val entity = appointmentSetEntity()
    val referenceCodeMap = mapOf(
      entity.categoryCode to appointmentCategoryReferenceCode(
        entity.categoryCode,
        "test category",
      ),
    )
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
    val userMap = mapOf(
      "CREATE.USER" to userDetail(1, "CREATE.USER", "CREATE", "USER"),
      "UPDATE.USER" to userDetail(2, "UPDATE.USER", "UPDATE", "USER"),
    )
    val prisonerMap = getPrisonerMap()

    with(entity.toDetails(prisonerMap, referenceCodeMap, locationMap, userMap)) {
      assertThat(appointmentName).isEqualTo("test category")
    }
  }

  @Test
  fun `entity to details mapping reference code not found`() {
    val entity = appointmentSetEntity()
    val referenceCodeMap = emptyMap<String, ReferenceCode>()
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
    val userMap = mapOf(
      entity.createdBy to userDetail(1, "CREATE.USER", "CREATE", "USER"),
    )
    val prisonerMap = getPrisonerMap()
    with(entity.toDetails(prisonerMap, referenceCodeMap, locationMap, userMap)) {
      assertThat(category.code).isEqualTo(entity.categoryCode)
      assertThat(category.description).isEqualTo(entity.categoryCode)
    }
  }

  @Test
  fun `entity to details mapping location not found`() {
    val entity = appointmentSetEntity()
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
      assertThat(internalLocation!!.description).isEqualTo("No information available")
    }
  }

  @Test
  fun `entity to details mapping users not found`() {
    val entity = appointmentSetEntity()
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
    val entity = appointmentSetEntity(inCell = true)
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

  @Test
  fun `entity to details mapping removes appointments with no attendees`() {
    val entity = appointmentSetEntity()
    val referenceCodeMap = mapOf(entity.categoryCode to appointmentCategoryReferenceCode(entity.categoryCode))
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
    val userMap = mapOf(
      "CREATE.USER" to userDetail(1, "CREATE.USER", "CREATE", "USER"),
      "UPDATE.USER" to userDetail(2, "UPDATE.USER", "UPDATE", "USER"),
    )
    val prisonerMap = getPrisonerMap()

    entity.appointments().first().apply { this.removeAttendee(this.attendees().first().prisonerNumber, LocalDateTime.now(), prisonerReleasedAppointmentAttendeeRemovalReason(), "OFFENDER_RELEASED_EVENT") }

    entity.toDetails(prisonerMap, referenceCodeMap, locationMap, userMap).appointments hasSize 2
  }

  @Test
  fun `appointments filters out soft deleted appointments`() {
    val entity = appointmentSetEntity(
      prisonerNumberToBookingIdMap = mapOf("A1234BC" to 456, "B2345CD" to 457, "C3456DE" to 458),
    )
    entity.appointments().first().isDeleted = true

    with(entity.appointments()) {
      assertThat(size).isEqualTo(2)
      assertThat(this.map { it.appointmentId }).isEqualTo(listOf(2L, 3L))
    }
  }

  @Test
  fun `appointments includes soft deleted appointments when "includeDeleted=true"`() {
    val entity = appointmentSetEntity(
      prisonerNumberToBookingIdMap = mapOf("A1234BC" to 456, "B2345CD" to 457, "C3456DE" to 458),
    )
    entity.appointments().first().isDeleted = true

    with(entity.appointments(true)) {
      assertThat(size).isEqualTo(3)
      assertThat(this.map { it.appointmentId }).isEqualTo(listOf(1L, 2L, 3L))
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

  @Test
  fun `throws error when setting an organiser if tier is not TIER_2`() {
    val entity = appointmentSetEntity(
      appointmentTier = EventTier(
        1,
        "TIER_1",
        "Tier 1",
      ),
      appointmentOrganiser = null,
    )

    val exception = assertThrows<IllegalArgumentException> {
      entity.appointmentOrganiser = EventOrganiser(1, "PRISON_STAFF", "Prison staff")
    }
    exception.message isEqualTo "Cannot add organiser unless appointment set is Tier 2."
  }
}
