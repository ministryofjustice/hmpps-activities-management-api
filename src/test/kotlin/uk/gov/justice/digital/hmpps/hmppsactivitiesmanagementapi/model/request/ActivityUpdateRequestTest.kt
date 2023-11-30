package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource

class ActivityUpdateRequestTest : ValidatorBase<ActivityUpdateRequest>() {

  @Test
  fun `start date must be in the future`() {
    val activityStartingToday = ActivityUpdateRequest(startDate = TimeSource.today())

    activityStartingToday failsWithSingle ModelError("startDate", "Activity start date must be in the future")
  }

  @Test
  fun `Schedule weeks must be greater than 0`() {
    val zeroScheduleWeeks = ActivityUpdateRequest(scheduleWeeks = 0)

    zeroScheduleWeeks failsWithSingle ModelError("scheduleWeeks", "Schedule weeks must be either 1 or 2")
  }

  @Test
  fun `Schedule weeks must be less than or equal to 2`() {
    val threeScheduleWeeks = ActivityUpdateRequest(scheduleWeeks = 3)

    threeScheduleWeeks failsWithSingle ModelError("scheduleWeeks", "Schedule weeks must be either 1 or 2")
  }

  @Test
  fun `Paid activity must have pay rates`() {
    val paidActivityNoPay = ActivityUpdateRequest(paid = true)

    paidActivityNoPay failsWithSingle ModelError("paid", "Paid activity must have at least one pay rate associated with it")
  }

  @Test
  fun `Unpaid activity must not have pay rates`() {
    val pay = ActivityPayCreateRequest(incentiveNomisCode = "BAS", incentiveLevel = "Basic", payBandId = 1L, rate = 125, pieceRate = 150, pieceRateItems = 1)
    val unpaidActivityWithPay = ActivityUpdateRequest(paid = false, pay = listOf(pay))

    unpaidActivityWithPay failsWithSingle ModelError("unpaid", "Unpaid activity cannot have pay rates associated with it")
  }
}
