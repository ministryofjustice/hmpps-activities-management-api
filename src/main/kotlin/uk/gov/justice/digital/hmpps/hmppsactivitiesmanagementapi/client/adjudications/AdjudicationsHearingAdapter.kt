package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.adjudications

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.OffenderAdjudicationHearing
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toIsoDateTime
import java.time.LocalDate

@Component
class AdjudicationsHearingAdapter(
  @Value("\${hearings.adjudications-source-of-truth}") private val manageAdjudicationsAsTruth: Boolean,
  private val prisonApiClient: PrisonApiClient,
  private val manageAdjudicationsApiFacade: ManageAdjudicationsApiFacade,
) {

  suspend fun getAdjudicationsByLocation(
    agencyId: String,
    date: LocalDate,
    timeSlot: TimeSlot?,
  ): Map<Long, List<OffenderAdjudicationHearing>> =
    if (manageAdjudicationsAsTruth) {
      manageAdjudicationsApiFacade.getAdjudicationHearingsForDate(
        agencyId = agencyId,
        date = date,
      ).hearings.filter { timeSlot == null || TimeSlot.slot(it.dateTimeOfHearing.toLocalTime()) == timeSlot }
        .map {
          OffenderAdjudicationHearing(
            offenderNo = it.prisonerNumber,
            hearingId = it.id!!,
            agencyId = agencyId,
            hearingType = it.oicHearingType.mapOicHearingType(),
            internalLocationId = it.locationId,
            internalLocationDescription = "Adjudication room",
            startTime = it.dateTimeOfHearing.toIsoDateTime(),
          )
        }
        .groupBy { it.internalLocationId }
    } else {
      emptyMap()
    }

  suspend fun getAdjudicationHearings(
    agencyId: String,
    dateRange: LocalDateRange,
    prisonerNumbers: Set<String>,
    timeSlot: TimeSlot? = null,
  ): List<OffenderAdjudicationHearing> {
    if (prisonerNumbers.isEmpty()) return emptyList()

    return when (manageAdjudicationsAsTruth) {
      true -> manageAdjudicationsApiFacade.getAdjudicationHearings(
        agencyId = agencyId,
        startDate = dateRange.start,
        endDate = dateRange.endInclusive,
        prisoners = prisonerNumbers,
      )
        .filter { timeSlot == null || TimeSlot.slot(it.hearing.dateTimeOfHearing.toLocalTime()) == timeSlot }
        .map {
          OffenderAdjudicationHearing(
            offenderNo = it.prisonerNumber,
            hearingId = it.hearing.id!!,
            agencyId = agencyId,
            hearingType = it.hearing.oicHearingType.mapOicHearingType(),
            internalLocationId = it.hearing.locationId,
            // this is a default, and generally exist for each prison as part of base setup in nomis,
            // the existing code will use the locationId in first instance to determine the description
            internalLocationDescription = "Adjudication room",
            startTime = it.hearing.dateTimeOfHearing.toIsoDateTime(),
          )
        }
      false -> prisonApiClient.getOffenderAdjudications(
        agencyId = agencyId,
        dateRange = dateRange,
        prisonerNumbers = prisonerNumbers,
        timeSlot = timeSlot,
      )
    }
  }

  companion object {
    fun String.mapOicHearingType(): String = when (this) {
      "GOV_ADULT" -> "Governor's Hearing Adult"
      "GOV_YOI" -> "Governor's Hearing YOI"
      "INAD_ADULT" -> "Independent Adjudicator Hearing Adult"
      "INAD_YOI" -> "Independent Adjudicator Hearing YOI"
      // adjudications does not support OicHearingType.GOV, however the existing tests do not use the correct codes
      else -> "Governor's Hearing Adult"
    }
  }
}
