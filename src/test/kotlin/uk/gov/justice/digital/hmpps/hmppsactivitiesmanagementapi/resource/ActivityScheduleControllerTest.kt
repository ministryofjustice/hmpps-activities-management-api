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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ActivityScheduleService
import java.time.LocalDate

@ExtendWith(SpringExtension::class)
@WebMvcTest(controllers = [ActivityScheduleController::class])
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = [ActivityScheduleController::class])
@ActiveProfiles("test")
@WebAppConfiguration
class ActivityScheduleControllerTest(@Autowired private val mapper: ObjectMapper) {

  private lateinit var mockMvc: MockMvc

  @MockBean
  private lateinit var activityScheduleService: ActivityScheduleService

  @BeforeEach
  fun before() {
    mockMvc = MockMvcBuilders
      .standaloneSetup(ActivityScheduleController(activityScheduleService))
      .setControllerAdvice(ControllerAdvice(mapper))
      .build()
  }

  @Test
  fun `200 response when get schedule by prison code and search criteria found`() {
    val schedules = activityModel(activityEntity()).schedules

    whenever(activityScheduleService.getActivitySchedulesByPrisonCode("PVI", LocalDate.MIN, TimeSlot.AM, 1)).thenReturn(
      schedules
    )

    val response = mockMvc.getSchedulesBy("PVI", LocalDate.MIN, TimeSlot.AM, 1)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(schedules))

    verify(activityScheduleService).getActivitySchedulesByPrisonCode("PVI", LocalDate.MIN, TimeSlot.AM, 1)
  }

  @Test
  fun `200 response when get schedule by prison code and search criteria not found`() {
    whenever(activityScheduleService.getActivitySchedulesByPrisonCode("PVI", LocalDate.MIN, TimeSlot.AM, 1)).thenReturn(
      emptyList()
    )

    val response = mockMvc.getSchedulesBy("PVI", LocalDate.MIN, TimeSlot.AM, 1)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo("[]")

    verify(activityScheduleService).getActivitySchedulesByPrisonCode("PVI", LocalDate.MIN, TimeSlot.AM, 1)
  }

  private fun MockMvc.getSchedulesBy(prisonCode: String, date: LocalDate, timeSlot: TimeSlot, locationId: Long) =
    get("/schedules/$prisonCode?date=$date&timeSlot=$timeSlot&locationId=$locationId")
}
