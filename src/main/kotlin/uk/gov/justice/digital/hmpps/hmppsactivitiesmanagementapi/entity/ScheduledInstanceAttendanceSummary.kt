package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import java.time.LocalDate
import java.time.LocalTime

@Entity
@Immutable
@Table(name = "v_scheduled_instance_attendance_summary")
data class ScheduledInstanceAttendanceSummary(
  @Id
  val scheduledInstanceId: Long,

  val activityId: Long,

  val activityScheduleId: Long,

  val prisonCode: String,

  val summary: String,

  val activityCategoryId: Long,

  val sessionDate: LocalDate,

  val startTime: LocalTime,

  val endTime: LocalTime,

  val inCell: Boolean,

  val onWing: Boolean,

  val offWing: Boolean,

  val internalLocationId: Long? = null,

  val internalLocationCode: String? = null,

  val internalLocationDescription: String? = null,

  val cancelled: Boolean,

  val allocations: Long,

  val attendees: Long? = null,

  val notRecorded: Long? = null,

  val attended: Long? = null,

  val absences: Long? = null,

  val paid: Long? = null,
)
