package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config

import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.stereotype.Component

@Component
class AuthAwareTokenConverter : Converter<Jwt, AbstractAuthenticationToken> {
  private val jwtGrantedAuthoritiesConverter: Converter<Jwt, Collection<GrantedAuthority>> = JwtGrantedAuthoritiesConverter()

  override fun convert(jwt: Jwt): AbstractAuthenticationToken {
    val claims = jwt.claims
    val principal = findPrincipal(claims)
    val authorities = extractAuthorities(jwt)
    return AuthAwareAuthenticationToken(jwt, principal, authorities)
  }

  private fun findPrincipal(claims: Map<String, Any?>): String {
    return if (claims.containsKey("user_name")) {
      claims["user_name"] as String
    } else {
      claims["client_id"] as String
    }
  }

  private fun extractAuthorities(jwt: Jwt): Collection<GrantedAuthority> =
    mutableListOf<GrantedAuthority>().apply {
      addAll(jwtGrantedAuthoritiesConverter.convert(jwt)!!)
      jwt.getClaimAsStringList("authorities")?.map(::SimpleGrantedAuthority)?.let(::addAll)
    }.toSet()
}

class AuthAwareAuthenticationToken(
  jwt: Jwt,
  private val aPrincipal: String,
  authorities: Collection<GrantedAuthority>,
) : JwtAuthenticationToken(jwt, authorities) {
  override fun getPrincipal(): Any {
    return aPrincipal
  }
}
