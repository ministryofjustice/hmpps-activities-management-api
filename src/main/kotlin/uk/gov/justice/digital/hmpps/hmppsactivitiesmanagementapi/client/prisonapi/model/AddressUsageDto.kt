package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * An Offender's address usage
 * @param addressId Address ID of the associated address
 * @param addressUsage The address usages
 * @param addressUsageDescription The address usages description
 * @param activeFlag Active Flag
 */
data class AddressUsageDto(

  @Schema(example = "23422313", description = "Address ID of the associated address")
  @JsonProperty("addressId")
  val addressId: Long? = null,

  @Schema(example = "HDC", description = "The address usages")
  @JsonProperty("addressUsage")
  val addressUsage: String? = null,

  @Schema(example = "HDC Address", description = "The address usages description")
  @JsonProperty("addressUsageDescription")
  val addressUsageDescription: String? = null,

  @Schema(example = "true", description = "Active Flag")
  @JsonProperty("activeFlag")
  val activeFlag: Boolean? = null,
)
