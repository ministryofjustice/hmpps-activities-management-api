package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import java.time.LocalDate
import java.util.UUID

@Entity
@Immutable
@Table(name = "v_attendance_creation_data")
data class AttendanceCreationData(
  @Id
  val id: UUID,

  val scheduledInstanceId: Long,

  val sessionDate: LocalDate,

  @Enumerated(EnumType.STRING)
  val timeSlot: TimeSlot,

  val prisonerNumber: String,

  val paid: Boolean?,

  val prisonPayBandId: Long?,

  val prisonCode: String,

  val activityId: Long,

  @Enumerated(EnumType.STRING)
  val prisonerStatus: PrisonerStatus,

  val allocationId: Long,

  val allocStart: LocalDate,

  val allocEnd: LocalDate?,

  val scheduleStart: LocalDate,

  val scheduleEnd: LocalDate?,

  var scheduleWeeks: Int,

  var cancelledBy: String? = null,

  var cancelledReason: String? = null,

  val possibleExclusion: Boolean,

  val plannedDeallocationDate: LocalDate?,
)
