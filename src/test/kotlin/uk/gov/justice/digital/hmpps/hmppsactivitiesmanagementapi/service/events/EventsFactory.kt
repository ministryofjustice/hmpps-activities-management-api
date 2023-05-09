package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.pentonvillePrisonCode

fun offenderTemporaryReleasedEvent(
  prisonCode: String = pentonvillePrisonCode,
  prisonerNumber: String = "XXXXXX",
) = OffenderReleasedEvent(
  ReleaseInformation(
    prisonerNumber,
    "TEMPORARY_ABSENCE_RELEASE",
    prisonCode,
  ),
)

fun offenderReleasedEvent(
  prisonCode: String = pentonvillePrisonCode,
  prisonerNumber: String = "XXXXXX",
) = OffenderReleasedEvent(
  ReleaseInformation(
    prisonerNumber,
    "RELEASED",
    prisonCode,
  ),
)

fun offenderTransferReleasedEvent(
  prisonCode: String = pentonvillePrisonCode,
  prisonerNumber: String = "XXXXXX",
) = OffenderReleasedEvent(
  ReleaseInformation(
    prisonerNumber,
    "TRANSFERRED",
    prisonCode,
  ),
)

fun offenderReceivedFromTemporaryAbsence(
  prisonCode: String = pentonvillePrisonCode,
  prisonerNumber: String = "XXXXXX",
) = OffenderReceivedEvent(
  ReceivedInformation(
    prisonerNumber,
    "TEMPORARY_ABSENCE_RETURN",
    prisonCode,
  ),
)

fun cellMoveEvent(prisonerNumber: String = "XXXXXX") =
  CellMoveEvent(
    CellMoveInformation(
      nomsNumber = prisonerNumber,
      bookingId = 123L,
      livingUnitId = 234L,
    ),
  )

fun iepReviewInsertedEvent(prisonerNumber: String = "XXXXXX", prisonId: String? = null, reason: String? = null) =
  IncentivesInsertedEvent(
    IncentivesInformation(
      nomsNumber = prisonerNumber,
      prisonId = prisonId,
      reason = reason,
    ),
  )
