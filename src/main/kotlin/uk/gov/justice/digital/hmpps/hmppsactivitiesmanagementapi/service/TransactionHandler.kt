package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * To be used in services or components where a new transaction is required to ensure any DB changes are committed prior
 * to taking any further action e.g. emitting new/update/delete events.
 */
@Component
class TransactionHandler {
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  fun <T> new(block: () -> T): Result<T> = runCatching { block() }
}
