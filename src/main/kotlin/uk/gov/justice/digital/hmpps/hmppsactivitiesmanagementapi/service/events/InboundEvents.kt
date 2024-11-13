package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events

import com.fasterxml.jackson.annotation.JsonGetter
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

enum class InboundEventType(val eventType: String) {
  PRISONER_RECEIVED("prisoner-offender-search.prisoner.received") {
    override fun toInboundEvent(mapper: ObjectMapper, message: String) =
      mapper.readValue<PrisonerReceivedEvent>(message)
  },
  PRISONER_RELEASED("prisoner-offender-search.prisoner.released") {
    override fun toInboundEvent(mapper: ObjectMapper, message: String) =
      mapper.readValue<PrisonerReleasedEvent>(message)
  },
  OFFENDER_MERGED("prison-offender-events.prisoner.merged") {
    override fun toInboundEvent(mapper: ObjectMapper, message: String) =
      mapper.readValue<OffenderMergedEvent>(message)
  },
  INCENTIVES_INSERTED("incentives.iep-review.inserted") {
    override fun toInboundEvent(mapper: ObjectMapper, message: String) =
      mapper.readValue<IncentivesInsertedEvent>(message)
  },
  INCENTIVES_UPDATED("incentives.iep-review.updated") {
    override fun toInboundEvent(mapper: ObjectMapper, message: String) =
      mapper.readValue<IncentivesUpdatedEvent>(message)
  },
  INCENTIVES_DELETED("incentives.iep-review.deleted") {
    override fun toInboundEvent(mapper: ObjectMapper, message: String) =
      mapper.readValue<IncentivesDeletedEvent>(message)
  },
  PRISONER_UPDATED("prisoner-offender-search.prisoner.updated") {
    override fun toInboundEvent(mapper: ObjectMapper, message: String) =
      mapper.readValue<PrisonerUpdatedEvent>(message)
  },
  NON_ASSOCIATIONS("prison-offender-events.prisoner.non-association-detail.changed") {
    override fun toInboundEvent(mapper: ObjectMapper, message: String) =
      mapper.readValue<NonAssociationsChangedEvent>(message)
  },
  ACTIVITIES_CHANGED("prison-offender-events.prisoner.activities-changed") {
    override fun toInboundEvent(mapper: ObjectMapper, message: String) =
      mapper.readValue<ActivitiesChangedEvent>(message)
  },
  APPOINTMENTS_CHANGED("prison-offender-events.prisoner.appointments-changed") {
    override fun toInboundEvent(mapper: ObjectMapper, message: String) =
      mapper.readValue<AppointmentsChangedEvent>(message)
  },
  ALERTS_UPDATED("prisoner-offender-search.prisoner.alerts-updated") {
    override fun toInboundEvent(mapper: ObjectMapper, message: String) =
      mapper.readValue<AlertsUpdatedEvent>(message)
  },
  ;

  abstract fun toInboundEvent(mapper: ObjectMapper, message: String): InboundEvent
}

interface InboundEvent {
  fun prisonerNumber(): String

  @JsonGetter
  fun eventType(): String
  fun eventMessage(): String? = "Unknown event"
  fun isMeaningful() = true
}

interface InboundReleaseEvent : InboundEvent {
  fun prisonCode(): String
}

interface EventOfInterest

// ------------ Prisoner released from prison events ------------------------------------------
data class PrisonerReleasedEvent(val additionalInformation: ReleaseInformation) : InboundReleaseEvent {
  override fun prisonCode() = additionalInformation.prisonId
  override fun prisonerNumber() = additionalInformation.nomsNumber
  override fun eventType() = InboundEventType.PRISONER_RELEASED.eventType
  override fun eventMessage() = "Prisoner released"
  fun isTemporary() = listOf("TEMPORARY_ABSENCE_RELEASE", "SENT_TO_COURT").contains(additionalInformation.reason)
  fun isPermanent() = listOf("RELEASED", "RELEASED_TO_HOSPITAL").contains(additionalInformation.reason)
}

data class ReleaseInformation(val nomsNumber: String, val reason: String, val prisonId: String)

// ------------ Prisoner received into prison events -------------------------------------------
data class PrisonerReceivedEvent(val additionalInformation: ReceivedInformation) : InboundEvent {
  fun prisonCode() = additionalInformation.prisonId
  override fun prisonerNumber() = additionalInformation.nomsNumber
  override fun eventType() = InboundEventType.PRISONER_RECEIVED.eventType
  override fun eventMessage() = "Prisoner received"
}

data class ReceivedInformation(val nomsNumber: String, val reason: String, val prisonId: String)

// ------------ Offender merged event -----------------------------------------------------------

data class OffenderMergedEvent(val additionalInformation: MergeInformation) : InboundEvent, EventOfInterest {
  override fun prisonerNumber() = additionalInformation.nomsNumber
  fun removedPrisonerNumber() = additionalInformation.removedNomsNumber
  override fun eventType() = InboundEventType.OFFENDER_MERGED.eventType
  override fun eventMessage() = "Prisoner merged from '${this.removedPrisonerNumber()}' to '${this.prisonerNumber()}'"
}

data class MergeInformation(val nomsNumber: String, val removedNomsNumber: String)

// ------------ Incentives review events --------------------------------------------------------

data class IncentivesInsertedEvent(val additionalInformation: IncentivesInformation) : InboundEvent, EventOfInterest {
  override fun prisonerNumber() = additionalInformation.nomsNumber
  override fun eventType() = InboundEventType.INCENTIVES_INSERTED.eventType
  override fun eventMessage() = "Incentive review created"
}

data class IncentivesUpdatedEvent(val additionalInformation: IncentivesInformation) : InboundEvent, EventOfInterest {
  override fun prisonerNumber() = additionalInformation.nomsNumber
  override fun eventType() = InboundEventType.INCENTIVES_UPDATED.eventType
  override fun eventMessage() = "Incentive review updated"
}

data class IncentivesDeletedEvent(val additionalInformation: IncentivesInformation) : InboundEvent, EventOfInterest {
  override fun prisonerNumber() = additionalInformation.nomsNumber
  override fun eventType() = InboundEventType.INCENTIVES_DELETED.eventType
  override fun eventMessage() = "Incentive review deleted"
}

data class IncentivesInformation(val nomsNumber: String, val reason: String?, val prisonId: String?)

// ------------ Prisoner updated events ------------------------------------------------------------------

data class PrisonerUpdatedEvent(val additionalInformation: PrisonerUpdatedInformation) : InboundEvent, EventOfInterest {
  override fun prisonerNumber() = additionalInformation.nomsNumber

  override fun eventType() = InboundEventType.PRISONER_UPDATED.eventType

  override fun eventMessage(): String? =
    with(additionalInformation.categoriesChanged) {
      when {
        contains("LOCATION") -> "Cell move"
        else -> null
      }
    }

  override fun isMeaningful() = eventMessage() != null
}

data class PrisonerUpdatedInformation(val nomsNumber: String, val categoriesChanged: List<String>)

// ------------ Non associations changed events ----------------------------------------------------

data class NonAssociationsChangedEvent(val additionalInformation: NonAssociationInformation) : InboundEvent, EventOfInterest {
  override fun prisonerNumber() = additionalInformation.nomsNumber
  override fun eventType() = InboundEventType.NON_ASSOCIATIONS.eventType
  override fun eventMessage() = "Non-associations changed"
  fun bookingId() = additionalInformation.bookingId
}

data class NonAssociationInformation(val nomsNumber: String, val bookingId: Long)

// ------------ Activities changed prison events ------------------------------------------

data class AppointmentsChangedEvent(
  val personReference: PersonReference,
  val additionalInformation: AppointmentsChangedInformation,
) : InboundReleaseEvent {
  override fun prisonerNumber(): String =
    personReference.identifiers.first { it.type == "NOMS" }.value

  override fun eventType() = InboundEventType.APPOINTMENTS_CHANGED.eventType

  override fun eventMessage() = "Appointments changed '${additionalInformation.action}'"

  override fun prisonCode() = additionalInformation.prisonId

  fun cancelAppointments() = additionalInformation.action == "YES"
}

data class ActivitiesChangedEvent(
  val personReference: PersonReference,
  val additionalInformation: ActivitiesChangedInformation,
) : InboundReleaseEvent {
  override fun prisonCode() = additionalInformation.prisonId

  fun action() = Action.entries.firstOrNull { it.name == additionalInformation.action }

  override fun prisonerNumber(): String =
    personReference.identifiers.first { it.type == "NOMS" }.value

  override fun eventType() = InboundEventType.ACTIVITIES_CHANGED.eventType

  override fun eventMessage() = "Activities changed"
}

data class PersonReference(val identifiers: List<Identifier>)

data class Identifier(val type: String, val value: String)

data class ActivitiesChangedInformation(val action: String, val prisonId: String, val user: String)

data class AppointmentsChangedInformation(val action: String, val prisonId: String, val user: String)

enum class Action {
  END,
  SUSPEND,
}

// ------------ New event contents here -------------------------------------------------------------

data class AlertsUpdatedEvent(val additionalInformation: AlertsUpdatedInformation) : InboundEvent, EventOfInterest {
  override fun prisonerNumber(): String = additionalInformation.nomsNumber

  override fun eventType() = InboundEventType.ALERTS_UPDATED.eventType

  override fun eventMessage() = "Alerts updated"
}

data class AlertsUpdatedInformation(
  val nomsNumber: String,
  val bookingId: Long,
  val alertsAdded: Set<String>,
  val alertsRemoved: Set<String>,
)
