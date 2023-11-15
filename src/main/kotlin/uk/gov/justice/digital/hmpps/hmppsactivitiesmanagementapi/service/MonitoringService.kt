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
    // Setting this to true enables configuration via environment variables e.g. SENTRY_ENVIRONMENT
    Sentry.init { options -> options.isEnableExternalConfiguration = true }

    log.info("Monitoring service is ${if (Sentry.isEnabled()) "enabled in environment '${Sentry.getCurrentHub().options.environment}'" else "disabled"}")
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
