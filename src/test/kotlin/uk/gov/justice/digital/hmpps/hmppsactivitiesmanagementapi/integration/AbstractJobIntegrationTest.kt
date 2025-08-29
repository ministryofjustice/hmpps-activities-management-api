package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.JobRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue

abstract class AbstractJobIntegrationTest : LocalStackTestBase() {

  @Autowired
  private lateinit var jobRepository: JobRepository

  @Autowired
  private lateinit var rolloutPrisonService: RolloutPrisonService

  protected val jobsQueue by lazy { hmppsQueueService.findByQueueId("activitiesmanagementjobs") as HmppsQueue }
  protected val jobsClient by lazy { jobsQueue.sqsClient }

  @BeforeEach
  fun `clear job queues`() {
    clearQueues(jobsClient, jobsQueue)
  }

  protected fun countMessagesInDlq(): Int = jobsClient.countAllMessagesOnQueue(jobsQueue.dlqUrl!!).get()

  protected fun getNumPrisons() = rolloutPrisonService.getRolloutPrisons().count()

  protected fun verifyJobComplete(jobType: JobType) {
    jobRepository.findAll().first().let {
      assertThat(it.jobType).isEqualTo(jobType)
      assertThat(it.successful).isTrue()
      val numPrisons = getNumPrisons()
      assertThat(it.totalSubTasks).isEqualTo(numPrisons)
      assertThat(it.completedSubTasks).isEqualTo(numPrisons)
    }
  }

  protected fun verifyJobIncomplete(jobType: JobType, totalSubTasks: Int, completedSubTasks: Int) {
    jobRepository.findAll().first().let {
      assertThat(it.jobType).isEqualTo(jobType)
      assertThat(it.successful).isFalse()
      assertThat(it.totalSubTasks).isEqualTo(totalSubTasks)
      assertThat(it.completedSubTasks).isEqualTo(completedSubTasks)
    }
  }
}
