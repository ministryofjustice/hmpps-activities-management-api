package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class AllocationEntityListenerTest(@Autowired private val listener: AllocationEntityListener) {

  @MockBean
  private lateinit var outboundEventsService: OutboundEventsService
  private val allocation = allocation()

  @Test
  fun `prisoner allocation event raised on creation`() {
    listener.onCreate(allocation)

    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATED, allocation.allocationId)
    verifyNoMoreInteractions(outboundEventsService)
  }

  @Test
  fun `prisoner allocation amended event raised on update`() {
    listener.onUpdate(allocation)

    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, allocation.allocationId)
    verifyNoMoreInteractions(outboundEventsService)
  }
}
