package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.toModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentAttendanceSummaryEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentAttendanceSummaryModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocationDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSearchEntity

class AppointmentAttendanceSummaryTest {
  @Test
  fun `entity to model mapping`() {
    val entity = appointmentAttendanceSummaryEntity()
    val expectedModel = appointmentAttendanceSummaryModel()
    val appointmentCategories = mapOf(entity.categoryCode to appointmentCategory(entity.categoryCode, "Chaplaincy"))
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocationDetails(entity.internalLocationId, entity.dpsLocationId!!, entity.prisonCode, description = "Chapel"))
    val attendees = appointmentSearchEntity().attendees
    assertThat(entity.toModel(attendees, appointmentCategories, locationMap)).isEqualTo(expectedModel)
  }

  @Test
  fun `entity list to results list mapping`() {
    val entityList = listOf(appointmentAttendanceSummaryEntity())
    val expectedModel = listOf(appointmentAttendanceSummaryModel())
    with(entityList.single()) {
      val appointmentCategories = mapOf(categoryCode to appointmentCategory(categoryCode, "Chaplaincy"))

      val locationMap = mapOf(
        internalLocationId!! to appointmentLocationDetails(
          internalLocationId,
          dpsLocationId!!,
          prisonCode,
          description = "Chapel",
        ),
      )

      val attendees = mapOf(1L to appointmentSearchEntity().attendees)
      assertThat(entityList.toModel(attendees, appointmentCategories, locationMap)).isEqualTo(expectedModel)
    }
  }

  @Test
  fun `entity to result mapping in cell nullifies internal location`() {
    val entity = appointmentAttendanceSummaryEntity(inCell = true)
    val appointmentCategories = mapOf(entity.categoryCode to appointmentCategory(entity.categoryCode, "Chaplaincy"))
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocationDetails(entity.internalLocationId, entity.dpsLocationId!!, entity.prisonCode, description = "Chapel"))
    val attendees = appointmentSearchEntity().attendees
    assertThat(entity.toModel(attendees, appointmentCategories, locationMap).internalLocation).isNull()
    assertThat(entity.toModel(attendees, appointmentCategories, locationMap).attendees[0].prisonerNumber).isEqualTo("A1234BC")
  }
}
