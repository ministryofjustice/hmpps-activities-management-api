package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.toResults
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.PrisonRegime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocationDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSearchEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSearchResultModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class AppointmentSearchTest {
  val now: LocalDateTime = LocalDate.now().atStartOfDay().plusHours(4)

  private val prisonRegime = PrisonRegime(
    prisonCode = "",
    amStart = now.toLocalTime(),
    amFinish = now.toLocalTime(),
    pmStart = now.plusHours(4).toLocalTime(),
    pmFinish = now.toLocalTime().plusHours(5),
    edStart = now.plusHours(8).toLocalTime(),
    edFinish = now.plusHours(9).toLocalTime(),
    prisonRegimeDaysOfWeek =
    emptyList(),
  )

  @Test
  fun `entity to result mapping`() {
    val entity = appointmentSearchEntity()
    val expectedModel = appointmentSearchResultModel(timeSlot = TimeSlot.PM)
    val referenceCodeMap = mapOf(entity.categoryCode to appointmentCategoryReferenceCode(entity.categoryCode))
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocationDetails(entity.internalLocationId!!, entity.dpsLocationId!!, "TPR"))
    assertThat(
      entity.toResult(
        entity.attendees,
        referenceCodeMap,
        locationMap,
        mapOf(
          DayOfWeek.entries.toSet() to prisonRegime,
        ),
      ),
    ).isEqualTo(expectedModel)
  }

  @Test
  fun `entity list to results list mapping`() {
    val entityList = listOf(appointmentSearchEntity())
    val expectedModel = listOf(appointmentSearchResultModel(timeSlot = TimeSlot.PM))
    with(entityList.first()) {
      val referenceCodeMap = mapOf(entityList.first().categoryCode to appointmentCategoryReferenceCode(categoryCode))

      val locationMap = mapOf(
        internalLocationId!! to appointmentLocationDetails(
          internalLocationId!!,
          dpsLocationId!!,
          "TPR",
        ),
      )

      assertThat(
        entityList.toResults(
          mapOf(entityList.first().appointmentId to entityList.first().attendees),
          referenceCodeMap,
          locationMap,
          mapOf(
            DayOfWeek.entries.toSet() to prisonRegime,
          ),
        ),
      ).isEqualTo(expectedModel)
    }
  }

  @Test
  fun `entity to result mapping in cell nullifies internal location`() {
    val entity = appointmentSearchEntity(inCell = true)
    entity.internalLocationId = 123
    entity.dpsLocationId = UUID.fromString("44444444-1111-2222-3333-444444444444")
    val referenceCodeMap = mapOf(entity.categoryCode to appointmentCategoryReferenceCode(entity.categoryCode))
    val locationMap = mapOf(entity.internalLocationId!! to appointmentLocationDetails(entity.internalLocationId!!, entity.dpsLocationId!!, "TPR"))
    with(
      entity.toResult(
        entity.attendees,
        referenceCodeMap,
        locationMap,
        mapOf(
          DayOfWeek.entries.toSet() to prisonRegime,
        ),
      ),
    ) {
      assertThat(internalLocation).isNull()
      assertThat(inCell).isTrue
    }
  }
}
