package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

interface EventHandler<T> {
  fun handle(event: T)
}
