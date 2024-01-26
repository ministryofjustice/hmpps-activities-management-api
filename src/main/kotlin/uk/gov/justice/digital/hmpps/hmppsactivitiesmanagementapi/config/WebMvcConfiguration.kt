package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.format.FormatterRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.converter.StringToTimeSlotConverter
import java.time.Clock
import java.time.LocalDateTime

@Configuration
class WebMvcConfiguration : WebMvcConfigurer {
  override fun addFormatters(registry: FormatterRegistry) {
    registry.addConverter(StringToTimeSlotConverter())
  }

  @Bean
  fun systemTimeSource() = SystemTimeSource { LocalDateTime.now(Clock.systemDefaultZone()) }
}

fun interface SystemTimeSource {
  fun now(): LocalDateTime
}
