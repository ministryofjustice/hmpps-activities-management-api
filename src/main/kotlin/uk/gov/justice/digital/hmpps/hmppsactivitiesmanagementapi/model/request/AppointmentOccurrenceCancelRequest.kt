package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
  description =
  """
  Describes an appointment or series of appointment occurrences to be cancelled. 
  """,
)
data class AppointmentOccurrenceCancelRequest(

  @Schema(
    description =
    """
    Specifies the id of the reason for the cancellation. The cancellation reason, identified byt this ID, will determine 
    whether the cancellation is also treated as a soft delete
    """,
    example = "1234",
  )
  val cancellationReasonId: Long,

  @Schema(
    description =
    """
    Specifies which appointment occurrence or occurrences this cancellation should apply to.
    Defaults to THIS_OCCURRENCE meaning the cancellation will be applied to the appointment occurrence specified by the
    supplied id only.
    """,
    example = "THIS_OCCURRENCE",
  )
  val applyTo: ApplyTo = ApplyTo.THIS_OCCURRENCE,
)
