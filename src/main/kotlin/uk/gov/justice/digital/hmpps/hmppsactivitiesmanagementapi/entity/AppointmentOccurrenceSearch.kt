package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AppointmentOccurrenceSearchResult
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toAppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toAppointmentLocationSummary
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
@Table(name = "v_appointment_occurrence_search")
data class AppointmentOccurrenceSearch(
  val appointmentId: Long,

  @Id
  val appointmentOccurrenceId: Long,

  @Enumerated(EnumType.STRING)
  val appointmentType: AppointmentType,

  val prisonCode: String,

  val categoryCode: String,

  val appointmentDescription: String?,

  var internalLocationId: Long?,

  val inCell: Boolean,

  val startDate: LocalDate,

  val startTime: LocalTime,

  val endTime: LocalTime?,

  val isRepeat: Boolean,

  val sequenceNumber: Int,

  val maxSequenceNumber: Int,

  val comment: String?,

  val createdBy: String,

  val isEdited: Boolean,

  val isCancelled: Boolean,
) {
  @OneToMany(mappedBy = "appointmentOccurrenceSearch", fetch = FetchType.LAZY)
  var allocations: List<AppointmentOccurrenceAllocationSearch> = listOf()

  fun toResult(
    allocations: List<AppointmentOccurrenceAllocationSearch>,
    referenceCodeMap: Map<String, ReferenceCode>,
    locationMap: Map<Long, Location>,
  ) = AppointmentOccurrenceSearchResult(
    appointmentId,
    appointmentOccurrenceId,
    appointmentType,
    prisonCode,
    allocations = allocations.toModel(),
    referenceCodeMap[categoryCode].toAppointmentCategorySummary(categoryCode),
    appointmentDescription,
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
    isCancelled,
    isExpired(),
  )

  fun isExpired() = LocalDateTime.of(startDate, startTime) < LocalDateTime.now()
}

fun List<AppointmentOccurrenceSearch>.toResults(
  allocationsMap: Map<Long, List<AppointmentOccurrenceAllocationSearch>>,
  referenceCodeMap: Map<String, ReferenceCode>,
  locationMap: Map<Long, Location>,
) = map { it.toResult(allocationsMap[it.appointmentOccurrenceId] ?: emptyList(), referenceCodeMap, locationMap) }
