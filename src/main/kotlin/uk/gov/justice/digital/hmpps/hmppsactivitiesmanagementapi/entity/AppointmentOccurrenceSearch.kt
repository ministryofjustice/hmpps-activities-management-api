package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AppointmentOccurrenceSearchResult
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toAppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toAppointmentLocationSummary
import java.time.LocalDate
import java.time.LocalTime

@Entity
@Table(name = "v_appointment_occurrence_search")
data class AppointmentOccurrenceSearch(
  val appointmentId: Long,

  @Id
  val appointmentOccurrenceId: Long,

  val prisonCode: String,

  @Enumerated(EnumType.STRING)
  val appointmentType: AppointmentType,

  val categoryCode: String,

  val internalLocationId: Long?,

  val inCell: Boolean,

  val startDate: LocalDate,

  val startTime: LocalTime,

  val endTime: LocalTime?,

  val isRepeat: Boolean,

  val sequenceNumber: Int,

  val maxSequenceNumber: Int,

  val comment: String?,

  val isEdited: Boolean,
) {
  fun toResult(referenceCodeMap: Map<String, ReferenceCode>, locationMap: Map<Long, Location>) = AppointmentOccurrenceSearchResult(
    appointmentId,
    appointmentOccurrenceId,
    prisonCode,
    appointmentType,
    referenceCodeMap[categoryCode].toAppointmentCategorySummary(categoryCode),
    if (inCell) {
      null
    } else {
      locationMap[internalLocationId].toAppointmentLocationSummary(internalLocationId!!, prisonCode)
    },
    inCell,
    startDate,
    startTime,
    endTime,
    isRepeat,
    sequenceNumber,
    maxSequenceNumber,
    isEdited,
  )
}
