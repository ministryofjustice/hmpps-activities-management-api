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
