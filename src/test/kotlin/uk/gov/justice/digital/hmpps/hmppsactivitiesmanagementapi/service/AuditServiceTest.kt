package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.audit.api.HmppsAuditApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.audit.model.HmppsAuditEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.HmppsAuditable

class AuditServiceTest {

  private val hmppsAuditApiClient = mock<HmppsAuditApiClient>()

  private val auditService = AuditService(hmppsAuditApiClient)

  @Test
  fun `should log hmpps auditable event correctly`() {
    val event = mock<HmppsAuditable>()
    val hmppsAuditEvent = mock<HmppsAuditEvent>()

    whenever(event.toAuditEvent()).thenReturn(hmppsAuditEvent)
    auditService.logEvent(event)

    verify(hmppsAuditApiClient).createEvent(hmppsAuditEvent)
  }
}
