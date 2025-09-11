package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventTierType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.LocationService.LocationDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.toAppointmentLocationSummary
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentAttendanceSummary as AppointmentAttendanceSummaryModel

@Entity
@Immutable
@Table(name = "v_appointment_attendance_summary")
data class AppointmentAttendanceSummary(
  @Id
  val appointmentId: Long,

  val prisonCode: String,

  val categoryCode: String,

  val customName: String?,

  val internalLocationId: Long?,

  val inCell: Boolean,

  val onWing: Boolean,

  val offWing: Boolean,

  val startDate: LocalDate,

  val startTime: LocalTime,

  val endTime: LocalTime?,

  val isCancelled: Boolean,

  val attendeeCount: Long,

  val attendedCount: Long,

  val nonAttendedCount: Long,

  val notRecordedCount: Long,

  val eventTier: String?,

  var dpsLocationId: UUID? = null,
) {
  fun toModel(
    attendees: List<AppointmentAttendeeSearch>,
    appointmentCategories: Map<String, AppointmentCategory>,
    locationMap: Map<Long, LocationDetails>,
  ) = AppointmentAttendanceSummaryModel(
    id = appointmentId,
    prisonCode = prisonCode,
    appointmentName = appointmentCategories[categoryCode].toAppointmentName(categoryCode, customName),
    internalLocation = if (inCell) {
      null
    } else {
      locationMap[internalLocationId].toAppointmentLocationSummary(internalLocationId!!, dpsLocationId, prisonCode)
    },
    startDate = startDate,
    startTime = startTime,
    endTime = endTime,
    isCancelled = isCancelled,
    attendeeCount = attendeeCount,
    attendedCount = attendedCount,
    nonAttendedCount = nonAttendedCount,
    notRecordedCount = notRecordedCount,
    attendees = attendees.toResult(),
    eventTierType = if (eventTier != null) EventTierType.valueOf(eventTier) else null,
    inCell = inCell,
  )
}

fun List<AppointmentAttendanceSummary>.toModel(
  attendeeMap: Map<Long, List<AppointmentAttendeeSearch>>,
  appointmentCategories: Map<String, AppointmentCategory>,
  locationMap: Map<Long, LocationDetails>,
) = map { it.toModel(attendeeMap[it.appointmentId] ?: emptyList(), appointmentCategories, locationMap) }
