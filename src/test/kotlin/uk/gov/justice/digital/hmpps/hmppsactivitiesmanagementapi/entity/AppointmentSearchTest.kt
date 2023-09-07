package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSearchEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentOccurrenceSearchResultModel

class AppointmentSearchTest {
  @Test
  fun `entity to result mapping`() {
    val entity = appointmentSearchEntity()
    val expectedModel = appointmentOccurrenceSearchResultModel()
    val referenceCodeMap = mapOf(entity.categoryCode to appointmentCategoryReferenceCode(entity.categoryCode))
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
    assertThat(entity.toResult(entity.attendees, referenceCodeMap, locationMap)).isEqualTo(expectedModel)
  }

  @Test
  fun `entity list to results list mapping`() {
    val entityList = listOf(appointmentSearchEntity())
    val expectedModel = listOf(appointmentOccurrenceSearchResultModel())
    val referenceCodeMap = mapOf(entityList.first().categoryCode to appointmentCategoryReferenceCode(entityList.first().categoryCode))
    val locationMap = mapOf(entityList.first().internalLocationId!! to appointmentLocation(entityList.first().internalLocationId!!, "TPR"))
    assertThat(entityList.toResults(mapOf(entityList.first().appointmentId to entityList.first().attendees), referenceCodeMap, locationMap)).isEqualTo(expectedModel)
  }

  @Test
  fun `entity to result mapping in cell nullifies internal location`() {
    val entity = appointmentSearchEntity(inCell = true)
    entity.internalLocationId = 123
    val referenceCodeMap = mapOf(entity.categoryCode to appointmentCategoryReferenceCode(entity.categoryCode))
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
    with(entity.toResult(entity.attendees, referenceCodeMap, locationMap)) {
      assertThat(internalLocation).isNull()
      assertThat(inCell).isTrue
    }
  }
}
