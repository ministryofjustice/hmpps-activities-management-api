package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails

object SecurityUtils {

  fun getUserNameForLoggedInUser(): String {
    val authentication = SecurityContextHolder.getContext().authentication
    if (authentication == null) return "ADMIN_USER" // can only be called when the job from inside ingress is running
    val principal = SecurityContextHolder.getContext().authentication.principal
    return if (principal is UserDetails) {
      principal.username
    } else {
      principal.toString()
    }
  }
}
