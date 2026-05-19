package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.health

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.info.BuildProperties
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.IntegrationTestBase

class InfoTest : IntegrationTestBase() {
  @Autowired
  private lateinit var buildProperties: BuildProperties

  @Test
  fun `Info page is accessible`() {
    webTestClient.get()
      .uri("/info")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("build.name").isEqualTo("hmpps-activities-management-api")
  }

  @Test
  fun `Info page reports version`() {
    webTestClient.get().uri("/info")
      .exchange()
      .expectStatus().isOk
      .expectBody().jsonPath("build.version").value<String> {
        assertThat(it).isEqualTo(buildProperties.version)
      }
  }
}
