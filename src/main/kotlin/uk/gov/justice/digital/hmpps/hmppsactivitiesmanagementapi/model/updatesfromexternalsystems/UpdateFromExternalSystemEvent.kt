package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.updatesfromexternalsystems

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AttendanceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonerDeallocationRequest

data class UpdateFromExternalSystemEvent(
  val messageId: String,
  val eventType: String,
  val description: String? = null,
  val messageAttributes: Map<String, Any?> = emptyMap(),
  val who: String,
) {
  fun toMarkPrisonerAttendanceEvent(): MarkPrisonerAttendanceEvent {
    val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
    return mapper.convertValue(this.messageAttributes, MarkPrisonerAttendanceEvent::class.java)
  }

  fun toPrisonerDeallocationRequest(): PrisonerDeallocationRequest {
    val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build()).registerModule(JavaTimeModule())
    return mapper.convertValue(this.messageAttributes, PrisonerDeallocationRequest::class.java)
  }
}

data class MarkPrisonerAttendanceEvent(
  val attendanceUpdateRequests: List<AttendanceUpdateRequest>,
)
