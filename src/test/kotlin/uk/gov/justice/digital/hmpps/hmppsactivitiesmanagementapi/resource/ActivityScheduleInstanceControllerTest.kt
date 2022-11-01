package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ScheduledInstanceFixture
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ScheduledInstanceService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.transformActivityScheduleInstances
import java.time.LocalDate

@ExtendWith(SpringExtension::class)
@WebMvcTest(controllers = [ActivityScheduleInstanceController::class])
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = [ActivityScheduleInstanceController::class])
@ActiveProfiles("test")
@WebAppConfiguration
class ActivityScheduleInstanceControllerTest(
  @Autowired private val mapper: ObjectMapper
) {
  private lateinit var mockMvc: MockMvc

  @MockBean
  private lateinit var scheduledInstanceService: ScheduledInstanceService

  @BeforeEach
  fun before() {
    mockMvc = MockMvcBuilders
      .standaloneSetup(ActivityScheduleInstanceController(scheduledInstanceService))
      .setControllerAdvice(ControllerAdvice())
      .build()
  }

  @Test
  fun `200 response when get prisoner scheduled instances found`() {
    val results = transformActivityScheduleInstances(listOf(ScheduledInstanceFixture.instance(id = 1, locationId = 22)))

    whenever(
      scheduledInstanceService.getActivityScheduleInstancesByPrisonerNumberAndDateRange(
        "MDI", "A11111A",
        LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))
      )
    ).thenReturn(results)

    val response = mockMvc.getPrisonerScheduledInstances(
      "MDI", "A11111A",
      LocalDate.of(2022, 10, 1),
      LocalDate.of(2022, 11, 5)
    )
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(results))

    verify(scheduledInstanceService).getActivityScheduleInstancesByPrisonerNumberAndDateRange(
      "MDI", "A11111A",
      LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))
    )
  }

  private fun MockMvc.getPrisonerScheduledInstances(
    prisonCode: String,
    prisonerNumber: String,
    startDate: LocalDate,
    endDate: LocalDate
  ) =
    get("/prisons/$prisonCode/prisoners/$prisonerNumber/scheduled-instances?startDate=$startDate&endDate=$endDate")
}
