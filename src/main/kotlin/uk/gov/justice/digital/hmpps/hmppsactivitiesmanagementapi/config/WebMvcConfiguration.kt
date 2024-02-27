package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.format.FormatterRegistry
import org.springframework.retry.annotation.EnableRetry
import org.springframework.retry.support.RetryTemplate
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.converter.StringToTimeSlotConverter
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime

@Configuration
@EnableRetry
class WebMvcConfiguration : WebMvcConfigurer {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override fun addFormatters(registry: FormatterRegistry) {
    registry.addConverter(StringToTimeSlotConverter())
  }

  @Bean
  fun systemTimeSource() = SystemTimeSource { LocalDateTime.now(Clock.systemDefaultZone()) }

  @Bean
  fun retryable(): Retryable {
    // Attempt 1 - 500ms
    // Attempt 2 - 1000ms
    // Attempt 3 - 2000ms
    // Attempt 4 - 4000ms

    val template = RetryTemplate.builder()
      .maxAttempts(4)
      .exponentialBackoff(Duration.ofMillis(500), 2.0, Duration.ofMillis(4000))
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

fun interface SystemTimeSource {
  fun now(): LocalDateTime
}
