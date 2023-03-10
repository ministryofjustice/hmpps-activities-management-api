package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
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
    inOutStatus: Prisoner.InOutStatus = Prisoner.InOutStatus.iN,
    status: String = "IN",
    mostSeriousOffence: String = "Test Offence",
    restrictedPatient: Boolean = false,
    bookingId: Long? = 900001,
    bookNumber: String = "BK01",
    middleNames: String = "James",
    prisonId: String? = "MDI",
    cellLocation: String? = "1-2-3",
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
    )
}
