package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent

@Schema(description = "Describes an event to be published to the domain events SNS topic")
data class PublishEventUtilityModel(

  @field:NotNull(message = "Outbound Event must be supplied")
  @Schema(description = "The outbound event to be published", implementation = OutboundEvent::class)
  val outboundEvent: OutboundEvent?,

  @field:NotEmpty(message = "At least one identifier must be supplied")
  @Schema(description = "A list of entity identifiers to be published with the event", example = "[1,2]")
  val identifiers: List<Long>?,
)
