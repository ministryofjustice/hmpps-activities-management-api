package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
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
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Configuration
class WebClientConfiguration(
  @Value("\${hmpps.auth.url}") private val oauthApiUrl: String,
  @Value("\${prison.api.url}") private val prisonApiUrl: String,
  @Value("\${prisoner-search.api.url}") private val prisonerSearchApiUrl: String,
  @Value("\${bank-holiday.api.url:https://www.gov.uk}") private val bankHolidayApiUrl: String,
  @Value("\${case-notes.api.url}") private val caseNotesApiUrl: String,
  private val webClientBuilder: WebClient.Builder,
) {

  @Bean
  fun oauthApiHealthWebClient(): WebClient {
    return webClientBuilder.baseUrl(oauthApiUrl).build()
  }

  @Bean
  fun prisonApiHealthWebClient(): WebClient {
    return WebClient.builder().baseUrl(prisonApiUrl).build()
  }

  @Bean
  fun prisonApiUserWebClient(): WebClient {
    val exchangeStrategies = ExchangeStrategies.builder()
      .codecs { configurer: ClientCodecConfigurer -> configurer.defaultCodecs().maxInMemorySize(-1) }
      .build()

    return WebClient.builder()
      .baseUrl(prisonApiUrl)
      .filter(addAuthHeaderFilterFunction())
      .exchangeStrategies(exchangeStrategies)
      .build()
  }

  @Bean
  @RequestScope
  fun prisonApiWebClient(
    clientRegistrationRepository: ClientRegistrationRepository,
    authorizedClientRepository: OAuth2AuthorizedClientRepository,
    builder: WebClient.Builder,
  ): WebClient {
    return getPrisonApiOAuthWebClient(
      authorizedClientManager(clientRegistrationRepository, authorizedClientRepository),
      builder,
      prisonApiUrl,
    )
  }

  @Bean
  fun prisonApiAppWebClient(
    @Qualifier(value = "authorizedClientManagerAppScope") authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient {
    return getPrisonApiOAuthWebClient(authorizedClientManager, builder, prisonApiUrl)
  }

  private fun getPrisonApiOAuthWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
    rootUri: String,
  ): WebClient {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
    oauth2Client.setDefaultClientRegistrationId("prison-api")
    return builder.baseUrl(rootUri)
      .apply(oauth2Client.oauth2Configuration())
      .build()
  }

  @Bean
  fun prisonerSearchApiHealthWebClient(): WebClient {
    return WebClient.builder().baseUrl(prisonerSearchApiUrl).build()
  }

  @Bean
  fun prisonerSearchApiUserWebClient(): WebClient {
    val exchangeStrategies = ExchangeStrategies.builder()
      .codecs { configurer: ClientCodecConfigurer -> configurer.defaultCodecs().maxInMemorySize(-1) }
      .build()

    return WebClient.builder()
      .baseUrl(prisonerSearchApiUrl)
      .filter(addAuthHeaderFilterFunction())
      .exchangeStrategies(exchangeStrategies)
      .build()
  }

  @Bean
  @RequestScope
  fun prisonerSearchApiWebClient(
    clientRegistrationRepository: ClientRegistrationRepository,
    authorizedClientRepository: OAuth2AuthorizedClientRepository,
    builder: WebClient.Builder,
  ): WebClient {
    return getPrisonerSearchApiOAuthWebClient(
      authorizedClientManager(clientRegistrationRepository, authorizedClientRepository),
      builder,
      prisonerSearchApiUrl,
    )
  }

  @Bean
  fun prisonerSearchApiAppWebClient(
    @Qualifier(value = "authorizedClientManagerAppScope") authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient {
    return getPrisonerSearchApiOAuthWebClient(authorizedClientManager, builder, prisonerSearchApiUrl)
  }

  private fun getPrisonerSearchApiOAuthWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
    rootUri: String,
  ): WebClient {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
    oauth2Client.setDefaultClientRegistrationId("prisoner-search-api")
    return builder.baseUrl(rootUri)
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
  fun bankHolidayApiWebClient(): WebClient {
    return webClientBuilder.baseUrl(bankHolidayApiUrl).build()
  }

  @Bean
  fun caseNotesApiWebClient(): WebClient {
    return webClientBuilder.baseUrl(caseNotesApiUrl)
      .filter(addAuthHeaderFilterFunction())
      //.filter(logRequestResponse())
      .build()
  }

  @Bean
  fun caseNotesApiAppWebClient(
    @Qualifier(value = "authorizedClientManagerAppScope") authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient {
    return getCaseNotesAPIOAuthWebClient(authorizedClientManager, builder, caseNotesApiUrl)
  }

  private fun getCaseNotesAPIOAuthWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
    rootUri: String,
  ): WebClient {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
    oauth2Client.setDefaultClientRegistrationId("case-notes-api")
    return builder.baseUrl(rootUri)
      .apply(oauth2Client.oauth2Configuration())
      .filter(logRequestResponse())
      .build()
  }

  private fun logRequestResponse(): ExchangeFilterFunction {
    return ExchangeFilterFunction.ofRequestProcessor { clientRequest: ClientRequest ->
      println("Request: ${clientRequest.method()} ${clientRequest.url()}")
      clientRequest.headers().forEach { name, values -> values.forEach { println("$name: $it") } }
      Mono.just(clientRequest)
    }.andThen(ExchangeFilterFunction.ofResponseProcessor { clientResponse: ClientResponse ->
      clientResponse.headers().asHttpHeaders().forEach { name, values -> values.forEach { println("$name: $it") } }
      Mono.just(clientResponse)
    })
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
