package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.PrisonRegime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AppointmentSearchResult
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.LocationService.LocationDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.PrisonRegimeService.Companion.getSlotForDayAndTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.toAppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toAppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toAppointmentName
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

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

  val createdTime: LocalDateTime,

  val updatedTime: LocalDateTime?,

  val cancelledTime: LocalDateTime?,

  val cancelledBy: String?,

  var dpsLocationId: UUID? = null,
) {
  @OneToMany(mappedBy = "appointmentSearch", fetch = FetchType.LAZY)
  var attendees: List<AppointmentAttendeeSearch> = listOf()

  fun toResult(
    attendees: List<AppointmentAttendeeSearch>,
    referenceCodeMap: Map<String, ReferenceCode>,
    locationMap: Map<Long, LocationDetails>,
    prisonRegime: Map<Set<DayOfWeek>, PrisonRegime>,
  ) = AppointmentSearchResult(
    appointmentSeriesId = appointmentSeriesId,
    appointmentId = appointmentId,
    appointmentType = appointmentType,
    prisonCode = prisonCode,
    appointmentName = referenceCodeMap[categoryCode].toAppointmentName(categoryCode, customName),
    attendees = attendees.toResult(),
    category = referenceCodeMap[categoryCode].toAppointmentCategorySummary(categoryCode),
    customName = customName,
    internalLocation = if (inCell) {
      null
    } else {
      locationMap[internalLocationId].toAppointmentLocationSummary(internalLocationId!!, dpsLocationId, prisonCode)
    },
    inCell = inCell,
    startDate = startDate,
    startTime = startTime,
    endTime = endTime,
    isRepeat = isRepeat,
    sequenceNumber = sequenceNumber,
    maxSequenceNumber = maxSequenceNumber,
    isEdited = isEdited,
    isCancelled = isCancelled,
    isExpired = isExpired(),
    timeSlot = prisonRegime.getSlotForDayAndTime(day = startDate.dayOfWeek, time = startTime),
    createdTime = createdTime,
    updatedTime = updatedTime,
    cancelledTime = cancelledTime,
    cancelledBy = cancelledBy,
  )

  private fun startDateTime(): LocalDateTime = LocalDateTime.of(startDate, startTime)

  private fun isExpired() = startDateTime() < LocalDateTime.now()
}

fun List<AppointmentSearch>.toResults(
  attendeeMap: Map<Long, List<AppointmentAttendeeSearch>>,
  referenceCodeMap: Map<String, ReferenceCode>,
  locationMap: Map<Long, LocationDetails>,
  prisonRegime: Map<Set<DayOfWeek>, PrisonRegime>,
) = map {
  it.toResult(
    attendees = attendeeMap[it.appointmentId] ?: emptyList(),
    referenceCodeMap = referenceCodeMap,
    locationMap = locationMap,
    prisonRegime = prisonRegime,
  )
}
