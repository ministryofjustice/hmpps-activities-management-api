package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AppointmentSearchResult
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toAppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toAppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toAppointmentName
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
@Immutable
@Table(name = "v_appointment_search")
data class AppointmentSearch(
  val appointmentSeriesId: Long,

  @Id
  val appointmentId: Long,

  @Enumerated(EnumType.STRING)
  val appointmentType: AppointmentType,

  val prisonCode: String,

  val categoryCode: String,

  val customName: String?,

  var internalLocationId: Long?,

  val customLocation: String?,

  val inCell: Boolean,

  val onWing: Boolean,

  val offWing: Boolean,

  val startDate: LocalDate,

  val startTime: LocalTime,

  val endTime: LocalTime?,

  val isRepeat: Boolean,

  val sequenceNumber: Int,

  val maxSequenceNumber: Int,

  var unlockNotes: String?,

  val extraInformation: String?,

  val createdBy: String,

  val isEdited: Boolean,

  val isCancelled: Boolean,
) {
  @OneToMany(mappedBy = "appointmentSearch", fetch = FetchType.LAZY)
  var attendees: List<AppointmentAttendeeSearch> = listOf()

  fun toResult(
    attendees: List<AppointmentAttendeeSearch>,
    referenceCodeMap: Map<String, ReferenceCode>,
    locationMap: Map<Long, Location>,
  ) = AppointmentSearchResult(
    appointmentSeriesId,
    appointmentId,
    appointmentType,
    prisonCode,
    referenceCodeMap[categoryCode].toAppointmentName(categoryCode, customName),
    attendees = attendees.toResult(),
    referenceCodeMap[categoryCode].toAppointmentCategorySummary(categoryCode),
    customName,
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

  private fun startDateTime(): LocalDateTime = LocalDateTime.of(startDate, startTime)

  private fun isExpired() = startDateTime() < LocalDateTime.now()
}

fun List<AppointmentSearch>.toResults(
  attendeeMap: Map<Long, List<AppointmentAttendeeSearch>>,
  referenceCodeMap: Map<String, ReferenceCode>,
  locationMap: Map<Long, Location>,
) = map { it.toResult(attendeeMap[it.appointmentId] ?: emptyList(), referenceCodeMap, locationMap) }
