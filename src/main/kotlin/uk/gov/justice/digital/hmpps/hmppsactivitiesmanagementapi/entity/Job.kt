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
) {
  var startedAt: LocalDateTime = LocalDateTime.now()
    private set

  var endedAt: LocalDateTime? = null

  var successful: Boolean = false
    private set

  fun succeeded() = this.apply {
    if (endedAt != null) {
      throw IllegalStateException("Job is already ended.")
    }

    endedAt = LocalDateTime.now()
    successful = true
  }

  companion object {
    fun start(jobType: JobType) = Job(jobType = jobType)
  }
}

enum class JobType {
  ALLOCATION,
  ATTENDANCE,
  DEALLOCATION,
  ;
}
