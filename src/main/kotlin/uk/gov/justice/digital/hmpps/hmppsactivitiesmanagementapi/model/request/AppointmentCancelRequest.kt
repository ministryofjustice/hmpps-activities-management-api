package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
  description =
  """
  Describes how to cancel or delete one or more appointments. 
  """,
)
data class AppointmentCancelRequest(

  @Schema(
    description =
    """
    Specifies the id of the reason for the cancellation. The cancellation reason, identified by this id, will determine 
    whether the cancellation is also treated as a soft delete
    """,
    example = "1234",
  )
  val cancellationReasonId: Long,

  @Schema(
    description =
    """
    Specifies which appointment or appointments this cancellation should apply to.
    Defaults to THIS_APPOINTMENT meaning the cancellation will be applied to the appointment specified by the
    supplied id only.
    """,
    example = "THIS_APPOINTMENT",
  )
  val applyTo: ApplyTo = ApplyTo.THIS_APPOINTMENT,
)
