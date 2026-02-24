package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param prisonerNumber Prisoner Number
 * @param firstName First Name
 * @param lastName Last name
 * @param status Status of the prisoner
 * @param restrictedPatient Indicates a restricted patient
 * @param bookingId Booking No.
 * @param lastMovementTypeCode Last Movement Type Code of prisoner
 * @param inOutStatus In/Out Status
 * @param prisonId Prison ID
 * @param cellLocation In prison cell location
 * @param alerts Alerts
 * @param category Prisoner Category
 * @param legalStatus Legal Status
 * @param releaseDate Actual of most likely Release Date
 * @param confirmedReleaseDate Release Date Confirmed
 * @param actualParoleDate Actual Parole Date
 * @param tariffDate Tariff Date
 * @param currentIncentive
 */
data class Prisoner(

    @Schema(example = "A1234AA", required = true, description = "Prisoner Number")
    @get:JsonProperty("prisonerNumber", required = true) val prisonerNumber: String,

    @Schema(example = "Robert", required = true, description = "First Name")
    @get:JsonProperty("firstName", required = true) val firstName: String,

    @Schema(example = "Larsen", required = true, description = "Last name")
    @get:JsonProperty("lastName", required = true) val lastName: String,

    @Schema(example = "ACTIVE IN", description = "Status of the prisoner")
    @get:JsonProperty("status") val status: String? = null,

    @Schema(example = "true", description = "Indicates a restricted patient")
    @get:JsonProperty("restrictedPatient") val restrictedPatient: Boolean? = null,

    @Schema(example = "0001200924", description = "Booking No.")
    @get:JsonProperty("bookingId") val bookingId: String? = null,

    @Schema(example = "CRT", description = "Last Movement Type Code of prisoner")
    @get:JsonProperty("lastMovementTypeCode") val lastMovementTypeCode: String? = null,

    @Schema(example = "IN", description = "In/Out Status")
    @get:JsonProperty("inOutStatus") val inOutStatus: InOutStatus? = null,

    @Schema(example = "MDI", description = "Prison ID")
    @get:JsonProperty("prisonId") val prisonId: String? = null,

    @Schema(example = "A-1-002", description = "In prison cell location")
    @get:JsonProperty("cellLocation") val cellLocation: String? = null,

    @Schema(example = "null", description = "Alerts")
    @get:JsonProperty("alerts") val alerts: List<PrisonerAlert>? = null,

    @Schema(example = "C", description = "Prisoner Category")
    @get:JsonProperty("category") val category: String? = null,

    @Schema(example = "SENTENCED", description = "Legal Status")
    @get:JsonProperty("legalStatus") val legalStatus: LegalStatus? = null,

    @Schema(example = "Tue May 02 01:00:00 BST 2023", description = "Actual of most likely Release Date")
    @get:JsonProperty("releaseDate") val releaseDate: java.time.LocalDate? = null,

    @Schema(example = "Mon May 01 01:00:00 BST 2023", description = "Release Date Confirmed")
    @get:JsonProperty("confirmedReleaseDate") val confirmedReleaseDate: java.time.LocalDate? = null,

    @Schema(example = "Mon May 01 01:00:00 BST 2023", description = "Actual Parole Date")
    @get:JsonProperty("actualParoleDate") val actualParoleDate: java.time.LocalDate? = null,

    @Schema(example = "Mon May 01 01:00:00 BST 2023", description = "Tariff Date")
    @get:JsonProperty("tariffDate") val tariffDate: java.time.LocalDate? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("currentIncentive") val currentIncentive: CurrentIncentive? = null,
) {

    /**
    * In/Out Status
    * Values: IN,OUT,TRN
    */
    enum class InOutStatus(val value: String) {

        @JsonProperty("IN") IN("IN"),
        @JsonProperty("OUT") OUT("OUT"),
        @JsonProperty("TRN") TRN("TRN")
    }

    /**
    * Legal Status
    * Values: RECALL,DEAD,INDETERMINATE_SENTENCE,SENTENCED,CONVICTED_UNSENTENCED,CIVIL_PRISONER,IMMIGRATION_DETAINEE,REMAND,UNKNOWN,OTHER
    */
    enum class LegalStatus(val value: String) {

        @JsonProperty("RECALL") RECALL("RECALL"),
        @JsonProperty("DEAD") DEAD("DEAD"),
        @JsonProperty("INDETERMINATE_SENTENCE") INDETERMINATE_SENTENCE("INDETERMINATE_SENTENCE"),
        @JsonProperty("SENTENCED") SENTENCED("SENTENCED"),
        @JsonProperty("CONVICTED_UNSENTENCED") CONVICTED_UNSENTENCED("CONVICTED_UNSENTENCED"),
        @JsonProperty("CIVIL_PRISONER") CIVIL_PRISONER("CIVIL_PRISONER"),
        @JsonProperty("IMMIGRATION_DETAINEE") IMMIGRATION_DETAINEE("IMMIGRATION_DETAINEE"),
        @JsonProperty("REMAND") REMAND("REMAND"),
        @JsonProperty("UNKNOWN") UNKNOWN("UNKNOWN"),
        @JsonProperty("OTHER") OTHER("OTHER")
    }
}

