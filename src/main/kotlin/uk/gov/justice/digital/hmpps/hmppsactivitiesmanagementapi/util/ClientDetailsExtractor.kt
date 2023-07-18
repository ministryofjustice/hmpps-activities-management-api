package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util

import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ClientDetails

@Component
class ClientDetailsExtractor(
    private val jwtDecoder: JwtDecoder
) {

    fun extract (caseLoadId: String? = null, bearerToken: String? = null) : ClientDetails {

        val jwt = if (bearerToken !=null) jwtDecoder.decode(SecurityUtils.extractJwtFromHeader(bearerToken)) else null
        return ClientDetails(jwt, caseLoadId)
    }
}