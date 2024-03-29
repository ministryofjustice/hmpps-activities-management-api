package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.InmateDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import java.time.LocalDate

val activeInMoorlandInmate = InmateDetailFixture.instance(agencyId = MOORLAND_PRISON_CODE)
val activeInPentonvilleInmate = InmateDetailFixture.instance(agencyId = PENTONVILLE_PRISON_CODE)

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
