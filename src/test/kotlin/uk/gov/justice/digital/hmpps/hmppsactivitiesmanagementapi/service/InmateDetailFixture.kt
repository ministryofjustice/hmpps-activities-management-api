package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Alert
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Alias
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Assessment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.AssignedLivingUnit
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.InmateDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.OffenceHistoryDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.OffenderIdentifier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.OffenderSentenceTerms
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.PersonalCareNeed
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.PhysicalAttributes
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.PhysicalCharacteristic
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.PhysicalMark
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.ProfileInformation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.SentenceCalcDates
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.LIVERPOOL_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import java.time.LocalDate

val activeInMoorlandInmate = InmateDetailFixture.instance(agencyId = MOORLAND_PRISON_CODE)
val activeInPentonvilleInmate = InmateDetailFixture.instance(agencyId = PENTONVILLE_PRISON_CODE)
val activeInLiverpoolInmate = InmateDetailFixture.instance(agencyId = LIVERPOOL_PRISON_CODE)

fun InmateDetail.convert(): Prisoner = Prisoner(
  prisonerNumber = this.offenderNo!!,
  firstName = this.firstName!!,
  lastName = this.lastName!!,
  status = "",
  bookingId = this.bookingId?.toString(),
  prisonId = this.agencyId,
)

object InmateDetailFixture {
  fun instance(
    offenderNo: String = "G4793VF",
    offenderId: Long = 11111,
    rootOffenderId: Long = 100011111,
    firstName: String = "Tim",
    lastName: String = "Harrison",
    dateOfBirth: LocalDate = LocalDate.of(1971, 8, 1),
    activeFlag: Boolean = true,
    inOutStatus: String = "IN",
    status: String = "IN",
    bookingId: Long? = 900001,
    bookingNo: String = "BK01",
    middleName: String = "James",
    agencyId: String = "MDI",
    age: Int = 23,
    facialImageId: Long? = 1111222,
    assignedLivingUnitId: Long? = 12345,
    religion: String? = "RELIGION",
    language: String? = "English",
    interpreterRequired: Boolean? = false,
    writtenLanguage: String? = "English",
    alertsCodes: List<String> = listOf("XEL"),
    activeAlertCount: Long? = 0,
    inactiveAlertCount: Long? = 0,
    alerts: List<Alert>? = emptyList(),
    assignedLivingUnit: AssignedLivingUnit? = defaultAssignedLivingUnit(),
    physicalAttributes: PhysicalAttributes? = defaultPhysicalAttributes(),
    physicalCharacteristics: List<PhysicalCharacteristic>? = emptyList(),
    profileInformation: List<ProfileInformation>? = emptyList(),
    physicalMarks: List<PhysicalMark>? = emptyList(),
    assessments: List<Assessment>? = emptyList(),
    csra: String? = "Standard",
    csraClassificationCode: String? = "STANDARD",
    csraClassificationDate: LocalDate? = LocalDate.of(2021, 1, 1),
    category: String? = "C",
    categoryCode: String? = "C",
    birthPlace: String? = "WALES",
    birthCountryCode: String? = "GBR",
    identifiers: List<OffenderIdentifier>? = emptyList(),
    personalCareNeeds: List<PersonalCareNeed>? = emptyList(),
    sentenceDetail: SentenceCalcDates? = defaultSentenceCalcDates(),
    offenceHistory: List<OffenceHistoryDetail>? = emptyList(),
    sentenceTerms: List<OffenderSentenceTerms>? = emptyList(),
    aliases: List<Alias>? = emptyList(),
    statusReason: String? = "CRT-CA",
    lastMovementTypeCode: String? = "TAP",
    lastMovementReasonCode: String? = "CA",
    legalStatus: InmateDetail.LegalStatus? = InmateDetail.LegalStatus.SENTENCED,
    recall: Boolean? = false,
    imprisonmentStatus: String? = "LIFE",
    imprisonmentStatusDescription: String? = "Serving Life Imprisonment",
    receptionDate: LocalDate? = LocalDate.of(2020, 1, 1),
    locationDescription: String? = "Outside - released from Leeds",
    latestLocationId: String? = "MDI",
  ) = InmateDetail(
    offenderNo = offenderNo,
    offenderId = offenderId,
    rootOffenderId = rootOffenderId,
    firstName = firstName,
    lastName = lastName,
    dateOfBirth = dateOfBirth,
    activeFlag = activeFlag,
    inOutStatus = inOutStatus,
    status = status,
    bookingId = bookingId,
    bookingNo = bookingNo,
    middleName = middleName,
    agencyId = agencyId,
    age = age,
    facialImageId = facialImageId,
    assignedLivingUnitId = assignedLivingUnitId,
    religion = religion,
    language = language,
    interpreterRequired = interpreterRequired,
    writtenLanguage = writtenLanguage,
    alertsCodes = alertsCodes,
    activeAlertCount = activeAlertCount,
    inactiveAlertCount = inactiveAlertCount,
    alerts = alerts,
    assignedLivingUnit = assignedLivingUnit,
    physicalAttributes = physicalAttributes,
    physicalCharacteristics = physicalCharacteristics,
    profileInformation = profileInformation,
    physicalMarks = physicalMarks,
    assessments = assessments,
    csra = csra,
    csraClassificationCode = csraClassificationCode,
    csraClassificationDate = csraClassificationDate,
    category = category,
    categoryCode = categoryCode,
    birthPlace = birthPlace,
    birthCountryCode = birthCountryCode,
    identifiers = identifiers,
    personalCareNeeds = personalCareNeeds,
    sentenceDetail = sentenceDetail,
    offenceHistory = offenceHistory,
    sentenceTerms = sentenceTerms,
    aliases = aliases,
    statusReason = statusReason,
    lastMovementTypeCode = lastMovementTypeCode,
    lastMovementReasonCode = lastMovementReasonCode,
    legalStatus = legalStatus,
    recall = recall,
    imprisonmentStatus = imprisonmentStatus,
    imprisonmentStatusDescription = imprisonmentStatusDescription,
    receptionDate = receptionDate,
    locationDescription = locationDescription,
    latestLocationId = latestLocationId,
  )

  fun defaultSentenceCalcDates() = SentenceCalcDates(
    bookingId = 900001,
    sentenceStartDate = LocalDate.of(2010, 2, 3),
    sentenceExpiryDate = LocalDate.of(2025, 2, 3),
    sentenceExpiryCalculatedDate = LocalDate.of(2025, 2, 3),
    sentenceExpiryOverrideDate = LocalDate.of(2025, 2, 3),
    automaticReleaseDate = LocalDate.of(2022, 4, 1),
    automaticReleaseOverrideDate = LocalDate.of(2022, 4, 1),
    conditionalReleaseDate = LocalDate.of(2022, 4, 1),
    conditionalReleaseOverrideDate = LocalDate.of(2022, 4, 1),
    nonParoleDate = LocalDate.of(2021, 6, 1),
    nonParoleOverrideDate = LocalDate.of(2021, 6, 1),
    postRecallReleaseDate = LocalDate.of(2022, 5, 1),
    postRecallReleaseOverrideDate = LocalDate.of(2022, 5, 1),
    licenceExpiryDate = LocalDate.of(2025, 2, 3),
    licenceExpiryCalculatedDate = LocalDate.of(2025, 2, 3),
    licenceExpiryOverrideDate = LocalDate.of(2025, 2, 3),
    homeDetentionCurfewEligibilityDate = LocalDate.of(2021, 1, 1),
    homeDetentionCurfewEligibilityCalculatedDate = LocalDate.of(2021, 1, 1),
    homeDetentionCurfewEligibilityOverrideDate = LocalDate.of(2021, 1, 1),
    homeDetentionCurfewActualDate = LocalDate.of(2021, 2, 1),
    homeDetentionCurfewEndDate = LocalDate.of(2022, 3, 31),
    paroleEligibilityDate = LocalDate.of(2021, 3, 1),
    paroleEligibilityCalculatedDate = LocalDate.of(2021, 3, 1),
    paroleEligibilityOverrideDate = LocalDate.of(2021, 3, 1),
    actualParoleDate = LocalDate.of(2021, 4, 1),
    releaseOnTemporaryLicenceDate = LocalDate.of(2021, 5, 1),
    earlyRemovalSchemeEligibilityDate = LocalDate.of(2021, 6, 1),
    earlyTermDate = LocalDate.of(2021, 7, 1),
    midTermDate = LocalDate.of(2021, 8, 1),
    lateTermDate = LocalDate.of(2021, 9, 1),
    topupSupervisionExpiryDate = LocalDate.of(2025, 6, 1),
    topupSupervisionExpiryCalculatedDate = LocalDate.of(2025, 6, 1),
    topupSupervisionExpiryOverrideDate = LocalDate.of(2025, 6, 1),
    topupSupervisionStartDate = LocalDate.of(2025, 2, 4),
    tariffDate = LocalDate.of(2030, 1, 1),
    tariffEarlyRemovalSchemeEligibilityDate = LocalDate.of(2029, 1, 1),
    dtoPostRecallReleaseDate = LocalDate.of(2022, 6, 1),
    dtoPostRecallReleaseDateOverride = LocalDate.of(2022, 6, 1),
    effectiveSentenceEndDate = LocalDate.of(2025, 2, 3),
    additionalDaysAwarded = 5,
    nonDtoReleaseDate = LocalDate.of(2022, 4, 1),
    nonDtoReleaseDateType = SentenceCalcDates.NonDtoReleaseDateType.CRD,
    confirmedReleaseDate = LocalDate.of(2022, 4, 1),
    releaseDate = LocalDate.of(2022, 4, 1),
    etdOverrideDate = LocalDate.of(2021, 7, 1),
    etdCalculatedDate = LocalDate.of(2021, 7, 1),
    mtdOverrideDate = LocalDate.of(2021, 8, 1),
    mtdCalculatedDate = LocalDate.of(2021, 8, 1),
    ltdOverrideDate = LocalDate.of(2021, 9, 1),
    ltdCalculatedDate = LocalDate.of(2021, 9, 1),
  )

  fun defaultPhysicalAttributes() = PhysicalAttributes(
    sexCode = "M",
    gender = "Male",
    raceCode = "W1",
    ethnicity = "White: Eng./Welsh/Scot./N.Irish/British",
    heightFeet = 5,
    heightInches = 60,
    heightMetres = java.math.BigDecimal("1.76"),
    heightCentimetres = 176,
    weightPounds = 150,
    weightKilograms = 68,
  )

  fun defaultAssignedLivingUnit() = AssignedLivingUnit(
    agencyId = "MDI",
    locationId = 12345,
    description = "1-2-003",
    agencyName = "Moorland",
  )
}
