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
  INCENTIVES_INSERTED("incentives.iep-review.inserted") {
    override fun toInboundEvent(mapper: ObjectMapper, message: String) =
      mapper.readValue<IncentivesEvent>(message)
  },
  INCENTIVES_UPDATED("incentives.iep-review.updated") {
    override fun toInboundEvent(mapper: ObjectMapper, message: String) =
      mapper.readValue<IncentivesEvent>(message)
  },
  INCENTIVES_DELETED("incentives.iep-review.deleted") {
    override fun toInboundEvent(mapper: ObjectMapper, message: String) =
      mapper.readValue<IncentivesEvent>(message)
  },
  CELL_MOVE("prison-offender-events.prisoner.cell.move") {
    override fun toInboundEvent(mapper: ObjectMapper, message: String) =
      mapper.readValue<CellMoveEvent>(message)
  },
  NON_ASSOCIATIONS("prison-offender-events.prisoner.non-association-detail.changed") {
    override fun toInboundEvent(mapper: ObjectMapper, message: String) =
      mapper.readValue<NonAssociationsChangedEvent>(message)
  },
  ;

  abstract fun toInboundEvent(mapper: ObjectMapper, message: String): InboundEvent
}

interface InboundEvent {
  fun prisonCode(): String
  fun prisonerNumber(): String
}

// ------------ Offender released from prison events ------------------------------------------

data class OffenderReleasedEvent(val additionalInformation: ReleaseInformation) : InboundEvent {
  override fun prisonCode() = additionalInformation.prisonId
  override fun prisonerNumber() = additionalInformation.nomsNumber
  fun isTemporary() = listOf("TEMPORARY_ABSENCE_RELEASE", "RELEASED_TO_HOSPITAL", "SENT_TO_COURT")
    .any { it == additionalInformation.reason }
  fun isPermanent() = listOf("RELEASED", "TRANSFERRED")
    .any { it == additionalInformation.reason }
}

data class ReleaseInformation(val nomsNumber: String, val reason: String, val prisonId: String)

// ------------ Offender received into prison events -------------------------------------------

data class OffenderReceivedEvent(val additionalInformation: ReceivedInformation) : InboundEvent {
  override fun prisonCode() = additionalInformation.prisonId
  override fun prisonerNumber() = additionalInformation.nomsNumber
}

data class ReceivedInformation(val nomsNumber: String, val reason: String, val prisonId: String)

// ------------ Incentives review events --------------------------------------------------------

data class IncentivesEvent(val additionalInformation: IncentivesInformation) : InboundEvent {
  override fun prisonCode() = "" // Not populated in additional information
  override fun prisonerNumber() = additionalInformation.nomsNumber
}

data class IncentivesInformation(val nomsNumber: String, val reason: String, val prisonId: String)

// ------------ Cell move events ------------------------------------------------------------------

data class CellMoveEvent(val additionalInformation: CellMoveInformation) : InboundEvent {
  override fun prisonCode() = "" // Not populated in additional information
  override fun prisonerNumber() = additionalInformation.nomsNumber
  fun bookingId() = additionalInformation.bookingId
}

data class CellMoveInformation(val nomsNumber: String, val livingUnitId: Long, val bookingId: Long)

// ------------ Non associations changed events ----------------------------------------------------

data class NonAssociationsChangedEvent(val additionalInformation: NonAssociationInformation) : InboundEvent {
  override fun prisonCode() = "" // Not populated in additional information
  override fun prisonerNumber() = additionalInformation.nomsNumber
  fun bookingId() = additionalInformation.bookingId
}

data class NonAssociationInformation(val nomsNumber: String, val bookingId: Long)

// ------------ New event contents here -------------------------------------------------------------