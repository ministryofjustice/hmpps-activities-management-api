package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.extensions

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.ScheduledEvent

fun ScheduledEvent.internalLocationId() = this.eventLocationId
