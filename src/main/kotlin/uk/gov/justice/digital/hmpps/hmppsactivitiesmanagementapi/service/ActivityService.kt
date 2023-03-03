package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.toModelLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityTierRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.EligibilityRuleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PrisonPayBandRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowIllegalArgument
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transform
import java.time.LocalDate
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Activity as ModelActivity

@Service
class ActivityService(
  private val activityRepository: ActivityRepository,
  private val activityCategoryRepository: ActivityCategoryRepository,
  private val activityTierRepository: ActivityTierRepository,
  private val eligibilityRuleRepository: EligibilityRuleRepository,
  private val activityScheduleRepository: ActivityScheduleRepository,
  private val prisonPayBandRepository: PrisonPayBandRepository,
  private val prisonApiClient: PrisonApiClient,
) {
  fun getActivityById(activityId: Long) =
    transform(
      activityRepository.findOrThrowNotFound(activityId),
    )

  fun getActivitiesByCategoryInPrison(
    prisonCode: String,
    categoryId: Long,
  ) =
    activityCategoryRepository.findOrThrowNotFound(categoryId).let {
      activityRepository.getAllByPrisonCodeAndActivityCategory(prisonCode, it).toModelLite()
    }

  fun getActivitiesInPrison(
    prisonCode: String,
  ) = activityRepository.getAllByPrisonCode(prisonCode).toModelLite()

  fun getSchedulesForActivity(activityId: Long) =
    activityRepository.findOrThrowNotFound(activityId)
      .let { activityScheduleRepository.getAllByActivity(it).toModelLite() }

  private fun failDuplicateActivity(prisonCode: String, summary: String) {
    val duplicateActivity = activityRepository.existsActivityByPrisonCodeAndSummary(prisonCode, summary)
    if (duplicateActivity) {
      throw IllegalArgumentException("Duplicate activity name detected for this prison ($prisonCode): '$summary'")
    }
  }

  @PreAuthorize("hasAnyRole('ACTIVITY_HUB', 'ACTIVITY_HUB_LEAD', 'ACTIVITY_ADMIN')")
  fun createActivity(request: ActivityCreateRequest, createdBy: String): ModelActivity {
    val category = activityCategoryRepository.findOrThrowIllegalArgument(request.categoryId!!)
    val tier = request.tierId?.let { activityTierRepository.findOrThrowIllegalArgument(it) }
    val eligibilityRules = request.eligibilityRuleIds.map { eligibilityRuleRepository.findOrThrowIllegalArgument(it) }
    val prisonPayBands = prisonPayBandRepository.findByPrisonCode(request.prisonCode!!)
      .associateBy { it.prisonPayBandId }
      .ifEmpty { throw IllegalArgumentException("No pay bands found for prison '${request.prisonCode}") }
    failDuplicateActivity(request.prisonCode, request.summary!!)
    checkEducationLevels(request)

    return Activity(
      prisonCode = request.prisonCode,
      activityCategory = category,
      activityTier = tier,
      attendanceRequired = request.attendanceRequired,
      summary = request.summary,
      description = request.description,
      startDate = request.startDate ?: LocalDate.now(),
      endDate = request.endDate,
      riskLevel = request.riskLevel!!,
      minimumIncentiveNomisCode = request.minimumIncentiveNomisCode!!,
      minimumIncentiveLevel = request.minimumIncentiveLevel!!,
      createdTime = LocalDateTime.now(),
      createdBy = createdBy,
    ).apply {
      eligibilityRules.forEach { this.addEligibilityRule(it) }
      request.pay.forEach {
        this.addPay(
          incentiveNomisCode = it.incentiveNomisCode!!,
          incentiveLevel = it.incentiveLevel!!,
          payBand = prisonPayBands[it.payBandId]
            ?: throw IllegalArgumentException("Pay band not found for prison '${request.prisonCode}'"),
          rate = it.rate,
          pieceRate = it.pieceRate,
          pieceRateItems = it.pieceRateItems,
        )
      }
      request.minimumEducationLevel.forEach {
        this.addMinimumEducationLevel(
          educationLevelCode = it.educationLevelCode!!,
          educationLevelDescription = it.educationLevelDescription!!,
        )
      }
    }.let { transform(activityRepository.saveAndFlush(it)) }
  }

  private fun checkEducationLevels(request: ActivityCreateRequest) {
    request.minimumEducationLevel.forEach {
      val educationLevelCode = it.educationLevelCode!!
      val educationLevel = prisonApiClient.getEducationLevel(educationLevelCode).block()!!
      if (educationLevel.activeFlag != "Y") {
        throw IllegalArgumentException("The education level code '$educationLevelCode' is not active in NOMIS")
      } else {
        failIfDescriptionDiffers(it.educationLevelDescription!!, educationLevel.description)
      }
    }
  }

  private fun failIfDescriptionDiffers(requestDescription: String, apiDescription: String?) {
    if (requestDescription != apiDescription) {
      throw IllegalArgumentException("The education level description '$requestDescription' does not match that of the NOMIS education level '$apiDescription'")
    }
  }
}
