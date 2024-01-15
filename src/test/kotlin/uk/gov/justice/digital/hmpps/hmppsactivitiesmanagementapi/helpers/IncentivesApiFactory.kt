package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.incentivesapi.model.PrisonIncentiveLevel

fun prisonIncentiveLevel(
  levelCode: String = "BAS",
  levelName: String = "Basic",
  prisonId: String = "PVI",
  active: Boolean = true,
) = PrisonIncentiveLevel(
  levelCode,
  levelName,
  prisonId,
  active,
  defaultOnAdmission = false,
  remandTransferLimitInPence = 1000,
  remandSpendLimitInPence = 2000,
  convictedTransferLimitInPence = 1000,
  convictedSpendLimitInPence = 2000,
  visitOrders = 2,
  privilegedVisitOrders = 1,
)
