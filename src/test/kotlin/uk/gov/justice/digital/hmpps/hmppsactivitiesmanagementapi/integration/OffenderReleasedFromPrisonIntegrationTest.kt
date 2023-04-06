package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.OffenderReleasedFromPrisonService

class OffenderReleasedFromPrisonIntegrationTest : IntegrationTestBase() {

  @Autowired
  private lateinit var offenderReleasedFromPrisonService: OffenderReleasedFromPrisonService
}
