package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

data class Education(
  @get:JsonProperty("bookingId", required = true) val bookingId: Long? = null,
  @get:JsonProperty("startDate", required = true) val startDate: LocalDate? = null,
  @get:JsonProperty("endDate", required = true) val endDate: LocalDate? = null,
  @get:JsonProperty("studyArea", required = true) val studyArea: String? = null,
  @get:JsonProperty("educationLevel", required = true) val educationLevel: String? = null,
  @get:JsonProperty("numberOfYears", required = true) val numberOfYears: Int? = null,
  @get:JsonProperty("graduationYear", required = true) val graduationYear: String? = null,
  @get:JsonProperty("comment", required = true) val comment: String? = null,
  @get:JsonProperty("school", required = true) val school: String? = null,
  @get:JsonProperty("isSpecialEducation", required = true) val isSpecialEducation: Boolean? = null,
  @get:JsonProperty("schedule", required = true) val schedule: String? = null,
  @get:JsonProperty("addresses", required = true) val addresses: List<AddressDto>? = emptyList(),
)
