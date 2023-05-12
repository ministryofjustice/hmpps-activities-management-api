package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.InboundEvent

interface EventHandler<T : InboundEvent> {

  /**
   * Returns true if the event was handled successfully and false if it was not handled successfully.
   */
  fun handle(event: T): Outcome
}

class Outcome(private val success: Boolean) {

  companion object {
    fun success() = Outcome(true)
    fun failed() = Outcome(false)
  }

  fun isSuccess() = success

  fun onSuccess(block: () -> Unit) {
    if (success) {
      block()
    }
  }

  fun onFailure(block: () -> Unit) {
    if (!success) {
      block()
    }
  }
}
