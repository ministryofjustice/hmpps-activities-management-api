package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.envers.repository.config.EnableEnversRepositories
import org.springframework.data.envers.repository.support.EnversRevisionRepositoryFactoryBean
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(
  repositoryFactoryBeanClass = EnversRevisionRepositoryFactoryBean::class,
  basePackages = ["uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository"])
class JPAConfig
