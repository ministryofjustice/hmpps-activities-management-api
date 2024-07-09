package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.adjudications

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDate
import java.time.LocalDateTime

data class Hearing(
  val id: Long? = null,
  val locationId: Long,
  val dateTimeOfHearing: LocalDateTime,
  val oicHearingType: String,
  val agencyId: String,
)

data class HearingsResponse(
  val prisonerNumber: String,
  val hearing: Hearing,
)

@Component
class ManageAdjudicationsApiFacade(@Qualifier("manageAdjudicationsApiWebClient") private val manageAdjudicationsApiWebClient: WebClient) {

  fun getHearings(agencyId: String, startDate: LocalDate, endDate: LocalDate, prisoners: Set<String>): List<HearingsResponse> = TODO("implement me")
}