package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import io.sentry.Sentry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * Simple service for capturing free text messages in the underlying monitoring tool.
 *
 * Extra care must be taken when using this service not to include any PII data in any of the calls.
 */
@Service
class MonitoringService(
  @Value("\${SENTRY_SDSN:#{null}}") dsn: String?,
  @Value("\${SENTRY_ENVIRONMENT:#{null}}") env: String?,
) {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  init {
    dsn?.let {
      Sentry.init { options ->
        options.dsn = dsn
        env?.let { options.environment = it }
      }
    }

    log.info("Monitoring service is ${if (Sentry.isEnabled()) "enabled in environment $env" else "disabled"}")
  }

  fun capture(message: String) {
    // This checks for the presence of the Sentry environment variable SENTRY_DSN, it is disabled if not found.
    if (Sentry.isEnabled()) {
      Sentry.captureMessage(message)
      log.info("Monitoring service is enabled, message: '$message' sent")
    } else {
      log.info("Monitoring service is disabled, ignoring message: '$message'")
    }
  }
}
