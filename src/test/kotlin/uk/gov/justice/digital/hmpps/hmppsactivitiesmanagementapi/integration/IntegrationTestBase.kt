package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.applicationinsights.TelemetryClient
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.http.HttpHeaders
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.SqlMergeMode
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.util.UriBuilder
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.InmateDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.config.LocalStackContainer
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.config.LocalStackContainer.setLocalStackProperties
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.config.PostgresContainer
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock.BankHolidayApiExtension
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock.CaseNotesApiMockServer
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock.IncentivesApiMockServer
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock.LocationsInsidePrisonApiMockServer
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock.ManageAdjudicationsApiMockServer
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock.NomisMappingApiMockServer
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock.NonAssociationsApiMockServer
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock.OAuthExtension
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock.PrisonApiMockServer
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock.PrisonerSearchApiMockServer
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.HmppsAuditApiClient
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper
import java.util.*

@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@ExtendWith(OAuthExtension::class, BankHolidayApiExtension::class)
@AutoConfigureWebTestClient(timeout = "36000")
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@Sql(
  "classpath:test_data/clean-all-data.sql",
  "classpath:test_data/seed-reference-data.sql",
)
abstract class IntegrationTestBase {

  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  lateinit var jwtAuthHelper: JwtAuthorisationHelper

  @Autowired
  lateinit var jdbcTemplate: JdbcTemplate

  @Autowired
  lateinit var mapper: ObjectMapper

  @MockitoBean
  protected lateinit var hmppsAuditApiClient: HmppsAuditApiClient

  @MockitoBean
  protected lateinit var telemetryClient: TelemetryClient

  companion object {
    @JvmField
    internal val prisonApiMockServer = PrisonApiMockServer()
    internal val prisonerSearchApiMockServer = PrisonerSearchApiMockServer()
    internal val nonAssociationsApiMockServer = NonAssociationsApiMockServer()
    internal val caseNotesApiMockServer = CaseNotesApiMockServer()
    internal val incentivesApiMockServer = IncentivesApiMockServer()
    internal val manageAdjudicationsApiMockServer = ManageAdjudicationsApiMockServer()
    internal val nomisMappingApiMockServer = NomisMappingApiMockServer()
    internal val locationsInsidePrisonApiMockServer = LocationsInsidePrisonApiMockServer()

    internal val db = PostgresContainer.instance
    internal val localStackContainer = LocalStackContainer.instance

    @JvmStatic
    @DynamicPropertySource
    fun properties(registry: DynamicPropertyRegistry) {
      db?.run {
        registry.add("spring.datasource.url", db::getJdbcUrl)
        registry.add("spring.datasource.username", db::getUsername)
        registry.add("spring.datasource.password", db::getPassword)
      }
      localStackContainer?.also { setLocalStackProperties(it, registry) }
    }

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      prisonApiMockServer.start()
      prisonerSearchApiMockServer.start()
      nonAssociationsApiMockServer.start()
      caseNotesApiMockServer.start()
      incentivesApiMockServer.start()
      manageAdjudicationsApiMockServer.start()
      nomisMappingApiMockServer.start()
      locationsInsidePrisonApiMockServer.start()
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      prisonApiMockServer.stop()
      prisonerSearchApiMockServer.stop()
      nonAssociationsApiMockServer.stop()
      caseNotesApiMockServer.stop()
      incentivesApiMockServer.stop()
      manageAdjudicationsApiMockServer.stop()
      nomisMappingApiMockServer.stop()
      locationsInsidePrisonApiMockServer.stop()
    }

    @BeforeEach
    fun initMocks() {
      prisonApiMockServer.resetAll()
      prisonerSearchApiMockServer.resetAll()
      incentivesApiMockServer.resetAll()
      nomisMappingApiMockServer.resetAll()
      locationsInsidePrisonApiMockServer.resetAll()
    }

    @AfterEach
    fun afterEach() {
      prisonApiMockServer.resetAll()
      prisonerSearchApiMockServer.resetAll()
      nonAssociationsApiMockServer.resetAll()
      caseNotesApiMockServer.resetAll()
      incentivesApiMockServer.resetAll()
      manageAdjudicationsApiMockServer.resetAll()
      nomisMappingApiMockServer.resetAll()
      locationsInsidePrisonApiMockServer.resetAll()
    }
  }

  internal fun setAuthorisationAsClient(
    clientId: String = "activities-management-admin-1",
    scopes: List<String> = listOf(),
    roles: List<String> = listOf(),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisationHeader(
    clientId = clientId,
    roles = roles,
    scope = scopes,
  )

  internal fun setAuthorisationAsUser(
    username: String = "test-client",
    scopes: List<String> = listOf(),
    roles: List<String> = listOf(),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisationHeader(
    roles = roles,
    username = username,
    scope = scopes,
  )

  internal fun <T> UriBuilder.maybeQueryParam(name: String, type: T?) = this.queryParamIfPresent(name, Optional.ofNullable(type))

  internal fun stubPrisonerForInterestingEvent(prisoner: InmateDetail) {
    prisonerSearchApiMockServer.stubSearchByPrisonerNumber(prisoner.offenderNo!!)
  }

  internal fun stubPrisonerForInterestingEvent(prisonerNumber: String) {
    prisonerSearchApiMockServer.stubSearchByPrisonerNumber(prisonerNumber)
  }

  internal fun waitForJobs(block: () -> Unit, numJobs: Int = 1) {
    block()

    await until {
      numJobs == jdbcTemplate.queryForObject<Int>("select count(*) from job where successful = true")
    }
  }
}
