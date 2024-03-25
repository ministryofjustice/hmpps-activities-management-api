package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE

@Deprecated(
  message = "Replaced by prisoner temporary released event",
  replaceWith = ReplaceWith("prisonerTemporaryReleasedEvent(prisonCode, prisonerNumber, reason)"),
)
fun offenderTemporaryReleasedEvent(
  prisonCode: String = PENTONVILLE_PRISON_CODE,
  prisonerNumber: String = "XXXXXX",
) = OffenderReleasedEvent(
  ReleaseInformation(
    prisonerNumber,
    "TEMPORARY_ABSENCE_RELEASE",
    prisonCode,
  ),
)

fun prisonerTemporaryReleasedEvent(
  prisonCode: String = PENTONVILLE_PRISON_CODE,
  prisonerNumber: String = "XXXXXX",
) = PrisonerReleasedEvent(
  ReleaseInformation(
    prisonerNumber,
    "TEMPORARY_ABSENCE_RELEASE",
    prisonCode,
  ),
)

@Deprecated(
  message = "Replaced by prisoner released event",
  replaceWith = ReplaceWith("prisonerReleasedEvent(prisonCode, prisonerNumber, reason)"),
)
fun offenderReleasedEvent(
  prisonCode: String = PENTONVILLE_PRISON_CODE,
  prisonerNumber: String = "XXXXXX",
  reason: String = "RELEASED",
) = OffenderReleasedEvent(
  ReleaseInformation(
    prisonerNumber,
    reason,
    prisonCode,
  ),
)

fun prisonerReleasedEvent(
  prisonCode: String = PENTONVILLE_PRISON_CODE,
  prisonerNumber: String = "XXXXXX",
  reason: String = "RELEASED",
) = PrisonerReleasedEvent(
  ReleaseInformation(
    prisonerNumber,
    reason,
    prisonCode,
  ),
)

@Deprecated(
  message = "Replaced by prisoner transfer released event",
  replaceWith = ReplaceWith("prisonerTransferReleasedEvent(prisonCode, prisonerNumber)"),
)
fun offenderTransferReleasedEvent(
  prisonCode: String = PENTONVILLE_PRISON_CODE,
  prisonerNumber: String = "XXXXXX",
) = OffenderReleasedEvent(
  ReleaseInformation(
    prisonerNumber,
    "TRANSFERRED",
    prisonCode,
  ),
)

fun prisonerTransferReleasedEvent(
  prisonCode: String = PENTONVILLE_PRISON_CODE,
  prisonerNumber: String = "XXXXXX",
) = PrisonerReleasedEvent(
  ReleaseInformation(
    prisonerNumber,
    "TRANSFERRED",
    prisonCode,
  ),
)

@Deprecated(
  message = "Replaced by prisoner received event",
  replaceWith = ReplaceWith("prisonerReceivedFromTemporaryAbsence(prisonCode, prisonerNumber)"),
)
fun offenderReceivedFromTemporaryAbsence(
  prisonCode: String = PENTONVILLE_PRISON_CODE,
  prisonerNumber: String = "XXXXXX",
) = OffenderReceivedEvent(
  ReceivedInformation(
    prisonerNumber,
    "TEMPORARY_ABSENCE_RETURN",
    prisonCode,
  ),
)

fun prisonerReceivedFromTemporaryAbsence(
  prisonCode: String = PENTONVILLE_PRISON_CODE,
  prisonerNumber: String = "XXXXXX",
) = PrisonerReceivedEvent(
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

fun appointmentsChangedEvent(prisonerNumber: String = "XXXXXX", prisonId: String = "MDI", action: String = "YES") =
  AppointmentsChangedEvent(
    personReference = PersonReference(
      identifiers = listOf(Identifier("NOMS", prisonerNumber)),
    ),
    AppointmentsChangedInformation(
      action,
      prisonId = prisonId,
      user = "SOME_USER",

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

fun activitiesChangedEvent(
  prisonerNumber: String,
  action: Action,
  prisonId: String,
) =
  ActivitiesChangedEvent(
    personReference = PersonReference(
      identifiers = listOf(Identifier("NOMS", prisonerNumber)),
    ),
    additionalInformation = ActivitiesChangedInformation(
      action = action.name,
      prisonId = prisonId,
      user = "SOME_USER",
    ),
  )

fun alertsUpdatedEvent(
  prisonerNumber: String = "123456",
  bookingId: Long = 42,
  alertsAdded: Set<String> = setOf("A1", "A2"),
  alertsRemoved: Set<String> = setOf("R1", "R2"),
) = AlertsUpdatedEvent(
  additionalInformation = AlertsUpdatedInformation(
    bookingId = bookingId,
    alertsAdded = alertsAdded,
    alertsRemoved = alertsRemoved,
    nomsNumber = prisonerNumber,
  ),
)

fun offenderMergedEvent(
  prisonerNumber: String = "B1111BB",
  removedPrisonerNumber: String = "A1111AA",
) = OffenderMergedEvent(
  MergeInformation(
    prisonerNumber,
    removedPrisonerNumber,
  ),
)
