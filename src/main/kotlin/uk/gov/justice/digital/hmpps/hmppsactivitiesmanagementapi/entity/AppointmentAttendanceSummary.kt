package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toAppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toAppointmentName
import java.time.LocalDate
import java.time.LocalTime
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
) {
  fun toModel(
    referenceCodeMap: Map<String, ReferenceCode>,
    locationMap: Map<Long, Location>,
  ) =
    AppointmentAttendanceSummaryModel(
      id = appointmentId,
      prisonCode,
      referenceCodeMap[categoryCode].toAppointmentName(categoryCode, customName),
      if (inCell) {
        null
      } else {
        locationMap[internalLocationId].toAppointmentLocationSummary(internalLocationId!!, prisonCode)
      },
      startDate,
      startTime,
      endTime,
      isCancelled,
      attendeeCount,
      attendedCount,
      nonAttendedCount,
      notRecordedCount,
    )
}

fun List<AppointmentAttendanceSummary>.toModel(
  referenceCodeMap: Map<String, ReferenceCode>,
  locationMap: Map<Long, Location>,
) = map { it.toModel(referenceCodeMap, locationMap) }
