package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.UserDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSetEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.userDetail
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
    entity.isCancelled() isEqualTo false
    entity.isDeleted isEqualTo false
  }

  @Test
  fun `cancelled but not deleted when cancelled time is not null and is deleted = false`() {
    val entity = appointmentSeriesEntity().appointments().first().apply {
      cancelledTime = LocalDateTime.now()
      isDeleted = false
    }
    entity.isCancelled() isEqualTo true
    entity.isDeleted isEqualTo false
  }

  @Test
  fun `deleted but not cancelled when cancelled time is not null and is deleted = true`() {
    val entity = appointmentSeriesEntity().appointments().first().apply {
      cancelledTime = LocalDateTime.now()
      isDeleted = true
    }
    entity.isCancelled() isEqualTo false
    entity.isDeleted isEqualTo true
  }

  @Test
  fun `expired when start date time is in the past`() {
    val entity = appointmentSeriesEntity().appointments().first().apply {
      startDate = LocalDate.now()
      startTime = LocalTime.now().minusMinutes(1)
    }
    entity.isExpired() isEqualTo true
  }

  @Test
  fun `not expired when start date time is in the future`() {
    val entity = appointmentSeriesEntity().appointments().first().apply {
      startDate = LocalDate.now()
      startTime = LocalTime.now().plusMinutes(1)
    }
    entity.isExpired() isEqualTo false
  }

  @Test
  fun `scheduled when start date time is in the future, not cancelled or deleted`() {
    val entity = appointmentSeriesEntity().appointments().first().apply {
      startDate = LocalDate.now()
      startTime = LocalTime.now().plusMinutes(1)
      cancelledTime = null
      isDeleted = false
    }
    entity.isScheduled() isEqualTo true
  }

  @Test
  fun `not scheduled when start date time is in the past, not cancelled or deleted`() {
    val entity = appointmentSeriesEntity().appointments().first().apply {
      startDate = LocalDate.now()
      startTime = LocalTime.now().minusMinutes(1)
      cancelledTime = null
      isDeleted = false
    }
    entity.isScheduled() isEqualTo false
  }

  @Test
  fun `not scheduled when start date time is in the future but is cancelled`() {
    val entity = appointmentSeriesEntity().appointments().first().apply {
      startDate = LocalDate.now()
      startTime = LocalTime.now().plusMinutes(1)
      cancelledTime = LocalDateTime.now()
      isDeleted = false
    }
    entity.isScheduled() isEqualTo false
  }

  @Test
  fun `not scheduled when start date time is in the future but is deleted`() {
    val entity = appointmentSeriesEntity().appointments().first().apply {
      startDate = LocalDate.now()
      startTime = LocalTime.now().plusMinutes(1)
      cancelledTime = LocalDateTime.now()
      isDeleted = true
    }
    entity.isScheduled() isEqualTo false
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
    val userMap = mapOf(
      appointmentSeries.createdBy to userDetail(1, "CREATE.USER", "CREATE", "USER"),
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
    assertThat(entity.toDetails(prisonerMap, referenceCodeMap, locationMap, userMap)).isEqualTo(
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
    val userMap = mapOf(
      appointmentSeries.createdBy to userDetail(1, "CREATE.USER", "CREATE", "USER"),
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
    assertThat(listOf(entity).toDetails(prisonerMap, referenceCodeMap, locationMap, userMap)).isEqualTo(
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
    val userMap = mapOf(
      appointmentSeries.createdBy to userDetail(1, "CREATE.USER", "CREATE", "USER"),
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
    assertThat(entity.toDetails(prisonerMap, referenceCodeMap, locationMap, userMap).appointmentSet).isEqualTo(AppointmentSetSummary(1, 3, 3))
  }

  @Test
  fun `entity to details mapping prisoner not found`() {
    val appointmentSeries = appointmentSeriesEntity()
    val entity = appointmentSeries.appointments().first()
    val referenceCodeMap = mapOf(appointmentSeries.categoryCode to appointmentCategoryReferenceCode(appointmentSeries.categoryCode))
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
    val userMap = mapOf(
      appointmentSeries.createdBy to userDetail(1, "CREATE.USER", "CREATE", "USER"),
      entity.updatedBy!! to userDetail(2, "UPDATE.USER", "UPDATE", "USER"),
    )
    val prisonerMap = emptyMap<String, Prisoner>()
    with(entity.toDetails(prisonerMap, referenceCodeMap, locationMap, userMap).attendees) {
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
    val userMap = mapOf(
      appointmentSeries.createdBy to userDetail(1, "CREATE.USER", "CREATE", "USER"),
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
    with(entity.toDetails(prisonersMap, referenceCodeMap, locationMap, userMap).attendees) {
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
    val userMap = mapOf(
      appointmentSeries.createdBy to userDetail(1, "CREATE.USER", "CREATE", "USER"),
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
    with(entity.toDetails(prisonerMap, referenceCodeMap, locationMap, userMap)) {
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
    val userMap = mapOf(
      appointmentSeries.createdBy to userDetail(1, "CREATE.USER", "CREATE", "USER"),
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
    with(entity.toDetails(prisonerMap, referenceCodeMap, locationMap, userMap)) {
      assertThat(internalLocation).isNotNull
      assertThat(internalLocation!!.id).isEqualTo(entity.internalLocationId)
      assertThat(internalLocation!!.prisonCode).isEqualTo("TPR")
      assertThat(internalLocation!!.description).isEqualTo("No information available")
    }
  }

  @Test
  fun `entity to details mapping users not found`() {
    val appointmentSeries = appointmentSeriesEntity()
    val entity = appointmentSeries.appointments().first()
    entity.cancelledBy = "CANCEL.USER"
    val referenceCodeMap = mapOf(appointmentSeries.categoryCode to appointmentCategoryReferenceCode(appointmentSeries.categoryCode))
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
    with(entity.toDetails(prisonerMap, referenceCodeMap, locationMap, userMap)) {
      assertThat(createdBy.id).isEqualTo(-1)
      assertThat(createdBy.username).isEqualTo("CREATE.USER")
      assertThat(createdBy.firstName).isEqualTo("UNKNOWN")
      assertThat(createdBy.lastName).isEqualTo("UNKNOWN")
      assertThat(updatedBy).isNotNull
      assertThat(updatedBy!!.id).isEqualTo(-1)
      assertThat(updatedBy!!.username).isEqualTo("UPDATE.USER")
      assertThat(updatedBy!!.firstName).isEqualTo("UNKNOWN")
      assertThat(updatedBy!!.lastName).isEqualTo("UNKNOWN")
      assertThat(cancelledBy).isNotNull
      assertThat(cancelledBy!!.id).isEqualTo(-1)
      assertThat(cancelledBy!!.username).isEqualTo("CANCEL.USER")
      assertThat(cancelledBy!!.firstName).isEqualTo("UNKNOWN")
      assertThat(cancelledBy!!.lastName).isEqualTo("UNKNOWN")
    }
  }

  @Test
  fun `entity to details mapping in cell nullifies internal location`() {
    val appointmentSeries = appointmentSeriesEntity(internalLocationId = 123, inCell = true)
    val entity = appointmentSeries.appointments().first()
    entity.internalLocationId = 123
    val referenceCodeMap = mapOf(appointmentSeries.categoryCode to appointmentCategoryReferenceCode(appointmentSeries.categoryCode))
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
    val userMap = mapOf(
      appointmentSeries.createdBy to userDetail(1, "CREATE.USER", "CREATE", "USER"),
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
    with(entity.toDetails(prisonerMap, referenceCodeMap, locationMap, userMap)) {
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
    val userMap = mapOf(
      appointmentSeries.createdBy to userDetail(1, "CREATE.USER", "CREATE", "USER"),
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
    with(entity.toDetails(prisonerMap, referenceCodeMap, locationMap, userMap)) {
      assertThat(updatedBy).isNull()
      assertThat(isEdited).isFalse
    }
  }

  @Test
  fun `entity to details mapping cancelled by`() {
    val appointmentSeries = appointmentSeriesEntity()
    val entity = appointmentSeries.appointments().first()
    entity.cancelledTime = LocalDateTime.now()
    entity.cancellationReason = AppointmentCancellationReason(2, "Cancelled", false)
    entity.cancelledBy = "CANCEL.USER"
    val referenceCodeMap = mapOf(appointmentSeries.categoryCode to appointmentCategoryReferenceCode(appointmentSeries.categoryCode))
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
    val userMap = mapOf(
      appointmentSeries.createdBy to userDetail(1, "CREATE.USER", "CREATE", "USER"),
      entity.updatedBy!! to userDetail(2, "UPDATE.USER", "UPDATE", "USER"),
      entity.cancelledBy!! to userDetail(3, "CANCEL.USER", "CANCEL", "USER"),
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
    with(entity.toDetails(prisonerMap, referenceCodeMap, locationMap, userMap)) {
      assertThat(isCancelled).isTrue
      assertThat(cancelledTime).isEqualTo(entity.cancelledTime)
      assertThat(cancelledBy).isNotNull
      assertThat(cancelledBy!!.id).isEqualTo(3)
      assertThat(cancelledBy!!.username).isEqualTo("CANCEL.USER")
      assertThat(cancelledBy!!.firstName).isEqualTo("CANCEL")
      assertThat(cancelledBy!!.lastName).isEqualTo("USER")
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
    val userMap = mapOf(
      appointmentSeries.createdBy to userDetail(1, "CREATE.USER", "CREATE", "USER"),
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
    with(entity.toDetails(prisonerMap, referenceCodeMap, locationMap, userMap)) {
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
    val userMap = mapOf(
      appointmentSeries.createdBy to userDetail(1, "CREATE.USER", "CREATE", "USER"),
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
    with(entity.toDetails(prisonerMap, referenceCodeMap, locationMap, userMap)) {
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
    val userMap = mapOf(
      appointmentSeries.createdBy to userDetail(1, "CREATE.USER", "CREATE", "USER"),
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
    with(entity.toDetails(prisonerMap, referenceCodeMap, locationMap, userMap)) {
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
    val userMap = mapOf(
      appointmentSeries.createdBy to userDetail(1, "CREATE.USER", "CREATE", "USER"),
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
    with(entity.toDetails(prisonerMap, referenceCodeMap, locationMap, userMap)) {
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
}
