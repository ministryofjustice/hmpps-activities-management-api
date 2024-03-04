package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.Retryable
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.WebMvcConfiguration
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [ WebMvcConfiguration::class ])
@TestPropertySource(
  properties = [
    "retry.max-attempts=4",
    "retry.initial-interval=100ms",
    "retry.multiplier=2.0",
    "retry.max-interval=600ms",
  ],
)
@SpringBootTest
class RetryConfigurationIntegrationTest {

  @Autowired
  private lateinit var retryable: Retryable

  @Test
  fun `retries a maximum of 4 times`() {
    var retryCounter = 0

    runCatching {
      retryable.retry {
        retryCounter++

        throw RuntimeException("Something went wrong but don't worry we retried it!")
      }
    }

    retryCounter isEqualTo 4
  }
}
