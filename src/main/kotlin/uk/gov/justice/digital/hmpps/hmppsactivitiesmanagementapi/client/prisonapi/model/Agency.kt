package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid

/**
 * Agency Details
 * @param agencyId Agency identifier.
 * @param description Agency description.
 * @param agencyType Agency type.  Reference domain is AGY_LOC_TYPE
 * @param active Indicates the Agency is active
 * @param longDescription Long description of the agency
 * @param courtType Court Type.  Reference domain is JURISDICTION
 * @param deactivationDate Date agency became inactive
 * @param addresses List of addresses associated with agency
 * @param phones List of phones associated with agency
 * @param emails List of emails associated with agency
 */
data class Agency(

  @Schema(example = "MDI", description = "Agency identifier.")
  @JsonProperty("agencyId")
  val agencyId: String,

  @Schema(example = "Moorland (HMP & YOI)", description = "Agency description.")
  @JsonProperty("description")
  val description: String,

  @Schema(example = "INST", description = "Agency type.  Reference domain is AGY_LOC_TYPE")
  @JsonProperty("agencyType")
  val agencyType: AgencyType,

  @Schema(example = "true", description = "Indicates the Agency is active")
  @JsonProperty("active")
  val active: Boolean,

  @Schema(example = "Moorland (HMP & YOI)", description = "Long description of the agency")
  @JsonProperty("longDescription")
  val longDescription: String? = null,

  @Schema(example = "CC", description = "Court Type.  Reference domain is JURISDICTION")
  @JsonProperty("courtType")
  val courtType: CourtType? = null,

  @Valid
  @Schema(example = "Thu Jan 12 00:00:00 GMT 2012", description = "Date agency became inactive")
  @JsonProperty("deactivationDate")
  val deactivationDate: java.time.LocalDate? = null,

  @Valid
  @Schema(example = "null", description = "List of addresses associated with agency")
  @JsonProperty("addresses")
  val addresses: List<AddressDto>? = null,

  @Valid
  @Schema(example = "null", description = "List of phones associated with agency")
  @JsonProperty("phones")
  val phones: List<Telephone>? = null,

  @Valid
  @Schema(example = "null", description = "List of emails associated with agency")
  @JsonProperty("emails")
  val emails: List<Email>? = null,
) {

  /**
   * Agency type.  Reference domain is AGY_LOC_TYPE
   * Values: cRC,pOLSTN,iNST,cOMM,aPPR,cRT,pOLICE,iMDC,tRN,oUT,yOT,sCH,sTC,hOST,aIRPORT,hSHOSP,hOSPITAL,pECS,pAR,pNP,pSY
   */
  enum class AgencyType(val value: String) {

    @JsonProperty("CRC")
    CRC("CRC"),

    @JsonProperty("POLSTN")
    POLSTN("POLSTN"),

    @JsonProperty("INST")
    INST("INST"),

    @JsonProperty("COMM")
    COMM("COMM"),

    @JsonProperty("APPR")
    APPR("APPR"),

    @JsonProperty("CRT")
    CRT("CRT"),

    @JsonProperty("POLICE")
    POLICE("POLICE"),

    @JsonProperty("IMDC")
    IMDC("IMDC"),

    @JsonProperty("TRN")
    TRN("TRN"),

    @JsonProperty("OUT")
    OUT("OUT"),

    @JsonProperty("YOT")
    YOT("YOT"),

    @JsonProperty("SCH")
    SCH("SCH"),

    @JsonProperty("STC")
    STC("STC"),

    @JsonProperty("HOST")
    HOST("HOST"),

    @JsonProperty("AIRPORT")
    AIRPORT("AIRPORT"),

    @JsonProperty("HSHOSP")
    HSHOSP("HSHOSP"),

    @JsonProperty("HOSPITAL")
    HOSPITAL("HOSPITAL"),

    @JsonProperty("PECS")
    PECS("PECS"),

    @JsonProperty("PAR")
    PAR("PAR"),

    @JsonProperty("PNP")
    PNP("PNP"),

    @JsonProperty("PSY")
    PSY("PSY"),
  }

  /**
   * Court Type.  Reference domain is JURISDICTION
   * Values: cACD,cB,cC,cO,dCM,gCM,iMM,mC,oTHER,yC
   */
  enum class CourtType(val value: String) {

    @JsonProperty("CACD")
    CACD("CACD"),

    @JsonProperty("CB")
    CB("CB"),

    @JsonProperty("CC")
    CC("CC"),

    @JsonProperty("CO")
    CO("CO"),

    @JsonProperty("DCM")
    DCM("DCM"),

    @JsonProperty("GCM")
    GCM("GCM"),

    @JsonProperty("IMM")
    IMM("IMM"),

    @JsonProperty("MC")
    MC("MC"),

    @JsonProperty("OTHER")
    OTHER("OTHER"),

    @JsonProperty("YC")
    YC("YC"),
  }
}
