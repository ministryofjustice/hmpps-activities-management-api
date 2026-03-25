package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ExternalActivitiesPrison
import java.util.Properties

@Service
class ExternalActivitiesPrisonService(
  @Qualifier("externalActivitiesEnabledPrisons") private val prisonsProperties: Properties,
) {
  fun getPrisonsEnabledForExternalActivities(): List<ExternalActivitiesPrison> = prisonsProperties.stringPropertyNames()
    .map { code ->
      ExternalActivitiesPrison(
        prisonCode = code,
        prisonName = prisonsProperties.getProperty(code),
      )
    }
    .sortedBy(ExternalActivitiesPrison::prisonCode)
}
