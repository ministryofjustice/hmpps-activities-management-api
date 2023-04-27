package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

interface EventHandler<T> {

  /**
   * Returns true if the event was handled successfully and false if it was not handled successfully.
   */
  fun handle(event: T): Boolean
}
