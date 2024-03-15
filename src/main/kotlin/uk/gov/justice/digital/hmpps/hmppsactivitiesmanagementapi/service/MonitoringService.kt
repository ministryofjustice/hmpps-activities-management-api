package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import io.sentry.Sentry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Simple service for capturing free text messages in the underlying monitoring tool.
 *
 * Extra care must be taken when using this service not to include any PII data in any of the calls.
 */
@Service
class MonitoringService {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  init {
    log.info("Monitoring service is ${if (Sentry.isEnabled()) "enabled" else "disabled"}")
  }

  fun capture(message: String, error: Throwable? = null) {
    // This checks for the presence of the Sentry environment variable SENTRY_DSN, it is disabled if not found.
    if (Sentry.isEnabled()) {
      if (error == null) {
        Sentry.captureMessage(message)
      } else {
        Sentry.captureMessage(message.plus(" - ${error.message}"))
      }
      log.info("Monitoring service is enabled, message: '$message' sent")
    } else {
      log.info("Monitoring service is disabled, ignoring message: '$message'")
    }
  }
}
