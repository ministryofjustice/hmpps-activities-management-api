package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
  description =
  """
  Describes how to un-cancel or delete one or more appointments. 
  """,
)
data class AppointmentUncancelRequest(
  @Schema(
    description =
    """
    Specifies which appointment or appointments this un-cancellation should apply to.
    Defaults to THIS_APPOINTMENT meaning the un-cancellation will be applied to the appointment specified by the
    supplied id only.
    """,
    example = "THIS_APPOINTMENT",
  )
  val applyTo: ApplyTo = ApplyTo.THIS_APPOINTMENT,
)
