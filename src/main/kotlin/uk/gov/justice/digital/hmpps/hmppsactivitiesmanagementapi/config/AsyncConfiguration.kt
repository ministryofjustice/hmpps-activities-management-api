package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
@EnableAsync
class AsyncConfiguration {

  @Bean
  fun asyncExecutor(): Executor? = ThreadPoolTaskExecutor().apply {
    setCorePoolSize(3)
    setMaxPoolSize(3)
    setQueueCapacity(100)
    setThreadNamePrefix("AsyncThread-")
    initialize()
  }
}
