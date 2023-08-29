package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.UserDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCancelledReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentDeletedReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentOccurrenceEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.userDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentRepeat
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.UserSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
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
  fun `occurrences filters out soft deleted occurrences`() {
    val entity = appointmentEntity(repeatPeriod = AppointmentRepeatPeriod.WEEKLY, numberOfOccurrences = 3).apply { occurrences().first().cancellationReason = appointmentDeletedReason() }
    with(entity.occurrences()) {
      assertThat(size).isEqualTo(2)
      assertThat(this.map { it.appointmentOccurrenceId }).isEqualTo(listOf(2L, 3L))
    }
  }

  @Test
  fun `scheduled occurrences filters out past occurrences`() {
    val entity = appointmentEntity(
      startDate = LocalDate.now().minusDays(3),
      repeatPeriod = AppointmentRepeatPeriod.WEEKLY,
      numberOfOccurrences = 3,
    )
    with(entity.scheduledOccurrences()) {
      assertThat(size).isEqualTo(2)
      assertThat(this.map { it.appointmentOccurrenceId }).isEqualTo(listOf(2L, 3L))
    }
  }

  @Test
  fun `scheduled occurrences filters out cancelled occurrences`() {
    val entity = appointmentEntity(
      startDate = LocalDate.now().minusDays(3),
      repeatPeriod = AppointmentRepeatPeriod.WEEKLY,
      numberOfOccurrences = 3,
    ).apply { occurrences()[1].cancellationReason = appointmentCancelledReason() }
    with(entity.scheduledOccurrences()) {
      assertThat(size).isEqualTo(1)
      assertThat(this.map { it.appointmentOccurrenceId }).isEqualTo(listOf(3L))
    }
  }

  @Test
  fun `scheduled occurrences filters out deleted occurrences`() {
    val entity = appointmentEntity(
      startDate = LocalDate.now().minusDays(3),
      repeatPeriod = AppointmentRepeatPeriod.WEEKLY,
      numberOfOccurrences = 3,
    ).apply { occurrences()[2].cancellationReason = appointmentDeletedReason() }
    with(entity.scheduledOccurrences()) {
      assertThat(size).isEqualTo(1)
      assertThat(this.map { it.appointmentOccurrenceId }).isEqualTo(listOf(2L))
    }
  }

  @Test
  fun `scheduled occurrences after filters out past occurrences`() {
    val entity = appointmentEntity(
      startDate = LocalDate.now().minusDays(3),
      repeatPeriod = AppointmentRepeatPeriod.WEEKLY,
      numberOfOccurrences = 4,
    )
    with(entity.scheduledOccurrencesAfter(LocalDateTime.now().minusDays(4))) {
      assertThat(size).isEqualTo(3)
      assertThat(this.map { it.appointmentOccurrenceId }).isEqualTo(listOf(2L, 3L, 4L))
    }
  }

  @Test
  fun `scheduled occurrences after filters out cancelled occurrences`() {
    val entity = appointmentEntity(
      startDate = LocalDate.now().minusDays(3),
      repeatPeriod = AppointmentRepeatPeriod.WEEKLY,
      numberOfOccurrences = 4,
    ).apply { occurrences()[2].cancellationReason = appointmentCancelledReason() }
    with(entity.scheduledOccurrencesAfter(entity.occurrences()[1].startDateTime())) {
      assertThat(size).isEqualTo(1)
      assertThat(this.map { it.appointmentOccurrenceId }).isEqualTo(listOf(4L))
    }
  }

  @Test
  fun `scheduled occurrences after filters out deleted occurrences`() {
    val entity = appointmentEntity(
      startDate = LocalDate.now().minusDays(3),
      repeatPeriod = AppointmentRepeatPeriod.WEEKLY,
      numberOfOccurrences = 4,
    ).apply { occurrences()[3].cancellationReason = appointmentDeletedReason() }
    with(entity.scheduledOccurrencesAfter(entity.occurrences()[1].startDateTime())) {
      assertThat(size).isEqualTo(1)
      assertThat(this.map { it.appointmentOccurrenceId }).isEqualTo(listOf(3L))
    }
  }

  @Test
  fun `apply to cannot action past occurrence`() {
    val entity = appointmentEntity(
      repeatPeriod = AppointmentRepeatPeriod.WEEKLY,
      numberOfOccurrences = 3,
    )
    val appointmentOccurrence = entity.occurrences()[1]
    appointmentOccurrence.startDate = LocalDate.now().minusDays(3)
    assertThatThrownBy { entity.applyToOccurrences(appointmentOccurrence, ApplyTo.THIS_OCCURRENCE, "update") }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Cannot update a past appointment occurrence")
  }

  @Test
  fun `apply to cannot action cancelled occurrence`() {
    val entity = appointmentEntity(
      repeatPeriod = AppointmentRepeatPeriod.WEEKLY,
      numberOfOccurrences = 3,
    )
    val appointmentOccurrence = entity.occurrences()[1]
    appointmentOccurrence.cancellationReason = appointmentCancelledReason()
    assertThatThrownBy { entity.applyToOccurrences(appointmentOccurrence, ApplyTo.THIS_OCCURRENCE, "modify") }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Cannot modify a cancelled appointment occurrence")
  }

  @Test
  fun `apply to cannot action deleted occurrence`() {
    val entity = appointmentEntity(
      repeatPeriod = AppointmentRepeatPeriod.WEEKLY,
      numberOfOccurrences = 3,
    )
    val appointmentOccurrence = entity.occurrences()[1]
    appointmentOccurrence.cancellationReason = appointmentDeletedReason()
    assertThatThrownBy { entity.applyToOccurrences(appointmentOccurrence, ApplyTo.THIS_OCCURRENCE, "cancel") }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Cannot cancel a deleted appointment occurrence")
  }

  @Test
  fun `apply to this occurrence`() {
    val entity = appointmentEntity(
      repeatPeriod = AppointmentRepeatPeriod.WEEKLY,
      numberOfOccurrences = 3,
    )
    val appointmentOccurrence = entity.occurrences()[1]
    with(entity.applyToOccurrences(appointmentOccurrence, ApplyTo.THIS_OCCURRENCE, "")) {
      assertThat(size).isEqualTo(1)
      assertThat(this.map { it.appointmentOccurrenceId }).isEqualTo(listOf(2L))
    }
  }

  @Test
  fun `apply to this and all future occurrences`() {
    val entity = appointmentEntity(
      repeatPeriod = AppointmentRepeatPeriod.WEEKLY,
      numberOfOccurrences = 3,
    )
    val appointmentOccurrence = entity.occurrences()[1]
    with(entity.applyToOccurrences(appointmentOccurrence, ApplyTo.THIS_AND_ALL_FUTURE_OCCURRENCES, "")) {
      assertThat(size).isEqualTo(2)
      assertThat(this.map { it.appointmentOccurrenceId }).isEqualTo(listOf(2L, 3L))
    }
  }

  @Test
  fun `apply to all future occurrences`() {
    val entity = appointmentEntity(
      repeatPeriod = AppointmentRepeatPeriod.WEEKLY,
      numberOfOccurrences = 3,
    )
    val appointmentOccurrence = entity.occurrences()[1]
    with(entity.applyToOccurrences(appointmentOccurrence, ApplyTo.ALL_FUTURE_OCCURRENCES, "")) {
      assertThat(size).isEqualTo(3)
      assertThat(this.map { it.appointmentOccurrenceId }).isEqualTo(listOf(1L, 2L, 3L))
    }
  }

  @Test
  fun `apply to this and all future occurrences filters out past occurrences`() {
    val entity = appointmentEntity(
      repeatPeriod = AppointmentRepeatPeriod.WEEKLY,
      numberOfOccurrences = 3,
    ).apply { occurrences()[2].startDate = LocalDate.now().minusDays(3) }
    val appointmentOccurrence = entity.occurrences()[1]
    with(entity.applyToOccurrences(appointmentOccurrence, ApplyTo.ALL_FUTURE_OCCURRENCES, "")) {
      assertThat(size).isEqualTo(2)
      assertThat(this.map { it.appointmentOccurrenceId }).isEqualTo(listOf(1L, 2L))
    }
  }

  @Test
  fun `apply to this and all future occurrences filters out cancelled occurrences`() {
    val entity = appointmentEntity(
      repeatPeriod = AppointmentRepeatPeriod.WEEKLY,
      numberOfOccurrences = 3,
    ).apply { occurrences()[2].cancellationReason = appointmentCancelledReason() }
    val appointmentOccurrence = entity.occurrences()[1]
    with(entity.applyToOccurrences(appointmentOccurrence, ApplyTo.ALL_FUTURE_OCCURRENCES, "")) {
      assertThat(size).isEqualTo(2)
      assertThat(this.map { it.appointmentOccurrenceId }).isEqualTo(listOf(1L, 2L))
    }
  }

  @Test
  fun `apply to this and all future occurrences filters out deleted occurrences`() {
    val entity = appointmentEntity(
      repeatPeriod = AppointmentRepeatPeriod.WEEKLY,
      numberOfOccurrences = 3,
    ).apply { occurrences()[2].cancellationReason = appointmentDeletedReason() }
    val appointmentOccurrence = entity.occurrences()[1]
    with(entity.applyToOccurrences(appointmentOccurrence, ApplyTo.ALL_FUTURE_OCCURRENCES, "")) {
      assertThat(size).isEqualTo(2)
      assertThat(this.map { it.appointmentOccurrenceId }).isEqualTo(listOf(1L, 2L))
    }
  }

  @Test
  fun `apply to all future occurrences filters out past occurrences`() {
    val entity = appointmentEntity(
      startDate = LocalDate.now().minusDays(3),
      repeatPeriod = AppointmentRepeatPeriod.WEEKLY,
      numberOfOccurrences = 3,
    )
    val appointmentOccurrence = entity.occurrences()[1]
    with(entity.applyToOccurrences(appointmentOccurrence, ApplyTo.ALL_FUTURE_OCCURRENCES, "")) {
      assertThat(size).isEqualTo(2)
      assertThat(this.map { it.appointmentOccurrenceId }).isEqualTo(listOf(2L, 3L))
    }
  }

  @Test
  fun `apply to all future occurrences filters out cancelled occurrences`() {
    val entity = appointmentEntity(
      repeatPeriod = AppointmentRepeatPeriod.WEEKLY,
      numberOfOccurrences = 3,
    ).apply { occurrences()[2].cancellationReason = appointmentCancelledReason() }
    val appointmentOccurrence = entity.occurrences()[1]
    with(entity.applyToOccurrences(appointmentOccurrence, ApplyTo.ALL_FUTURE_OCCURRENCES, "")) {
      assertThat(size).isEqualTo(2)
      assertThat(this.map { it.appointmentOccurrenceId }).isEqualTo(listOf(1L, 2L))
    }
  }

  @Test
  fun `apply to all future occurrences filters out deleted occurrences`() {
    val entity = appointmentEntity(
      repeatPeriod = AppointmentRepeatPeriod.WEEKLY,
      numberOfOccurrences = 3,
    ).apply { occurrences()[2].cancellationReason = appointmentDeletedReason() }
    val appointmentOccurrence = entity.occurrences()[1]
    with(entity.applyToOccurrences(appointmentOccurrence, ApplyTo.ALL_FUTURE_OCCURRENCES, "")) {
      assertThat(size).isEqualTo(2)
      assertThat(this.map { it.appointmentOccurrenceId }).isEqualTo(listOf(1L, 2L))
    }
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
  fun `prisoner numbers uses prisoners allocated to the first future occurrence`() {
    val entity = appointmentEntity(prisonerNumberToBookingIdMap = mapOf("A1234BC" to 456))
    entity.occurrences().first().let {
      it.startDate = LocalDate.now()
      it.startTime = LocalTime.now().minusMinutes(1)
    }
    entity.addOccurrence(appointmentOccurrenceEntity(entity, 2, 2, LocalDate.now(), LocalTime.now().plusMinutes(1), prisonerNumberToBookingIdMap = mapOf("B2345CD" to 457)))
    entity.addOccurrence(appointmentOccurrenceEntity(entity, 3, 3, LocalDate.now(), LocalTime.now().plusMinutes(2), prisonerNumberToBookingIdMap = mapOf("C3456DE" to 458)))
    assertThat(entity.prisonerNumbers()).containsExactly("B2345CD")
  }

  @Test
  fun `prisoner numbers uses prisoners allocated to the last occurrence if all occurrences have past`() {
    val entity = appointmentEntity(prisonerNumberToBookingIdMap = mapOf("A1234BC" to 456))
    entity.occurrences().first().let {
      it.startDate = LocalDate.now()
      it.startTime = LocalTime.now().minusMinutes(3)
    }
    entity.addOccurrence(appointmentOccurrenceEntity(entity, 2, 2, LocalDate.now(), LocalTime.now().minusMinutes(2), prisonerNumberToBookingIdMap = mapOf("B2345CD" to 457)))
    entity.addOccurrence(appointmentOccurrenceEntity(entity, 3, 3, LocalDate.now(), LocalTime.now().minusMinutes(1), prisonerNumberToBookingIdMap = mapOf("C3456DE" to 458)))
    assertThat(entity.prisonerNumbers()).containsExactly("C3456DE")
  }

  @Test
  fun `prisoner numbers uses empty list if there are no occurrences`() {
    val entity = appointmentEntity(prisonerNumberToBookingIdMap = mapOf("A1234BC" to 456))
    entity.removeOccurrence(entity.occurrences().first())
    assertThat(entity.prisonerNumbers()).isEmpty()
  }

  @Test
  fun `usernames includes created by, updated by and occurrence updated by and cancelled by`() {
    val entity = appointmentEntity(createdBy = "CREATE.USER", updatedBy = "UPDATE.USER").apply {
      occurrences().first().updatedBy = "OCCURRENCE.UPDATE.USER"
      occurrences().first().cancelledBy = "OCCURRENCE.CANCEL.USER"
    }
    assertThat(entity.usernames()).containsExactly("CREATE.USER", "UPDATE.USER", "OCCURRENCE.UPDATE.USER", "OCCURRENCE.CANCEL.USER")
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
    val referenceCodeMap = mapOf(entity.categoryCode to appointmentCategoryReferenceCode(entity.categoryCode))
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
    assertThat(entity.toDetails(prisoners, referenceCodeMap, locationMap, userMap)).isEqualTo(
      appointmentDetails(
        appointmentDescription = "Appointment description",
        created = entity.created,
        updated = entity.updated,
        updatedBy = UserSummary(2, "UPDATE.USER", "UPDATE", "USER"),
      ),
    )
  }

  @Test
  fun `entity to details mapping reference code not found`() {
    val entity = appointmentEntity()
    val referenceCodeMap = emptyMap<String, ReferenceCode>()
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
    with(entity.toDetails(prisoners, referenceCodeMap, locationMap, userMap)) {
      assertThat(category.code).isEqualTo(entity.categoryCode)
      assertThat(category.description).isEqualTo(entity.categoryCode)
    }
  }

  @Test
  fun `entity to details mapping location not found`() {
    val entity = appointmentEntity()
    val referenceCodeMap = mapOf(entity.categoryCode to appointmentCategoryReferenceCode(entity.categoryCode))
    val locationMap = emptyMap<Long, Location>()
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
    with(entity.toDetails(prisoners, referenceCodeMap, locationMap, userMap)) {
      assertThat(internalLocation).isNotNull
      assertThat(internalLocation!!.id).isEqualTo(entity.internalLocationId)
      assertThat(internalLocation!!.prisonCode).isEqualTo("TPR")
      assertThat(internalLocation!!.description).isEqualTo("No information available")
    }
  }

  @Test
  fun `entity to details mapping users not found`() {
    val entity = appointmentEntity()
    val referenceCodeMap = mapOf(entity.categoryCode to appointmentCategoryReferenceCode(entity.categoryCode))
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
    val userMap = emptyMap<String, UserDetail>()
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
    with(entity.toDetails(prisoners, referenceCodeMap, locationMap, userMap)) {
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
    val entity = appointmentEntity(inCell = true)
    entity.internalLocationId = 123
    val referenceCodeMap = mapOf(entity.categoryCode to appointmentCategoryReferenceCode(entity.categoryCode))
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
    with(entity.toDetails(prisoners, referenceCodeMap, locationMap, userMap)) {
      assertThat(internalLocation).isNull()
      assertThat(inCell).isTrue
    }
  }

  @Test
  fun `entity to details mapping updated by null`() {
    val entity = appointmentEntity(updatedBy = null)
    val referenceCodeMap = mapOf(entity.categoryCode to appointmentCategoryReferenceCode(entity.categoryCode))
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
    with(entity.toDetails(prisoners, referenceCodeMap, locationMap, userMap)) {
      assertThat(updatedBy).isNull()
    }
  }

  @Test
  fun `entity to details mapping schedule to repeat`() {
    val entity = appointmentEntity(updatedBy = null, repeatPeriod = AppointmentRepeatPeriod.FORTNIGHTLY, numberOfOccurrences = 2)
    val referenceCodeMap = mapOf(entity.categoryCode to appointmentCategoryReferenceCode(entity.categoryCode))
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
    with(entity.toDetails(prisoners, referenceCodeMap, locationMap, userMap)) {
      assertThat(repeat).isEqualTo(AppointmentRepeat(AppointmentRepeatPeriodModel.FORTNIGHTLY, 2))
    }
  }

  @Test
  fun `entity to details mapping includes appointment description in name`() {
    val entity = appointmentEntity(appointmentDescription = "appointment name")
    val referenceCodeMap = mapOf(
      entity.categoryCode to appointmentCategoryReferenceCode(
        entity.categoryCode,
        "test category",
      ),
    )
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
    with(entity.toDetails(prisoners, referenceCodeMap, locationMap, userMap)) {
      assertThat(appointmentName).isEqualTo("appointment name (test category)")
    }
  }

  @Test
  fun `entity to details mapping does not include appointment description in name`() {
    val entity = appointmentEntity(appointmentDescription = null)
    val referenceCodeMap = mapOf(
      entity.categoryCode to appointmentCategoryReferenceCode(
        entity.categoryCode,
        "test category",
      ),
    )
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
    with(entity.toDetails(prisoners, referenceCodeMap, locationMap, userMap)) {
      assertThat(appointmentName).isEqualTo("test category")
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
