package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.incentives.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * * @param levelCode The incentive level code this refers to
 * @param levelName * @param prisonId The prison this refers to
 * @param active Indicates that this incentive level is enabled in this prison
 * @param defaultOnAdmission Indicates that this incentive level is the default for new admissions
 * @param remandTransferLimitInPence The amount transferred weekly from the private cash account to the spends account for a remand prisoner to use
 * @param remandSpendLimitInPence The maximum amount allowed in the spends account for a remand prisoner
 * @param convictedTransferLimitInPence The amount transferred weekly from the private cash account to the spends account for a convicted prisoner to use
 * @param convictedSpendLimitInPence The maximum amount allowed in the spends account for a convicted prisoner
 * @param visitOrders The number of weekday visits for a convicted prisoner per fortnight
 * @param privilegedVisitOrders The number of privileged/weekend visits for a convicted prisoner per 4 weeks
 */
data class PrisonIncentiveLevel(

  @Schema(example = "STD", required = true, description = "The incentive level code this refers to")
  @get:JsonProperty("levelCode", required = true)
  val levelCode: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("levelName", required = true)
  val levelName: kotlin.String,

  @Schema(example = "MDI", required = true, description = "The prison this refers to")
  @get:JsonProperty("prisonId", required = true)
  val prisonId: kotlin.String,

  @Schema(example = "true", required = true, description = "Indicates that this incentive level is enabled in this prison")
  @get:JsonProperty("active", required = true)
  val active: kotlin.Boolean = true,

  @Schema(example = "true", required = true, description = "Indicates that this incentive level is the default for new admissions")
  @get:JsonProperty("defaultOnAdmission", required = true)
  val defaultOnAdmission: kotlin.Boolean = false,

  @Schema(example = "5500", required = true, description = "The amount transferred weekly from the private cash account to the spends account for a remand prisoner to use")
  @get:JsonProperty("remandTransferLimitInPence", required = true)
  val remandTransferLimitInPence: kotlin.Int,

  @Schema(example = "55000", required = true, description = "The maximum amount allowed in the spends account for a remand prisoner")
  @get:JsonProperty("remandSpendLimitInPence", required = true)
  val remandSpendLimitInPence: kotlin.Int,

  @Schema(example = "1800", required = true, description = "The amount transferred weekly from the private cash account to the spends account for a convicted prisoner to use")
  @get:JsonProperty("convictedTransferLimitInPence", required = true)
  val convictedTransferLimitInPence: kotlin.Int,

  @Schema(example = "18000", required = true, description = "The maximum amount allowed in the spends account for a convicted prisoner")
  @get:JsonProperty("convictedSpendLimitInPence", required = true)
  val convictedSpendLimitInPence: kotlin.Int,

  @Schema(example = "2", required = true, description = "The number of weekday visits for a convicted prisoner per fortnight")
  @get:JsonProperty("visitOrders", required = true)
  val visitOrders: kotlin.Int,

  @Schema(example = "1", required = true, description = "The number of privileged/weekend visits for a convicted prisoner per 4 weeks")
  @get:JsonProperty("privilegedVisitOrders", required = true)
  val privilegedVisitOrders: kotlin.Int,
)
