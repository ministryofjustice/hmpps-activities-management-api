package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCancelledReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCreatedInErrorReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSetEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.permanentRemovalByUserAppointmentAttendeeRemovalReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.tempRemovalByUserAppointmentAttendeeRemovalReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentFrequency
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeriesSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSetSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentFrequency as AppointmentRepeatPeriodEntity

class AppointmentTest {
  @Test
  fun `not cancelled or deleted when cancelled time is null`() {
    val entity = appointmentSeriesEntity().appointments().first().apply {
      cancelledTime = null
    }
    entity.isCancelled() isBool false
    entity.isDeleted isBool false
  }

  @Test
  fun `cancelled but not deleted when cancelled time is not null and is deleted = false`() {
    val entity = appointmentSeriesEntity().appointments().first().apply {
      cancelledTime = LocalDateTime.now()
      isDeleted = false
    }
    entity.isCancelled() isBool true
    entity.isDeleted isBool false
  }

  @Test
  fun `deleted but not cancelled when cancelled time is not null and is deleted = true`() {
    val entity = appointmentSeriesEntity().appointments().first().apply {
      cancelledTime = LocalDateTime.now()
      isDeleted = true
    }
    entity.isCancelled() isBool false
    entity.isDeleted isBool true
  }

  @Test
  fun `expired when start date time is in the past`() {
    val entity = appointmentSeriesEntity().appointments().first().apply {
      startDate = LocalDate.now()
      startTime = LocalTime.now().minusMinutes(1)
    }
    entity.isExpired() isBool true
  }

  @Test
  fun `not expired when start date time is in the future`() {
    val entity = appointmentSeriesEntity().appointments().first().apply {
      startDate = LocalDate.now()
      startTime = LocalTime.now().plusMinutes(1)
    }
    entity.isExpired() isBool false
  }

  @Test
  fun `scheduled when start date time is in the future, not cancelled or deleted`() {
    val entity = appointmentSeriesEntity().appointments().first().apply {
      startDate = LocalDate.now()
      startTime = LocalTime.now().plusMinutes(1)
      cancelledTime = null
      isDeleted = false
    }
    entity.isScheduled() isBool true
  }

  @Test
  fun `not scheduled when start date time is in the past, not cancelled or deleted`() {
    val entity = appointmentSeriesEntity().appointments().first().apply {
      startDate = LocalDate.now()
      startTime = LocalTime.now().minusMinutes(1)
      cancelledTime = null
      isDeleted = false
    }
    entity.isScheduled() isBool false
  }

  @Test
  fun `not scheduled when start date time is in the future but is cancelled`() {
    val entity = appointmentSeriesEntity().appointments().first().apply {
      startDate = LocalDate.now()
      startTime = LocalTime.now().plusMinutes(1)
      cancelledTime = LocalDateTime.now()
      isDeleted = false
    }
    entity.isScheduled() isBool false
  }

  @Test
  fun `not scheduled when start date time is in the future but is deleted`() {
    val entity = appointmentSeriesEntity().appointments().first().apply {
      startDate = LocalDate.now()
      startTime = LocalTime.now().plusMinutes(1)
      cancelledTime = LocalDateTime.now()
      isDeleted = true
    }
    entity.isScheduled() isBool false
  }

  @Test
  fun `cancel appointment with non delete reason`() {
    val cancelledTime = LocalDateTime.now()
    val cancellationReason = appointmentCancelledReason()
    val cancelledBy = "CANCELLED_BY_USER"
    val entity = appointmentSeriesEntity().appointments().first()

    entity.cancel(cancelledTime, cancellationReason, cancelledBy)

    entity.cancelledTime isEqualTo cancelledTime
    entity.cancellationReason isEqualTo cancellationReason
    entity.cancelledBy isEqualTo cancelledBy
    entity.isDeleted isBool false
  }

  @Test
  fun `cancel appointment with delete reason`() {
    val cancelledTime = LocalDateTime.now()
    val cancellationReason = appointmentCreatedInErrorReason()
    val cancelledBy = "DELETED_BY_USER"
    val entity = appointmentSeriesEntity().appointments().first()

    entity.cancel(cancelledTime, cancellationReason, cancelledBy)

    entity.cancelledTime isEqualTo cancelledTime
    entity.cancellationReason isEqualTo cancellationReason
    entity.cancelledBy isEqualTo cancelledBy
    entity.isDeleted isBool true
  }

  @Test
  fun `attendees filters out soft deleted attendees`() {
    val entity = appointmentSeriesEntity(prisonerNumberToBookingIdMap = mapOf("A1234BC" to 123, "B2345CD" to 456, "C3456DE" to 789)).appointments().first()
      .apply { attendees().first().isDeleted = true }
    with(entity.attendees()) {
      assertThat(size).isEqualTo(2)
      assertThat(this.map { it.prisonerNumber }).isEqualTo(listOf("B2345CD", "C3456DE"))
    }
  }

  @Test
  fun `findAttendees returns attendees matching prison numbers`() {
    val entity = appointmentSeriesEntity(prisonerNumberToBookingIdMap = mapOf("A1234BC" to 123, "B2345CD" to 456, "C3456DE" to 789)).appointments().first()
    with(entity.findAttendees(listOf("B2345CD", "C3456DE"))) {
      assertThat(size).isEqualTo(2)
      assertThat(this.map { it.prisonerNumber }).isEqualTo(listOf("B2345CD", "C3456DE"))
    }
  }

  @Test
  fun `entity to model mapping`() {
    val appointmentSeries = appointmentSeriesEntity()
    val entity = appointmentSeries.appointments().first()
    val expectedModel = appointmentModel(appointmentSeries.createdTime, entity.updatedTime)
    assertThat(entity.toModel()).isEqualTo(expectedModel)
  }

  @Test
  fun `entity list to model list mapping`() {
    val appointmentSeries = appointmentSeriesEntity()
    val entityList = appointmentSeries.appointments()
    val expectedModel = listOf(appointmentModel(appointmentSeries.createdTime, entityList.first().updatedTime))
    assertThat(entityList.toModel()).isEqualTo(expectedModel)
  }

  @Test
  fun `prisoner numbers concatenates allocations`() {
    val entity = appointmentSeriesEntity(prisonerNumberToBookingIdMap = mapOf("A1234BC" to 456, "B2345CD" to 789)).appointments().first()
    assertThat(entity.prisonerNumbers()).containsExactly("A1234BC", "B2345CD")
  }

  @Test
  fun `prisoner numbers removes duplicates`() {
    val entity = appointmentSeriesEntity(
      appointmentType = AppointmentType.GROUP,
      prisonerNumberToBookingIdMap = mapOf("A1234BC" to 456),
      numberOfAppointments = 2,
    )
    val appointment = entity.appointments().first()
    entity.appointments().map { it.attendees() }.flatten().forEach { appointment.addAttendee(it) }
    assertThat(appointment.attendees().map { it.prisonerNumber }).isEqualTo(listOf("A1234BC", "A1234BC"))
    assertThat(appointment.prisonerNumbers()).containsExactly("A1234BC")
  }

  @Test
  fun `entity to summary mapping`() {
    val entity = appointmentSeriesEntity().appointments().first()
    assertThat(entity.toSummary()).isEqualTo(
      AppointmentSummary(
        entity.appointmentId,
        1,
        LocalDate.now().plusDays(1),
        LocalTime.of(9, 0),
        LocalTime.of(10, 30),
        isEdited = true,
        isCancelled = false,
        isDeleted = false,
      ),
    )
  }

  @Test
  fun `entity list to summary list mapping`() {
    val entity = appointmentSeriesEntity().appointments().first()
    assertThat(listOf(entity).toSummary()).isEqualTo(
      listOf(
        AppointmentSummary(
          entity.appointmentId,
          1,
          entity.startDate,
          entity.startTime,
          entity.endTime,
          isEdited = true,
          isCancelled = false,
          isDeleted = false,
        ),
      ),
    )
  }

  @Test
  fun `entity to details mapping`() {
    val appointmentSeries = appointmentSeriesEntity()
    val entity = appointmentSeries.appointments().first()
    val referenceCodeMap = mapOf(appointmentSeries.categoryCode to appointmentCategoryReferenceCode(appointmentSeries.categoryCode))
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
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
    assertThat(entity.toDetails(prisonerMap, referenceCodeMap, locationMap)).isEqualTo(
      appointmentDetails(
        entity.appointmentId,
        appointmentSeries.appointmentSeriesId,
        sequenceNumber = 1,
        customName = appointmentSeries.customName,
        createdTime = appointmentSeries.createdTime,
        updatedTime = entity.updatedTime,
      ),
    )
  }

  @Test
  fun `entity list to details list mapping`() {
    val appointmentSeries = appointmentSeriesEntity()
    val entity = appointmentSeries.appointments().first()
    val referenceCodeMap = mapOf(appointmentSeries.categoryCode to appointmentCategoryReferenceCode(appointmentSeries.categoryCode))
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
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
    assertThat(listOf(entity).toDetails(prisonerMap, referenceCodeMap, locationMap)).isEqualTo(
      listOf(
        appointmentDetails(
          entity.appointmentId,
          appointmentSeries.appointmentSeriesId,
          sequenceNumber = 1,
          customName = appointmentSeries.customName,
          createdTime = appointmentSeries.createdTime,
          updatedTime = entity.updatedTime,
        ),
      ),
    )
  }

  @Test
  fun `entity to details mapping appointment set`() {
    val appointmentSeries = appointmentSetEntity().appointmentSeries().first()
    val entity = appointmentSeries.appointments().first()
    val referenceCodeMap = mapOf(appointmentSeries.categoryCode to appointmentCategoryReferenceCode(appointmentSeries.categoryCode))
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
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
    assertThat(entity.toDetails(prisonerMap, referenceCodeMap, locationMap).appointmentSet).isEqualTo(AppointmentSetSummary(1, 3, 3))
  }

  @Test
  fun `entity to details mapping prisoner not found`() {
    val appointmentSeries = appointmentSeriesEntity()
    val entity = appointmentSeries.appointments().first()
    val referenceCodeMap = mapOf(appointmentSeries.categoryCode to appointmentCategoryReferenceCode(appointmentSeries.categoryCode))
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
    val prisonerMap = emptyMap<String, Prisoner>()
    with(entity.toDetails(prisonerMap, referenceCodeMap, locationMap).attendees) {
      assertThat(size).isEqualTo(1)
      with(first().prisoner) {
        assertThat(prisonerNumber).isEqualTo("A1234BC")
        assertThat(bookingId).isEqualTo(456)
        assertThat(firstName).isEqualTo("UNKNOWN")
        assertThat(lastName).isEqualTo("UNKNOWN")
        assertThat(prisonCode).isEqualTo("UNKNOWN")
        assertThat(cellLocation).isEqualTo("UNKNOWN")
      }
    }
  }

  @Test
  fun `entity to details mapping map single prisoner`() {
    val appointmentSeries = appointmentSeriesEntity()
    val entity = appointmentSeries.appointments().first()
    val referenceCodeMap = mapOf(appointmentSeries.categoryCode to appointmentCategoryReferenceCode(appointmentSeries.categoryCode))
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
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
    with(entity.toDetails(prisonersMap, referenceCodeMap, locationMap).attendees) {
      assertThat(size).isEqualTo(1)
      with(first().prisoner) {
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
    val appointmentSeries = appointmentSeriesEntity()
    val entity = appointmentSeries.appointments().first()
    val referenceCodeMap = emptyMap<String, ReferenceCode>()
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
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
    with(entity.toDetails(prisonerMap, referenceCodeMap, locationMap)) {
      assertThat(category.code).isEqualTo(appointmentSeries.categoryCode)
      assertThat(category.description).isEqualTo(appointmentSeries.categoryCode)
    }
  }

  @Test
  fun `entity to details mapping location not found`() {
    val appointmentSeries = appointmentSeriesEntity()
    val entity = appointmentSeries.appointments().first()
    val referenceCodeMap = mapOf(appointmentSeries.categoryCode to appointmentCategoryReferenceCode(appointmentSeries.categoryCode))
    val locationMap = emptyMap<Long, Location>()
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
    with(entity.toDetails(prisonerMap, referenceCodeMap, locationMap)) {
      assertThat(internalLocation).isNotNull
      assertThat(internalLocation!!.id).isEqualTo(entity.internalLocationId)
      assertThat(internalLocation!!.prisonCode).isEqualTo("TPR")
      assertThat(internalLocation!!.description).isEqualTo("No information available")
    }
  }

  @Test
  fun `entity to details mapping in cell nullifies internal location`() {
    val appointmentSeries = appointmentSeriesEntity(internalLocationId = 123, inCell = true)
    val entity = appointmentSeries.appointments().first()
    entity.internalLocationId = 123
    val referenceCodeMap = mapOf(appointmentSeries.categoryCode to appointmentCategoryReferenceCode(appointmentSeries.categoryCode))
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
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
    with(entity.toDetails(prisonerMap, referenceCodeMap, locationMap)) {
      assertThat(internalLocation).isNull()
      assertThat(inCell).isTrue
    }
  }

  @Test
  fun `entity to details mapping updated by null`() {
    val appointmentSeries = appointmentSeriesEntity(updatedBy = null)
    val entity = appointmentSeries.appointments().first()
    entity.updatedBy = null
    val referenceCodeMap = mapOf(appointmentSeries.categoryCode to appointmentCategoryReferenceCode(appointmentSeries.categoryCode))
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
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
    with(entity.toDetails(prisonerMap, referenceCodeMap, locationMap)) {
      assertThat(updatedBy).isNull()
      assertThat(isEdited).isFalse
    }
  }

  @Test
  fun `entity to details mapping cancelled by null`() {
    val appointmentSeries = appointmentSeriesEntity()
    val entity = appointmentSeries.appointments().first()
    entity.cancelledTime = null
    entity.cancellationReason = null
    entity.cancelledBy = null
    val referenceCodeMap = mapOf(appointmentSeries.categoryCode to appointmentCategoryReferenceCode(appointmentSeries.categoryCode))
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
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
    with(entity.toDetails(prisonerMap, referenceCodeMap, locationMap)) {
      assertThat(isCancelled).isFalse
      assertThat(cancelledTime).isNull()
      assertThat(cancelledBy).isNull()
    }
  }

  @Test
  fun `entity to details mapping repeat appointment`() {
    val appointmentSeries = appointmentSeriesEntity(frequency = AppointmentRepeatPeriodEntity.WEEKLY, numberOfAppointments = 4)
    val entity = appointmentSeries.appointments().first()
    val referenceCodeMap = mapOf(appointmentSeries.categoryCode to appointmentCategoryReferenceCode(appointmentSeries.categoryCode))
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
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
    with(entity.toDetails(prisonerMap, referenceCodeMap, locationMap)) {
      assertThat(this.appointmentSeries!!.schedule).isEqualTo(AppointmentSeriesSchedule(AppointmentFrequency.WEEKLY, 4))
    }
  }

  @Test
  fun `entity to details mapping includes appointment description in name`() {
    val appointmentSeries = appointmentSeriesEntity(customName = "appointment name")
    val entity = appointmentSeries.appointments().first()
    val referenceCodeMap = mapOf(
      appointmentSeries.categoryCode to appointmentCategoryReferenceCode(
        appointmentSeries.categoryCode,
        "test category",
      ),
    )
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
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
    with(entity.toDetails(prisonerMap, referenceCodeMap, locationMap)) {
      assertThat(appointmentName).isEqualTo("appointment name (test category)")
    }
  }

  @Test
  fun `entity to details mapping does not include appointment description in name`() {
    val appointmentSeries = appointmentSeriesEntity(customName = null)
    val entity = appointmentSeries.appointments().first()
    val referenceCodeMap = mapOf(
      appointmentSeries.categoryCode to appointmentCategoryReferenceCode(
        appointmentSeries.categoryCode,
        "test category",
      ),
    )
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
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
    with(entity.toDetails(prisonerMap, referenceCodeMap, locationMap)) {
      assertThat(appointmentName).isEqualTo("test category")
    }
  }

  @Test
  fun `cannot allocate multiple prisoners to individual appointment`() {
    assertThrows<IllegalArgumentException>(
      "Cannot allocate multiple prisoners to an individual appointment",
    ) {
      appointmentSeriesEntity(
        appointmentType = AppointmentType.INDIVIDUAL,
        prisonerNumberToBookingIdMap = mapOf("A1234BC" to 456, "B2345CD" to 789),
      )
    }
  }

  @Test
  fun `isCancelled is false when cancellation reason is false`() {
    val entity = appointmentSeriesEntity().appointments().first()
    assertThat(entity.isCancelled()).isFalse
  }

  @Test
  fun `isCancelled is false when cancellation reason deleted is true`() {
    val entity = appointmentSeriesEntity().appointments().first().apply {
      cancelledTime = LocalDateTime.now()
      isDeleted = true
    }
    assertThat(entity.isCancelled()).isFalse
  }

  @Test
  fun `isCancelled is true when cancellation reason deleted is false`() {
    val entity = appointmentSeriesEntity().appointments().first().apply {
      cancelledTime = LocalDateTime.now()
      isDeleted = false
    }
    assertThat(entity.isCancelled()).isTrue
  }

  @Test
  fun `addAttendee creates new attendee entity`() {
    val entity = appointmentSeriesEntity(prisonerNumberToBookingIdMap = emptyMap()).appointments().first()
    assertThat(entity.attendees()).isEmpty()
    val addedTime = LocalDateTime.now()
    entity.addAttendee("A1234BC", 123, addedTime, "ADDED_BY_USER")
    with(entity.attendees().single()) {
      appointment isEqualTo entity
      prisonerNumber isEqualTo "A1234BC"
      bookingId isEqualTo 123
      addedTime isEqualTo addedTime
      addedBy isEqualTo "ADDED_BY_USER"
      attended isEqualTo null
      attendanceRecordedTime isEqualTo null
      attendanceRecordedBy isEqualTo null
      removedTime isEqualTo null
      removalReason isEqualTo null
      removedBy isEqualTo null
      isRemoved() isBool false
      isDeleted isBool false
    }
  }

  @Test
  fun `addAttendee returns new attendee entity`() {
    val entity = appointmentSeriesEntity(prisonerNumberToBookingIdMap = emptyMap()).appointments().first()
    assertThat(entity.attendees()).isEmpty()
    val addedTime = LocalDateTime.now()
    with(entity.addAttendee("A1234BC", 123, addedTime, "ADDED_BY_USER")!!) {
      appointment isEqualTo entity
      prisonerNumber isEqualTo "A1234BC"
      bookingId isEqualTo 123
      addedTime isEqualTo addedTime
      addedBy isEqualTo "ADDED_BY_USER"
      attended isEqualTo null
      attendanceRecordedTime isEqualTo null
      attendanceRecordedBy isEqualTo null
      removedTime isEqualTo null
      removalReason isEqualTo null
      removedBy isEqualTo null
      isRemoved() isBool false
      isDeleted isBool false
    }
  }

  @Test
  fun `addAttendee does not create duplicate attendee entity`() {
    val entity = appointmentSeriesEntity(prisonerNumberToBookingIdMap = mapOf("A1234BC" to 123)).appointments().first()
    val attendee = entity.attendees().single()
    val addedTime = LocalDateTime.now()
    entity.addAttendee("A1234BC", 123, addedTime, "ADDED_BY_USER")
    entity.attendees().single() isEqualTo attendee
  }

  @Test
  fun `addAttendee creates new attendee entity when soft deleted attendee exists`() {
    val entity = appointmentSeriesEntity(prisonerNumberToBookingIdMap = mapOf("A1234BC" to 123)).appointments().first().apply {
      removeAttendee("A1234BC", removalReason = permanentRemovalByUserAppointmentAttendeeRemovalReason(), removedBy = "REMOVED_BY_USER")
    }
    assertThat(entity.attendees()).isEmpty()
    val addedTime = LocalDateTime.now()
    entity.addAttendee("A1234BC", 123, addedTime, "ADDED_BY_USER")
    with(entity.attendees().single()) {
      appointment isEqualTo entity
      prisonerNumber isEqualTo "A1234BC"
      bookingId isEqualTo 123
      addedTime isEqualTo addedTime
      addedBy isEqualTo "ADDED_BY_USER"
      attended isEqualTo null
      attendanceRecordedTime isEqualTo null
      attendanceRecordedBy isEqualTo null
      removedTime isEqualTo null
      removalReason isEqualTo null
      removedBy isEqualTo null
      isRemoved() isBool false
      isDeleted isBool false
    }
  }

  @Test
  fun `addAttendee soft deletes any existing attendee records for prisoner and creates new attendee entity`() {
    val removedTime = LocalDateTime.now()
    val entity = appointmentSeriesEntity(prisonerNumberToBookingIdMap = mapOf("A1234BC" to 123)).appointments().first().apply {
      removeAttendee("A1234BC", removedTime, tempRemovalByUserAppointmentAttendeeRemovalReason(), "REMOVED_BY_USER")
    }
    val removedAttendee = entity.attendees().single()
    with(removedAttendee) {
      this.removedTime isEqualTo removedTime
      removalReason isEqualTo tempRemovalByUserAppointmentAttendeeRemovalReason()
      removedBy isEqualTo "REMOVED_BY_USER"
      isRemoved() isBool true
      isDeleted isBool false
    }
    val addedTime = LocalDateTime.now()
    entity.addAttendee("A1234BC", 123, addedTime, "ADDED_BY_USER")
    with(entity.attendees().single()) {
      appointment isEqualTo entity
      prisonerNumber isEqualTo "A1234BC"
      bookingId isEqualTo 123
      this.addedTime isEqualTo addedTime
      addedBy isEqualTo "ADDED_BY_USER"
      attended isEqualTo null
      attendanceRecordedTime isEqualTo null
      attendanceRecordedBy isEqualTo null
      this.removedTime isEqualTo null
      removalReason isEqualTo null
      removedBy isEqualTo null
      isRemoved() isBool false
      isDeleted isBool false
    }
    with(removedAttendee) {
      this.removedTime isEqualTo removedTime
      removalReason isEqualTo tempRemovalByUserAppointmentAttendeeRemovalReason()
      removedBy isEqualTo "REMOVED_BY_USER"
      isRemoved() isBool false
      isDeleted isBool true
    }
  }

  @Test
  fun `addAttendee throws exception when adding prisoner to individual appointment`() {
    val entity = appointmentSeriesEntity(appointmentType = AppointmentType.INDIVIDUAL, prisonerNumberToBookingIdMap = mapOf("A1234BC" to 123)).appointments().first()
    assertThrows<IllegalArgumentException>(
      "Cannot allocate multiple prisoners to an individual appointment",
    ) {
      entity.addAttendee("B2345CD", 456, LocalDateTime.now(), "ADDED_BY_USER")
    }
  }

  @Test
  fun `removeAttendee with soft delete reason`() {
    val entity = appointmentSeriesEntity(prisonerNumberToBookingIdMap = mapOf("A1234BC" to 123)).appointments().first()
    val removedAttendee = entity.attendees().single()
    with(removedAttendee) {
      this.removedTime isEqualTo null
      removalReason isEqualTo null
      removedBy isEqualTo null
      isRemoved() isBool false
      isDeleted isBool false
    }
    val removedTime = LocalDateTime.now()
    entity.removeAttendee("A1234BC", removedTime, permanentRemovalByUserAppointmentAttendeeRemovalReason(), "REMOVED_BY_USER")
    assertThat(entity.attendees()).isEmpty()
    with(removedAttendee) {
      this.removedTime isEqualTo removedTime
      removalReason isEqualTo permanentRemovalByUserAppointmentAttendeeRemovalReason()
      removedBy isEqualTo "REMOVED_BY_USER"
      isRemoved() isBool false
      isDeleted isBool true
    }
  }

  @Test
  fun `removeAttendee with non soft delete reason`() {
    val entity = appointmentSeriesEntity(prisonerNumberToBookingIdMap = mapOf("A1234BC" to 123)).appointments().first()
    with(entity.attendees().single()) {
      this.removedTime isEqualTo null
      removalReason isEqualTo null
      removedBy isEqualTo null
      isRemoved() isBool false
      isDeleted isBool false
    }
    val removedTime = LocalDateTime.now()
    entity.removeAttendee("A1234BC", removedTime, tempRemovalByUserAppointmentAttendeeRemovalReason(), "REMOVED_BY_USER")
    with(entity.attendees().single()) {
      this.removedTime isEqualTo removedTime
      removalReason isEqualTo tempRemovalByUserAppointmentAttendeeRemovalReason()
      removedBy isEqualTo "REMOVED_BY_USER"
      isRemoved() isBool true
      isDeleted isBool false
    }
  }

  @Test
  fun `removeAttendee returns attendee entity`() {
    val appointment = appointmentSeriesEntity(prisonerNumberToBookingIdMap = mapOf("A1234BC" to 123)).appointments().first()
    val removedTime = LocalDateTime.now()
    with(
      appointment.removeAttendee(
        "A1234BC",
        removedTime,
        tempRemovalByUserAppointmentAttendeeRemovalReason(),
        "REMOVED_BY_USER",
      ).first(),
    ) {
      this.removedTime isEqualTo removedTime
      removalReason isEqualTo tempRemovalByUserAppointmentAttendeeRemovalReason()
      removedBy isEqualTo "REMOVED_BY_USER"
      isRemoved() isBool true
      isDeleted isBool false
    }
  }

  @Nested
  @DisplayName("mark prisoner attendance")
  inner class MarkPrisonerAttendance {
    @Test
    fun `cannot mark attendance for a cancelled appointment`() {
      val entity = appointmentSeriesEntity(prisonerNumberToBookingIdMap = mapOf("A1234BC" to 123, "B2345CD" to 456, "C3456DE" to 789)).appointments().first()
        .apply {
          cancelledTime = LocalDateTime.now().minusDays(1)
          cancellationReason = appointmentCancelledReason()
          cancelledBy = "CANCEL.USER"
        }
      val now = LocalDateTime.now()
      val username = "ATTENDANCE.RECORDED.BY"

      assertThrows<IllegalArgumentException>(
        "Cannot mark attendance for a cancelled appointment",
      ) {
        entity.markPrisonerAttendance(
          attendedPrisonNumbers = listOf("B2345CD"),
          nonAttendedPrisonNumbers = emptyList(),
          attendanceRecordedTime = now,
          attendanceRecordedBy = username,
        )
      }

      assertThat(entity.publishedDomainEvents()).isEmpty()
    }

    @Test
    fun `record attendance`() {
      val entity = appointmentSeriesEntity(prisonerNumberToBookingIdMap = mapOf("A1234BC" to 123, "B2345CD" to 456, "C3456DE" to 789)).appointments().first()
      val now = LocalDateTime.now()
      val username = "ATTENDANCE.RECORDED.BY"

      entity.markPrisonerAttendance(
        attendedPrisonNumbers = listOf("B2345CD"),
        nonAttendedPrisonNumbers = emptyList(),
        attendanceRecordedTime = now,
        attendanceRecordedBy = username,
      )

      with(entity.attendees().single { it.prisonerNumber == "A1234BC" }) {
        assertThat(attended).isNull()
        assertThat(attendanceRecordedTime).isNull()
        assertThat(attendanceRecordedBy).isNull()
      }
      with(entity.attendees().single { it.prisonerNumber == "B2345CD" }) {
        assertThat(attended).isTrue()
        assertThat(attendanceRecordedTime).isEqualTo(now)
        assertThat(attendanceRecordedBy).isEqualTo(username)
      }
      with(entity.attendees().single { it.prisonerNumber == "C3456DE" }) {
        assertThat(attended).isNull()
        assertThat(attendanceRecordedTime).isNull()
        assertThat(attendanceRecordedBy).isNull()
      }

      assertThat(entity.publishedDomainEvents().single()).isEqualTo(
        AppointmentAttendanceMarkedEvent(
          appointmentId = entity.appointmentId,
          prisonCode = entity.prisonCode,
          attendedPrisonNumbers = mutableListOf("B2345CD"),
          nonAttendedPrisonNumbers = mutableListOf(),
          attendanceChangedPrisonNumbers = mutableListOf(),
          attendanceRecordedTime = now,
          attendanceRecordedBy = username,
        ),
      )
    }

    @Test
    fun `record non-attendance`() {
      val entity = appointmentSeriesEntity(prisonerNumberToBookingIdMap = mapOf("A1234BC" to 123, "B2345CD" to 456, "C3456DE" to 789)).appointments().first()
      val now = LocalDateTime.now()
      val username = "ATTENDANCE.RECORDED.BY"

      entity.markPrisonerAttendance(
        attendedPrisonNumbers = emptyList(),
        nonAttendedPrisonNumbers = listOf("C3456DE"),
        attendanceRecordedTime = now,
        attendanceRecordedBy = username,
      )

      with(entity.attendees().single { it.prisonerNumber == "A1234BC" }) {
        assertThat(attended).isNull()
        assertThat(attendanceRecordedTime).isNull()
        assertThat(attendanceRecordedBy).isNull()
      }
      with(entity.attendees().single { it.prisonerNumber == "B2345CD" }) {
        assertThat(attended).isNull()
        assertThat(attendanceRecordedTime).isNull()
        assertThat(attendanceRecordedBy).isNull()
      }
      with(entity.attendees().single { it.prisonerNumber == "C3456DE" }) {
        assertThat(attended).isFalse()
        assertThat(attendanceRecordedTime).isEqualTo(now)
        assertThat(attendanceRecordedBy).isEqualTo(username)
      }

      assertThat(entity.publishedDomainEvents().single()).isEqualTo(
        AppointmentAttendanceMarkedEvent(
          appointmentId = entity.appointmentId,
          prisonCode = entity.prisonCode,
          attendedPrisonNumbers = mutableListOf(),
          nonAttendedPrisonNumbers = mutableListOf("C3456DE"),
          attendanceChangedPrisonNumbers = mutableListOf(),
          attendanceRecordedTime = now,
          attendanceRecordedBy = username,
        ),
      )
    }

    @Test
    fun `change attendance`() {
      val entity = appointmentSeriesEntity(prisonerNumberToBookingIdMap = mapOf("A1234BC" to 123, "B2345CD" to 456, "C3456DE" to 789)).appointments().first()
        .apply {
          with(attendees().single { it.prisonerNumber == "B2345CD" }) {
            attended = true
            attendanceRecordedTime = LocalDateTime.now().minusDays(1)
            attendanceRecordedBy = "PREV.ATTENDANCE.RECORDED.BY"
          }
          with(attendees().single { it.prisonerNumber == "C3456DE" }) {
            attended = false
            attendanceRecordedTime = LocalDateTime.now().minusDays(1)
            attendanceRecordedBy = "PREV.ATTENDANCE.RECORDED.BY"
          }
        }
      val now = LocalDateTime.now()
      val username = "ATTENDANCE.RECORDED.BY"

      entity.markPrisonerAttendance(
        attendedPrisonNumbers = listOf("A1234BC", "C3456DE"),
        nonAttendedPrisonNumbers = listOf("B2345CD"),
        attendanceRecordedTime = now,
        attendanceRecordedBy = username,
      )

      with(entity.attendees().single { it.prisonerNumber == "A1234BC" }) {
        assertThat(attended).isTrue()
        assertThat(attendanceRecordedTime).isEqualTo(now)
        assertThat(attendanceRecordedBy).isEqualTo(username)
      }
      with(entity.attendees().single { it.prisonerNumber == "B2345CD" }) {
        assertThat(attended).isFalse()
        assertThat(attendanceRecordedTime).isEqualTo(now)
        assertThat(attendanceRecordedBy).isEqualTo(username)
      }
      with(entity.attendees().single { it.prisonerNumber == "C3456DE" }) {
        assertThat(attended).isTrue()
        assertThat(attendanceRecordedTime).isEqualTo(now)
        assertThat(attendanceRecordedBy).isEqualTo(username)
      }

      assertThat(entity.publishedDomainEvents().single()).isEqualTo(
        AppointmentAttendanceMarkedEvent(
          appointmentId = entity.appointmentId,
          prisonCode = entity.prisonCode,
          attendedPrisonNumbers = mutableListOf("A1234BC", "C3456DE"),
          nonAttendedPrisonNumbers = mutableListOf("B2345CD"),
          attendanceChangedPrisonNumbers = mutableListOf("C3456DE", "B2345CD"),
          attendanceRecordedTime = now,
          attendanceRecordedBy = username,
        ),
      )
    }

    @Test
    fun `record attendance applies only to found prisoners`() {
      val entity = appointmentSeriesEntity(prisonerNumberToBookingIdMap = mapOf("A1234BC" to 123, "B2345CD" to 456, "C3456DE" to 789)).appointments().first()
      val now = LocalDateTime.now()
      val username = "ATTENDANCE.RECORDED.BY"

      entity.markPrisonerAttendance(
        attendedPrisonNumbers = listOf("B2345CD", "D4567EF"),
        nonAttendedPrisonNumbers = listOf("C3456DE", "E5678FG"),
        attendanceRecordedTime = now,
        attendanceRecordedBy = username,
      )

      with(entity.attendees().single { it.prisonerNumber == "A1234BC" }) {
        assertThat(attended).isNull()
        assertThat(attendanceRecordedTime).isNull()
        assertThat(attendanceRecordedBy).isNull()
      }
      with(entity.attendees().single { it.prisonerNumber == "B2345CD" }) {
        assertThat(attended).isTrue()
        assertThat(attendanceRecordedTime).isEqualTo(now)
        assertThat(attendanceRecordedBy).isEqualTo(username)
      }
      with(entity.attendees().single { it.prisonerNumber == "C3456DE" }) {
        assertThat(attended).isFalse()
        assertThat(attendanceRecordedTime).isEqualTo(now)
        assertThat(attendanceRecordedBy).isEqualTo(username)
      }

      assertThat(entity.publishedDomainEvents().single()).isEqualTo(
        AppointmentAttendanceMarkedEvent(
          appointmentId = entity.appointmentId,
          prisonCode = entity.prisonCode,
          attendedPrisonNumbers = mutableListOf("B2345CD"),
          nonAttendedPrisonNumbers = mutableListOf("C3456DE"),
          attendanceChangedPrisonNumbers = mutableListOf(),
          attendanceRecordedTime = now,
          attendanceRecordedBy = username,
        ),
      )
    }

    @Test
    fun `throws error when setting an organiser if tier is not TIER_2`() {
      val entity = appointmentSeriesEntity().appointments().first()

      entity.appointmentTier = EventTier(1, "TIER_1", "Tier 1")

      val exception = assertThrows<IllegalArgumentException> {
        entity.appointmentOrganiser = EventOrganiser(1, "PRISON_STAFF", "Prison staff")
      }
      exception.message isEqualTo "Cannot add organiser unless appointment is Tier 2."
    }
  }
}
