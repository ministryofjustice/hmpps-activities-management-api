package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Job

@Repository
interface JobRepository : JpaRepository<Job, Long> {
  @Modifying
  @Query(
    """
        update Job j 
        set j.totalSubTasks = :totalSubTasks, j.completedSubTasks = 0
        where j.jobId = :jobId
    """,
  )
  fun initialiseCounts(jobId: Long, totalSubTasks: Int)

  @Modifying
  @Query(
    """
        update Job j 
        set j.completedSubTasks = j.completedSubTasks + 1
        where j.jobId = :jobId
    """,
  )
  fun incrementCount(jobId: Long)
}
