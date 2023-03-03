package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid

/**
 * Alias
 * @param firstName First name of offender alias
 * @param lastName Last name of offender alias
 * @param age Age of Offender
 * @param dob Date of Birth of Offender
 * @param gender Gender
 * @param createDate Date of creation
 * @param middleName Middle names of offender alias
 * @param ethnicity Ethnicity
 * @param nameType Type of Alias
 */
data class Alias(

  @Schema(example = "Mike", description = "First name of offender alias")
  @JsonProperty("firstName")
  val firstName: String,

  @Schema(example = "Smith", description = "Last name of offender alias")
  @JsonProperty("lastName")
  val lastName: String,

  @Schema(example = "32", description = "Age of Offender")
  @JsonProperty("age")
  val age: Int,

  @Valid
  @Schema(example = "Thu Feb 28 00:00:00 GMT 1980", description = "Date of Birth of Offender")
  @JsonProperty("dob")
  val dob: java.time.LocalDate,

  @Schema(example = "Male", description = "Gender")
  @JsonProperty("gender")
  val gender: String,

  @Valid
  @Schema(example = "Fri Feb 15 00:00:00 GMT 2019", description = "Date of creation")
  @JsonProperty("createDate")
  val createDate: java.time.LocalDate,

  @Schema(example = "John", description = "Middle names of offender alias")
  @JsonProperty("middleName")
  val middleName: String? = null,

  @Schema(example = "Mixed: White and Black African", description = "Ethnicity")
  @JsonProperty("ethnicity")
  val ethnicity: String? = null,

  @Schema(example = "Alias Name", description = "Type of Alias")
  @JsonProperty("nameType")
  val nameType: String? = null,
)
