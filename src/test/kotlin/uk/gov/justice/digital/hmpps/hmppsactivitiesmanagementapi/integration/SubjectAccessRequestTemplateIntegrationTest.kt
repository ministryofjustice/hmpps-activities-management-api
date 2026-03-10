package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelper
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelperConfig
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarReportTest
import java.time.LocalDate

@Import(SarIntegrationTestHelperConfig::class)
class SubjectAccessRequestTemplateIntegrationTest :
  IntegrationTestBase(),
  SarReportTest {

  @Autowired
  lateinit var sarIntegrationTestHelper: SarIntegrationTestHelper

  override fun getSarHelper(): SarIntegrationTestHelper = sarIntegrationTestHelper

  override fun getPrn(): String? = "111111"

  override fun getWebTestClientInstance(): WebTestClient = webTestClient

  // fix the 'to' date because we currently use it in the template and the test lib
  // defaults to "current date" which means the date in the expected output will keep
  // changing, causing the comparison with the baseline report to fail
  override fun getToDate(): LocalDate? = LocalDate.parse("2026-02-26")

  override fun setupTestData() {} // Test data set up via sql annotations below:

  @Test
  @Sql("classpath:test_data/seed-subject-access-request-template.sql")
  override fun `SAR report should render as expected`() {
    super.`SAR report should render as expected`()
  }
}
