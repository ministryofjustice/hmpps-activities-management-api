package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.extensions

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Movement
import java.time.LocalDateTime
import java.time.LocalTime

fun Movement.movementDateTime(): LocalDateTime = LocalDateTime.of(movementDate, LocalTime.parse(movementTime))
