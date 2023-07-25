package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.health

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import java.security.KeyPair
import java.time.Duration
import java.util.Date
import java.util.UUID

@Component
class JwtAuthHelper(private val keyPair: KeyPair) {

  fun setAuthorisation(user: String = "activities-management-admin-1", roles: List<String> = listOf(), isClientToken: Boolean = true): (HttpHeaders) -> Unit {
    val token = createJwt(subject = user, scope = listOf("read"), expiryTime = Duration.ofHours(1L), roles = roles, isClientToken = isClientToken)
    return { it.set(HttpHeaders.AUTHORIZATION, "Bearer $token") }
  }

  private fun createJwt(
    subject: String?,
    scope: List<String>? = listOf(),
    roles: List<String>? = listOf(),
    isClientToken: Boolean,
    expiryTime: Duration = Duration.ofHours(1),
    jwtId: String = UUID.randomUUID().toString(),
  ): String =
    mutableMapOf<String, Any>()
      .also { if (!isClientToken) subject?.let { subject -> it["user_name"] = subject } }
      .also { it["client_id"] = "activities-management-admin-1" }
      .also { roles?.let { roles -> it["authorities"] = roles } }
      .also { scope?.let { scope -> it["scope"] = scope } }
      .let {
        Jwts.builder()
          .setId(jwtId)
          .setSubject(subject)
          .addClaims(it.toMap())
          .setExpiration(Date(System.currentTimeMillis() + expiryTime.toMillis()))
          .signWith(keyPair.private, SignatureAlgorithm.RS256)
          .compact()
      }
}
