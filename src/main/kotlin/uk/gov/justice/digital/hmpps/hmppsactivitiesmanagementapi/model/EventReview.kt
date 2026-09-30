package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Describes one event that has occurred to indicate a change of circumstance")
data class EventReview(
  @Schema(description = "The internally-generated ID for this event", example = "123456")
  val eventReviewId: Long = 0,

  @Schema(description = "Describes the service which generated this event", example = "prisoner-offender-search")
  val serviceIdentifier: String? = null,

  @Schema(description = "The internal name for the event", example = "prisoner-offender-events.prisoner.cell-move")
  val eventType: String? = null,

  @Schema(description = "The date and time that this event occurred", example = "2022-10-01T23:11:01")
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val eventTime: LocalDateTime? = null,

  @Schema(description = "The prison code where this event took place", example = "MDI")
  val prisonCode: String? = null,

  @Schema(description = "The prisoner number which this event relates to", example = "G1234FF")
  val prisonerNumber: String? = null,

  @Schema(description = "The booking ID related to this prisoner", example = "123456")
  val bookingId: Int? = null,

  @Schema(
    description = "The description of the event that occurred",
    example = "The prisoner was moved to a different cell.",
  )
  val eventData: String? = null,

  @Schema(description = "The date and time that this event was acknowledged.", example = "2022-10-01T23:11:01")
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val acknowledgedTime: LocalDateTime? = null,

  @Schema(description = "The username of the person who acknowledged the event.", example = "U4588F")
  val acknowledgedBy: String? = null,

  @Schema(
    description = "A simple description of the event acton",
    example = "ACTIVITY_SUSPENDED",
  )
  val eventDescription: EventDescription? = null,

  @Schema(
    description = "The current allocations for the prisoner",
    example = "[\"KITCHEN AM\", \"GYM PM\"]",
  )
  val activeAllocations: List<String> = emptyList(),

  @Schema(
    description = "The alert codes added and/or removed, for alerts-updated events. Null for all other events.",
    nullable = true,
  )
  val alertDetails: AlertsUpdatedDetails? = null,

  @Schema(
    description = "The new and previous incentive levels, for incentives-changed events. Null for all other events.",
    nullable = true,
  )
  val incentiveDetails: IncentiveLevelsChangedDetails? = null,

  @Schema(
    description = "The previous and new cell locations, for prisoner-updated events. Null for all other events. " +
      "The cell values are populated by the orchestrator.",
    nullable = true,
  )
  val prisonerUpdatedDetails: PrisonerUpdatedDetails? = null,

  @Schema(
    description = "The removed and retained prisoner numbers, for prisoner-merged events. Null for all other events.",
    nullable = true,
  )
  val mergeDetails: OffenderMergedDetails? = null,
)

enum class EventDescription {
  @Schema(
    description = "A suspended activity",
  )
  ACTIVITY_SUSPENDED,

  @Schema(
    description = "An ended activity",
  )
  ACTIVITY_ENDED,

  @Schema(
    description = "A released prisoner",
  )
  RELEASED,

  @Schema(
    description = "A permanently released prisoner",
  )
  PERMANENT_RELEASE,

  @Schema(
    description = "A temporarily released prisoner",
  )
  TEMPORARY_RELEASE,

  @Schema(
    description = "An alert has been added",
  )
  ALERT_ADDED,

  @Schema(
    description = "An alert has been removed",
  )
  ALERT_CLOSED,

  @Schema(
    description = "Alerts have been added and removed",
  )
  ALERTS_ADDED_AND_CLOSED,

  @Schema(
    description = "A prisoner transferred out to another prison",
  )
  TRANSFER_OUT,

  @Schema(
    description = "A prisoner arrived at or returned to a prison",
  )
  ARRIVAL_OR_RETURN,

  @Schema(
    description = "A new non-association for a prisoner",
  )
  NON_ASSOCIATION,

  @Schema(
    description = "A prisoner has moved location",
  )
  CELL_MOVE,

  @Schema(
    description = "A prisoner's incentive level has changed",
  )
  INCENTIVE_LEVEL_CHANGED,

  @Schema(
    description = "A prisoner has been merged into an existing prisoner record",
  )
  PRISONER_MERGED,

  @Schema(
    description = "A prisoner's future appointments have been cancelled",
  )
  APPOINTMENTS_CANCELLED,

  @Schema(
    description = "A prisoner's appointments have been kept",
  )
  APPOINTMENTS_KEPT,
}

@Schema(description = "Alert codes added and/or removed by an alerts-updated event")
data class AlertsUpdatedDetails(
  @Schema(description = "The alert codes that were added", example = "[\"A1\", \"A2\"]")
  val alertsAdded: List<String> = emptyList(),

  @Schema(description = "The alert codes that were removed", example = "[\"C1\", \"C2\"]")
  val alertsClosed: List<String> = emptyList(),
) {
  companion object {
    // Stored in event_data as "A1,A2;C1,C2" (added before ';', closed after).
    fun decode(eventData: String): AlertsUpdatedDetails {
      val parts = eventData.split(";", limit = 2)
      return AlertsUpdatedDetails(
        alertsAdded = parts.getOrNull(0).toCodes(),
        alertsClosed = parts.getOrNull(1).toCodes(),
      )
    }

    private fun String?.toCodes(): List<String> = this
      ?.split(",")
      ?.map { it.trim() }
      ?.filter { it.isNotEmpty() }
      ?: emptyList()
  }
}

@Schema(description = "The new and previous incentive levels from an incentives-changed event")
data class IncentiveLevelsChangedDetails(
  @Schema(description = "The new incentive level", example = "STD")
  val newLevel: String? = null,

  @Schema(description = "The previous incentive level", example = "BAS")
  val previousLevel: String? = null,
) {
  companion object {
    // Stored in event_data as "<new>;<previous>" (new level before ';', previous after).
    fun decode(eventData: String): IncentiveLevelsChangedDetails {
      val parts = eventData.split(";", limit = 2)
      return IncentiveLevelsChangedDetails(
        newLevel = parts.getOrNull(0).toLevel(),
        previousLevel = parts.getOrNull(1).toLevel(),
      )
    }

    private fun String?.toLevel(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
  }
}

@Schema(description = "The previous and new cell locations for a prisoner updated (cell-move) event, populated by the orchestrator")
data class PrisonerUpdatedDetails(
  @Schema(description = "The cell the prisoner was moved from", example = "MDI-1-1-001")
  val previousCell: String? = null,

  @Schema(description = "The cell the prisoner was moved to", example = "MDI-1-1-002")
  val newCell: String? = null,
)

@Schema(description = "The removed and retained prisoner numbers from a prisoner-merged event")
data class OffenderMergedDetails(
  @Schema(description = "The prisoner number that was removed by the merge", example = "A1234BC")
  val removedPrisonerNumber: String? = null,

  @Schema(description = "The prisoner number that was retained by the merge", example = "A1234BD")
  val prisonerNumber: String? = null,
) {
  companion object {
    // Stored in event_data as "<removed>;<retained>" (removed prisoner number before ';', retained after).
    fun decode(eventData: String): OffenderMergedDetails {
      val parts = eventData.split(";", limit = 2)
      return OffenderMergedDetails(
        removedPrisonerNumber = parts.getOrNull(0).toPrisonerNumber(),
        prisonerNumber = parts.getOrNull(1).toPrisonerNumber(),
      )
    }

    private fun String?.toPrisonerNumber(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
  }
}
