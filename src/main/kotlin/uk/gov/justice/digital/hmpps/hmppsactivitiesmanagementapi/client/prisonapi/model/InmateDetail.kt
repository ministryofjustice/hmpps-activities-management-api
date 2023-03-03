package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid

/**
 * Inmate Detail
 * @param offenderNo Offender Unique Reference
 * @param offenderId Internal Offender ID
 * @param rootOffenderId Internal Root Offender ID
 * @param firstName First Name
 * @param lastName Last Name
 * @param dateOfBirth Date of Birth of prisoner
 * @param activeFlag Indicates that the person is currently in prison
 * @param inOutStatus In/Out Status
 * @param status Status of prisoner
 * @param bookingId Offender Booking Id
 * @param bookingNo Booking Number
 * @param middleName Middle Name(s)
 * @param age Age of prisoner. Note: Full Details Only
 * @param facialImageId Image Id Ref of prisoner
 * @param agencyId Identifier of agency to which the prisoner is associated.
 * @param assignedLivingUnitId Identifier of living unit (e.g. cell) that prisoner is assigned to.
 * @param religion Religion of the prisoner
 * @param language Preferred spoken language
 * @param interpreterRequired Interpreter required
 * @param writtenLanguage Preferred written language
 * @param alertsCodes List of Alerts
 * @param activeAlertCount number of active alerts. Note: Full Details Only
 * @param inactiveAlertCount number of inactive alerts. Note: Full Details Only
 * @param alerts List of alert details
 * @param assignedLivingUnit
 * @param physicalAttributes
 * @param physicalCharacteristics List of physical characteristics
 * @param profileInformation List of profile information
 * @param physicalMarks List of physical marks
 * @param assessments List of assessments
 * @param csra CSRA (Latest assessment with cellSharing=true from list of assessments)
 * @param csraClassificationCode The CSRA classification (calculated from the list of CSRA assessments)
 * @param csraClassificationDate The date that the csraClassificationCode was assessed
 * @param category Category description (from list of assessments)
 * @param categoryCode Category code (from list of assessments)
 * @param birthPlace Place of birth
 * @param birthCountryCode Country of birth
 * @param identifiers Identifiers. Note: Only returned when requesting extra details
 * @param personalCareNeeds Personal Care Needs. Note: Only returned when requesting extra details
 * @param sentenceDetail
 * @param offenceHistory Offence History. Note: Only returned when requesting extra details
 * @param sentenceTerms Current Sentence Terms. Note: Only returned when requesting extra details
 * @param aliases Aliases. Note: Only returned when requesting extra details
 * @param statusReason Last movement status of the prison
 * @param lastMovementTypeCode Last Movement Type Code of prisoner. Note: Reference Data from MOVE_TYPE Domain
 * @param lastMovementReasonCode Last Movement Reason of prisoner. Note: Reference Data from MOVE_RSN Domain
 * @param legalStatus Legal Status. Note: Only returned when requesting extra details
 * @param recall Recall. Note: Only returned when requesting extra details
 * @param imprisonmentStatus The prisoner's imprisonment status. Note: Only returned when requesting extra details
 * @param imprisonmentStatusDescription The prisoner's imprisonment status description. Note: Only returned when requesting extra details
 * @param privilegeSummary
 * @param receptionDate Date prisoner was received into the prison.
 * @param locationDescription current prison or outside with last movement information.
 * @param latestLocationId the current prison id or the last prison before release
 */
data class InmateDetail(

  @Schema(example = "A1234AA", description = "Offender Unique Reference")
  @JsonProperty("offenderNo")
  val offenderNo: String,

  @Schema(example = "null", description = "Internal Offender ID")
  @JsonProperty("offenderId")
  val offenderId: Long,

  @Schema(example = "null", description = "Internal Root Offender ID")
  @JsonProperty("rootOffenderId")
  val rootOffenderId: Long,

  @Schema(example = "null", description = "First Name")
  @JsonProperty("firstName")
  val firstName: String,

  @Schema(example = "null", description = "Last Name")
  @JsonProperty("lastName")
  val lastName: String,

  @Valid
  @Schema(example = "Sun Mar 15 01:00:00 GMT 1970", description = "Date of Birth of prisoner")
  @JsonProperty("dateOfBirth")
  val dateOfBirth: java.time.LocalDate,

  @Schema(example = "null", description = "Indicates that the person is currently in prison")
  @JsonProperty("activeFlag")
  val activeFlag: Boolean,

  @Schema(example = "IN", description = "In/Out Status")
  @JsonProperty("inOutStatus")
  val inOutStatus: InOutStatus?,

  @Schema(example = "ACTIVE IN", description = "Status of prisoner")
  @JsonProperty("status")
  val status: Status?,

  @Schema(example = "432132", description = "Offender Booking Id")
  @JsonProperty("bookingId")
  val bookingId: Long? = null,

  @Schema(example = "null", description = "Booking Number")
  @JsonProperty("bookingNo")
  val bookingNo: String? = null,

  @Schema(example = "null", description = "Middle Name(s)")
  @JsonProperty("middleName")
  val middleName: String? = null,

  @Schema(example = "null", description = "Age of prisoner. Note: Full Details Only")
  @JsonProperty("age")
  val age: Int? = null,

  @Schema(example = "null", description = "Image Id Ref of prisoner")
  @JsonProperty("facialImageId")
  val facialImageId: Long? = null,

  @Schema(example = "null", description = "Identifier of agency to which the prisoner is associated.")
  @JsonProperty("agencyId")
  val agencyId: String? = null,

  @Schema(example = "null", description = "Identifier of living unit (e.g. cell) that prisoner is assigned to.")
  @JsonProperty("assignedLivingUnitId")
  val assignedLivingUnitId: Long? = null,

  @Schema(example = "null", description = "Religion of the prisoner")
  @JsonProperty("religion")
  val religion: String? = null,

  @Schema(example = "null", description = "Preferred spoken language")
  @JsonProperty("language")
  val language: String? = null,

  @Schema(example = "null", description = "Interpreter required")
  @JsonProperty("interpreterRequired")
  val interpreterRequired: Boolean? = null,

  @Schema(example = "null", description = "Preferred written language")
  @JsonProperty("writtenLanguage")
  val writtenLanguage: String? = null,

  @Schema(example = "null", description = "List of Alerts")
  @JsonProperty("alertsCodes")
  val alertsCodes: List<String>? = null,

  @Schema(example = "null", description = "number of active alerts. Note: Full Details Only")
  @JsonProperty("activeAlertCount")
  val activeAlertCount: Long? = null,

  @Schema(example = "null", description = "number of inactive alerts. Note: Full Details Only")
  @JsonProperty("inactiveAlertCount")
  val inactiveAlertCount: Long? = null,

  @Valid
  @Schema(example = "null", description = "List of alert details")
  @JsonProperty("alerts")
  val alerts: List<Alert>? = null,

  @Valid
  @Schema(example = "null", description = "")
  @JsonProperty("assignedLivingUnit")
  val assignedLivingUnit: AssignedLivingUnit? = null,

  @Valid
  @Schema(example = "null", description = "")
  @JsonProperty("physicalAttributes")
  val physicalAttributes: PhysicalAttributes? = null,

  @Valid
  @Schema(example = "null", description = "List of physical characteristics")
  @JsonProperty("physicalCharacteristics")
  val physicalCharacteristics: List<PhysicalCharacteristic>? = null,

  @Valid
  @Schema(example = "null", description = "List of profile information")
  @JsonProperty("profileInformation")
  val profileInformation: List<ProfileInformation>? = null,

  @Valid
  @Schema(example = "null", description = "List of physical marks")
  @JsonProperty("physicalMarks")
  val physicalMarks: List<PhysicalMark>? = null,

  @Valid
  @Schema(example = "null", description = "List of assessments")
  @JsonProperty("assessments")
  val assessments: List<Assessment>? = null,

  @Schema(example = "null", description = "CSRA (Latest assessment with cellSharing=true from list of assessments)")
  @JsonProperty("csra")
  val csra: String? = null,

  @Schema(example = "STANDARD", description = "The CSRA classification (calculated from the list of CSRA assessments)")
  @JsonProperty("csraClassificationCode")
  val csraClassificationCode: String? = null,

  @Valid
  @Schema(example = "null", description = "The date that the csraClassificationCode was assessed")
  @JsonProperty("csraClassificationDate")
  val csraClassificationDate: java.time.LocalDate? = null,

  @Schema(example = "null", description = "Category description (from list of assessments)")
  @JsonProperty("category")
  val category: String? = null,

  @Schema(example = "null", description = "Category code (from list of assessments)")
  @JsonProperty("categoryCode")
  val categoryCode: String? = null,

  @Schema(example = "WALES", description = "Place of birth")
  @JsonProperty("birthPlace")
  val birthPlace: String? = null,

  @Schema(example = "GBR", description = "Country of birth")
  @JsonProperty("birthCountryCode")
  val birthCountryCode: String? = null,

  @Valid
  @Schema(example = "null", description = "Identifiers. Note: Only returned when requesting extra details")
  @JsonProperty("identifiers")
  val identifiers: List<OffenderIdentifier>? = null,

  @Valid
  @Schema(example = "null", description = "Personal Care Needs. Note: Only returned when requesting extra details")
  @JsonProperty("personalCareNeeds")
  val personalCareNeeds: List<PersonalCareNeed>? = null,

  @Valid
  @Schema(example = "null", description = "")
  @JsonProperty("sentenceDetail")
  val sentenceDetail: SentenceCalcDates? = null,

  @Valid
  @Schema(example = "null", description = "Offence History. Note: Only returned when requesting extra details")
  @JsonProperty("offenceHistory")
  val offenceHistory: List<OffenceHistoryDetail>? = null,

  @Valid
  @Schema(example = "null", description = "Current Sentence Terms. Note: Only returned when requesting extra details")
  @JsonProperty("sentenceTerms")
  val sentenceTerms: List<OffenderSentenceTerms>? = null,

  @Valid
  @Schema(example = "null", description = "Aliases. Note: Only returned when requesting extra details")
  @JsonProperty("aliases")
  val aliases: List<Alias>? = null,

  @Schema(example = "CRT-CA", description = "Last movement status of the prison")
  @JsonProperty("statusReason")
  val statusReason: String? = null,

  @Schema(
    example = "TAP",
    description = "Last Movement Type Code of prisoner. Note: Reference Data from MOVE_TYPE Domain",
  )
  @JsonProperty("lastMovementTypeCode")
  val lastMovementTypeCode: LastMovementTypeCode? = null,

  @Schema(example = "CA", description = "Last Movement Reason of prisoner. Note: Reference Data from MOVE_RSN Domain")
  @JsonProperty("lastMovementReasonCode")
  val lastMovementReasonCode: String? = null,

  @Schema(example = "REMAND", description = "Legal Status. Note: Only returned when requesting extra details")
  @JsonProperty("legalStatus")
  val legalStatus: LegalStatus? = null,

  @Schema(example = "true", description = "Recall. Note: Only returned when requesting extra details")
  @JsonProperty("recall")
  val recall: Boolean? = null,

  @Schema(
    example = "LIFE",
    description = "The prisoner's imprisonment status. Note: Only returned when requesting extra details",
  )
  @JsonProperty("imprisonmentStatus")
  val imprisonmentStatus: String? = null,

  @Schema(
    example = "Serving Life Imprisonment",
    description = "The prisoner's imprisonment status description. Note: Only returned when requesting extra details",
  )
  @JsonProperty("imprisonmentStatusDescription")
  val imprisonmentStatusDescription: String? = null,

  @Valid
  @Schema(example = "null", description = "")
  @JsonProperty("privilegeSummary")
  val privilegeSummary: PrivilegeSummary? = null,

  @Valid
  @Schema(example = "Tue Jan 01 00:00:00 GMT 1980", description = "Date prisoner was received into the prison.")
  @JsonProperty("receptionDate")
  val receptionDate: java.time.LocalDate? = null,

  @Schema(
    example = "Outside - released from Leeds",
    description = "current prison or outside with last movement information.",
  )
  @JsonProperty("locationDescription")
  val locationDescription: String? = null,

  @Schema(example = "MDI", description = "the current prison id or the last prison before release")
  @JsonProperty("latestLocationId")
  val latestLocationId: String? = null,
) {

  /**
   * In/Out Status
   * Values: iN,oUT,tRN
   */
  enum class InOutStatus(val value: String) {

    @JsonProperty("IN")
    IN("IN"),

    @JsonProperty("OUT")
    OUT("OUT"),

    @JsonProperty("TRN")
    TRN("TRN"),
  }

  /**
   * Status of prisoner
   * Values: iN,oUT
   */
  enum class Status(val value: String) {

    @JsonProperty("ACTIVE IN")
    IN("ACTIVE IN"),

    @JsonProperty("ACTIVE OUT")
    OUT("ACTIVE OUT"),
  }

  /**
   * Last Movement Type Code of prisoner. Note: Reference Data from MOVE_TYPE Domain
   * Values: tAP,cRT,tRN,aDM,rEL
   */
  enum class LastMovementTypeCode(val value: String) {

    @JsonProperty("TAP")
    TAP("TAP"),

    @JsonProperty("CRT")
    CRT("CRT"),

    @JsonProperty("TRN")
    TRN("TRN"),

    @JsonProperty("ADM")
    ADM("ADM"),

    @JsonProperty("REL")
    REL("REL"),
  }

  /**
   * Legal Status. Note: Only returned when requesting extra details
   * Values: rECALL,dEAD,iNDETERMINATESENTENCE,sENTENCED,cONVICTEDUNSENTENCED,cIVILPRISONER,iMMIGRATIONDETAINEE,rEMAND,uNKNOWN,oTHER
   */
  enum class LegalStatus(val value: String) {

    @JsonProperty("RECALL")
    RECALL("RECALL"),

    @JsonProperty("DEAD")
    DEAD("DEAD"),

    @JsonProperty("INDETERMINATE_SENTENCE")
    INDETERMINATE_SENTENCE("INDETERMINATE_SENTENCE"),

    @JsonProperty("SENTENCED")
    SENTENCED("SENTENCED"),

    @JsonProperty("CONVICTED_UNSENTENCED")
    CONVICTED_UNSENTENCED("CONVICTED_UNSENTENCED"),

    @JsonProperty("CIVIL_PRISONER")
    CIVIL_PRISONER("CIVIL_PRISONER"),

    @JsonProperty("IMMIGRATION_DETAINEE")
    IMMIGRATION_DETAINEE("IMMIGRATION_DETAINEE"),

    @JsonProperty("REMAND")
    REMAND("REMAND"),

    @JsonProperty("UNKNOWN")
    UNKNOWN("UNKNOWN"),

    @JsonProperty("OTHER")
    OTHER("OTHER"),
  }
}
