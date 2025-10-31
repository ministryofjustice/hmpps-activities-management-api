package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.JobRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.LocalStackTestHelper.Companion.clearQueues
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue

@Component
class JobIntegrationTestHelper {
  @Autowired
  lateinit var hmppsQueueService: HmppsQueueService

  @Autowired
  private lateinit var jobRepository: JobRepository

  @Autowired
  private lateinit var rolloutPrisonService: RolloutPrisonService

  private val jobsQueue by lazy { hmppsQueueService.findByQueueId("activitiesmanagementjobs") as HmppsQueue }
  private val jobsClient by lazy { jobsQueue.sqsClient }

  @BeforeEach
  fun `clear job queues`() {
    clearQueues(jobsClient, jobsQueue)
  }

  fun countMessagesInDlq(): Int = jobsClient.countAllMessagesOnQueue(jobsQueue.dlqUrl!!).get()

  fun verifyJobComplete(jobType: JobType) {
    jobRepository.findAll().first().let {
      Assertions.assertThat(it.jobType).isEqualTo(jobType)
      Assertions.assertThat(it.successful).isTrue()
      val numPrisons = rolloutPrisonService.getRolloutPrisons().count()
      Assertions.assertThat(it.totalSubTasks).isEqualTo(numPrisons)
      Assertions.assertThat(it.completedSubTasks).isEqualTo(numPrisons)
    }
  }

  fun verifyJobIncomplete(jobType: JobType, totalSubTasks: Int, completedSubTasks: Int) {
    jobRepository.findAll().first().let {
      Assertions.assertThat(it.jobType).isEqualTo(jobType)
      Assertions.assertThat(it.successful).isFalse()
      Assertions.assertThat(it.totalSubTasks).isEqualTo(totalSubTasks)
      Assertions.assertThat(it.completedSubTasks).isEqualTo(completedSubTasks)
    }
  }
}
