package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.config.PropertiesFactoryBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource

@Configuration
class PropertiesConfiguration(
  @Value("classpath:whereabouts/patterns/*.properties") private val resources: Array<Resource>,
  @Value("classpath:external_activities/prisons.properties") private val prisonsResource: Resource,
) {

  @Bean
  @Qualifier("whereaboutsGroups")
  fun pfb(): PropertiesFactoryBean {
    val pfb = PropertiesFactoryBean()
    pfb.setLocations(*resources)
    return pfb
  }

  @Bean
  @Qualifier("externalActivitiesEnabledPrisons")
  fun prisonProperties(): PropertiesFactoryBean {
    val pfb = PropertiesFactoryBean()
    pfb.setLocation(prisonsResource)
    return pfb
  }
}
