package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerReceivedHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.activeInMoorlandPrisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.activeOutMoorlandPrisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.prisonerReceivedFromTemporaryAbsence
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService

class PrisonerReceivedEventHandlerTest {
  private val rolloutPrisonService = RolloutPrisonService("MDI", "MDI", "MDI")
  private val prisonerSearchApiClient: PrisonerSearchApiApplicationClient = mock()
  private val prisonerReceivedHandler: PrisonerReceivedHandler = mock()

  private val handler = PrisonerReceivedEventHandler(rolloutPrisonService, prisonerSearchApiClient, prisonerReceivedHandler)

  @Test
  fun `inbound received event is not handled for an inactive prison`() {
    val outcome = handler.handle(prisonerReceivedFromTemporaryAbsence("PVI", "123456"))

    assertThat(outcome.isSuccess()).isTrue

    verifyNoInteractions(prisonerReceivedHandler)
  }

  @Test
  fun `prisoner is not received if not active in the prison`() {
    prisonerSearchApiClient.stub {
      on { findByPrisonerNumber("123456") } doReturn activeOutMoorlandPrisoner.copy(prisonerNumber = "123456")
    }

    val outcome = handler.handle(prisonerReceivedFromTemporaryAbsence(MOORLAND_PRISON_CODE, "123456"))

    assertThat(outcome.isSuccess()).isTrue

    verifyNoInteractions(prisonerReceivedHandler)
  }

  @Test
  fun `auto-suspended allocations are reactivated on receipt of prisoner`() {
    prisonerSearchApiClient.stub {
      on { findByPrisonerNumber("123456") } doReturn activeInMoorlandPrisoner.copy(prisonerNumber = "123456")
    }

    val outcome = handler.handle(prisonerReceivedFromTemporaryAbsence(MOORLAND_PRISON_CODE, "123456"))

    assertThat(outcome.isSuccess()).isTrue

    verify(prisonerReceivedHandler).receivePrisoner(MOORLAND_PRISON_CODE, "123456")
  }

  @Test
  fun `prisoner is not received if search api returned null`() {
    prisonerSearchApiClient.stub {
      on { findByPrisonerNumber("123456") } doReturn null
    }

    val outcome = handler.handle(prisonerReceivedFromTemporaryAbsence(MOORLAND_PRISON_CODE, "123456"))

    assertThat(outcome.isSuccess()).isTrue

    verifyNoInteractions(prisonerReceivedHandler)
  }
}
