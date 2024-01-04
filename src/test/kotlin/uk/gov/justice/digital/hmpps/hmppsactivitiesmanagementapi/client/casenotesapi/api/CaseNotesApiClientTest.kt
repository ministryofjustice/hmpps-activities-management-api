package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock.CaseNotesApiMockServer

class CaseNotesApiClientTest {

  private lateinit var caseNotesApiClient: CaseNotesApiClient

  companion object {

    @JvmField
    internal val caseNotesApiMockServer = CaseNotesApiMockServer()

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      caseNotesApiMockServer.start()
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      caseNotesApiMockServer.stop()
    }
  }

  @BeforeEach
  fun resetStubs() {
    caseNotesApiMockServer.resetRequests()

    val webClient = WebClient.create("http://localhost:${caseNotesApiMockServer.port()}")
    caseNotesApiClient = CaseNotesApiClient(webClient)
  }

  @Test
  fun `getCaseNote - success`() {
    caseNotesApiMockServer.stubGetCaseNote("A1234AA", 1)
    val caseNote = caseNotesApiClient.getCaseNote("A1234AA", 1)
    assertThat(caseNote?.text).isEqualTo("Case Note Text")
  }

  @Test
  fun `getCaseNote without passing ID - success`() {
    val caseNote = caseNotesApiClient.getCaseNote("A1234AA", null)
    assertThat(caseNote).isNull()
  }

  @Test
  fun `postCaseNote - success`() {
    caseNotesApiMockServer.stubPostCaseNote(1, "MDI", "A1234AA", "Case Note Text", "NEG", "NEG_GEN")
    val caseNote = caseNotesApiClient.postCaseNote("MDI", "A1234AA", "Case Note Text", "NEG", "NEG_GEN")
    assertThat(caseNote?.text).isEqualTo("Case Note Text")
  }
}
