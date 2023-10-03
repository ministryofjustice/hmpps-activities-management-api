package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.MovementType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.CurrentIncentive
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.IncentiveLevel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.PagedPrisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.PrisonerAlert
import java.time.LocalDate

object PrisonerSearchPrisonerFixture {
  fun instance(
    prisonerNumber: String = "G4793VF",
    firstName: String = "Tim",
    lastName: String = "Harrison",
    dateOfBirth: LocalDate = LocalDate.of(1971, 8, 1),
    gender: String = "Male",
    ethnicity: String = "Test Ethnicity",
    youthOffender: Boolean = false,
    maritalStatus: String = "Test Marital Status",
    religion: String = "Test Religion",
    nationality: String = "Test Nationality",
    inOutStatus: Prisoner.InOutStatus = Prisoner.InOutStatus.IN,
    status: String = "ACTIVE IN",
    mostSeriousOffence: String = "Test Offence",
    restrictedPatient: Boolean = false,
    bookingId: Long? = 900001,
    bookNumber: String = "BK01",
    middleNames: String = "James",
    prisonId: String? = "MDI",
    cellLocation: String? = "1-2-3",
    currentIncentive: CurrentIncentive? = CurrentIncentive(
      level = IncentiveLevel("Basic", "BAS"),
      dateTime = "2020-07-20T10:36:53",
      nextReviewDate = LocalDate.of(2021, 7, 20),
    ),
    lastMovementType: MovementType? = null,
    releaseDate: LocalDate? = null,
    confirmedReleaseDate: LocalDate? = null,
    alerts: List<PrisonerAlert> = emptyList(),
  ) =
    Prisoner(
      prisonerNumber = prisonerNumber,
      firstName = firstName,
      lastName = lastName,
      dateOfBirth = dateOfBirth,
      gender = gender,
      ethnicity = ethnicity,
      youthOffender = youthOffender,
      maritalStatus = maritalStatus,
      religion = religion,
      nationality = nationality,
      status = status,
      mostSeriousOffence = mostSeriousOffence,
      restrictedPatient = restrictedPatient,
      inOutStatus = inOutStatus,
      bookingId = bookingId?.toString(),
      bookNumber = bookNumber,
      middleNames = middleNames,
      prisonId = prisonId,
      cellLocation = cellLocation,
      currentIncentive = currentIncentive,
      lastMovementTypeCode = lastMovementType?.nomisShortCode,
      releaseDate = releaseDate,
      confirmedReleaseDate = confirmedReleaseDate,
      alerts = alerts,
    )

  fun pagedResult(prisonerNumber: String = "G4793VF") = PagedPrisoner(content = listOf(instance(prisonerNumber)))
}
