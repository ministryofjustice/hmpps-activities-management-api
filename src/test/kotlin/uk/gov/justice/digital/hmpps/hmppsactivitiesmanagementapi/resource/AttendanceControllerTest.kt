package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.verify
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
import org.springframework.test.web.servlet.put
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ControllerAdvice
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AttendanceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AttendancesService

@ExtendWith(SpringExtension::class)
@WebMvcTest(controllers = [AttendanceController::class])
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = [AttendanceController::class])
@ActiveProfiles("test")
@WebAppConfiguration
class AttendanceControllerTest(
  @Autowired private val mapper: ObjectMapper
) {
  private lateinit var mockMvc: MockMvc

  @MockBean
  private lateinit var attendancesService: AttendancesService

  @BeforeEach
  fun before() {
    mockMvc = MockMvcBuilders
      .standaloneSetup(AttendanceController(attendancesService))
      .setControllerAdvice(ControllerAdvice())
      .build()
  }

  @Test
  fun `200 response when mark attendance records`() {
    mockMvc.put("/attendances") {
      accept = MediaType.APPLICATION_JSON
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(
        listOf(
          AttendanceUpdateRequest(1, "ATT"),
          AttendanceUpdateRequest(2, "ABS")
        )
      )
    }
      .andExpect { status { isOk() } }

    verify(attendancesService).mark(
      listOf(
        AttendanceUpdateRequest(1, "ATT"),
        AttendanceUpdateRequest(2, "ABS")
      )
    )
  }
}
