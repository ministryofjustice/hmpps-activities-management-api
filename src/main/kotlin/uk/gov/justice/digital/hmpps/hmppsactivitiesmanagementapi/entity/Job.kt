package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "job")
data class Job(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val jobId: Long = 0,

  @Enumerated(EnumType.STRING)
  val jobType: JobType,

  val startedAt: LocalDateTime = LocalDateTime.now(),
) {

  var endedAt: LocalDateTime? = null
    private set

  var successful: Boolean = false
    private set

  fun succeeded() = this.apply {
    if (endedAt != null) {
      throw IllegalStateException("Job is already ended.")
    }

    endedAt = LocalDateTime.now()
    successful = true
  }

  fun failed() = this.apply {
    if (endedAt != null) {
      throw IllegalStateException("Job is already ended.")
    }

    endedAt = LocalDateTime.now()
    successful = false
  }
}

enum class JobType {
  ACTIVITIES_METRICS,
  ALLOCATE,
  APPOINTMENTS_METRICS,
  ATTENDANCE_CREATE,
  ATTENDANCE_CREATE_EXPERIMENTAL,
  ATTENDANCE_EXPIRE,
  DEALLOCATE_ENDING,
  DEALLOCATE_EXPIRING,
  SCHEDULES,
  CREATE_APPOINTMENTS,
  UPDATE_APPOINTMENTS,
  CANCEL_APPOINTMENTS,
  UNCANCEL_APPOINTMENTS,
  DELETE_MIGRATED_APPOINTMENTS,
  MANAGE_APPOINTMENT_ATTENDEES,
  START_SUSPENSIONS,
  END_SUSPENSIONS,
  FIX_ZERO_PAY_DEALLOCATE,
  FIX_ZERO_PAY_MAKE_UNPAID,
  FIX_ZERO_PAY_REALLOCATE,
}
