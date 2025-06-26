package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.updatesfromexternalsystems

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Slot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AddCaseNoteRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AttendanceUpdateRequest
import java.time.LocalDate

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

  fun toPrisonerDeallocationEvent(): PrisonerDeallocationEvent {
    val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build()).registerModule(JavaTimeModule())
    return mapper.convertValue(this.messageAttributes, PrisonerDeallocationEvent::class.java)
  }

  fun toPrisonerAllocationEvent(): PrisonerAllocationEvent {
    val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build()).registerModule(JavaTimeModule())
    return mapper.convertValue(this.messageAttributes, PrisonerAllocationEvent::class.java)
  }
}

data class MarkPrisonerAttendanceEvent(
  val attendanceUpdateRequests: List<AttendanceUpdateRequest>,
)

data class PrisonerDeallocationEvent(
  val scheduleId: Long,
  val prisonerNumbers: List<String>?,
  val reasonCode: String?,
  val endDate: LocalDate?,
  val caseNote: AddCaseNoteRequest? = null,
  val scheduleInstanceId: Long? = null,
)

data class PrisonerAllocationEvent(
  val scheduleId: Long,
  val prisonerNumber: String?,
  val payBandId: Long?,
  val startDate: LocalDate,
  val endDate: LocalDate?,
  val exclusions: List<Slot>?,
  val scheduleInstanceId: Long? = null,
)
