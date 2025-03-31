package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.util.retry.Retry
import reactor.util.retry.RetryBackoffSpec
import java.time.Duration

@Service
class RetryApiService(
  @Value("\${hmpps.web-client.max-retries:3}") private val maxRetryAttempts: Long,
  @Value("\${hmpps.web-client.backoff-millis:250}") private val backoffMillis: Long,
) {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getBackoffSpec(maxRetryAttempts: Long? = null, backoffMillis: Long? = null): RetryBackoffSpec = Retry.backoff(
    maxRetryAttempts ?: this.maxRetryAttempts,
    Duration.ofMillis(backoffMillis ?: this.backoffMillis),
  )
    .filter { isRetryable(it) }
    .doBeforeRetry { logRetrySignal(it) }
    .onRetryExhaustedThrow { _, signal ->
      signal.failure()
    }

  private fun isRetryable(t: Throwable): Boolean = t is WebClientRequestException ||
    t.cause is WebClientRequestException ||
    t is WebClientResponseException.BadGateway

  private fun logRetrySignal(retrySignal: Retry.RetrySignal) {
    val exception = retrySignal.failure()?.cause ?: retrySignal.failure()
    val message = exception.message ?: exception.javaClass.canonicalName
    log.debug(
      "Retrying due to {}, total retries: {}, context: {}",
      message,
      retrySignal.totalRetries(),
      retrySignal.retryContextView(),
    )
  }
}
