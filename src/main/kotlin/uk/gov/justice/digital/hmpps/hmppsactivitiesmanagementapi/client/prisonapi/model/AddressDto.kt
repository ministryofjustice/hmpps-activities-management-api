package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid

/**
 * An Address
 * @param primary Primary Address
 * @param noFixedAddress No Fixed Address
 * @param addressId Address Id
 * @param addressType Address Type. Note: Reference domain is ADDR_TYPE
 * @param flat Flat
 * @param premise Premise
 * @param street Street
 * @param locality Locality
 * @param town Town/City. Note: Reference domain is CITY
 * @param postalCode Postal Code
 * @param county County. Note: Reference domain is COUNTY
 * @param country Country. Note: Reference domain is COUNTRY
 * @param comment Comment
 * @param startDate Date Added
 * @param endDate Date ended
 * @param phones The phone number associated with the address
 * @param addressUsages The address usages/types
 */
data class AddressDto(

  @Schema(example = "false", description = "Primary Address")
  @JsonProperty("primary")
  val primary: Boolean,

  @Schema(example = "false", description = "No Fixed Address")
  @JsonProperty("noFixedAddress")
  val noFixedAddress: Boolean,

  @Schema(example = "543524", description = "Address Id")
  @JsonProperty("addressId")
  val addressId: Long? = null,

  @Schema(example = "BUS", description = "Address Type. Note: Reference domain is ADDR_TYPE")
  @JsonProperty("addressType")
  val addressType: String? = null,

  @Schema(example = "3B", description = "Flat")
  @JsonProperty("flat")
  val flat: String? = null,

  @Schema(example = "Liverpool Prison", description = "Premise")
  @JsonProperty("premise")
  val premise: String? = null,

  @Schema(example = "Slinn Street", description = "Street")
  @JsonProperty("street")
  val street: String? = null,

  @Schema(example = "Brincliffe", description = "Locality")
  @JsonProperty("locality")
  val locality: String? = null,

  @Schema(example = "Liverpool", description = "Town/City. Note: Reference domain is CITY")
  @JsonProperty("town")
  val town: String? = null,

  @Schema(example = "LI1 5TH", description = "Postal Code")
  @JsonProperty("postalCode")
  val postalCode: String? = null,

  @Schema(example = "HEREFORD", description = "County. Note: Reference domain is COUNTY")
  @JsonProperty("county")
  val county: String? = null,

  @Schema(example = "ENG", description = "Country. Note: Reference domain is COUNTRY")
  @JsonProperty("country")
  val country: String? = null,

  @Schema(example = "This is a comment text", description = "Comment")
  @JsonProperty("comment")
  val comment: String? = null,

  @Valid
  @Schema(example = "Thu May 12 01:00:00 BST 2005", description = "Date Added")
  @JsonProperty("startDate")
  val startDate: java.time.LocalDate? = null,

  @Valid
  @Schema(example = "Fri Feb 12 00:00:00 GMT 2021", description = "Date ended")
  @JsonProperty("endDate")
  val endDate: java.time.LocalDate? = null,

  @Valid
  @Schema(example = "null", description = "The phone number associated with the address")
  @JsonProperty("phones")
  val phones: List<Telephone>? = null,

  @Valid
  @Schema(example = "null", description = "The address usages/types")
  @JsonProperty("addressUsages")
  val addressUsages: List<AddressUsageDto>? = null,
)
