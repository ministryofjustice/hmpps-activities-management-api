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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AttendancesService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transform
import javax.persistence.EntityNotFoundException

@ExtendWith(SpringExtension::class)
@WebMvcTest(controllers = [ScheduledInstanceController::class])
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = [ScheduledInstanceController::class])
@ActiveProfiles("test")
@WebAppConfiguration
class ScheduledInstanceControllerTest(
  @Autowired private val mapper: ObjectMapper
) {
  private lateinit var mockMvc: MockMvc

  @MockBean
  private lateinit var attendancesService: AttendancesService

  @BeforeEach
  fun before() {
    mockMvc = MockMvcBuilders
      .standaloneSetup(ScheduledInstanceController(attendancesService))
      .setControllerAdvice(ControllerAdvice(mapper))
      .build()
  }

  @Test
  fun `200 response when get attendances by schedule ID found`() {
    val attendances = activityEntity().schedules.first().instances.first().attendances.map { transform(it) }

    whenever(attendancesService.findAttendancesByScheduledInstance(1)).thenReturn(attendances)

    val response = mockMvc.getAttendancesByScheduledInstance("1")
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(attendances))

    verify(attendancesService).findAttendancesByScheduledInstance(1)
  }

  @Test
  fun `404 response when get attendances by scheduled instance ID not found`() {
    whenever(attendancesService.findAttendancesByScheduledInstance(2)).thenThrow(EntityNotFoundException("not found"))

    val response = mockMvc.getAttendancesByScheduledInstance("2")
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNotFound() } }
      .andReturn().response

    assertThat(response.contentAsString).contains("Not found")

    verify(attendancesService).findAttendancesByScheduledInstance(2)
  }

  private fun MockMvc.getAttendancesByScheduledInstance(instanceId: String) =
    get("/scheduled-instances/$instanceId/attendances")
}
