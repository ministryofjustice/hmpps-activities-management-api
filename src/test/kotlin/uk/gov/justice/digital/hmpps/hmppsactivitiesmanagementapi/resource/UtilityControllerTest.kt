package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PublishEventUtilityModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService

@WebMvcTest(controllers = [UtilityController::class])
@ContextConfiguration(classes = [UtilityController::class])
class UtilityControllerTest : ControllerTestBase<UtilityController>() {

  @MockitoBean
  private lateinit var outboundEventsService: OutboundEventsService

  private val identifierCaptor = argumentCaptor<Long>()

  override fun controller() = UtilityController(outboundEventsService)

  @Test
  fun `201 response when outbound event is published`() {
    val response = mockMvc.publishEvents(OutboundEvent.PRISONER_ALLOCATION_AMENDED, listOf(1, 1, 2))
      .andExpect { status { isCreated() } }.andReturn().response

    verify(outboundEventsService, times(2)).send(eq(OutboundEvent.PRISONER_ALLOCATION_AMENDED), identifierCaptor.capture(), eq(null))

    response.contentAsString isEqualTo "Domain event PRISONER_ALLOCATION_AMENDED published"
    identifierCaptor.firstValue isEqualTo 1
    identifierCaptor.secondValue isEqualTo 2
  }

  private fun MockMvc.publishEvents(event: OutboundEvent, identifiers: List<Long>) =
    post("/utility/publish-events") {
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(
        PublishEventUtilityModel(
          outboundEvent = event,
          identifiers = identifiers,
        ),
      )
    }
}
