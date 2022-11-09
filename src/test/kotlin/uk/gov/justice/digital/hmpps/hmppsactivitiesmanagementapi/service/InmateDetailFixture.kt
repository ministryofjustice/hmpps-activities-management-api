package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.InmateDetail
import java.time.LocalDate

class InmateDetailFixture {
  companion object {
    fun instance(
      offenderNo: String = "G4793VF",
      offenderId: Long = 11111,
      rootOffenderId: Long = 100011111,
      firstName: String = "Tim",
      lastName: String = "Harrison",
      dateOfBirth: LocalDate = LocalDate.of(1971, 8, 1),
      activeFlag: Boolean = true,
      inOutStatus: InmateDetail.InOutStatus = InmateDetail.InOutStatus.IN,
      status: InmateDetail.Status = InmateDetail.Status.IN,
      bookingId: Long = 900001,
      bookingNo: String = "BK01",
      middleName: String = "James",
      agencyId: String = "MDI",
      age: Int = 23,

    ): InmateDetail = InmateDetail(
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
      age = age
    )
  }
}
