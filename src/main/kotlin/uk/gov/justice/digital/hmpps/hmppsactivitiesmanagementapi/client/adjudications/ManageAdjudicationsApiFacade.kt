package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.adjudications

import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriBuilder
import reactor.util.context.Context
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.RetryApiService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.typeReference
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Hearing(
  val id: Long? = null,
  val locationUuid: UUID,
  val dateTimeOfHearing: LocalDateTime,
  val oicHearingType: String,
  val agencyId: String,
)

data class HearingsResponse(
  val prisonerNumber: String,
  val hearing: Hearing,
)

data class HearingSummaryResponse(
  val hearings: List<HearingSummaryDto>,
)

data class HearingSummaryDto(
  val id: Long? = null,
  val dateTimeOfHearing: LocalDateTime,
  val dateTimeOfDiscovery: LocalDateTime,
  val chargeNumber: String,
  val prisonerNumber: String,
  val oicHearingType: String,
  val status: String,
  val locationUuid: UUID,
)

@Component
class ManageAdjudicationsApiFacade(
  @Qualifier("manageAdjudicationsApiWebClient") private val manageAdjudicationsApiWebClient: WebClient,
  retryApiService: RetryApiService,
  @Value("\${manage.adjudications.api.retry.max-retries:2}") private val maxRetryAttempts: Long = 2,
  @Value("\${manage.adjudications.api.retry.backoff-millis:250}") private val backoffMillis: Long = 250,
) {
  private val backoffSpec = retryApiService.getBackoffSpec(maxRetryAttempts, backoffMillis)

  suspend fun getAdjudicationHearings(
    agencyId: String,
    startDate: LocalDate,
    endDate: LocalDate,
    prisoners: Set<String>,
  ): List<HearingsResponse> = manageAdjudicationsApiWebClient.post()
    .uri { uriBuilder: UriBuilder ->
      uriBuilder
        .path("/reported-adjudications/hearings/$agencyId")
        .queryParam("startDate", startDate)
        .queryParam("endDate", endDate)
        .build()
    }
    .bodyValue(prisoners.toList())
    .retrieve()
    .bodyToMono(typeReference<List<HearingsResponse>>())
    .retryWhen(backoffSpec.withRetryContext(Context.of("api", "manage-adjudications-api", "path", "/reported-adjudications/hearings/{agencyId}")))
    .awaitSingle()

  suspend fun getAdjudicationHearingsForDate(agencyId: String, date: LocalDate): HearingSummaryResponse = manageAdjudicationsApiWebClient.get()
    .uri { uriBuilder: UriBuilder ->
      uriBuilder
        .path("/reported-adjudications/hearings")
        .queryParam("hearingDate", date)
        .build()
    }
    .header("Active-Caseload", agencyId)
    .retrieve()
    .bodyToMono(HearingSummaryResponse::class.java)
    .retryWhen(backoffSpec.withRetryContext(Context.of("api", "manage-adjudications-api", "path", "/reported-adjudications/hearings")))
    .awaitSingle()
}
