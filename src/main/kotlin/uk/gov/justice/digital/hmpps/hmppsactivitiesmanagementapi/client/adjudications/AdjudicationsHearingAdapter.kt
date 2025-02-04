package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.adjudications

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.OffenderAdjudicationHearing
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toIsoDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.PrisonRegime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.PrisonRegimeService.Companion.getSlotForDayAndTime
import java.time.DayOfWeek
import java.time.LocalDate

@Component
class AdjudicationsHearingAdapter(
  private val manageAdjudicationsApiFacade: ManageAdjudicationsApiFacade,
) {

  suspend fun getAdjudicationsByLocation(
    agencyId: String,
    date: LocalDate,
    timeSlot: TimeSlot?,
    prisonRegime: Map<Set<DayOfWeek>, PrisonRegime>,
  ): Map<Long, List<OffenderAdjudicationHearing>> = manageAdjudicationsApiFacade.getAdjudicationHearingsForDate(
    agencyId = agencyId,
    date = date,
  ).hearings.filter {
    timeSlot == null ||
      prisonRegime.getSlotForDayAndTime(
        day = date.dayOfWeek,
        time = it.dateTimeOfHearing.toLocalTime(),
      ) == timeSlot
  }
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

  suspend fun getAdjudicationHearings(
    agencyId: String,
    date: LocalDate,
    prisonerNumbers: Set<String>,
    timeSlot: TimeSlot? = null,
    prisonRegime: Map<Set<DayOfWeek>, PrisonRegime>,
  ): List<OffenderAdjudicationHearing> {
    if (prisonerNumbers.isEmpty()) return emptyList()

    return manageAdjudicationsApiFacade.getAdjudicationHearings(
      agencyId = agencyId,
      startDate = date,
      endDate = date,
      prisoners = prisonerNumbers,
    )
      .filter {
        timeSlot == null ||
          prisonRegime.getSlotForDayAndTime(
            day = it.hearing.dateTimeOfHearing.dayOfWeek,
            time = it.hearing.dateTimeOfHearing.toLocalTime(),
          ) == timeSlot
      }
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
