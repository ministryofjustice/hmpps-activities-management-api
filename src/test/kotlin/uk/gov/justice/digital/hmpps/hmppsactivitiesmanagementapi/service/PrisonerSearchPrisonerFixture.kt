package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.MovementType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.CurrentIncentive
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.IncentiveLevel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.PagedPrisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.PrisonerAlert
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.pentonvillePrisonCode
import java.time.LocalDate

val activeInMoorlandPrisoner = PrisonerSearchPrisonerFixture.instance(prisonId = moorlandPrisonCode, status = "ACTIVE IN")
val activeInPentonvillePrisoner = activeInMoorlandPrisoner.copy(prisonId = pentonvillePrisonCode)
val activeOutMoorlandPrisoner = PrisonerSearchPrisonerFixture.instance(prisonId = moorlandPrisonCode, status = "ACTIVE OUT", inOutStatus = Prisoner.InOutStatus.OUT)
val activeOutPentonvillePrisoner = activeOutMoorlandPrisoner.copy(prisonId = pentonvillePrisonCode)
val temporarilyReleasedFromMoorland = PrisonerSearchPrisonerFixture.instance(prisonId = moorlandPrisonCode, status = "ACTIVE OUT", inOutStatus = Prisoner.InOutStatus.OUT)
val permanentlyReleasedPrisonerToday = PrisonerSearchPrisonerFixture.instance(prisonId = null, status = "INACTIVE OUT", inOutStatus = Prisoner.InOutStatus.OUT, lastMovementType = MovementType.RELEASE, confirmedReleaseDate = TimeSource.today())

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
    legalStatus: Prisoner.LegalStatus? = null,
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
      legalStatus = legalStatus,
    )

  fun pagedResult(prisonerNumbers: List<String> = listOf("G4793VF")) =
    PagedPrisoner(content = prisonerNumbers.map { instance(it) })
}
