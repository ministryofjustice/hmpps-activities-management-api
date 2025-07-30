package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.ClientCodecConfigurer
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.endpoint.DefaultClientCredentialsTokenResponseClient
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.web.context.annotation.RequestScope
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import uk.gov.justice.hmpps.kotlin.auth.healthWebClient
import java.time.Duration

@Configuration
class WebClientConfiguration(
  @Value("\${hmpps.auth.url}") private val oauthApiUrl: String,
  @Value("\${prison.api.url}") private val prisonApiUrl: String,
  @Value("\${prisoner-search.api.url}") private val prisonerSearchApiUrl: String,
  @Value("\${bank-holiday.api.url:https://www.gov.uk}") private val bankHolidayApiUrl: String,
  @Value("\${case-notes.api.url}") private val caseNotesApiUrl: String,
  @Value("\${non-associations.api.url}") private val nonAssociationsApiUrl: String,
  @Value("\${incentives.api.url}") private val incentivesApiUrl: String,
  @Value("\${manage.adjudications.api.url}") private val manageAdjudicationsApiUrl: String,
  @Value("\${locations-inside-prison.api.url}") private val locationsInsidePrisonApiUrl: String,
  @Value("\${nomis-mapping.api.url}") private val nomisMappingApiUrl: String,
  @Value("\${api.health-timeout:2s}") private val healthTimeout: Duration,
  @Value("\${api.timeout:30s}") private val apiTimeout: Duration,
  @Value("\${prison.api.timeout:10s}") private val shorterTimeout: Duration,
  private val webClientBuilder: WebClient.Builder,
) {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Bean
  fun oauthApiHealthWebClient(builder: WebClient.Builder) = builder.healthWebClient(oauthApiUrl, healthTimeout)

  @Bean
  fun prisonApiHealthWebClient(builder: WebClient.Builder) = builder.healthWebClient(prisonApiUrl, healthTimeout)

  @Bean
  fun caseNotesApiHealthWebClient(builder: WebClient.Builder) = builder.healthWebClient(caseNotesApiUrl, healthTimeout)

  @Bean
  fun nonAssociationsApiHealthWebClient(builder: WebClient.Builder) = builder.healthWebClient(nonAssociationsApiUrl, healthTimeout)

  @Bean
  fun incentivesApiHealthWebClient(builder: WebClient.Builder) = builder.healthWebClient(incentivesApiUrl, healthTimeout)

  @Bean
  fun manageAdjudicationsApiHealthWebClient(builder: WebClient.Builder) = builder.healthWebClient(manageAdjudicationsApiUrl, healthTimeout)

  @Bean
  fun locationsInsidePrisonApiHealthWebClient(builder: WebClient.Builder) = builder.healthWebClient(locationsInsidePrisonApiUrl, healthTimeout)

  @Bean
  fun nomisMappingApiHealthWebClient(builder: WebClient.Builder) = builder.healthWebClient(nomisMappingApiUrl, healthTimeout)

  @Bean
  fun incentivesApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder) = builder
    .authorisedWebClient(authorizedClientManager, "incentives-api", incentivesApiUrl, apiTimeout)
    .also { log.info("WEB CLIENT CONFIG: creating incentives api web client") }

  @Bean
  fun manageAdjudicationsApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder) = builder
    .authorisedWebClient(authorizedClientManager, "manage-adjudications-api", manageAdjudicationsApiUrl, apiTimeout)
    .also { log.info("WEB CLIENT CONFIG: creating manage adjudications api web client") }

  @Bean
  fun prisonApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder) = builder
    .authorisedWebClient(authorizedClientManager, "prison-api", prisonApiUrl, shorterTimeout)
    .also { log.info("WEB CLIENT CONFIG: creating prison api web client") }

  @Bean
  fun prisonerSearchApiHealthWebClient(builder: WebClient.Builder) = builder.healthWebClient(prisonerSearchApiUrl, healthTimeout)

  @Bean
  fun prisonerSearchApiUserWebClient(): WebClient {
    val exchangeStrategies = ExchangeStrategies.builder()
      .codecs { configurer: ClientCodecConfigurer -> configurer.defaultCodecs().maxInMemorySize(-1) }
      .build()

    return WebClient.builder()
      .baseUrl(prisonerSearchApiUrl)
      .timeout(apiTimeout)
      .filter(addAuthHeaderFilterFunction())
      .exchangeStrategies(exchangeStrategies)
      .build().also { log.info("WEB CLIENT CONFIG: creating prisoner search api user web client") }
  }

  @Bean
  @RequestScope
  fun prisonerSearchApiWebClient(
    clientRegistrationRepository: ClientRegistrationRepository,
    authorizedClientRepository: OAuth2AuthorizedClientRepository,
    builder: WebClient.Builder,
  ): WebClient = getOAuthWebClient(
    authorizedClientManager(clientRegistrationRepository, authorizedClientRepository),
    builder,
    prisonerSearchApiUrl,
    "prisoner-search-api",
    shorterTimeout,
  ).also { log.info("WEB CLIENT CONFIG: creating prisoner search api request scope web client") }

  @Bean
  fun prisonerSearchApiAppWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder) = builder
    .authorisedWebClient(authorizedClientManager, "prisoner-search-api", prisonerSearchApiUrl, shorterTimeout)
    .also { log.info("WEB CLIENT CONFIG: creating prisoner search api app web client") }

  @Bean
  fun bankHolidayApiWebClient(): WebClient = WebClient.builder().baseUrl(bankHolidayApiUrl).timeout(apiTimeout).build()
    .also { log.info("WEB CLIENT CONFIG: bank holiday api web client") }

  @Bean
  fun caseNotesApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder) = builder
    .authorisedWebClient(authorizedClientManager, "offender-case-notes-api", caseNotesApiUrl, shorterTimeout)
    .also { log.info("WEB CLIENT CONFIG: creating case notes api web client") }

  @Bean
  fun nonAssociationsApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder) = builder
    .authorisedWebClient(authorizedClientManager, "non-associations-api", nonAssociationsApiUrl, shorterTimeout)
    .also { log.info("WEB CLIENT CONFIG: creating non associations api web client") }

  @Bean
  fun locationsInsidePrisonApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder) = builder
    .authorisedWebClient(authorizedClientManager, "locations-inside-prison", locationsInsidePrisonApiUrl, shorterTimeout)
    .also { log.info("WEB CLIENT CONFIG: creating locations inside prison api web client") }

  @Bean
  fun nomisMappingApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder) = builder
    .authorisedWebClient(authorizedClientManager, "nomis-mapping-api", nomisMappingApiUrl, shorterTimeout)
    .also { log.info("WEB CLIENT CONFIG: creating NOMIS mapping api web client") }

  private fun getOAuthWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
    rootUri: String,
    clientRegistrationId: String,
    timeout: Duration,
  ): WebClient {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
    oauth2Client.setDefaultClientRegistrationId(clientRegistrationId)
    return builder.baseUrl(rootUri)
      .timeout(timeout)
      .apply(oauth2Client.oauth2Configuration())
      .build()
  }

  // Differs from the 'app scope' auth client manager in that it gets the username from the authentication context
  // and adds it to the request
  private fun authorizedClientManager(
    clientRegistrationRepository: ClientRegistrationRepository,
    authorizedClientRepository: OAuth2AuthorizedClientRepository,
  ): OAuth2AuthorizedClientManager {
    val defaultClientCredentialsTokenResponseClient = DefaultClientCredentialsTokenResponseClient()
    val authentication = UserContext.getAuthentication()

    defaultClientCredentialsTokenResponseClient.setRequestEntityConverter { grantRequest: OAuth2ClientCredentialsGrantRequest? ->
      val converter = CustomOAuth2ClientCredentialsGrantRequestEntityConverter()
      val username = authentication.name
      converter.enhanceWithUsername(grantRequest, username)
    }

    val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
      .clientCredentials { clientCredentialsGrantBuilder: OAuth2AuthorizedClientProviderBuilder.ClientCredentialsGrantBuilder ->
        clientCredentialsGrantBuilder.accessTokenResponseClient(
          defaultClientCredentialsTokenResponseClient,
        )
      }
      .build()
    val authorizedClientManager = DefaultOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientRepository)
    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider)
    return authorizedClientManager
  }

  private fun addAuthHeaderFilterFunction() = ExchangeFilterFunction { request: ClientRequest, next: ExchangeFunction ->
    val token = when (val authentication = SecurityContextHolder.getContext().authentication) {
      is AuthAwareAuthenticationToken -> authentication.token.tokenValue
      else -> throw IllegalStateException("Auth token not present")
    }

    next.exchange(
      ClientRequest.from(request)
        .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
        .build(),
    )
  }

  private fun WebClient.Builder.timeout(duration: Duration) = this.clientConnector(ReactorClientHttpConnector(HttpClient.create().responseTimeout(duration)))
}
