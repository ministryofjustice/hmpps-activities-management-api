package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

enum class InboundEventType(val eventType: String) {
  OFFENDER_RECEIVED("prison-offender-events.prisoner.received") {
    override fun toInboundEvent(mapper: ObjectMapper, message: String) =
      mapper.readValue<OffenderReceivedEvent>(message)
  },
  OFFENDER_RELEASED("prison-offender-events.prisoner.released") {
    override fun toInboundEvent(mapper: ObjectMapper, message: String) =
      mapper.readValue<OffenderReleasedEvent>(message)
  },
  ;

  abstract fun toInboundEvent(mapper: ObjectMapper, message: String): InboundEvent
}

interface InboundEvent {
  fun prisonCode(): String

  fun prisonerNumber(): String
}

data class OffenderReleasedEvent(val additionalInformation: ReleaseInformation) : InboundEvent {
  override fun prisonCode() = additionalInformation.prisonId

  override fun prisonerNumber() = additionalInformation.nomsNumber

  fun isTemporary() = listOf(
    "TEMPORARY_ABSENCE_RELEASE",
    "RELEASED_TO_HOSPITAL",
    "SENT_TO_COURT",
  ).any { it == additionalInformation.reason }
}

data class ReleaseInformation(val nomsNumber: String, val reason: String, val prisonId: String)

data class OffenderReceivedEvent(val additionalInformation: ReceivedInformation) : InboundEvent {
  override fun prisonCode() = additionalInformation.prisonId

  override fun prisonerNumber() = additionalInformation.nomsNumber
}

data class ReceivedInformation(val nomsNumber: String, val reason: String, val prisonId: String)
