package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AllocationsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelPrisonerAllocations

@WebMvcTest(controllers = [PrisonerAllocationController::class])
@ContextConfiguration(classes = [PrisonerAllocationController::class])
class PrisonerAllocationControllerTest : ControllerTestBase<PrisonerAllocationController>() {

  @MockitoBean
  private lateinit var service: AllocationsService

  override fun controller() = PrisonerAllocationController(service)

  @Test
  fun `200 response when post prison numbers`() {
    val allocations = activityEntity().schedules().flatMap { it.allocations() }.toModelPrisonerAllocations()
    val prisonNumbers = allocations.map { it.prisonerNumber }.toSet()

    whenever(service.findByPrisonCodeAndPrisonerNumbers("MDI", prisonNumbers)).thenReturn(allocations)

    val response = mockMvc.postPrisonerNumbers("MDI", prisonNumbers)
      .andDo { print() }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(allocations))

    verify(service).findByPrisonCodeAndPrisonerNumbers("MDI", prisonNumbers)
  }

  private fun MockMvc.postPrisonerNumbers(prisonCode: String, prisonerNumbers: Collection<String>) =
    post("/prisons/$prisonCode/prisoner-allocations") {
      content = mapper.writeValueAsString(prisonerNumbers)
      contentType = MediaType.APPLICATION_JSON
    }
}
