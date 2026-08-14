package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.ACTIVITY_SCHEDULE_UPDATED

class ActivitiesFixRotlCategoryJobIntegrationTest : LocalStackTestBase() {

  @Autowired
  private lateinit var activityRepository: ActivityRepository

  @Sql("classpath:test_data/seed-activities-fix-rotl-category-job.sql")
  @Test
  fun `after running the job outside work activities have their category and location flags fixed`() {
    webTestClient.fixRotlCategory()

    await untilAsserted {
      with(activityRepository.findById(401).orElseThrow()) {
        assertThat(activityCategory.code).isEqualTo("SAA_ROTL")
        assertThat(onWing).isFalse
        assertThat(offWing).isFalse
        assertThat(inCell).isFalse
        assertThat(updatedBy).isEqualTo("activities-management-admin-1")
        assertThat(updatedTime).isNotNull
      }

      with(activityRepository.findById(402).orElseThrow()) {
        assertThat(activityCategory.code).isEqualTo("SAA_ROTL")
        assertThat(onWing).isFalse
        assertThat(offWing).isFalse
        assertThat(inCell).isFalse
        assertThat(updatedBy).isEqualTo("activities-management-admin-1")
        assertThat(updatedTime).isNotNull
      }
    }

    // Confirm each activity's change was audited via Envers
    assertThat(activityRepository.findRevisions(401).toList()).isNotEmpty
    assertThat(activityRepository.findRevisions(402).toList()).isNotEmpty

    // Confirm the SQS sync events were triggered for both activities
    validateOutboundEvents(
      ExpectedOutboundEvent(ACTIVITY_SCHEDULE_UPDATED, 401),
      ExpectedOutboundEvent(ACTIVITY_SCHEDULE_UPDATED, 402),
    )
  }

  private fun WebTestClient.fixRotlCategory() {
    post()
      .uri("/job/activities-fix-rotl-category")
      .accept(MediaType.TEXT_PLAIN)
      .exchange()
      .expectStatus().isAccepted
  }
}
