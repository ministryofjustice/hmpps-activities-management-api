package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import org.junit.jupiter.api.BeforeEach
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.SecurityTestUtils

open class AuditableEventTestBase {

  @BeforeEach
  fun setup() {
    SecurityTestUtils.setLoggedInUser("Bob")
  }
}
