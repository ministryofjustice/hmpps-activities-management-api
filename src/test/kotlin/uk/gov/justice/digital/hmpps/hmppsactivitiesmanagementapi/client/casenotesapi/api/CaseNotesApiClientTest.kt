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
    assertThat(caseNote.text).isEqualTo("Case Note Text")
  }

  @Test
  fun `getCaseNoteUUID - success`() {
    caseNotesApiMockServer.stubGetCaseNoteUUID("A1234AA", 1)
    val caseNote = caseNotesApiClient.getCaseNoteUUID("A1234AA", 1)
    assertThat(caseNote.text).isEqualTo("Case Note Text")
  }

  @Test
  fun `postCaseNote - success`() {
    caseNotesApiMockServer.stubPostCaseNote(1, "MDI", "A1234AA", "Prefix\n\nCase Note Text", CaseNoteType.NEG, CaseNoteSubType.NEG_GEN)
    val caseNote = caseNotesApiClient.postCaseNote("MDI", "A1234AA", "Case Note Text", CaseNoteType.NEG, CaseNoteSubType.NEG_GEN, "Prefix")
    assertThat(caseNote.text).isEqualTo("Case Note Text")
  }
}
