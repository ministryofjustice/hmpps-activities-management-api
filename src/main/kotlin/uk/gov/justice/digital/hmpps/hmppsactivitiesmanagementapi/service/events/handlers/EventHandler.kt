package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.InboundEvent

interface EventHandler<T : InboundEvent> {

  /**
   * Returns true if the event was handled successfully and false if it was not handled successfully.
   */
  fun handle(event: T): Outcome
}

class Outcome private constructor(private val success: Boolean, val message: String? = null) {

  companion object {
    fun success() = Outcome(true)
    fun failed(lazyMessage: () -> String? = { null }) = Outcome(false, lazyMessage())
  }

  fun isSuccess() = success

  fun onFailure(block: () -> Unit) {
    if (!success) {
      block()
    }
  }
}
