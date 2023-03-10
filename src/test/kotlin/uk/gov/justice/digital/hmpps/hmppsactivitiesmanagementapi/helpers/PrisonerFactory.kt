package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import java.time.LocalDate

fun prisoners(
  prisonerNumber: String = "A12345",
  firstName: String = "John",
  lastName: String = "Smith",
  gender: String = "Male",
  status: String = "ACTIVE IN",
  dateOfBirth: LocalDate = LocalDate.now(),
  cellLocation: String = "A-1-002",

) = listOf(
  Prisoner(
    prisonerNumber = prisonerNumber,
    firstName = firstName,
    lastName = lastName,
    gender = gender,
    status = status,
    dateOfBirth = dateOfBirth,
    cellLocation = cellLocation,
  ),
)
