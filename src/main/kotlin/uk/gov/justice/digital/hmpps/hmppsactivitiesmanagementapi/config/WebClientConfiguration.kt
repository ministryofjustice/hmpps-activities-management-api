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
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfiguration(
  @Value("\${hmpps.auth.url}") private val oauthApiUrl: String,
  @Value("\${prison.api.url}") private val prisonApiUrl: String,
  @Value("\${bank-holiday.api.url:https://www.gov.uk}") private val bankHolidayApiUrl: String,
  private val webClientBuilder: WebClient.Builder
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
    builder: WebClient.Builder
  ): WebClient {
    return getOAuthWebClient(
      authorizedClientManager(clientRegistrationRepository, authorizedClientRepository),
      builder,
      prisonApiUrl
    )
  }

  @Bean
  fun prisonApiAppWebClient(
    @Qualifier(value = "authorizedClientManagerAppScope") authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder
  ): WebClient {
    return getOAuthWebClient(authorizedClientManager, builder, prisonApiUrl)
  }

  private fun getOAuthWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
    rootUri: String
  ): WebClient {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
    oauth2Client.setDefaultClientRegistrationId("prison-api")
    return builder.baseUrl(rootUri)
      .apply(oauth2Client.oauth2Configuration())
      .build()
  }

  @Bean
  fun authorizedClientManagerAppScope(
    clientRegistrationRepository: ClientRegistrationRepository?,
    oAuth2AuthorizedClientService: OAuth2AuthorizedClientService?
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

  // Differs from the 'app scope' auth client manager in that it gets the username from the authentication context
  // and adds it to the request
  private fun authorizedClientManager(
    clientRegistrationRepository: ClientRegistrationRepository,
    authorizedClientRepository: OAuth2AuthorizedClientRepository
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
          defaultClientCredentialsTokenResponseClient
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
          .build()
      )
    }
}
