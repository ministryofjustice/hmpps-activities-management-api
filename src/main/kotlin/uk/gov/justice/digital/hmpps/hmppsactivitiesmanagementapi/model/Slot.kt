package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import java.time.DayOfWeek

@Schema(
  description = """
    Describes time slot and day (or days) the scheduled activity would run. At least one day must be specified.
    
    e.g. 'AM, Monday, Wednesday and Friday' or 'PM Tuesday, Thursday, Sunday'
  """,
)

data class Slot(
  @field:Positive(message = "The week number must be a positive integer")
  @Schema(description = "The week of the schedule this slot relates to", example = "1")
  val weekNumber: Int,

  @field:NotEmpty(message = "The time slot must supplied")
  @Schema(
    description = "The time slot of the activity schedule, morning afternoon or evening e.g. AM, PM or ED",
    example = "AM",
  )
  val timeSlot: String?,

  val monday: Boolean = false,

  val tuesday: Boolean = false,

  val wednesday: Boolean = false,

  val thursday: Boolean = false,

  val friday: Boolean = false,

  val saturday: Boolean = false,

  val sunday: Boolean = false,
) {
  fun getDaysOfWeek(): Set<DayOfWeek> {
    return setOfNotNull(
      DayOfWeek.MONDAY.takeIf { monday },
      DayOfWeek.TUESDAY.takeIf { tuesday },
      DayOfWeek.WEDNESDAY.takeIf { wednesday },
      DayOfWeek.THURSDAY.takeIf { thursday },
      DayOfWeek.FRIDAY.takeIf { friday },
      DayOfWeek.SATURDAY.takeIf { saturday },
      DayOfWeek.SUNDAY.takeIf { sunday },
    )
  }

  fun timeSlot() = TimeSlot.valueOf(timeSlot!!)
}

fun List<Slot>.consolidateMatchingSlots() =
  groupBy { it.weekNumber to it.timeSlot }
    .let { rulesBySlots ->
      rulesBySlots.map { (slots, groupedRules) ->
        Slot(
          weekNumber = slots.first,
          timeSlot = slots.second,
          monday = groupedRules.any { it.monday },
          tuesday = groupedRules.any { it.tuesday },
          wednesday = groupedRules.any { it.wednesday },
          thursday = groupedRules.any { it.thursday },
          friday = groupedRules.any { it.friday },
          saturday = groupedRules.any { it.saturday },
          sunday = groupedRules.any { it.sunday },
        )
      }
    }
