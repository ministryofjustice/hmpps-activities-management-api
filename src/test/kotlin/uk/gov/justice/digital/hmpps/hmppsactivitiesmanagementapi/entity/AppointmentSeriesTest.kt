package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.UserDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.userDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.UserSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentFrequency as AppointmentRepeatPeriodModel

class AppointmentSeriesTest {
  @Test
  fun `entity to model mapping`() {
    val entity = appointmentSeriesEntity()
    val expectedModel = appointmentSeriesModel(entity.createdTime, entity.updatedTime, entity.appointments().first().updatedTime)
    assertThat(entity.toModel()).isEqualTo(expectedModel)
  }

  @Test
  fun `entity list to model list mapping`() {
    val entity = appointmentSeriesEntity()
    val expectedModel = listOf(appointmentSeriesModel(entity.createdTime, entity.updatedTime, entity.appointments().first().updatedTime))
    assertThat(listOf(entity).toModel()).isEqualTo(expectedModel)
  }

  @Test
  fun `appointments filters out soft deleted appointments`() {
    val entity = appointmentSeriesEntity(frequency = AppointmentFrequency.WEEKLY, numberOfAppointments = 3).apply { appointments().first().isDeleted = true }
    with(entity.appointments()) {
      assertThat(size).isEqualTo(2)
      assertThat(this.map { it.appointmentId }).isEqualTo(listOf(2L, 3L))
    }
  }

  @Test
  fun `scheduled appointments filters out past appointments`() {
    val entity = appointmentSeriesEntity(
      startDate = LocalDate.now().minusDays(3),
      frequency = AppointmentFrequency.WEEKLY,
      numberOfAppointments = 3,
    )
    with(entity.scheduledAppointments()) {
      assertThat(size).isEqualTo(2)
      assertThat(this.map { it.appointmentId }).isEqualTo(listOf(2L, 3L))
    }
  }

  @Test
  fun `scheduled appointments filters out cancelled appointments`() {
    val entity = appointmentSeriesEntity(
      startDate = LocalDate.now().minusDays(3),
      frequency = AppointmentFrequency.WEEKLY,
      numberOfAppointments = 3,
    ).apply { appointments()[1].cancelledTime = LocalDateTime.now() }
    with(entity.scheduledAppointments()) {
      assertThat(size).isEqualTo(1)
      assertThat(this.map { it.appointmentId }).isEqualTo(listOf(3L))
    }
  }

  @Test
  fun `scheduled appointments filters out deleted appointments`() {
    val entity = appointmentSeriesEntity(
      startDate = LocalDate.now().minusDays(3),
      frequency = AppointmentFrequency.WEEKLY,
      numberOfAppointments = 3,
    ).apply { appointments()[2].isDeleted = true }
    with(entity.scheduledAppointments()) {
      assertThat(size).isEqualTo(1)
      assertThat(this.map { it.appointmentId }).isEqualTo(listOf(2L))
    }
  }

  @Test
  fun `scheduled appointments after filters out past appointments`() {
    val entity = appointmentSeriesEntity(
      startDate = LocalDate.now().minusDays(3),
      frequency = AppointmentFrequency.WEEKLY,
      numberOfAppointments = 4,
    )
    with(entity.scheduledAppointmentsAfter(LocalDateTime.now().minusDays(4))) {
      assertThat(size).isEqualTo(3)
      assertThat(this.map { it.appointmentId }).isEqualTo(listOf(2L, 3L, 4L))
    }
  }

  @Test
  fun `scheduled appointments after filters out cancelled appointments`() {
    val entity = appointmentSeriesEntity(
      startDate = LocalDate.now().minusDays(3),
      frequency = AppointmentFrequency.WEEKLY,
      numberOfAppointments = 4,
    ).apply { appointments()[2].cancelledTime = LocalDateTime.now() }
    with(entity.scheduledAppointmentsAfter(entity.appointments()[1].startDateTime())) {
      assertThat(size).isEqualTo(1)
      assertThat(this.map { it.appointmentId }).isEqualTo(listOf(4L))
    }
  }

  @Test
  fun `scheduled appointments after filters out deleted appointments`() {
    val entity = appointmentSeriesEntity(
      startDate = LocalDate.now().minusDays(3),
      frequency = AppointmentFrequency.WEEKLY,
      numberOfAppointments = 4,
    ).apply { appointments()[3].isDeleted = true }
    with(entity.scheduledAppointmentsAfter(entity.appointments()[1].startDateTime())) {
      assertThat(size).isEqualTo(1)
      assertThat(this.map { it.appointmentId }).isEqualTo(listOf(3L))
    }
  }

  @Test
  fun `apply to cannot action past appointment`() {
    val entity = appointmentSeriesEntity(
      frequency = AppointmentFrequency.WEEKLY,
      numberOfAppointments = 3,
    )
    val appointment = entity.appointments()[1]
    appointment.startDate = LocalDate.now().minusDays(3)
    assertThatThrownBy { entity.applyToAppointments(appointment, ApplyTo.THIS_APPOINTMENT, "update") }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Cannot update a past appointment")
  }

  @Test
  fun `apply to cannot action cancelled appointment`() {
    val entity = appointmentSeriesEntity(
      frequency = AppointmentFrequency.WEEKLY,
      numberOfAppointments = 3,
    )
    val appointment = entity.appointments()[1]
    appointment.cancelledTime = LocalDateTime.now()
    assertThatThrownBy { entity.applyToAppointments(appointment, ApplyTo.THIS_APPOINTMENT, "modify") }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Cannot modify a cancelled appointment")
  }

  @Test
  fun `apply to cannot action deleted appointment`() {
    val entity = appointmentSeriesEntity(
      frequency = AppointmentFrequency.WEEKLY,
      numberOfAppointments = 3,
    )
    val appointment = entity.appointments()[1]
    appointment.isDeleted = true
    assertThatThrownBy { entity.applyToAppointments(appointment, ApplyTo.THIS_APPOINTMENT, "cancel") }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Cannot cancel a deleted appointment")
  }

  @Test
  fun `apply to this appointment`() {
    val entity = appointmentSeriesEntity(
      frequency = AppointmentFrequency.WEEKLY,
      numberOfAppointments = 3,
    )
    val appointment = entity.appointments()[1]
    with(entity.applyToAppointments(appointment, ApplyTo.THIS_APPOINTMENT, "")) {
      assertThat(size).isEqualTo(1)
      assertThat(this.map { it.appointmentId }).isEqualTo(listOf(2L))
    }
  }

  @Test
  fun `apply to this and all future appointments`() {
    val entity = appointmentSeriesEntity(
      frequency = AppointmentFrequency.WEEKLY,
      numberOfAppointments = 3,
    )
    val appointment = entity.appointments()[1]
    with(entity.applyToAppointments(appointment, ApplyTo.THIS_AND_ALL_FUTURE_APPOINTMENTS, "")) {
      assertThat(size).isEqualTo(2)
      assertThat(this.map { it.appointmentId }).isEqualTo(listOf(2L, 3L))
    }
  }

  @Test
  fun `apply to all future appointments`() {
    val entity = appointmentSeriesEntity(
      frequency = AppointmentFrequency.WEEKLY,
      numberOfAppointments = 3,
    )
    val appointment = entity.appointments()[1]
    with(entity.applyToAppointments(appointment, ApplyTo.ALL_FUTURE_APPOINTMENTS, "")) {
      assertThat(size).isEqualTo(3)
      assertThat(this.map { it.appointmentId }).isEqualTo(listOf(1L, 2L, 3L))
    }
  }

  @Test
  fun `apply to this and all future appointments filters out past appointment`() {
    val entity = appointmentSeriesEntity(
      frequency = AppointmentFrequency.WEEKLY,
      numberOfAppointments = 3,
    ).apply { appointments()[2].startDate = LocalDate.now().minusDays(3) }
    val appointment = entity.appointments()[1]
    with(entity.applyToAppointments(appointment, ApplyTo.ALL_FUTURE_APPOINTMENTS, "")) {
      assertThat(size).isEqualTo(2)
      assertThat(this.map { it.appointmentId }).isEqualTo(listOf(1L, 2L))
    }
  }

  @Test
  fun `apply to this and all future appointments filters out cancelled appointments`() {
    val entity = appointmentSeriesEntity(
      frequency = AppointmentFrequency.WEEKLY,
      numberOfAppointments = 3,
    ).apply { appointments()[2].cancelledTime = LocalDateTime.now() }
    val appointment = entity.appointments()[1]
    with(entity.applyToAppointments(appointment, ApplyTo.ALL_FUTURE_APPOINTMENTS, "")) {
      assertThat(size).isEqualTo(2)
      assertThat(this.map { it.appointmentId }).isEqualTo(listOf(1L, 2L))
    }
  }

  @Test
  fun `apply to this and all future appointments filters out deleted appointments`() {
    val entity = appointmentSeriesEntity(
      frequency = AppointmentFrequency.WEEKLY,
      numberOfAppointments = 3,
    ).apply { appointments()[2].isDeleted = true }
    val appointment = entity.appointments()[1]
    with(entity.applyToAppointments(appointment, ApplyTo.ALL_FUTURE_APPOINTMENTS, "")) {
      assertThat(size).isEqualTo(2)
      assertThat(this.map { it.appointmentId }).isEqualTo(listOf(1L, 2L))
    }
  }

  @Test
  fun `apply to all future appointments filters out past appointments`() {
    val entity = appointmentSeriesEntity(
      startDate = LocalDate.now().minusDays(3),
      frequency = AppointmentFrequency.WEEKLY,
      numberOfAppointments = 3,
    )
    val appointment = entity.appointments()[1]
    with(entity.applyToAppointments(appointment, ApplyTo.ALL_FUTURE_APPOINTMENTS, "")) {
      assertThat(size).isEqualTo(2)
      assertThat(this.map { it.appointmentId }).isEqualTo(listOf(2L, 3L))
    }
  }

  @Test
  fun `apply to all future appointments filters out cancelled appointments`() {
    val entity = appointmentSeriesEntity(
      frequency = AppointmentFrequency.WEEKLY,
      numberOfAppointments = 3,
    ).apply { appointments()[2].cancelledTime = LocalDateTime.now() }
    val appointment = entity.appointments()[1]
    with(entity.applyToAppointments(appointment, ApplyTo.ALL_FUTURE_APPOINTMENTS, "")) {
      assertThat(size).isEqualTo(2)
      assertThat(this.map { it.appointmentId }).isEqualTo(listOf(1L, 2L))
    }
  }

  @Test
  fun `apply to all future appointments filters out deleted appointments`() {
    val entity = appointmentSeriesEntity(
      frequency = AppointmentFrequency.WEEKLY,
      numberOfAppointments = 3,
    ).apply { appointments()[2].isDeleted = true }
    val appointment = entity.appointments()[1]
    with(entity.applyToAppointments(appointment, ApplyTo.ALL_FUTURE_APPOINTMENTS, "")) {
      assertThat(size).isEqualTo(2)
      assertThat(this.map { it.appointmentId }).isEqualTo(listOf(1L, 2L))
    }
  }

  @Test
  fun `internal location ids includes appointment ids`() {
    val entity = appointmentSeriesEntity(123).apply { appointments().first().internalLocationId = 124 }
    assertThat(entity.internalLocationIds()).containsExactly(123, 124)
  }

  @Test
  fun `internal location ids removes duplicates`() {
    val entity = appointmentSeriesEntity(123).apply { appointments().first().internalLocationId = 123 }
    assertThat(entity.internalLocationIds()).containsExactly(123)
  }

  @Test
  fun `internal location ids removes null`() {
    val entity = appointmentSeriesEntity(123).apply { appointments().first().internalLocationId = null }
    assertThat(entity.internalLocationIds()).containsExactly(123)
  }

  @Test
  fun `prisoner numbers concatenates all appointment attendees`() {
    val entity = appointmentSeriesEntity(prisonerNumberToBookingIdMap = mapOf("A1234BC" to 456, "B2345CD" to 789))
    assertThat(entity.prisonerNumbers()).containsExactly("A1234BC", "B2345CD")
  }

  @Test
  fun `prisoner numbers uses prisoners allocated to the first future appointment`() {
    val entity = appointmentSeriesEntity(prisonerNumberToBookingIdMap = mapOf("A1234BC" to 456))
    entity.appointments().first().let {
      it.startDate = LocalDate.now()
      it.startTime = LocalTime.now().minusMinutes(1)
    }
    entity.addAppointment(appointmentEntity(entity, 2, 2, LocalDate.now(), LocalTime.now().plusMinutes(1), prisonerNumberToBookingIdMap = mapOf("B2345CD" to 457)))
    entity.addAppointment(appointmentEntity(entity, 3, 3, LocalDate.now(), LocalTime.now().plusMinutes(2), prisonerNumberToBookingIdMap = mapOf("C3456DE" to 458)))
    assertThat(entity.prisonerNumbers()).containsExactly("B2345CD")
  }

  @Test
  fun `prisoner numbers uses prisoners allocated to the last appointment if all appointments have past`() {
    val entity = appointmentSeriesEntity(prisonerNumberToBookingIdMap = mapOf("A1234BC" to 456))
    entity.appointments().first().let {
      it.startDate = LocalDate.now()
      it.startTime = LocalTime.now().minusMinutes(3)
    }
    entity.addAppointment(appointmentEntity(entity, 2, 2, LocalDate.now(), LocalTime.now().minusMinutes(2), prisonerNumberToBookingIdMap = mapOf("B2345CD" to 457)))
    entity.addAppointment(appointmentEntity(entity, 3, 3, LocalDate.now(), LocalTime.now().minusMinutes(1), prisonerNumberToBookingIdMap = mapOf("C3456DE" to 458)))
    assertThat(entity.prisonerNumbers()).containsExactly("C3456DE")
  }

  @Test
  fun `prisoner numbers uses empty list if there are no appointments`() {
    val entity = appointmentSeriesEntity(prisonerNumberToBookingIdMap = mapOf("A1234BC" to 456))
    entity.removeAppointment(entity.appointments().first())
    assertThat(entity.prisonerNumbers()).isEmpty()
  }

  @Test
  fun `usernames includes created by, updated by and appointment updated by and cancelled by`() {
    val entity = appointmentSeriesEntity(createdBy = "CREATE.USER", updatedBy = "UPDATE.USER").apply {
      appointments().first().updatedBy = "APPOINTMENT.UPDATE.USER"
      appointments().first().cancelledBy = "APPOINTMENT.CANCEL.USER"
    }
    assertThat(entity.usernames()).containsExactly("CREATE.USER", "UPDATE.USER", "APPOINTMENT.UPDATE.USER", "APPOINTMENT.CANCEL.USER")
  }

  @Test
  fun `usernames removes null`() {
    val entity = appointmentSeriesEntity(createdBy = "CREATE.USER", updatedBy = null).apply { appointments().first().updatedBy = "APPOINTMENT.UPDATE.USER" }
    assertThat(entity.usernames()).containsExactly("CREATE.USER", "APPOINTMENT.UPDATE.USER")
  }

  @Test
  fun `usernames removes duplicates`() {
    val entity = appointmentSeriesEntity(createdBy = "CREATE.USER", updatedBy = "CREATE.USER").apply { appointments().first().updatedBy = "CREATE.USER" }
    assertThat(entity.usernames()).containsExactly("CREATE.USER")
  }

  @Test
  fun `entity to details mapping`() {
    val entity = appointmentSeriesEntity()
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
      appointmentSeriesDetails(
        customName = "Appointment description",
        createdTime = entity.createdTime,
        updatedTime = entity.updatedTime,
        updatedBy = UserSummary(2, "UPDATE.USER", "UPDATE", "USER"),
      ),
    )
  }

  @Test
  fun `entity to details mapping reference code not found`() {
    val entity = appointmentSeriesEntity()
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
    val entity = appointmentSeriesEntity()
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
    val entity = appointmentSeriesEntity()
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
    val entity = appointmentSeriesEntity(internalLocationId = 123, inCell = true)
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
    val entity = appointmentSeriesEntity(updatedBy = null)
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
    val entity = appointmentSeriesEntity(updatedBy = null, frequency = AppointmentFrequency.FORTNIGHTLY, numberOfAppointments = 2)
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
      assertThat(schedule).isEqualTo(AppointmentSchedule(AppointmentRepeatPeriodModel.FORTNIGHTLY, 2))
    }
  }

  @Test
  fun `entity to details mapping includes custom name in name`() {
    val entity = appointmentSeriesEntity(customName = "appointment name")
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
  fun `entity to details mapping does not include custom name in name`() {
    val entity = appointmentSeriesEntity(customName = null)
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
    val entity = appointmentSeriesEntity(startDate = today).apply { schedule = null }
    assertThat(entity.scheduleIterator().asSequence().toList()).containsExactly(
      today,
    )
  }

  @Test
  fun `schedule values populates iterator`() {
    val today = LocalDate.now()
    val entity = appointmentSeriesEntity(startDate = today, frequency = AppointmentFrequency.WEEKLY, numberOfAppointments = 3)
    assertThat(entity.scheduleIterator().asSequence().toList()).containsExactly(
      today,
      today.plusWeeks(1),
      today.plusWeeks(2),
    )
  }
}
