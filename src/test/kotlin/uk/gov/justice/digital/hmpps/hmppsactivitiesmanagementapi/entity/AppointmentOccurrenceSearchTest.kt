package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentOccurrenceSearchEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentOccurrenceSearchResultModel

class AppointmentOccurrenceSearchTest {
  @Test
  fun `entity to result mapping`() {
    val entity = appointmentOccurrenceSearchEntity()
    val expectedModel = appointmentOccurrenceSearchResultModel()
    val referenceCodeMap = mapOf(entity.categoryCode to appointmentCategoryReferenceCode(entity.categoryCode))
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
    assertThat(entity.toResult(referenceCodeMap, locationMap)).isEqualTo(expectedModel)
  }

  @Test
  fun `entity list to results list mapping`() {
    val entityList = listOf(appointmentOccurrenceSearchEntity())
    val expectedModel = listOf(appointmentOccurrenceSearchResultModel())
    val referenceCodeMap = mapOf(entityList.first().categoryCode to appointmentCategoryReferenceCode(entityList.first().categoryCode))
    val locationMap = mapOf(entityList.first().internalLocationId!! to appointmentLocation(entityList.first().internalLocationId!!, "TPR"))
    assertThat(entityList.toResults(referenceCodeMap, locationMap)).isEqualTo(expectedModel)
  }

  @Test
  fun `entity to result mapping in cell nullifies internal location`() {
    val entity = appointmentOccurrenceSearchEntity(inCell = true)
    entity.internalLocationId = 123
    val referenceCodeMap = mapOf(entity.categoryCode to appointmentCategoryReferenceCode(entity.categoryCode))
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocation(entity.internalLocationId!!, "TPR"))
    with(entity.toResult(referenceCodeMap, locationMap)) {
      assertThat(internalLocation).isNull()
      assertThat(inCell).isTrue
    }
  }
}
