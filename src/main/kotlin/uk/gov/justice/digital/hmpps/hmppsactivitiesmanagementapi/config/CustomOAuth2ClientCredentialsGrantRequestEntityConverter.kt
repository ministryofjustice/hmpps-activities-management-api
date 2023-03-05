package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config

import org.springframework.http.HttpMethod
import org.springframework.http.RequestEntity
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequestEntityConverter
import org.springframework.util.MultiValueMap
import java.util.Objects

class CustomOAuth2ClientCredentialsGrantRequestEntityConverter : OAuth2ClientCredentialsGrantRequestEntityConverter() {
  fun enhanceWithUsername(grantRequest: OAuth2ClientCredentialsGrantRequest?, username: String?): RequestEntity<Any> {
    val request = super.convert(grantRequest)
    val body = Objects.requireNonNull(request).body
    val headers = request.headers

    @Suppress("UNCHECKED_CAST")
    val formParameters = body as MultiValueMap<String, Any>
    formParameters.add("username", username)
    return RequestEntity(formParameters, headers, HttpMethod.POST, request.url)
  }
}
