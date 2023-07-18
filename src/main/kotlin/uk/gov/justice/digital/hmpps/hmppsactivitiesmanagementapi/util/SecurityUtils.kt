package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails

object SecurityUtils {

  fun extractJwtFromHeader(headerValue: String?) = headerValue?.replace("Bearer ", "")

  fun getUserNameForLoggedInUser(): String {
    val principal = SecurityContextHolder.getContext().authentication.principal
    return if (principal is UserDetails) {
      principal.username
    } else {
      principal.toString()
    }
  }
}
