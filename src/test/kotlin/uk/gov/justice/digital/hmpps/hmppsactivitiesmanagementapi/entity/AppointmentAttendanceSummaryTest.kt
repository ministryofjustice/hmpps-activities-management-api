package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.toModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentAttendanceSummaryEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentAttendanceSummaryModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSearchEntity

class AppointmentAttendanceSummaryTest {
  @Test
  fun `entity to model mapping`() {
    val entity = appointmentAttendanceSummaryEntity()
    val expectedModel = appointmentAttendanceSummaryModel()
    val referenceCodeMap = mapOf(entity.categoryCode to appointmentCategoryReferenceCode(entity.categoryCode, "Chaplaincy"))
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, entity.prisonCode, userDescription = "Chapel"))
    val attendees = appointmentSearchEntity().attendees
    assertThat(entity.toModel(attendees, referenceCodeMap, locationMap)).isEqualTo(expectedModel)
  }

  @Test
  fun `entity list to results list mapping`() {
    val entityList = listOf(appointmentAttendanceSummaryEntity())
    val expectedModel = listOf(appointmentAttendanceSummaryModel())
    val referenceCodeMap = mapOf(entityList.single().categoryCode to appointmentCategoryReferenceCode(entityList.single().categoryCode, "Chaplaincy"))
    val locationMap = mapOf(entityList.single().internalLocationId!! to appointmentLocation(entityList.single().internalLocationId!!, entityList.single().prisonCode, userDescription = "Chapel"))
    val attendees = mapOf(1L to appointmentSearchEntity().attendees)
    assertThat(entityList.toModel(attendees, referenceCodeMap, locationMap)).isEqualTo(expectedModel)
  }

  @Test
  fun `entity to result mapping in cell nullifies internal location`() {
    val entity = appointmentAttendanceSummaryEntity(inCell = true)
    val referenceCodeMap = mapOf(entity.categoryCode to appointmentCategoryReferenceCode(entity.categoryCode, "Chaplaincy"))
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, entity.prisonCode, userDescription = "Chapel"))
    val attendees = appointmentSearchEntity().attendees
    assertThat(entity.toModel(attendees, referenceCodeMap, locationMap).internalLocation).isNull()
    assertThat(entity.toModel(attendees, referenceCodeMap, locationMap).attendees[0].prisonerNumber).isEqualTo("A1234BC")
  }
}
