package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE

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

fun prisonerUpdatedEvent(prisonerNumber: String = "XXXXXX", categoriesChanged: List<String> = listOf<String>("LOCATION")) =
  PrisonerUpdatedEvent(
    PrisonerUpdatedInformation(
      nomsNumber = prisonerNumber,
      categoriesChanged = categoriesChanged,
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

fun iepReviewUpdatedEvent(prisonerNumber: String = "XXXXXX", prisonId: String? = null, reason: String? = null) =
  IncentivesUpdatedEvent(
    IncentivesInformation(
      nomsNumber = prisonerNumber,
      prisonId = prisonId,
      reason = reason,
    ),
  )

fun iepReviewDeletedEvent(prisonerNumber: String = "XXXXXX", prisonId: String? = null, reason: String? = null) =
  IncentivesDeletedEvent(
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

fun nonAssociationsChangedEvent(
  prisonerNumber: String = "123456",
  bookingId: Long = 42,
) = NonAssociationsChangedEvent(
  additionalInformation = NonAssociationInformation(
    bookingId = bookingId,
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
