package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client

import io.netty.channel.ConnectTimeoutException
import io.netty.handler.timeout.ReadTimeoutException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.function.client.WebClientRequestException
import reactor.core.publisher.Mono
import reactor.util.retry.Retry

abstract class AbstractAPIClient {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  protected fun <T> Mono<T>.withRetryPolicy(maxNumberOfRetriesCondition: Long = 2L): Mono<T> {
    return this
      .retryWhen(
        Retry.max(maxNumberOfRetriesCondition)
          .filter { isTimeoutException(it) }
          .doBeforeRetry { logRetrySignal(it) },
      )
  }

  private fun isTimeoutException(it: Throwable): Boolean {
    // Timeout for NO_RESPONSE is wrapped in a WebClientRequestException
    return it is ReadTimeoutException || it is ConnectTimeoutException ||
      it.cause is ReadTimeoutException || it.cause is ConnectTimeoutException ||
      it is WebClientRequestException || it.cause is WebClientRequestException
  }

  private fun logRetrySignal(retrySignal: Retry.RetrySignal) {
    val exception = retrySignal.failure()?.cause ?: retrySignal.failure()
    val message = exception.message ?: exception.javaClass.canonicalName
    log.debug("Retrying due to {}, totalRetries: {}", message, retrySignal.totalRetries())
  }
}