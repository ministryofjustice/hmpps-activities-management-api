package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.updatesfromexternalsystems

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AttendanceUpdateRequest

data class UpdateFromExternalSystemEvent(
  val messageId: String,
  val eventType: String,
  val description: String? = null,
  val messageAttributes: Map<String, Any?> = emptyMap(),
  val who: String,
){
  fun toMarkPrisonerAttendanceEvent(): MarkPrisonerAttendanceEvent {
    val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
    return mapper.convertValue(this.messageAttributes, MarkPrisonerAttendanceEvent::class.java)
  }
}

data class MarkPrisonerAttendanceEvent(
  val attendanceUpdateRequests: List<AttendanceUpdateRequest>,
)