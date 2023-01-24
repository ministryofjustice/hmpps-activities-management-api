package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.FilterConfig
import jakarta.servlet.ServletException
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.io.IOException

@Component
@Order(4)
class UserContextFilter : Filter {
  @Throws(IOException::class, ServletException::class)
  override fun doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain) {
    val httpServletRequest = servletRequest as HttpServletRequest
    val authToken = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION)
    val authentication = SecurityContextHolder.getContext().authentication
    UserContext.setAuthToken(authToken)
    UserContext.setAuthentication(authentication)
    filterChain.doFilter(httpServletRequest, servletResponse)
  }

  override fun init(filterConfig: FilterConfig) {}
  override fun destroy() {}
}
