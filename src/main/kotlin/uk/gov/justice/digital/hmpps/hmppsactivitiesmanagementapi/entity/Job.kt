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

  val startedAt: LocalDateTime,
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

  companion object {
    fun successful(jobType: JobType, start: LocalDateTime) = Job(jobType = jobType, startedAt = start).succeeded()

    fun failed(jobType: JobType, start: LocalDateTime) = Job(jobType = jobType, startedAt = start).failed()
  }
}

enum class JobType {
  ACTIVITIES_METRICS,
  ALLOCATE,
  ATTENDANCE_CREATE,
  ATTENDANCE_EXPIRE,
  DEALLOCATE_ENDING,
  DEALLOCATE_EXPIRING,
  SCHEDULES,
  CREATE_APPOINTMENTS,
  UPDATE_APPOINTMENTS,
  CANCEL_APPOINTMENTS,
  ;
}
