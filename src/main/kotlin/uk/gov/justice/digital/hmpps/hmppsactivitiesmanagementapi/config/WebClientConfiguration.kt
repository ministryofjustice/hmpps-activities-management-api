package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.ClientCodecConfigurer
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
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
  @Value("\${api.health-timeout:2s}") private val healthTimeout: Duration,
  @Value("\${api.timeout:30s}") private val apiTimeout: Duration,
  @Value("\${prison.api.timeout:10s}") private val shorterTimeout: Duration,
  private val webClientBuilder: WebClient.Builder,
) {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Bean
  fun oauthApiHealthWebClient(): WebClient = webClientBuilder.baseUrl(oauthApiUrl).timeout(healthTimeout).build()

  @Bean
  fun prisonApiHealthWebClient(): WebClient = WebClient.builder().baseUrl(prisonApiUrl).timeout(healthTimeout).build()

  @Bean
  fun caseNotesApiHealthWebClient(): WebClient = WebClient.builder().baseUrl(caseNotesApiUrl).timeout(healthTimeout).build()

  @Bean
  fun nonAssociationsApiHealthWebClient(): WebClient = WebClient.builder().baseUrl(nonAssociationsApiUrl).timeout(healthTimeout).build()

  @Bean
  fun incentivesApiHealthWebClient(): WebClient = WebClient.builder().baseUrl(incentivesApiUrl).timeout(healthTimeout).build()

  @Bean
  fun prisonApiUserWebClient(): WebClient {
    val exchangeStrategies = ExchangeStrategies.builder()
      .codecs { configurer: ClientCodecConfigurer -> configurer.defaultCodecs().maxInMemorySize(-1) }
      .build()

    return WebClient.builder()
      .baseUrl(prisonApiUrl)
      .timeout(apiTimeout)
      .filter(addAuthHeaderFilterFunction())
      .exchangeStrategies(exchangeStrategies)
      .build()
      .also { log.info("WEB CLIENT CONFIG: creating prison api user web client") }
  }

  @Bean
  @RequestScope
  fun prisonApiWebClient(
    clientRegistrationRepository: ClientRegistrationRepository,
    authorizedClientRepository: OAuth2AuthorizedClientRepository,
    builder: WebClient.Builder,
  ): WebClient =
    getPrisonApiOAuthWebClient(
      authorizedClientManager(clientRegistrationRepository, authorizedClientRepository),
      builder,
      prisonApiUrl,
    ).also { log.info("WEB CLIENT CONFIG: creating prison api request scope web client") }

  @Bean
  fun incentivesApiWebClient(
    @Qualifier(value = "authorizedClientManagerAppScope") authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = getIncentivesApiOAuthWebClient(authorizedClientManager, builder, incentivesApiUrl)
    .also { log.info("WEB CLIENT CONFIG: creating incentives api web client") }

  private fun getIncentivesApiOAuthWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
    rootUri: String,
  ): WebClient {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
    oauth2Client.setDefaultClientRegistrationId("incentives-api")
    return builder.baseUrl(rootUri)
      .timeout(apiTimeout)
      .apply(oauth2Client.oauth2Configuration())
      .build()
  }

  @Bean
  fun prisonApiAppWebClient(
    @Qualifier(value = "authorizedClientManagerAppScope") authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient =
    getPrisonApiOAuthWebClient(authorizedClientManager, builder, prisonApiUrl)
      .also { log.info("WEB CLIENT CONFIG: creating prison api app web client") }

  private fun getPrisonApiOAuthWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
    rootUri: String,
  ): WebClient {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
    oauth2Client.setDefaultClientRegistrationId("prison-api")
    return builder.baseUrl(rootUri)
      .timeout(apiTimeout)
      .apply(oauth2Client.oauth2Configuration())
      .build()
  }

  @Bean
  fun prisonerSearchApiHealthWebClient(): WebClient =
    WebClient.builder().baseUrl(prisonerSearchApiUrl).timeout(apiTimeout).build()

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
  ): WebClient =
    getPrisonerSearchApiOAuthWebClient(
      authorizedClientManager(clientRegistrationRepository, authorizedClientRepository),
      builder,
      prisonerSearchApiUrl,
    ).also { log.info("WEB CLIENT CONFIG: creating prisoner search api request scope web client") }

  @Bean
  fun prisonerSearchApiAppWebClient(
    @Qualifier(value = "authorizedClientManagerAppScope") authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient =
    getPrisonerSearchApiOAuthWebClient(authorizedClientManager, builder, prisonerSearchApiUrl)
      .also { log.info("WEB CLIENT CONFIG: creating prisoner search api app web client") }

  private fun getPrisonerSearchApiOAuthWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
    rootUri: String,
  ): WebClient {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
    oauth2Client.setDefaultClientRegistrationId("prisoner-search-api")
    return builder.baseUrl(rootUri)
      .timeout(shorterTimeout)
      .apply(oauth2Client.oauth2Configuration())
      .build()
  }

  private fun getNonAssociationsApiOAuthWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
    rootUri: String,
  ): WebClient {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
    oauth2Client.setDefaultClientRegistrationId("non-associations-api")
    return builder.baseUrl(rootUri)
      .timeout(apiTimeout)
      .apply(oauth2Client.oauth2Configuration())
      .build()
  }

  @Bean
  fun authorizedClientManagerAppScope(
    clientRegistrationRepository: ClientRegistrationRepository?,
    oAuth2AuthorizedClientService: OAuth2AuthorizedClientService?,
  ): OAuth2AuthorizedClientManager {
    val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build()
    val authorizedClientManager =
      AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, oAuth2AuthorizedClientService)
    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider)
    return authorizedClientManager
  }

  @Bean
  fun bankHolidayApiWebClient(): WebClient = webClientBuilder.baseUrl(bankHolidayApiUrl).timeout(apiTimeout).build()
    .also { log.info("WEB CLIENT CONFIG: bank holiday api web client") }

  @Bean
  fun caseNotesApiWebClient(): WebClient =
    webClientBuilder.baseUrl(caseNotesApiUrl)
      .timeout(apiTimeout)
      .filter(addAuthHeaderFilterFunction())
      .build().also { log.info("WEB CLIENT CONFIG: creating case notes api web client") }

  private fun WebClient.Builder.timeout(duration: Duration) =
    this.clientConnector(ReactorClientHttpConnector(HttpClient.create().responseTimeout(duration)))

  @Bean
  fun nonAssociationsApiWebClient(
    @Qualifier(value = "authorizedClientManagerAppScope") authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ) = getNonAssociationsApiOAuthWebClient(authorizedClientManager, builder, nonAssociationsApiUrl)
    .also { log.info("WEB CLIENT CONFIG: creating non associations api web client") }

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
    val authorizedClientManager =
      DefaultOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientRepository)
    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider)
    return authorizedClientManager
  }

  private fun addAuthHeaderFilterFunction() =
    ExchangeFilterFunction { request: ClientRequest, next: ExchangeFunction ->
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
}
