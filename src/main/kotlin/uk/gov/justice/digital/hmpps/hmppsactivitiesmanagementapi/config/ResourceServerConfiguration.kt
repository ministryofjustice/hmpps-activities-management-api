package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, proxyTargetClass = true)
class ResourceServerConfiguration {

  @Bean
  fun filterChain(http: HttpSecurity): SecurityFilterChain {
    return http
      .sessionManagement()
      .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
      .and().headers().frameOptions().sameOrigin()
      .and().csrf().disable()
      .authorizeRequests { auth ->
        auth.antMatchers(
          "/**" // This endpoint is secured in the ingress rather than the app so that it can be called from within the namespace without requiring authentication
        ).permitAll()
      }.also { it.oauth2ResourceServer().jwt().jwtAuthenticationConverter(AuthAwareTokenConverter()) }.build()
  }
}
