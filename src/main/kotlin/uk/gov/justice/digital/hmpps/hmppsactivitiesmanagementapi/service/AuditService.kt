package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.audit.api.HmppsAuditApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.audit.model.HmppsAuditEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditableEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.HmppsAuditable

@Service
class AuditService(
  private val hmppsAuditApiClient: HmppsAuditApiClient,
) {

  private val objectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

  fun logEvent(event: AuditableEvent) {
    if (event is HmppsAuditable) {
      hmppsAuditApiClient.createEvent(
        HmppsAuditEvent(
          what = event.type().name,
          details = objectMapper.writeValueAsString(event),
        ),
      )
    }
  }
}
