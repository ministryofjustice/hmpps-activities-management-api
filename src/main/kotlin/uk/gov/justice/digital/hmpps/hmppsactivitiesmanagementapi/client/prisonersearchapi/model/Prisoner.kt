package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.BodyPartDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.CurrentIncentive
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.PrisonerAlert
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.PrisonerAlias
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param prisonerNumber Prisoner Number
 * @param firstName First Name
 * @param lastName Last name
 * @param dateOfBirth Date of Birth
 * @param gender Gender
 * @param ethnicity Ethnicity
 * @param youthOffender Youth Offender?
 * @param maritalStatus Marital Status
 * @param religion Religion
 * @param nationality Nationality
 * @param status Status of the prisoner
 * @param mostSeriousOffence Most serious offence for this sentence
 * @param restrictedPatient Indicates a restricted patient
 * @param pncNumber PNC Number
 * @param pncNumberCanonicalShort PNC Number
 * @param pncNumberCanonicalLong PNC Number
 * @param croNumber CRO Number
 * @param bookingId Booking No.
 * @param bookNumber Book Number
 * @param middleNames Middle Names
 * @param lastMovementTypeCode Last Movement Type Code of prisoner
 * @param lastMovementReasonCode Last Movement Reason of prisoner
 * @param inOutStatus In/Out Status
 * @param prisonId Prison ID
 * @param prisonName Prison Name
 * @param cellLocation In prison cell location
 * @param aliases Aliases Names and Details
 * @param alerts Alerts
 * @param csra Cell Sharing Risk Assessment
 * @param category Prisoner Category
 * @param legalStatus Legal Status
 * @param imprisonmentStatus The prisoner's imprisonment status code.
 * @param imprisonmentStatusDescription The prisoner's imprisonment status description.
 * @param recall Indicates that the offender has been recalled
 * @param indeterminateSentence Indicates that the offender has an indeterminate sentence
 * @param sentenceStartDate Start Date for this sentence
 * @param releaseDate Actual of most likely Release Date
 * @param confirmedReleaseDate Release Date Confirmed
 * @param sentenceExpiryDate Sentence Expiry Date
 * @param licenceExpiryDate Licence Expiry Date
 * @param homeDetentionCurfewEligibilityDate HDC Eligibility Date
 * @param homeDetentionCurfewActualDate HDC Actual Date
 * @param homeDetentionCurfewEndDate HDC End Date
 * @param topupSupervisionStartDate Top-up supervision start date
 * @param topupSupervisionExpiryDate Top-up supervision expiry date
 * @param additionalDaysAwarded Days added to sentence term due to adjustments.
 * @param nonDtoReleaseDate Release date for Non determinant sentence (if applicable). This will be based on one of ARD, CRD, NPD or PRRD.
 * @param nonDtoReleaseDateType Indicates which type of non-DTO release date is the effective release date. One of 'ARD’, 'CRD’, ‘NPD’ or 'PRRD’.
 * @param receptionDate Date prisoner was received into the prison
 * @param paroleEligibilityDate Parole  Eligibility Date
 * @param automaticReleaseDate Automatic Release Date. If automaticReleaseOverrideDate is available then it will be set as automaticReleaseDate
 * @param postRecallReleaseDate Post Recall Release Date. if postRecallReleaseOverrideDate is available then it will be set as postRecallReleaseDate
 * @param conditionalReleaseDate Conditional Release Date. If conditionalReleaseOverrideDate is available then it will be set as conditionalReleaseDate
 * @param actualParoleDate Actual Parole Date
 * @param tariffDate Tariff Date
 * @param locationDescription current prison or outside with last movement information.
 * @param supportingPrisonId Supporting prison ID for POM
 * @param dischargedHospitalId Which hospital the offender has been discharged to
 * @param dischargedHospitalDescription Hospital name to which the offender was discharged
 * @param dischargeDate Date of discharge
 * @param dischargeDetails Any additional discharge details
 * @param currentIncentive 
 * @param heightCentimetres Height in centimetres of the offender
 * @param weightKilograms Weight in kilograms of the offender
 * @param hairColour Hair colour. From PROFILE_CODES table where PROFILE_TYPE = HAIR. Allowable values extracted 07/02/2023.
 * @param rightEyeColour Right eye colour. From PROFILE_CODES table where PROFILE_TYPE = R_EYE_C. Allowable values extracted 07/02/2023.
 * @param leftEyeColour Left eye colour. From PROFILE_CODES table where PROFILE_TYPE = L_EYE_C. Allowable values extracted 07/02/2023.
 * @param facialHair Facial hair. From PROFILE_CODES table where PROFILE_TYPE = FACIAL_HAIR. Allowable values extracted 07/02/2023.
 * @param shapeOfFace Shape of face. From PROFILE_CODES table where PROFILE_TYPE = FACE. Allowable values extracted 07/02/2023.
 * @param build Build. From PROFILE_CODES table where PROFILE_TYPE = BUILD. Allowable values extracted 07/02/2023.
 * @param shoeSize UK shoe size
 * @param tattoos List of parts of the body that have tattoos. From REFERENCE_CODES table where DOMAIN = BODY_PART. Allowable values extracted 08/02/2023.
 * @param scars List of parts of the body that have scars. From REFERENCE_CODES table where DOMAIN = BODY_PART. Allowable values extracted 08/02/2023.
 * @param marks List of parts of the body that have marks. From REFERENCE_CODES table where DOMAIN = BODY_PART. Allowable values extracted 08/02/2023.
 * @param otherMarks List of parts of the body that have other marks. From REFERENCE_CODES table where DOMAIN = BODY_PART. Allowable values extracted 08/02/2023.
 */
data class Prisoner(

    @Schema(example = "A1234AA", required = true, description = "Prisoner Number")
    @get:JsonProperty("prisonerNumber", required = true) val prisonerNumber: kotlin.String,

    @Schema(example = "Robert", required = true, description = "First Name")
    @get:JsonProperty("firstName", required = true) val firstName: kotlin.String,

    @Schema(example = "Larsen", required = true, description = "Last name")
    @get:JsonProperty("lastName", required = true) val lastName: kotlin.String,

    @Schema(example = "Wed Apr 02 01:00:00 BST 1975", required = true, description = "Date of Birth")
    @get:JsonProperty("dateOfBirth", required = true) val dateOfBirth: java.time.LocalDate,

    @Schema(example = "Female", required = true, description = "Gender")
    @get:JsonProperty("gender", required = true) val gender: kotlin.String,

    @Schema(example = "White: Eng./Welsh/Scot./N.Irish/British", description = "Ethnicity")
    @get:JsonProperty("ethnicity") val ethnicity: kotlin.String? = null,

    @Schema(example = "true", description = "Youth Offender?")
    @get:JsonProperty("youthOffender") val youthOffender: kotlin.Boolean? = null,

    @Schema(example = "Widowed", description = "Marital Status")
    @get:JsonProperty("maritalStatus") val maritalStatus: kotlin.String? = null,

    @Schema(example = "Church of England (Anglican)", description = "Religion")
    @get:JsonProperty("religion") val religion: kotlin.String? = null,

    @Schema(example = "Egyptian", description = "Nationality")
    @get:JsonProperty("nationality") val nationality: kotlin.String? = null,

    @Schema(example = "ACTIVE IN", required = true, description = "Status of the prisoner")
    @get:JsonProperty("status", required = true) val status: kotlin.String,

    @Schema(example = "Robbery", description = "Most serious offence for this sentence")
    @get:JsonProperty("mostSeriousOffence") val mostSeriousOffence: kotlin.String? = null,

    @Schema(example = "true", description = "Indicates a restricted patient")
    @get:JsonProperty("restrictedPatient") val restrictedPatient: kotlin.Boolean? = null,

    @Schema(example = "12/394773H", description = "PNC Number")
    @get:JsonProperty("pncNumber") val pncNumber: kotlin.String? = null,

    @Schema(example = "12/394773H", description = "PNC Number")
    @get:JsonProperty("pncNumberCanonicalShort") val pncNumberCanonicalShort: kotlin.String? = null,

    @Schema(example = "2012/394773H", description = "PNC Number")
    @get:JsonProperty("pncNumberCanonicalLong") val pncNumberCanonicalLong: kotlin.String? = null,

    @Schema(example = "29906/12J", description = "CRO Number")
    @get:JsonProperty("croNumber") val croNumber: kotlin.String? = null,

    @Schema(example = "0001200924", description = "Booking No.")
    @get:JsonProperty("bookingId") val bookingId: kotlin.String? = null,

    @Schema(example = "38412A", description = "Book Number")
    @get:JsonProperty("bookNumber") val bookNumber: kotlin.String? = null,

    @Schema(example = "John James", description = "Middle Names")
    @get:JsonProperty("middleNames") val middleNames: kotlin.String? = null,

    @Schema(example = "CRT", description = "Last Movement Type Code of prisoner")
    @get:JsonProperty("lastMovementTypeCode") val lastMovementTypeCode: kotlin.String? = null,

    @Schema(example = "CA", description = "Last Movement Reason of prisoner")
    @get:JsonProperty("lastMovementReasonCode") val lastMovementReasonCode: kotlin.String? = null,

    @Schema(example = "IN", description = "In/Out Status")
    @get:JsonProperty("inOutStatus") val inOutStatus: Prisoner.InOutStatus? = null,

    @Schema(example = "MDI", description = "Prison ID")
    @get:JsonProperty("prisonId") val prisonId: kotlin.String? = null,

    @Schema(example = "HMP Leeds", description = "Prison Name")
    @get:JsonProperty("prisonName") val prisonName: kotlin.String? = null,

    @Schema(example = "A-1-002", description = "In prison cell location")
    @get:JsonProperty("cellLocation") val cellLocation: kotlin.String? = null,

    @Schema(example = "null", description = "Aliases Names and Details")
    @get:JsonProperty("aliases") val aliases: kotlin.collections.List<PrisonerAlias>? = null,

    @Schema(example = "null", description = "Alerts")
    @get:JsonProperty("alerts") val alerts: kotlin.collections.List<PrisonerAlert>? = null,

    @Schema(example = "HIGH", description = "Cell Sharing Risk Assessment")
    @get:JsonProperty("csra") val csra: kotlin.String? = null,

    @Schema(example = "C", description = "Prisoner Category")
    @get:JsonProperty("category") val category: kotlin.String? = null,

    @Schema(example = "SENTENCED", description = "Legal Status")
    @get:JsonProperty("legalStatus") val legalStatus: Prisoner.LegalStatus? = null,

    @Schema(example = "LIFE", description = "The prisoner's imprisonment status code.")
    @get:JsonProperty("imprisonmentStatus") val imprisonmentStatus: kotlin.String? = null,

    @Schema(example = "Serving Life Imprisonment", description = "The prisoner's imprisonment status description.")
    @get:JsonProperty("imprisonmentStatusDescription") val imprisonmentStatusDescription: kotlin.String? = null,

    @Schema(example = "false", description = "Indicates that the offender has been recalled")
    @get:JsonProperty("recall") val recall: kotlin.Boolean? = null,

    @Schema(example = "true", description = "Indicates that the offender has an indeterminate sentence")
    @get:JsonProperty("indeterminateSentence") val indeterminateSentence: kotlin.Boolean? = null,

    @Schema(example = "Fri Apr 03 01:00:00 BST 2020", description = "Start Date for this sentence")
    @get:JsonProperty("sentenceStartDate") val sentenceStartDate: java.time.LocalDate? = null,

    @Schema(example = "Tue May 02 01:00:00 BST 2023", description = "Actual of most likely Release Date")
    @get:JsonProperty("releaseDate") val releaseDate: java.time.LocalDate? = null,

    @Schema(example = "Mon May 01 01:00:00 BST 2023", description = "Release Date Confirmed")
    @get:JsonProperty("confirmedReleaseDate") val confirmedReleaseDate: java.time.LocalDate? = null,

    @Schema(example = "Mon May 01 01:00:00 BST 2023", description = "Sentence Expiry Date")
    @get:JsonProperty("sentenceExpiryDate") val sentenceExpiryDate: java.time.LocalDate? = null,

    @Schema(example = "Mon May 01 01:00:00 BST 2023", description = "Licence Expiry Date")
    @get:JsonProperty("licenceExpiryDate") val licenceExpiryDate: java.time.LocalDate? = null,

    @Schema(example = "Mon May 01 01:00:00 BST 2023", description = "HDC Eligibility Date")
    @get:JsonProperty("homeDetentionCurfewEligibilityDate") val homeDetentionCurfewEligibilityDate: java.time.LocalDate? = null,

    @Schema(example = "Mon May 01 01:00:00 BST 2023", description = "HDC Actual Date")
    @get:JsonProperty("homeDetentionCurfewActualDate") val homeDetentionCurfewActualDate: java.time.LocalDate? = null,

    @Schema(example = "Tue May 02 01:00:00 BST 2023", description = "HDC End Date")
    @get:JsonProperty("homeDetentionCurfewEndDate") val homeDetentionCurfewEndDate: java.time.LocalDate? = null,

    @Schema(example = "Sat Apr 29 01:00:00 BST 2023", description = "Top-up supervision start date")
    @get:JsonProperty("topupSupervisionStartDate") val topupSupervisionStartDate: java.time.LocalDate? = null,

    @Schema(example = "Mon May 01 01:00:00 BST 2023", description = "Top-up supervision expiry date")
    @get:JsonProperty("topupSupervisionExpiryDate") val topupSupervisionExpiryDate: java.time.LocalDate? = null,

    @Schema(example = "10", description = "Days added to sentence term due to adjustments.")
    @get:JsonProperty("additionalDaysAwarded") val additionalDaysAwarded: kotlin.Int? = null,

    @Schema(example = "Mon May 01 01:00:00 BST 2023", description = "Release date for Non determinant sentence (if applicable). This will be based on one of ARD, CRD, NPD or PRRD.")
    @get:JsonProperty("nonDtoReleaseDate") val nonDtoReleaseDate: java.time.LocalDate? = null,

    @Schema(example = "ARD", description = "Indicates which type of non-DTO release date is the effective release date. One of 'ARD’, 'CRD’, ‘NPD’ or 'PRRD’.")
    @get:JsonProperty("nonDtoReleaseDateType") val nonDtoReleaseDateType: Prisoner.NonDtoReleaseDateType? = null,

    @Schema(example = "Mon May 01 01:00:00 BST 2023", description = "Date prisoner was received into the prison")
    @get:JsonProperty("receptionDate") val receptionDate: java.time.LocalDate? = null,

    @Schema(example = "Mon May 01 01:00:00 BST 2023", description = "Parole  Eligibility Date")
    @get:JsonProperty("paroleEligibilityDate") val paroleEligibilityDate: java.time.LocalDate? = null,

    @Schema(example = "Mon May 01 01:00:00 BST 2023", description = "Automatic Release Date. If automaticReleaseOverrideDate is available then it will be set as automaticReleaseDate")
    @get:JsonProperty("automaticReleaseDate") val automaticReleaseDate: java.time.LocalDate? = null,

    @Schema(example = "Mon May 01 01:00:00 BST 2023", description = "Post Recall Release Date. if postRecallReleaseOverrideDate is available then it will be set as postRecallReleaseDate")
    @get:JsonProperty("postRecallReleaseDate") val postRecallReleaseDate: java.time.LocalDate? = null,

    @Schema(example = "Mon May 01 01:00:00 BST 2023", description = "Conditional Release Date. If conditionalReleaseOverrideDate is available then it will be set as conditionalReleaseDate")
    @get:JsonProperty("conditionalReleaseDate") val conditionalReleaseDate: java.time.LocalDate? = null,

    @Schema(example = "Mon May 01 01:00:00 BST 2023", description = "Actual Parole Date")
    @get:JsonProperty("actualParoleDate") val actualParoleDate: java.time.LocalDate? = null,

    @Schema(example = "Mon May 01 01:00:00 BST 2023", description = "Tariff Date")
    @get:JsonProperty("tariffDate") val tariffDate: java.time.LocalDate? = null,

    @Schema(example = "Outside - released from Leeds", description = "current prison or outside with last movement information.")
    @get:JsonProperty("locationDescription") val locationDescription: kotlin.String? = null,

    @Schema(example = "LEI", description = "Supporting prison ID for POM")
    @get:JsonProperty("supportingPrisonId") val supportingPrisonId: kotlin.String? = null,

    @Schema(example = "HAZLWD", description = "Which hospital the offender has been discharged to")
    @get:JsonProperty("dischargedHospitalId") val dischargedHospitalId: kotlin.String? = null,

    @Schema(example = "Hazelwood House", description = "Hospital name to which the offender was discharged")
    @get:JsonProperty("dischargedHospitalDescription") val dischargedHospitalDescription: kotlin.String? = null,

    @Schema(example = "Fri May 01 01:00:00 BST 2020", description = "Date of discharge")
    @get:JsonProperty("dischargeDate") val dischargeDate: java.time.LocalDate? = null,

    @Schema(example = "Psychiatric Hospital Discharge to Hazelwood House", description = "Any additional discharge details")
    @get:JsonProperty("dischargeDetails") val dischargeDetails: kotlin.String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("currentIncentive") val currentIncentive: CurrentIncentive? = null,

    @Schema(example = "200", description = "Height in centimetres of the offender")
    @get:JsonProperty("heightCentimetres") val heightCentimetres: kotlin.Int? = null,

    @Schema(example = "102", description = "Weight in kilograms of the offender")
    @get:JsonProperty("weightKilograms") val weightKilograms: kotlin.Int? = null,

    @Schema(example = "Blonde", description = "Hair colour. From PROFILE_CODES table where PROFILE_TYPE = HAIR. Allowable values extracted 07/02/2023.")
    @get:JsonProperty("hairColour") val hairColour: Prisoner.HairColour? = null,

    @Schema(example = "Green", description = "Right eye colour. From PROFILE_CODES table where PROFILE_TYPE = R_EYE_C. Allowable values extracted 07/02/2023.")
    @get:JsonProperty("rightEyeColour") val rightEyeColour: Prisoner.RightEyeColour? = null,

    @Schema(example = "Hazel", description = "Left eye colour. From PROFILE_CODES table where PROFILE_TYPE = L_EYE_C. Allowable values extracted 07/02/2023.")
    @get:JsonProperty("leftEyeColour") val leftEyeColour: Prisoner.LeftEyeColour? = null,

    @Schema(example = "Clean Shaven", description = "Facial hair. From PROFILE_CODES table where PROFILE_TYPE = FACIAL_HAIR. Allowable values extracted 07/02/2023.")
    @get:JsonProperty("facialHair") val facialHair: Prisoner.FacialHair? = null,

    @Schema(example = "Round", description = "Shape of face. From PROFILE_CODES table where PROFILE_TYPE = FACE. Allowable values extracted 07/02/2023.")
    @get:JsonProperty("shapeOfFace") val shapeOfFace: Prisoner.ShapeOfFace? = null,

    @Schema(example = "Muscular", description = "Build. From PROFILE_CODES table where PROFILE_TYPE = BUILD. Allowable values extracted 07/02/2023.")
    @get:JsonProperty("build") val build: Prisoner.Build? = null,

    @Schema(example = "10", description = "UK shoe size")
    @get:JsonProperty("shoeSize") val shoeSize: kotlin.Int? = null,

    @Schema(example = "null", description = "List of parts of the body that have tattoos. From REFERENCE_CODES table where DOMAIN = BODY_PART. Allowable values extracted 08/02/2023.")
    @get:JsonProperty("tattoos") val tattoos: kotlin.collections.List<BodyPartDetail>? = null,

    @Schema(example = "null", description = "List of parts of the body that have scars. From REFERENCE_CODES table where DOMAIN = BODY_PART. Allowable values extracted 08/02/2023.")
    @get:JsonProperty("scars") val scars: kotlin.collections.List<BodyPartDetail>? = null,

    @Schema(example = "null", description = "List of parts of the body that have marks. From REFERENCE_CODES table where DOMAIN = BODY_PART. Allowable values extracted 08/02/2023.")
    @get:JsonProperty("marks") val marks: kotlin.collections.List<BodyPartDetail>? = null,

    @Schema(example = "null", description = "List of parts of the body that have other marks. From REFERENCE_CODES table where DOMAIN = BODY_PART. Allowable values extracted 08/02/2023.")
    @get:JsonProperty("otherMarks") val otherMarks: kotlin.collections.List<BodyPartDetail>? = null
) {

    /**
    * In/Out Status
    * Values: IN,OUT,TRN
    */
    enum class InOutStatus(val value: kotlin.String) {

        @JsonProperty("IN") IN("IN"),
        @JsonProperty("OUT") OUT("OUT"),
        @JsonProperty("TRN") TRN("TRN")
    }

    /**
    * Legal Status
    * Values: RECALL,DEAD,INDETERMINATE_SENTENCE,SENTENCED,CONVICTED_UNSENTENCED,CIVIL_PRISONER,IMMIGRATION_DETAINEE,REMAND,UNKNOWN,OTHER
    */
    enum class LegalStatus(val value: kotlin.String) {

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

    /**
    * Indicates which type of non-DTO release date is the effective release date. One of 'ARD’, 'CRD’, ‘NPD’ or 'PRRD’.
    * Values: ARD,CRD,NPD,PRRD
    */
    enum class NonDtoReleaseDateType(val value: kotlin.String) {

        @JsonProperty("ARD") ARD("ARD"),
        @JsonProperty("CRD") CRD("CRD"),
        @JsonProperty("NPD") NPD("NPD"),
        @JsonProperty("PRRD") PRRD("PRRD")
    }

    /**
    * Hair colour. From PROFILE_CODES table where PROFILE_TYPE = HAIR. Allowable values extracted 07/02/2023.
    * Values: BALD,BALDING,BLACK,BLONDE,BROWN,BRUNETTE,DARK,DYED,GINGER,GREY,LIGHT,MOUSE,MULTI_MINUS_COLOURED,RED,WHITE
    */
    enum class HairColour(val value: kotlin.String) {

        @JsonProperty("Bald") BALD("Bald"),
        @JsonProperty("Balding") BALDING("Balding"),
        @JsonProperty("Black") BLACK("Black"),
        @JsonProperty("Blonde") BLONDE("Blonde"),
        @JsonProperty("Brown") BROWN("Brown"),
        @JsonProperty("Brunette") BRUNETTE("Brunette"),
        @JsonProperty("Dark") DARK("Dark"),
        @JsonProperty("Dyed") DYED("Dyed"),
        @JsonProperty("Ginger") GINGER("Ginger"),
        @JsonProperty("Grey") GREY("Grey"),
        @JsonProperty("Light") LIGHT("Light"),
        @JsonProperty("Mouse") MOUSE("Mouse"),
        @JsonProperty("Multi-coloured") MULTI_MINUS_COLOURED("Multi-coloured"),
        @JsonProperty("Red") RED("Red"),
        @JsonProperty("White") WHITE("White")
    }

    /**
    * Right eye colour. From PROFILE_CODES table where PROFILE_TYPE = R_EYE_C. Allowable values extracted 07/02/2023.
    * Values: BLUE,BROWN,CLOUDED,GREEN,GREY,HAZEL,MISSING,PINK,WHITE
    */
    enum class RightEyeColour(val value: kotlin.String) {

        @JsonProperty("Blue") BLUE("Blue"),
        @JsonProperty("Brown") BROWN("Brown"),
        @JsonProperty("Clouded") CLOUDED("Clouded"),
        @JsonProperty("Green") GREEN("Green"),
        @JsonProperty("Grey") GREY("Grey"),
        @JsonProperty("Hazel") HAZEL("Hazel"),
        @JsonProperty("Missing") MISSING("Missing"),
        @JsonProperty("Pink") PINK("Pink"),
        @JsonProperty("White") WHITE("White")
    }

    /**
    * Left eye colour. From PROFILE_CODES table where PROFILE_TYPE = L_EYE_C. Allowable values extracted 07/02/2023.
    * Values: BLUE,BROWN,CLOUDED,GREEN,GREY,HAZEL,MISSING,PINK,WHITE
    */
    enum class LeftEyeColour(val value: kotlin.String) {

        @JsonProperty("Blue") BLUE("Blue"),
        @JsonProperty("Brown") BROWN("Brown"),
        @JsonProperty("Clouded") CLOUDED("Clouded"),
        @JsonProperty("Green") GREEN("Green"),
        @JsonProperty("Grey") GREY("Grey"),
        @JsonProperty("Hazel") HAZEL("Hazel"),
        @JsonProperty("Missing") MISSING("Missing"),
        @JsonProperty("Pink") PINK("Pink"),
        @JsonProperty("White") WHITE("White")
    }

    /**
    * Facial hair. From PROFILE_CODES table where PROFILE_TYPE = FACIAL_HAIR. Allowable values extracted 07/02/2023.
    * Values: FULL_BEARD,CLEAN_SHAVEN,GOATEE_BEARD,MOUSTACHE_ONLY,NOT_APPLICABLE_LEFT_PARENTHESIS_FEMALE_OFFENDER_RIGHT_PARENTHESIS,NO_FACIAL_HAIR,SIDEBURNS
    */
    enum class FacialHair(val value: kotlin.String) {

        @JsonProperty("Full Beard") FULL_BEARD("Full Beard"),
        @JsonProperty("Clean Shaven") CLEAN_SHAVEN("Clean Shaven"),
        @JsonProperty("Goatee Beard") GOATEE_BEARD("Goatee Beard"),
        @JsonProperty("Moustache Only") MOUSTACHE_ONLY("Moustache Only"),
        @JsonProperty("Not Applicable (Female Offender)") NOT_APPLICABLE_LEFT_PARENTHESIS_FEMALE_OFFENDER_RIGHT_PARENTHESIS("Not Applicable (Female Offender)"),
        @JsonProperty("No Facial Hair") NO_FACIAL_HAIR("No Facial Hair"),
        @JsonProperty("Sideburns") SIDEBURNS("Sideburns"),
        @JsonProperty("Not Asked") NOT_ASKED("Not Asked")
    }

    /**
    * Shape of face. From PROFILE_CODES table where PROFILE_TYPE = FACE. Allowable values extracted 07/02/2023.
    * Values: ANGULAR,BULLET,OVAL,ROUND,SQUARE,TRIANGULAR
    */
    enum class ShapeOfFace(val value: kotlin.String) {

        @JsonProperty("Angular") ANGULAR("Angular"),
        @JsonProperty("Bullet") BULLET("Bullet"),
        @JsonProperty("Oval") OVAL("Oval"),
        @JsonProperty("Round") ROUND("Round"),
        @JsonProperty("Square") SQUARE("Square"),
        @JsonProperty("Triangular") TRIANGULAR("Triangular")
    }

    /**
    * Build. From PROFILE_CODES table where PROFILE_TYPE = BUILD. Allowable values extracted 07/02/2023.
    * Values: FAT,FRAIL,HEAVY,MEDIUM,MUSCULAR,OBESE,PROPORTIONAL,SLIGHT,SMALL,STOCKY,STOOPED,THIN
    */
    enum class Build(val value: kotlin.String) {

        @JsonProperty("Fat") FAT("Fat"),
        @JsonProperty("Frail") FRAIL("Frail"),
        @JsonProperty("Heavy") HEAVY("Heavy"),
        @JsonProperty("Medium") MEDIUM("Medium"),
        @JsonProperty("Muscular") MUSCULAR("Muscular"),
        @JsonProperty("Obese") OBESE("Obese"),
        @JsonProperty("Proportional") PROPORTIONAL("Proportional"),
        @JsonProperty("Slight") SLIGHT("Slight"),
        @JsonProperty("Small") SMALL("Small"),
        @JsonProperty("Stocky") STOCKY("Stocky"),
        @JsonProperty("Stooped") STOOPED("Stooped"),
        @JsonProperty("Thin") THIN("Thin")
    }

}

