package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.retry.annotation.EnableRetry
import org.springframework.retry.support.RetryTemplate
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.time.Duration

@Configuration
@EnableRetry
class WebMvcConfiguration(
  @Value("\${retry.max-attempts:5}") private val retryMaxAttempts: Int,
  @Value("\${retry.initial-interval:1s}") private val retryInitialInterval: Duration,
  @Value("\${retry.multiplier:2.0}") private val retryMultiplier: Double,
  @Value("\${retry.max-interval:16s}") private val retryMaxInterval: Duration,
) : WebMvcConfigurer {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Bean
  fun retryable(): Retryable {
    log.info("RETRY: max-attempts=$retryMaxAttempts, initial-interval=$retryInitialInterval, multiplier=$retryMultiplier, max-interval=$retryMaxInterval")

    val template = RetryTemplate.builder()
      .maxAttempts(retryMaxAttempts)
      .exponentialBackoff(retryInitialInterval, retryMultiplier, retryMaxInterval)
      .build()

    return Retryable { block ->
      template.execute<Unit, RuntimeException> {
        runCatching { block() }.onFailure { error -> log.error("RETRYABLE: Failure in retry block.", error) }.getOrThrow()
      }
    }
  }
}

fun interface Retryable {
  fun retry(block: () -> Unit)
}
