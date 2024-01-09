package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.InmateDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.pentonvillePrisonCode
import java.time.LocalDate

val activeInMoorlandInmate = InmateDetailFixture.instance(agencyId = moorlandPrisonCode)
val activeInPentonvilleInmate = InmateDetailFixture.instance(agencyId = pentonvillePrisonCode)

object InmateDetailFixture {
  fun instance(
    offenderNo: String = "G4793VF",
    offenderId: Long = 11111,
    rootOffenderId: Long = 100011111,
    firstName: String = "Tim",
    lastName: String = "Harrison",
    dateOfBirth: LocalDate = LocalDate.of(1971, 8, 1),
    activeFlag: Boolean = true,
    inOutStatus: String = "IN",
    status: String = "IN",
    bookingId: Long? = 900001,
    bookingNo: String = "BK01",
    middleName: String = "James",
    agencyId: String = "MDI",
    age: Int = 23,
  ) =
    InmateDetail(
      offenderNo = offenderNo,
      offenderId = offenderId,
      rootOffenderId = rootOffenderId,
      firstName = firstName,
      lastName = lastName,
      dateOfBirth = dateOfBirth,
      activeFlag = activeFlag,
      inOutStatus = inOutStatus,
      status = status,
      bookingId = bookingId,
      bookingNo = bookingNo,
      middleName = middleName,
      agencyId = agencyId,
      age = age,
    )
}
