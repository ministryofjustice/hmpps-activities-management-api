package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.Valid

/**
 * Alert
 * @param alertId Alert Id
 * @param bookingId Offender booking id.
 * @param offenderNo Offender Unique Reference
 * @param alertType Alert Type
 * @param alertTypeDescription Alert Type Description
 * @param alertCode Alert Code
 * @param alertCodeDescription Alert Code Description
 * @param comment Alert comments
 * @param dateCreated Date of the alert, which might differ to the date it was created
 * @param expired True / False based on presence of expiry date
 * @param active True / False based on alert status
 * @param dateExpires Date the alert expires
 * @param addedByFirstName First name of the user who added the alert
 * @param addedByLastName Last name of the user who added the alert
 * @param expiredByFirstName First name of the user who last modified the alert
 * @param expiredByLastName Last name of the user who last modified the alert
 */
data class Alert(

  @Schema(example = "1", description = "Alert Id")
  @JsonProperty("alertId") val alertId: Long,

  @Schema(example = "14", description = "Offender booking id.")
  @JsonProperty("bookingId") val bookingId: Long,

  @Schema(example = "G3878UK", description = "Offender Unique Reference")
  @JsonProperty("offenderNo") val offenderNo: String?,

  @Schema(example = "X", description = "Alert Type")
  @JsonProperty("alertType") val alertType: String,

  @Schema(example = "Security", description = "Alert Type Description")
  @JsonProperty("alertTypeDescription") val alertTypeDescription: String,

  @Schema(example = "XER", description = "Alert Code")
  @JsonProperty("alertCode") val alertCode: String,

  @Schema(example = "Escape Risk", description = "Alert Code Description")
  @JsonProperty("alertCodeDescription") val alertCodeDescription: String,

  @Schema(example = "Profession lock pick.", description = "Alert comments")
  @JsonProperty("comment") val comment: String,

  @Valid
  @Schema(
    example = "Tue Aug 20 01:00:00 BST 2019",
    description = "Date of the alert, which might differ to the date it was created"
  )
  @JsonProperty("dateCreated") val dateCreated: java.time.LocalDate,

  @Schema(example = "true", description = "True / False based on presence of expiry date")
  @JsonProperty("expired") val expired: Boolean,

  @Schema(example = "false", description = "True / False based on alert status")
  @JsonProperty("active") val active: Boolean,

  @Valid
  @Schema(example = "Thu Aug 20 01:00:00 BST 2020", description = "Date the alert expires")
  @JsonProperty("dateExpires") val dateExpires: java.time.LocalDate? = null,

  @Schema(example = "John", description = "First name of the user who added the alert")
  @JsonProperty("addedByFirstName") val addedByFirstName: String? = null,

  @Schema(example = "Smith", description = "Last name of the user who added the alert")
  @JsonProperty("addedByLastName") val addedByLastName: String? = null,

  @Schema(example = "John", description = "First name of the user who last modified the alert")
  @JsonProperty("expiredByFirstName") val expiredByFirstName: String? = null,

  @Schema(example = "Smith", description = "Last name of the user who last modified the alert")
  @JsonProperty("expiredByLastName") val expiredByLastName: String? = null
)
